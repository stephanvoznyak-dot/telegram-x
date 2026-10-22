/*
 * Telegram X + «Я рядом»
 *
 * Объявление = канал (supergroup) с ChatLocation.
 * Поиск = TdApi.SearchChatsNearby.
 * Создание = TdApi.CreateNewSupergroupChat + ChatLocation.
 */
package org.thunderdog.challegram.ui;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.core.StringUtils;
import me.vkryl.core.lambda.RunnableData;

public final class YaRyadomGeo {

  public static final class Listing {
    public final long chatId;
    public final String title;
    public final String address;
    public final double latitude;
    public final double longitude;
    public final int distanceMeters; // -1 если неизвестно

    public Listing (long chatId, String title, String address,
                    double latitude, double longitude, int distanceMeters) {
      this.chatId = chatId;
      this.title = title != null ? title : "";
      this.address = address != null ? address : "";
      this.latitude = latitude;
      this.longitude = longitude;
      this.distanceMeters = distanceMeters;
    }

    public String distanceLabel () {
      if (distanceMeters < 0) return "";
      if (distanceMeters < 1000) return distanceMeters + " м";
      return String.format("%.1f км", distanceMeters / 1000.0);
    }
  }

  private YaRyadomGeo () {}

  /**
   * Поиск каналов/групп с геометкой рядом с точкой.
   */
  public static void searchNearby (Tdlib tdlib, double lat, double lng,
                                   RunnableData<List<Listing>> onResult,
                                   RunnableData<String> onError) {
    if (tdlib == null) {
      if (onError != null) onError.runWithData("Telegram не готов");
      return;
    }

    tdlib.client().send(
      new TdApi.SearchChatsNearby(new TdApi.Location(lat, lng, 0.0)),
      object -> tdlib.ui().post(() -> {
        if (object instanceof TdApi.Error) {
          TdApi.Error err = (TdApi.Error) object;
          if (onError != null) onError.runWithData(err.message);
          return;
        }
        if (!(object instanceof TdApi.ChatsNearby)) {
          if (onError != null) onError.runWithData("Неожиданный ответ TDLib");
          return;
        }

        TdApi.ChatsNearby nearby = (TdApi.ChatsNearby) object;
        List<Listing> list = new ArrayList<>();

        // locationsNearby — supergroups с геолокацией
        if (nearby.usersNearby != null) {
          // usersNearby — люди рядом, не объявления; пропускаем
        }
        // В TDLib ChatsNearby содержит:
        // - usersNearby: ChatNearby[]
        // - locationsNearby: ChatNearby[]  (location-based groups)

        appendNearby(tdlib, list, getLocationsNearby(nearby));

        if (onResult != null) onResult.runWithData(list);
      })
    );
  }

  /**
   * Совместимость с разными версиями TDLib: locationsNearby может называться иначе.
   * Берём все ChatNearby из ответа, у которых у чата есть location.
   */
  private static TdApi.ChatNearby[] getLocationsNearby (TdApi.ChatsNearby nearby) {
    try {
      // Стандартное поле TDLib
      java.lang.reflect.Field f = nearby.getClass().getField("locationsNearby");
      Object val = f.get(nearby);
      if (val instanceof TdApi.ChatNearby[]) {
        return (TdApi.ChatNearby[]) val;
      }
    } catch (Throwable ignored) {
    }
    // Fallback: usersNearby иногда содержит смешанные данные — не используем
    return new TdApi.ChatNearby[0];
  }

  private static void appendNearby (Tdlib tdlib, List<Listing> out, TdApi.ChatNearby[] arr) {
    if (arr == null) return;
    for (TdApi.ChatNearby n : arr) {
      if (n == null || n.chatId == 0) continue;
      String title = tdlib.chatTitle(n.chatId);
      if (StringUtils.isEmpty(title)) title = "Канал #";

      double lat = 0, lng = 0;
      String address = "";
      TdApi.ChatLocation loc = tdlib.chatLocation(n.chatId);
      if (loc != null && loc.location != null) {
        lat = loc.location.latitude;
        lng = loc.location.longitude;
        address = loc.address != null ? loc.address : "";
      }

      out.add(new Listing(n.chatId, title, address, lat, lng, n.distance));
    }
  }

