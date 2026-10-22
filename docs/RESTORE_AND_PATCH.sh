#!/bin/bash
# Восстановление DrawerController + добавление пункта «Я рядом»
# Запускать из корня репозитория telegram-x

set -e

echo "=== 1. Восстановление DrawerController из официального Telegram X ==="
git fetch https://github.com/TGX-Android/Telegram-X.git main
git checkout FETCH_HEAD -- app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java

FILE="app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java"

echo "=== 2. Добавление import ==="
if ! grep -q "YaRyadomController" "$FILE"; then
  sed -i 's/import org.thunderdog.challegram.ui.SettingsController;/import org.thunderdog.challegram.ui.SettingsController;\nimport org.thunderdog.challegram.ui.YaRyadomController;/' "$FILE"
  echo "  import добавлен"
else
  echo "  import уже есть"
fi

echo "=== 3. Добавление пункта меню ==="
if ! grep -q "btn_yaryadom" "$FILE"; then
  sed -i '/btn_savedMessages.*SavedMessages/a\    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_yaryadom, R.drawable.baseline_home_24, "Я рядом", false));' "$FILE"
  echo "  пункт меню добавлен"
else
  echo "  пункт меню уже есть"
fi

echo "=== 4. Добавление обработчика клика ==="
if ! grep -q "btn_yaryadom)" "$FILE"; then
  # Insert after openSavedMessages block
  python3 - << 'PY'
from pathlib import Path
path = Path("app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java")
text = path.read_text()
old = """    } else if (viewId == R.id.btn_savedMessages) {
      openSavedMessages();
    } else if (viewId == R.id.btn_addAccount) {"""
new = """    } else if (viewId == R.id.btn_savedMessages) {
      openSavedMessages();
    } else if (viewId == R.id.btn_yaryadom) {
      close(0f, () -> {
        UI.navigateTo(new YaRyadomController(context, context.currentTdlib()));
      });
    } else if (viewId == R.id.btn_addAccount) {"""
if old in text:
    path.write_text(text.replace(old, new, 1))
    print("  обработчик клика добавлен")
else:
    print("  не удалось найти место для обработчика — добавьте вручную")
PY
else
  echo "  обработчик уже есть"
fi

echo ""
echo "=== Готово ==="
echo "Проверьте изменения: git diff $FILE"
echo "Затем: ./gradlew assembleLatestUniversalDebug"
