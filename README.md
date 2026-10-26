# PohuismAndroid

Android-приложение на Kotlin и XML (без Jetpack Compose).

## Возможности
- Полноэкранный режим: системные статус- и навигационная панели скрыты, их можно временно вызвать системным жестом.
- Список четырёх контактов и отдельный чат для каждого.
- Встроенная экранная клавиатура RU/EN с режимом символов, пробелом, удалением, переводом строки и кнопкой эмодзи; системная IME не открывается.
- Сообщения пользователя справа, демонстрационный ответ слева через 1 секунду.
- GitHub Actions автоматически собирает тестовый APK при каждом push в `main`.

## Как получить APK
1. Открой вкладку [Actions](https://github.com/HelterFall/PohuismAndroid/actions).
2. Выбери успешный запуск **Build Android APK**.
3. Скачай артефакт **Pohuism-debug-apk** внизу страницы.
4. Распакуй ZIP и установи `app-debug.apk` на Android.

## Как добавить настоящие аватарки в исходники
Это изменение исходного кода, а не настройка внутри приложения.

1. Подготовь четыре квадратных изображения (лучше WebP или JPG, например 256×256 px). Используй изображения, на которые у тебя есть право.
2. В GitHub открой `app/src/main/res/drawable/` и загрузи туда файлы с такими именами в нижнем регистре:
   - `avatar_durov.webp`
   - `avatar_a4.webp`
   - `avatar_mrbeast.webp`
   - `avatar_shaman.webp`
3. Открой `app/src/main/java/com/example/pohuism/MainActivity.kt` и замени список `contacts` на этот:

```kotlin
val contacts = listOf(
    Contact(
        "Павел Дуров", "ПД", Color.rgb(45, 105, 200),
        avatarResId = R.drawable.avatar_durov
    ),
    Contact(
        "Влад А4", "А4", Color.rgb(130, 75, 220),
        avatarResId = R.drawable.avatar_a4
    ),
    Contact(
        "Мистер Бист (MrBeast)", "МБ", Color.rgb(235, 145, 45),
        avatarResId = R.drawable.avatar_mrbeast
    ),
    Contact(
        "Шаман (SHAMAN)", "Ш", Color.rgb(35, 155, 115),
        avatarResId = R.drawable.avatar_shaman
    )
)
```

4. Сохрани изменения в `main`. Следующая сборка запустится автоматически. Если изображения пока не подключены в коде, контакты показывают цветные инициалы.
5. Имена Android-ресурсов должны быть в нижнем регистре, без пробелов и дефисов. Если используешь JPG, оставь имя ресурса тем же, но укажи файл с расширением `.jpg` — в Kotlin по-прежнему пиши `R.drawable.avatar_durov`.

### Как изменить клавиатуру
Код встроенной клавиатуры находится в `app/src/main/java/com/example/pohuism/ChatActivity.kt`, методе `renderCustomKeyboard()`. Там можно изменить раскладку, подписи, цвета и обработчики клавиш. Она вводит текст только в этом приложении и не является системной клавиатурой Android.

## Технологии
Kotlin, стандартные Android Activity, XML layouts, RecyclerView, AndroidX Core.
