/*
 * Telegram X + «Я рядом»
 *
 * Объявление = канал (supergroup) с ChatLocation.
 * Поиск = TdApi.SearchChatsNearby.
 * Создание = TdApi.CreateNewSupergroupChat + ChatLocation.
 * Избранное = сообщение в Saved Messages со списком chatId.
 */
package org.thunderdog.challegram.ui;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.core.StringUtils;
import tgx.td.ChatId;

public final class YaRyadomGeo {

  public interface ListCallback {
    void onResult (List<Listing> list);
  }

  public interface ErrorCallback {
    void onError (String message);
  }

  public interface LongCallback {
    void onResult (long value);
  }

  public interface IdsCallback {
    void onResult (List<Long> ids);
  }

  public static final class Listing {
    public final long chatId;
    public final String title;
    public final String address;
    public final double latitude;
    public final double longitude;
    public final int distanceMeters;

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

  public static final String FAV_PREFIX = "YR_FAV";

  private YaRyadomGeo () {}

  public static void searchNearby (Tdlib tdlib, double lat, double lng,
                                   ListCallback onResult, ErrorCallback onError) {
    if (tdlib == null) {
      if (onError != null) onError.onError("Telegram не готов");
      return;
    }

    tdlib.client().send(
      new TdApi.SearchChatsNearby(new TdApi.Location(lat, lng, 0.0)),
      object -> tdlib.ui().post(() -> {
        if (object instanceof TdApi.Error) {
          if (onError != null) onError.onError(((TdApi.Error) object).message);
          return;
        }
        if (!(object instanceof TdApi.ChatsNearby)) {
          if (onError != null) onError.onError("Неожиданный ответ TDLib");
          return;
        }

        TdApi.ChatsNearby nearby = (TdApi.ChatsNearby) object;
        List<Listing> list = new ArrayList<>();
        appendNearby(tdlib, list, readLocationsNearby(nearby));
        if (onResult != null) onResult.onResult(list);
      })
    );
  }

  private static TdApi.ChatNearby[] readLocationsNearby (TdApi.ChatsNearby nearby) {
    try {
      java.lang.reflect.Field f = nearby.getClass().getField("locationsNearby");
      Object val = f.get(nearby);
      if (val instanceof TdApi.ChatNearby[]) {
        return (TdApi.ChatNearby[]) val;
      }
    } catch (Throwable ignored) {
    }
    return new TdApi.ChatNearby[0];
  }

  private static void appendNearby (Tdlib tdlib, List<Listing> out, TdApi.ChatNearby[] arr) {
    if (arr == null) return;
    for (TdApi.ChatNearby n : arr) {
      if (n == null || n.chatId == 0) continue;
      String title = tdlib.chatTitle(n.chatId);
      if (StringUtils.isEmpty(title)) title = "Канал";

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

  public static void createListingChannel (Tdlib tdlib,
                                           String title,
                                           String description,
                                           double lat,
                                           double lng,
                                           String address,
                                           LongCallback onChatId,
                                           ErrorCallback onError) {
    if (tdlib == null) {
      if (onError != null) onError.onError("Telegram не готов");
      return;
    }

    TdApi.Location location = new TdApi.Location(lat, lng, 0.0);
    TdApi.ChatLocation chatLocation = new TdApi.ChatLocation(
      location,
      StringUtils.isEmpty(address) ? "Адрес не указан" : address
    );

    final TdApi.CreateNewSupergroupChat req;
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
      if (onError != null) onError.onError("CreateNewSupergroupChat: " + t.getMessage());
      return;
    }

    tdlib.client().send(req, object -> tdlib.ui().post(() -> {
      if (object instanceof TdApi.Error) {
        if (onError != null) onError.onError(((TdApi.Error) object).message);
        return;
      }
      if (object instanceof TdApi.Chat) {
        if (onChatId != null) onChatId.onResult(((TdApi.Chat) object).id);
      } else if (onError != null) {
        onError.onError("Не удалось создать канал");
      }
    }));
  }

  public static void loadFavoriteChatIds (Tdlib tdlib, IdsCallback onResult) {
    if (tdlib == null) {
      if (onResult != null) onResult.onResult(new ArrayList<>());
      return;
    }
    long savedChatId = ChatId.fromUserId(tdlib.myUserId());
    if (savedChatId == 0) {
      if (onResult != null) onResult.onResult(new ArrayList<>());
      return;
    }

    tdlib.client().send(new TdApi.GetChatHistory(savedChatId, 0, 0, 50, false),
      object -> tdlib.ui().post(() -> {
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
              break;
            }
          }
        }
        if (onResult != null) onResult.onResult(ids);
      }));
  }

  public static void saveFavoriteChatIds (Tdlib tdlib, List<Long> chatIds,
                                          @Nullable Runnable onDone) {
    if (tdlib == null) {
      if (onDone != null) onDone.run();
      return;
    }
    long savedChatId = ChatId.fromUserId(tdlib.myUserId());
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
