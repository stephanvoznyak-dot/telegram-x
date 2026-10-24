# Сборка APK — Telegram X + «Я рядом»

## Важно перед сборкой

В репозитории `DrawerController.java` сейчас **заглушка**. Перед сборкой нужно восстановить полный файл и добавить пункт меню:

```bash
chmod +x docs/RESTORE_AND_PATCH.sh
./docs/RESTORE_AND_PATCH.sh
```

Скрипт:
1. Берёт `DrawerController.java` из официального Telegram X
2. Добавляет import `YaRyadomController`
3. Добавляет пункт «Я рядом» в боковое меню
4. Добавляет обработчик нажатия

## Требования

| Компонент | Минимум |
|-----------|---------|
| JDK | 17+ (лучше 21) |
| Android SDK | API 21+ |
| NDK | по README Telegram X |
| RAM | ≥ 8 GB |
| Диск | ≥ 20 GB свободно |
| Git | с submodule + LFS |

## Шаги

```bash
# 1. Клон
git clone --recursive https://github.com/stephanvoznyak-dot/telegram-x.git tgx
cd tgx

# 2. Патч меню «Я рядом»
chmod +x docs/RESTORE_AND_PATCH.sh
./docs/RESTORE_AND_PATCH.sh

# 3. local.properties
cat > local.properties << EOF
sdk.dir=/path/to/Android/Sdk
telegram.api_id=ВАШ_API_ID
telegram.api_hash=ВАШ_API_HASH
EOF

# API credentials: https://core.telegram.org/api/obtaining_api_id

# 4. Сборка debug APK
./gradlew assembleLatestUniversalDebug

# APK появится примерно здесь:
# app/build/outputs/apk/
```

## Модуль «Я рядом» (уже в репозитории)

- `YaRyadomController.java` — главный экран
- `YaRyadomCreateController.java` — форма
- `YaRyadomGeo.java` — TDLib nearby / create / favorites
- `YaRyadomFormat.java` — YR1
- `YaRyadomFavorites*.java` — избранное
- `ids_yaryadom.xml` — ID

## Сборка без компьютера

1. Откройте репозиторий на github.com с телефона
2. Code → Codespaces → Create codespace (если доступно)
3. В терминале Codespace выполните шаги выше

Либо попросите кого-то с ПК/Mac выполнить скрипт и прислать APK.

## Почему нельзя собрать «здесь» (в Grok)

- Нет Android SDK / NDK
- ~1.2 GB RAM (нужно ≥ 8 GB)
- Нет `telegram.api_id` / `api_hash` (личные ключи)
