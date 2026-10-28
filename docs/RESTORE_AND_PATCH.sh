#!/bin/bash
# Восстановление полного DrawerController + пункт «Я рядом»
# Запускать из корня репозитория telegram-x

set -euo pipefail

FILE="app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java"
PATCH="docs/yaryadom_drawer.patch"

echo "=== 1. Восстановление DrawerController из TGX-Android/Telegram-X ==="
git fetch https://github.com/TGX-Android/Telegram-X.git main
git checkout FETCH_HEAD -- "$FILE"

if ! grep -q "class DrawerController" "$FILE"; then
  echo "ERROR: не удалось восстановить $FILE"
  exit 1
fi
echo "  восстановлен ($(wc -l < "$FILE") строк)"

echo "=== 2. Применение патча «Я рядом» ==="
if grep -q "btn_yaryadom" "$FILE"; then
  echo "  патч уже применён — пропуск"
else
  if command -v patch >/dev/null 2>&1 && [ -f "$PATCH" ]; then
    # patch expects paths relative to -p1
    (cd app/src/main/java/org/thunderdog/challegram/navigation && \
      patch -p1 --forward < "$(pwd)/../../../../../../../../$PATCH" 2>/dev/null) || true
  fi

  if ! grep -q "btn_yaryadom" "$FILE"; then
    echo "  применяем встроенный fallback-патч..."
    # import
    if ! grep -q "YaRyadomController" "$FILE"; then
      sed -i 's/import org.thunderdog.challegram.ui.SettingsController;/import org.thunderdog.challegram.ui.SettingsController;\nimport org.thunderdog.challegram.ui.YaRyadomController;/' "$FILE"
    fi
    # menu item
    if ! grep -q "btn_yaryadom" "$FILE"; then
      sed -i '/btn_savedMessages.*SavedMessages/a\    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_yaryadom, R.drawable.baseline_home_24, "Я рядом", false));' "$FILE"
    fi
    # click handler via python for reliability
    python3 - << 'PY'
from pathlib import Path
path = Path("app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java")
text = path.read_text(encoding="utf-8")
if "viewId == R.id.btn_yaryadom" in text:
    print("  handler already present")
else:
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
        raise SystemExit("  не найден якорь для обработчика клика — добавьте вручную (docs/PATCH_YARYADOM_DRAWER.md)")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("  handler добавлен")
PY
  fi
fi

echo ""
echo "=== Проверка ==="
grep -n "YaRyadomController\|btn_yaryadom" "$FILE" || echo "WARNING: маркеры не найдены"
echo ""
echo "=== Готово ==="
echo "Дальше:"
echo "  1. Настройте local.properties (sdk.dir, telegram.api_id, telegram.api_hash)"
echo "  2. ./gradlew assembleLatestUniversalDebug"