  /**
   * Создать канал-объявление с геометкой.
   * title — название объявления, address — текстовый адрес,
   * description — описание (цена, комнаты и т.д.).
   */
  public static void createListingChannel (Tdlib tdlib,
                                          String title,
                                          String description,
                                          double lat,
                                          double lng,
                                          String address,
                                          RunnableData<Long> onChatId,
                                          RunnableData<String> onError) {
    if (tdlib == null) {
      if (onError != null) onError.runWithData("Telegram не готов");
      return;
    }

    TdApi.Location location = new TdApi.Location(lat, lng, 0.0);
    TdApi.ChatLocation chatLocation = new TdApi.ChatLocation(location,
      StringUtils.isEmpty(address) ? "Адрес не указан" : address);

    // CreateNewSupergroupChat(title, isForum, description, location, messageAutoDeleteTime, forImport)
    TdApi.CreateNewSupergroupChat req;
    try {
      req = new TdApi.CreateNewSupergroupChat(
        title,
        false,
        description != null ? description : "",
        chatLocation,
        0,
        false
      );
    } catch (Throwable t) {
      // Старые биндинги без location — пробуем без гео
      try {
        req = new TdApi.CreateNewSupergroupChat(
          title,
          false,
          description != null ? description : "",
          null,
          0,
          false
        );
      } catch (Throwable t2) {
        if (onError != null) onError.runWithData("CreateNewSupergroupChat недоступен: " + t2.getMessage());
        return;
      }
    }

    tdlib.client().send(req, object -> tdlib.ui().post(() -> {
      if (object instanceof TdApi.Error) {
        if (onError != null) onError.runWithData(((TdApi.Error) object).message);
        return;
      }
      if (object instanceof TdApi.Chat) {
        long chatId = ((TdApi.Chat) object).id;
        if (onChatId != null) onChatId.runWithData(chatId);
      } else if (onError != null) {
        onError.runWithData("Не удалось создать канал");
      }
    }));
  }

  /**
   * Избранное: храним chatId списком в «Избранном»-сообщении в Saved Messages.
   * Формат текста: YR_FAV\n123\n456\n...
   */
  public static final String FAV_PREFIX = "YR_FAV";

  public static void loadFavoriteChatIds (Tdlib tdlib,
                                          RunnableData<List<Long>> onResult) {
    if (tdlib == null) {
      if (onResult != null) onResult.runWithData(new ArrayList<>());
      return;
    }
    long savedChatId = org.thunderdog.challegram.telegram.ChatId.fromUserId(tdlib.myUserId());
    if (savedChatId == 0) {
      if (onResult != null) onResult.runWithData(new ArrayList<>());
      return;
    }

    tdlib.client().send(new TdApi.GetChatHistory(savedChatId, 0, 0, 50, false), object -> tdlib.ui().post(() -> {
      List<Long> ids = new ArrayList<>();
      if (object instanceof TdApi.Messages) {
        for (TdApi.Message msg : ((TdApi.Messages) object).messages) {
          if (msg == null || !(msg.content instanceof TdApi.MessageText)) continue;
          String text = ((TdApi.MessageText) msg.content).text.text;
          if (text != null && text.startsWith(FAV_PREFIX)) {
            String[] lines = text.split("\n");
            for (int i = 1; i < lines.length; i++) {
              try {
                long id = Long.parseLong(lines[i].trim());
                if (id != 0 && !ids.contains(id)) ids.add(id);
              } catch (NumberFormatException ignored) {}
            }
            break; // берём первое (самое новое) сообщение-список
          }
        }
      }
      if (onResult != null) onResult.runWithData(ids);
    }));
  }

  public static void saveFavoriteChatIds (Tdlib tdlib, List<Long> chatIds,
                                          @Nullable Runnable onDone) {
    if (tdlib == null) {
      if (onDone != null) onDone.run();
      return;
    }
    long savedChatId = org.thunderdog.challegram.telegram.ChatId.fromUserId(tdlib.myUserId());
    if (savedChatId == 0) {
      if (onDone != null) onDone.run();
      return;
    }

    StringBuilder sb = new StringBuilder(FAV_PREFIX);
    if (chatIds != null) {
      for (Long id : chatIds) {
        if (id != null && id != 0) sb.append('\n').append(id);
      }
    }

    TdApi.InputMessageText content = new TdApi.InputMessageText(
      new TdApi.FormattedText(sb.toString(), null),
      null,
      false
    );

    tdlib.client().send(
      new TdApi.SendMessage(savedChatId, null, null, null, null, content),
      object -> tdlib.ui().post(() -> {
        if (onDone != null) onDone.run();
      })
    );
  }
}
