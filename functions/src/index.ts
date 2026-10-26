import { createHash } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";

initializeApp();
setGlobalOptions({
  region: "europe-west1",
  maxInstances: 10,
});

const db = getFirestore();
const FIXED_MESSAGE = "Иди нахуй";
const E164_PHONE = /^\+[1-9]\d{7,14}$/;
const UID_PATTERN = /^[^/]{1,128}$/;
const HASH_PATTERN = /^[a-f0-9]{64}$/;

function requirePhoneUid(request: {
  auth: { uid: string; token: { phone_number?: unknown } } | null;
}): string {
  const auth = request.auth;
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Сначала войдите в аккаунт.");
  }
  if (typeof auth.token.phone_number !== "string") {
    throw new HttpsError(
      "failed-precondition",
      "Для этой функции нужен аккаунт с подтверждённым номером телефона."
    );
  }
  return auth.uid;
}

function stablePairId(firstUid: string, secondUid: string): string {
  return createHash("sha256")
    .update([firstUid, secondUid].sort().join("\u0000"))
    .digest("hex");
}

function rateLimitRef(action: string, uid: string) {
  const userKey = createHash("sha256").update(uid).digest("hex").slice(0, 40);
  return db.collection("_rateLimits").doc(`${action}_${userKey}`);
}

