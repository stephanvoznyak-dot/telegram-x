# «Я рядом» — интеграция в Telegram X

## Модель

| Сущность | Реализация |
|----------|------------|
| Объявление | Supergroup-канал с `ChatLocation` + описание **YR1** |
| Поиск | `TdApi.SearchChatsNearby` + кэш 90 с |
| Фильтр | Только каналы с маркером `YR ·` / `#ЯРядом` |
| Избранное | Одно сообщение `YR_FAV1` в Saved Messages (edit + debounce 1.2 с) |
| Размещение | `CreateNewSupergroupChat` + geo, fallback без geo |

## Формат YR1

```
#ЯРядом
YR1|title=...|price=...|rooms=...|area=...|address=...|lat=...|lng=...|desc=...
```

Название канала: `YR · {title}`

## Файлы

| Файл | Роль |
|------|------|
| `YaRyadomFormat.java` | Парсер/билдер YR1, избранное payload |
| `YaRyadomGeo.java` | TDLib nearby, create, favorites persist |
| `YaRyadomController.java` | UI поиска, GPS, фильтр, список |
| `YaRyadomCreateController.java` | Форма публикации |
| `ids_yaryadom.xml` | `btn_yaryadom`, `controller_yaryadom` |
| `docs/RESTORE_AND_PATCH.sh` | Пункт в боковом меню |

## Меню

```bash
chmod +x docs/RESTORE_AND_PATCH.sh
./docs/RESTORE_AND_PATCH.sh
./gradlew assembleLatestUniversalDebug
```

## Оптимизации (сделано)

- Кэш nearby 90 секунд
- Фильтр «только YR» / «все location-каналы»
- GPS last-known для координат
- Debounce записи избранного + `editMessageText`
- Fallback создания канала без geo
- Человекочитаемые ошибки (flood, location)
- Prefill координат в форму из поиска
- Блокировка кнопки на время поиска/публикации

## Ограничения Telegram

- Location-группы зависят от политики TDLib/аккаунта
- Nearby видит только чаты с геометкой в радиусе сервера
- Канал на каждое объявление — trade-off MVP

## Дальше

- Каталог-канал + сообщения вместо N каналов
- Выбор точки на карте TGX
- RecyclerView / ListItem в стиле TGX
- GetSupergroupFullInfo для парсинга description
