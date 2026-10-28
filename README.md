# Telegram X + «Я рядом»

> **Основной репозиторий** клиента: полный [Telegram X](https://github.com/TGX-Android/Telegram-X)  
> с встроенным сервисом поиска и размещения жилья **«Я рядом»**.

**Репозиторий:** https://github.com/stephanvoznyak-dot/telegram-x  
**Модуль отдельно:** https://github.com/stephanvoznyak-dot/telegram-x-ya-ryadom

## Что это

Полноценный Android-клиент Telegram (на базе TDLib) + нативный раздел **«Я рядом»**:

- поиск объявлений рядом (`SearchChatsNearby`)
- фильтр только объявлений YR
- размещение объявления (канал с геолокацией + формат YR1)
- избранное
- открытие чата с автором

## Модуль «Я рядом»

| Файл | Роль |
|------|------|
| `YaRyadomController.java` | Главный экран |
| `YaRyadomCreateController.java` | Форма размещения |
| `YaRyadomGeo.java` | TDLib nearby, create, favorites |
| `YaRyadomFormat.java` | Формат YR1 |
| `YaRyadomFavorites*.java` | Избранное |
| `ids_yaryadom.xml` | ID меню и контроллера |

Формат объявления:

```
#ЯРядом
YR1|title=...|price=...|rooms=...|area=...|address=...|lat=...|lng=...|desc=...
```

- Интеграция: [docs/YA_RYADOM_INTEGRATION.md](docs/YA_RYADOM_INTEGRATION.md)
- Сборка APK: [docs/BUILD_APK.md](docs/BUILD_APK.md)
- Патч меню: [docs/RESTORE_AND_PATCH.sh](docs/RESTORE_AND_PATCH.sh)

## Статус

**Рабочий MVP** (октябрь 2026). Код модуля готов. Перед сборкой обязателен шаг `RESTORE_AND_PATCH.sh` (восстанавливает Drawer + пункт «Я рядом»).

## Сборка

```bash
git clone --recursive https://github.com/stephanvoznyak-dot/telegram-x.git tgx
cd tgx

chmod +x docs/RESTORE_AND_PATCH.sh
./docs/RESTORE_AND_PATCH.sh

# local.properties: sdk.dir, telegram.api_id, telegram.api_hash
# Ключи: https://core.telegram.org/api/obtaining_api_id

./gradlew assembleLatestUniversalDebug
```

Требования: JDK 17+, Android SDK/NDK, ≥8 GB RAM, клон с `--recursive`.

## Лицензия

GNU GPL v3.0 (Telegram X и изменения «Я рядом»).