async function consumeRateLimit(
  uid: string,
  action: string,
  limit: number,
  windowMs: number,
  message: string
): Promise<void> {
  const ref = rateLimitRef(action, uid);
  const now = Date.now();

  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(ref);
    const data = snapshot.data();
    const previousStart =
      typeof data?.windowStartMs === "number" ? data.windowStartMs : 0;
    const previousCount = typeof data?.count === "number" ? data.count : 0;
    const inWindow =
      now >= previousStart && now - previousStart < windowMs;
    const nextCount = inWindow ? previousCount + 1 : 1;

    if (nextCount > limit) {
      throw new HttpsError("resource-exhausted", message);
    }

    transaction.set(ref, {
      windowStartMs: inWindow ? previousStart : now,
      count: nextCount,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
}

/**
 * Call after phone sign-in, and whenever the user changes their display name.
 * The phone number is deliberately not copied into Firestore.
 */
export const updateMyProfile = onCall(async (request) => {
  const uid = requirePhoneUid(request);
  const rawName = request.data?.displayName;
  if (typeof rawName !== "string") {
    throw new HttpsError("invalid-argument", "Укажите имя профиля.");
  }

  const displayName = rawName.trim().replace(/\s+/g, " ");
  if (displayName.length < 1 || displayName.length > 32) {
    throw new HttpsError(
      "invalid-argument",
      "Имя должно содержать от 1 до 32 символов."
    );
  }

  await db.collection("profiles").doc(uid).set(
    {
      displayName,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  return { uid, displayName };
});

/**
 * Resolves one E.164 phone number on the server. The caller never receives
 * another user's phone number or a list of registered accounts.
 */
export const findUserByPhone = onCall(async (request) => {
  const uid = requirePhoneUid(request);
  const phoneNumber = request.data?.phoneNumber;

  if (typeof phoneNumber !== "string" || !E164_PHONE.test(phoneNumber)) {
    throw new HttpsError(
      "invalid-argument",
      "Номер должен быть в международном формате, например +358401234567."
    );
  }

  await consumeRateLimit(
    uid,
    "phone_lookup",
    20,
    60 * 60 * 1000,
    "Слишком много поисков за час. Попробуйте позже."
  );

  try {
    const target = await getAuth().getUserByPhoneNumber(phoneNumber);
    if (target.uid === uid) {
      return { found: false };
    }

    const profileSnapshot = await db.collection("profiles").doc(target.uid).get();
    const profileName = profileSnapshot.data()?.displayName;
    const displayName =
      typeof profileName === "string" && profileName.trim()
        ? profileName
        : target.displayName?.trim() || "Пользователь";

    return {
      found: true,
      user: {
        uid: target.uid,
        displayName,
      },
    };
  } catch (error: unknown) {
    const code =
      typeof error === "object" && error !== null && "code" in error
        ? String((error as { code?: unknown }).code)
        : "";

    if (code === "auth/user-not-found") {
      return { found: false };
    }

    throw new HttpsError("internal", "Не удалось выполнить поиск. Попробуйте позже.");
  }
});

/** Creates a pending request; duplicate pending requests for a pair are rejected. */
export const sendFriendRequest = onCall(async (request) => {
  const uid = requirePhoneUid(request);
  const recipientUid = request.data?.recipientUid;

  if (
    typeof recipientUid !== "string" ||
    !UID_PATTERN.test(recipientUid) ||
    recipientUid === uid
  ) {
    throw new HttpsError("invalid-argument", "Некорректный получатель.");
  }

  let recipient;
  try {
    recipient = await getAuth().getUser(recipientUid);
  } catch (error: unknown) {
    const code =
      typeof error === "object" && error !== null && "code" in error
        ? String((error as { code?: unknown }).code)
        : "";
    if (code === "auth/user-not-found") {
      throw new HttpsError("not-found", "Пользователь не найден.");
    }
    throw new HttpsError("internal", "Не удалось проверить получателя.");
  }
  if (!recipient.phoneNumber) {
    throw new HttpsError(
      "failed-precondition",
      "У пользователя нет подтверждённого номера телефона."
    );
  }

  await consumeRateLimit(
    uid,
    "friend_request",
    15,
    24 * 60 * 60 * 1000,
    "Слишком много заявок за сутки. Попробуйте завтра."
  );

  const pairId = stablePairId(uid, recipientUid);
  const requestRef = db.collection("friendRequests").doc(pairId);
  const friendshipRef = db.collection("friendships").doc(pairId);

  await db.runTransaction(async (transaction) => {
    const friendship = await transaction.get(friendshipRef);
    const previousRequest = await transaction.get(requestRef);
    if (friendship.exists || previousRequest.data()?.status === "accepted") {
      throw new HttpsError("already-exists", "Вы уже добавили друг друга.");
    }
    if (previousRequest.data()?.status === "pending") {
      throw new HttpsError("already-exists", "Заявка уже отправлена.");
    }

    transaction.set(requestRef, {
      senderUid: uid,
      recipientUid,
      status: "pending",
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return { requestId: pairId, status: "pending" };
});

/** Only the recipient may accept or reject a pending request. */
export const respondToFriendRequest = onCall(async (request) => {
  const uid = requirePhoneUid(request);
  const requestId = request.data?.requestId;
  const decision = request.data?.decision;

  if (typeof requestId !== "string" || !HASH_PATTERN.test(requestId)) {
    throw new HttpsError("invalid-argument", "Некорректная заявка.");
  }
  if (decision !== "accepted" && decision !== "rejected") {
    throw new HttpsError("invalid-argument", "Решение должно быть accepted или rejected.");
  }

  const requestRef = db.collection("friendRequests").doc(requestId);
  await db.runTransaction(async (transaction) => {
    const requestSnapshot = await transaction.get(requestRef);
    if (!requestSnapshot.exists) {
      throw new HttpsError("not-found", "Заявка не найдена.");
    }

    const requestData = requestSnapshot.data()!;
    const senderUid = requestData.senderUid;
    const recipientUid = requestData.recipientUid;

    if (
      typeof senderUid !== "string" ||
      typeof recipientUid !== "string" ||
      recipientUid !== uid ||
      stablePairId(senderUid, recipientUid) !== requestId
    ) {
      throw new HttpsError("permission-denied", "Эту заявку нельзя изменить.");
    }
    if (requestData.status !== "pending") {
      throw new HttpsError("failed-precondition", "Заявка уже обработана.");
    }

    const updatedAt = FieldValue.serverTimestamp();
    if (decision === "accepted") {
      const members = [senderUid, recipientUid].sort();
      const friendshipRef = db.collection("friendships").doc(requestId);
      const conversationRef = db.collection("conversations").doc(requestId);
      const friendship = await transaction.get(friendshipRef);
      const conversation = await transaction.get(conversationRef);

      if (!friendship.exists) {
        transaction.create(friendshipRef, {
          members,
          createdAt: updatedAt,
        });
      }
      if (!conversation.exists) {
        transaction.create(conversationRef, {
          members,
          createdAt: updatedAt,
        });
      }
    }

    transaction.update(requestRef, {
      status: decision,
      respondedAt: updatedAt,
      updatedAt,
    });
  });

  return { requestId, status: decision };
});

/**
 * This endpoint intentionally accepts a conversation ID, not message text.
 * Message content is fixed on the server and throttled per account.
 */
export const sendPohuismMessage = onCall(async (request) => {
  const uid = requirePhoneUid(request);
  const conversationId = request.data?.conversationId;

  if (typeof conversationId !== "string" || !HASH_PATTERN.test(conversationId)) {
    throw new HttpsError("invalid-argument", "Некорректный чат.");
  }

  const conversationRef = db.collection("conversations").doc(conversationId);
  const messageRef = conversationRef.collection("messages").doc();
  const limitRef = rateLimitRef("message_cooldown", uid);
  const now = Date.now();

  await db.runTransaction(async (transaction) => {
    const conversation = await transaction.get(conversationRef);
    const cooldown = await transaction.get(limitRef);

    if (!conversation.exists) {
      throw new HttpsError("not-found", "Чат не найден.");
    }
    const members = conversation.data()?.members;
    if (!Array.isArray(members) || !members.includes(uid) || members.length !== 2) {
      throw new HttpsError("permission-denied", "Вы не участник этого чата.");
    }

    const lastSentAtMs = cooldown.data()?.lastSentAtMs;
    if (typeof lastSentAtMs === "number" && now - lastSentAtMs < 1500) {
      throw new HttpsError(
        "resource-exhausted",
        "Не так быстро. Дайте кнопке передохнуть."
      );
    }

    transaction.set(limitRef, {
      lastSentAtMs: now,
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(messageRef, {
      text: FIXED_MESSAGE,
      senderUid: uid,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return {
    messageId: messageRef.id,
    text: FIXED_MESSAGE,
  };
});
