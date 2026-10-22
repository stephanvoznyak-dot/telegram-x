# Интеграция «Я рядом» в Telegram X

## Текущий статус

- [x] Форк Telegram X: https://github.com/stephanvoznyak-dot/telegram-x
- [x] Создан контроллер-заглушка: `app/src/main/java/org/thunderdog/challegram/ui/YaRyadomController.java`
- [ ] Пункт «Я рядом» в боковом меню (Drawer)
- [ ] Привязка клика к открытию контроллера

## Как добавить пункт в боковое меню

Файл: `app/src/main/java/org/thunderdog/challegram/navigation/DrawerController.java`

### 1. Добавить пункт в список items

Найдите место, где создаются пункты меню (примерно после `btn_savedMessages` или перед `btn_settings`):

```java
items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_savedMessages, R.drawable.baseline_bookmark_24, R.string.SavedMessages));
```

Добавьте сразу после него:

```java
// «Я рядом» — сервис аренды жилья
items.add(new ListItem(ListItem.TYPE_DRAWER_ITEM, R.id.btn_help, R.drawable.baseline_home_24, "Я рядом", false));
```

> Пока используем существующий `R.id.btn_help` только как временный id (или добавьте новый id в `ids.xml`).  
> Иконку `baseline_home_24` замените на любую подходящую из `R.drawable`.

Лучший вариант — добавить новый id:

В `app/src/main/res/values/ids.xml` (или соответствующий файл):

```xml
<item name="btn_yaryadom" type="id"/>
<item name="controller_yaryadom" type="id"/>
```

И использовать `R.id.btn_yaryadom`.

### 2. Обработать клик в onClick

В методе `onClick(View v)` добавьте:

```java
} else if (viewId == R.id.btn_yaryadom) {   // или временный id
  close(0f, () -> {
    UI.navigateTo(new YaRyadomController(context, context.currentTdlib()));
  });
```

Не забудьте импорт:

```java
import org.thunderdog.challegram.ui.YaRyadomController;
```

### 3. Сборка

После изменений:

```bash
./gradlew assembleLatestUniversalDebug
```

## Следующие шаги

1. Добавить id-ресурсы
2. Внести изменения в DrawerController (пункт + onClick)
3. Собрать и проверить, что экран открывается
4. Наполнить YaRyadomController реальным функционалом (список объявлений, поиск и т.д.)

## Репозиторий

https://github.com/stephanvoznyak-dot/telegram-x
