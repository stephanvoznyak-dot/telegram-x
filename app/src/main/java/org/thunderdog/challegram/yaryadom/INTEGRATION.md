# Интеграция модуля «Я рядом» в Telegram X

Модуль уже размещён в:

```
app/src/main/java/org/thunderdog/challegram/yaryadom/
```

## Состав

- `YaRyadomController.kt` — главный контроллер
- `data/YaRyadomApi.kt` — HTTP-клиент к backend
- `data/models/Models.kt` — модели данных
- `ui/YaRyadomScreens.kt` — UI-экраны
- `util/LocationHelper.kt` — геолокация

## Разрешения

В `AndroidManifest.xml` уже есть:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

## Подключение в навигацию

Найдите место формирования списка разделов / нижней навигации (MainActivity, HomeController, Drawer и т.п.).

Добавьте пункт «Я рядом». При нажатии:

```kotlin
val me = tdlib.myUser() // текущий пользователь из TDLib

val controller = YaRyadomController(
    context = context,
    baseUrl = "https://your-ya-ryadom-domain.com",
    userId = me.id,
    firstName = me.firstName,
    username = me.username
)

container.removeAllViews()
container.addView(controller.getView())
```

## Backend

Модуль отправляет:

```json
{
  "nativeClient": true,
  "userId": 123456789,
  "firstName": "Иван",
  "username": "ivan"
}
```

Необходимо доработать backend (`apps/bot`), чтобы при `nativeClient: true` принимать `userId` + `firstName` вместо `initData`.

## API endpoints

- `POST /api/orders`
- `POST /api/orders/nearby`
- `POST /api/orders/take`
- `POST /api/orders/mine`
- `POST /api/orders/complete`
