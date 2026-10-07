# Интеграция модуля «Я рядом» в Telegram X

Актуальная версия модуля с поддержкой HMAC-авторизации и улучшенным UI.

## Состав

```
yaryadom/
├── YaRyadomController.kt
├── data/
│   ├── YaRyadomApi.kt
│   └── models/
│       └── Models.kt
├── ui/
│   └── YaRyadomScreens.kt
├── util/
│   └── LocationHelper.kt
└── INTEGRATION.md
```

## 1. Копирование файлов

Скопируйте содержимое модуля в исходники Telegram X:

```
app/src/main/java/org/thunderdog/challegram/yaryadom/
```

Структура пакетов должна совпадать с объявлениями `package` в файлах.

## 2. Разрешения (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

## 3. Backend (обязательно)

1. Добавьте в `.env` бота:

```
NATIVE_CLIENT_SECRET=сгенерируйте-длинный-случайный-секрет-не-менее-32-символов
```

2. Примените патч из `backend-native-auth-patch.ts`:
   - Импортируйте `resolveUser` и схемы.
   - Во всех `/api/*` хендлерах замените получение пользователя на:

```typescript
const user = resolveUser(parsed.data, process.env.TOKEN!, process.env.NATIVE_CLIENT_SECRET);
if (!user) {
  return reply.status(401).send({ error: 'UNAUTHORIZED' });
}
```

Без корректного `NATIVE_CLIENT_SECRET` native-запросы будут отклоняться.

## 4. Подключение в навигацию Telegram X

Найдите место формирования меню / нижней навигации / drawer (классы вида `MainActivity`, `HomeController`, `DrawerController`, `SettingsController` и т.п.).

Пример создания контроллера:

```kotlin
val me = tdlib.myUser() // или актуальный способ получения текущего пользователя

val controller = YaRyadomController(
    context = context,
    baseUrl = "https://your-ya-ryadom-domain.com",
    userId = me.id,
    firstName = me.firstName,
    username = me.username,
    nativeSecret = "тот-же-секрет-что-и-NATIVE_CLIENT_SECRET"
)

// Показать view
container.removeAllViews()
container.addView(controller.getView())
```

**Важно:** `nativeSecret` должен совпадать с `NATIVE_CLIENT_SECRET` на backend.  
Не храните секрет в открытом виде в репозитории — используйте BuildConfig / local.properties / encrypted storage.

## 5. Сборка APK

### Требования

- JDK 17+ (рекомендуется 21)
- Android SDK + NDK (см. README Telegram X)
- Клонированный репозиторий с `--recursive`

### Команды

```bash
# из корня Telegram X
./gradlew assembleLatestUniversalDebug     # debug APK
./gradlew assembleLatestUniversalRelease   # release APK
```

Готовые APK обычно появляются в:

```
app/build/outputs/apk/
```

### Проверка модуля

1. Запустите debug-сборку.
2. Откройте раздел «Я рядом».
3. Проверьте: создание заявки → поиск рядом → TAKE → COMPLETE.
4. Убедитесь, что на backend в логах появляются корректные native-запросы с подписью.

## 6. Дальнейшие улучшения UI

Текущий UI использует стандартные Android View с поддержкой светлой/тёмной темы и цветом акцента Telegram (`#2AABEE`).

Для полного визуального соответствия Telegram X:

- Замените `TextView` / `EditText` / кнопки на компоненты TGX (`CustomTextView` и аналоги).
- Используйте `Theme` / `ThemeId` Telegram X для цветов.
- Интегрируйте запрос разрешений геолокации через существующие механизмы TGX.

## 7. Безопасность

- HMAC-SHA256, окно времени ±5 минут.
- Payload: `userId:firstName:timestamp`.
- При отсутствии или неверной подписи backend возвращает 401.
- GPS-координаты чужих заявок никогда не отдаются в ответы API.

## Контакты API

- `POST /api/orders`
- `POST /api/orders/nearby`
- `POST /api/orders/take`
- `POST /api/orders/mine`
- `POST /api/orders/complete`
