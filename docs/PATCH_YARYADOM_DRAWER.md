# Патч: пункт «Я рядом» в боковом меню Telegram X

## Шаг 1. Добавить id-ресурсы

Файл: `app/src/main/res/values/ids.xml`

Перед закрывающим `</resources>` добавьте:

```xml
  <!-- Я рядом -->
  <item type="id" name="btn_yaryadom" />
  <item type="id" name="controller_yaryadom" />
```

## Шаг 2. Изменить DrawerController.java

Файл: `app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java`

### 2.1. Импорт

В начало файла (к другим import) добавьте:

```java
import org.thunderdog.challegram.ui.YaRyadomController;
```

### 2.2. Пункт меню

Найдите строки:

```java
    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_savedMessages, R.drawable.baseline_bookmark_24, R.string.SavedMessages));
    this.settingsClickBait = getSettingsClickBait();
    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_settings, R.drawable.baseline_settings_24, R.string.Settings));
```

Замените на:

```java
    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_savedMessages, R.drawable.baseline_bookmark_24, R.string.SavedMessages));
    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_yaryadom, R.drawable.baseline_home_24, "Я рядом", false));
    this.settingsClickBait = getSettingsClickBait();
    items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_settings, R.drawable.baseline_settings_24, R.string.Settings));
```

### 2.3. Обработка клика

В методе `onClick(View v)` найдите блок с `btn_savedMessages` и добавьте рядом:

```java
    } else if (viewId == R.id.btn_yaryadom) {
      close(0f, () -> {
        UI.navigateTo(new YaRyadomController(context, context.currentTdlib()));
      });
```

## Шаг 3. Обновить getId() в контроллере (опционально)

В `YaRyadomController.java` замените:

```java
return R.id.controller_settings;
```

на:

```java
return R.id.controller_yaryadom;
```

## Шаг 4. Сборка

```bash
./gradlew assembleLatestUniversalDebug
```

После установки APK в боковом меню должен появиться пункт **«Я рядом»**. При нажатии откроется экран сервиса.
