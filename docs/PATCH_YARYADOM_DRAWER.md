# Интеграция «Я рядом» в Telegram X — инструкция

## Быстрый способ (рекомендуется)

Из корня репозитория `telegram-x`:

```bash
chmod +x docs/RESTORE_AND_PATCH.sh
./docs/RESTORE_AND_PATCH.sh
```

Скрипт:
1. Восстановит `DrawerController.java` из официального Telegram X
2. Добавит import `YaRyadomController`
3. Добавит пункт «Я рядом» в боковое меню
4. Добавит обработку нажатия

Затем:

```bash
./gradlew assembleLatestUniversalDebug
```

---

## Что уже есть в репозитории

| Файл | Назначение |
|------|------------|
| `app/src/main/java/.../ui/YaRyadomController.java` | Экран «Я рядом» |
| `app/src/main/res/values/ids_yaryadom.xml` | ID: `btn_yaryadom`, `controller_yaryadom` |
| `docs/RESTORE_AND_PATCH.sh` | Скрипт восстановления + патча |

---

## Ручной способ (если скрипт не сработал)

### 1. Восстановить DrawerController

```bash
git fetch https://github.com/TGX-Android/Telegram-X.git main
git checkout FETCH_HEAD -- app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java
```

### 2. Import

```java
import org.thunderdog.challegram.ui.YaRyadomController;
```

### 3. Пункт меню (после SavedMessages)

```java
items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_yaryadom, R.drawable.baseline_home_24, "Я рядом", false));
```

### 4. Клик в onClick

```java
} else if (viewId == R.id.btn_yaryadom) {
  close(0f, () -> {
    UI.navigateTo(new YaRyadomController(context, context.currentTdlib()));
  });
```

---

## После успешной сборки

В боковом меню появится пункт **«Я рядом»**.  
При нажатии откроется экран сервиса.

Дальше можно наполнять `YaRyadomController` функционалом (поиск, объявления, заявки).
