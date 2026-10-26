#!/bin/bash
# Восстановление полного DrawerController из upstream Telegram X
# + пункт меню «Я рядом»
# Запускать из корня репозитория telegram-x

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

FILE="app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java"

echo "=== 1. Восстановление DrawerController из TGX-Android/Telegram-X ==="
git fetch https://github.com/TGX-Android/Telegram-X.git main --depth=1
git checkout FETCH_HEAD -- "$FILE"
echo "  восстановлен: $(wc -c < "$FILE") bytes"

echo "=== 2. Import YaRyadomController ==="
if ! grep -q "YaRyadomController" "$FILE"; then
  if grep -q "import org.thunderdog.challegram.ui.SettingsController;" "$FILE"; then
    sed -i 's/import org.thunderdog.challegram.ui.SettingsController;/import org.thunderdog.challegram.ui.SettingsController;\nimport org.thunderdog.challegram.ui.YaRyadomController;/' "$FILE"
    echo "  import добавлен"
  else
    echo "  WARN: не найден SettingsController import — добавьте вручную"
  fi
else
  echo "  import уже есть"
fi

echo "=== 3. Пункт меню «Я рядом» ==="
if ! grep -q "btn_yaryadom" "$FILE"; then
  sed -i '/btn_savedMessages.*SavedMessages/a\    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_yaryadom, R.drawable.baseline_home_24, "Я рядом", false));' "$FILE"
  echo "  пункт меню добавлен"
else
  echo "  пункт меню уже есть"
fi

echo "=== 4. Обработчик клика ==="
if ! grep -q "viewId == R.id.btn_yaryadom" "$FILE"; then
  python3 - << 'PY'
from pathlib import Path
path = Path("app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java")
text = path.read_text(encoding="utf-8")
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
if old not in text:
    print("  WARN: якорь обработчика не найден — см. docs/PATCH_YARYADOM_DRAWER.md")
else:
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("  обработчик добавлен")
PY
else
  echo "  обработчик уже есть"
fi

echo ""
echo "=== Готово ==="
grep -n "btn_yaryadom\|YaRyadomController" "$FILE" | head -10 || true
echo ""
echo "Дальше: ./gradlew assembleLatestUniversalDebug"
