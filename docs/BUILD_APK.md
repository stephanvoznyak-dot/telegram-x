# Сборка APK — Telegram X + «Я рядом»

## Быстрый старт

```bash
git clone --recursive https://github.com/stephanvoznyak-dot/telegram-x.git tgx
cd tgx

chmod +x docs/RESTORE_AND_PATCH.sh
./docs/RESTORE_AND_PATCH.sh

# local.properties
cat > local.properties << EOF
sdk.dir=/path/to/Android/Sdk
telegram.api_id=ВАШ_API_ID
telegram.api_hash=ВАШ_API_HASH
EOF
# Ключи: https://core.telegram.org/api/obtaining_api_id

./gradlew assembleLatestUniversalDebug
```

APK: `app/build/outputs/apk/`

## Что делает RESTORE_AND_PATCH.sh

1. Восстанавливает полный `DrawerController.java` из upstream Telegram X
2. Добавляет пункт меню **«Я рядом»** и обработчик клика

Без этого шага сборка возможна, но пункта «Я рядом» в боковом меню не будет (в репозитории лежит заглушка Drawer).

## Требования

| Компонент | Минимум |
|-----------|---------|
| JDK | 17+ (рекомендуется 21) |
| Android SDK + NDK | по README Telegram X |
| RAM | ≥ 8 GB |
| Диск | ≥ 20 GB |
| Git | submodule + LFS |

## Модуль «Я рядом» (уже в репозитории)

| Файл | Роль |
|------|------|
| `YaRyadomController.java` | Главный экран |
| `YaRyadomCreateController.java` | Форма размещения |
| `YaRyadomGeo.java` | SearchChatsNearby, create, favorites |
| `YaRyadomFormat.java` | Формат YR1 |
| `YaRyadomFavorites*.java` | Избранное |
| `ids_yaryadom.xml` | ID |

## Без компьютера

GitHub → Code → Codespaces → Create codespace, затем команды выше в терминале Codespace.
