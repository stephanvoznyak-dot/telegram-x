/*
 * Telegram X + «Я рядом»
 * TDLib: SearchChatsNearby, создание канала с geo, избранное в Saved Messages.
 */
package org.thunderdog.challegram.ui;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import me.vkryl.core.StringUtils;
import tgx.td.ChatId;

public final class YaRyadomGeo {

  public interface ListCallback { void onResult (List<Listing> list); }
  public interface ErrorCallback { void onError (String message); }
  public interface LongCallback { void onResult (long value); }
  public interface IdsCallback { void onResult (List<Long> ids); }

  public static final class Listing {
    public final long chatId;
    public final String title;
    public final String address;
    public final double latitude;
    public final double longitude;
    public final int distanceMeters;
    @Nullable public final YaRyadomFormat.ListingData parsed;

    public Listing (long chatId, String title, String address,
                    double latitude, double longitude, int distanceMeters,
                    @Nullable YaRyadomFormat.ListingData parsed) {
      this.chatId = chatId;
      this.title = title != null ? title : "";
      this.address = address != null ? address : "";
      this.latitude = latitude;
      this.longitude = longitude;
      this.distanceMeters = distanceMeters;
      this.parsed = parsed;
    }

    public String distanceLabel () {
      if (distanceMeters < 0) return "";
      if (distanceMeters < 1000) return distanceMeters + " м";
      return String.format("%.1f км", distanceMeters / 1000.0);
    }

    public String subtitle () {
      if (parsed != null) {
        String meta = parsed.metaLine();
        if (!StringUtils.isEmpty(meta)) return meta;
        if (!StringUtils.isEmpty(parsed.address)) return parsed.address;
      }
      if (!StringUtils.isEmpty(address)) return address;
      if (latitude != 0 || longitude != 0) {
        return String.format("%.5f, %.5f", latitude, longitude);
      }
      return "";
    }
  }

  private static final long CACHE_TTL_MS = 90_000L;
  private static double cacheLat, cacheLng;
  private static long cacheAt;
  private static List<Listing> cacheList;

  private static long favMessageId = 0;
  private static long favChatId = 0;
  private static final List<Long> favIds = new ArrayList<>();
  private static final Object favLock = new Object();
  private static final AtomicBoolean favWriteScheduled = new AtomicBoolean(false);
  private static final long FAV_DEBOUNCE_MS = 1200L;

  private YaRyadomGeo () {}

  public static void clearNearbyCache () {
    cacheList = null;
    cacheAt = 0;
  }

  public static void searchNearby (Tdlib tdlib, double lat, double lng,
                                   boolean onlyYaRyadom,
                                   boolean forceRefresh,
                                   ListCallback onResult, ErrorCallback onError) {
    if (tdlib == null) {
      if (onError != null) onError.onError("Telegram не готов");
      return;
    }

    if (!forceRefresh && cacheList != null
      && System.currentTimeMillis() - cacheAt < CACHE_TTL_MS
      && almostSame(cacheLat, lat) && almostSame(cacheLng, lng)) {
      if (onResult != null) onResult.onResult(filterList(cacheList, onlyYaRyadom));
      return;
    }

    tdlib.client().send(
      new TdApi.SearchChatsNearby(new TdApi.Location(lat, lng, 0.0)),
      object -> tdlib.ui().post(() -> {
        if (object instanceof TdApi.Error) {
          if (onError != null) onError.onError(humanError(((TdApi.Error) object).message));
          return;
        }
        if (!(object instanceof TdApi.ChatsNearby)) {
          if (onError != null) onError.onError("Неожиданный ответ TDLib");
          return;
        }

        TdApi.ChatsNearby nearby = (TdApi.ChatsNearby) object;
        List<Listing> list = new ArrayList<>();
        appendNearby(tdlib, list, readLocationsNearby(nearby));

        cacheLat = lat;
        cacheLng = lng;
        cacheAt = System.currentTimeMillis();
        cacheList = list;

        if (onResult != null) onResult.onResult(filterList(list, onlyYaRyadom));
      })
    );
  }

  private static List<Listing> filterList (List<Listing> src, boolean onlyYaRyadom) {
    if (!onlyYaRyadom || src == null) return src != null ? new ArrayList<>(src) : new ArrayList<>();
    List<Listing> out = new ArrayList<>();
    for (Listing l : src) {
      if (YaRyadomFormat.isYaRyadomTitle(l.title) || l.parsed != null) {
        out.add(l);
      }
    }
    return out;
  }

  private static boolean almostSame (double a, double b) {
    return Math.abs(a - b) < 0.0008; // ~80–90 m
  }

  private static TdApi.ChatNearby[] readLocationsNearby (TdApi.ChatsNearby nearby) {
    try {
      java.lang.reflect.Field f = nearby.getClass().getField("locationsNearby");
      Object val = f.get(nearby);
      if (val instanceof TdApi.ChatNearby[]) return (TdApi.ChatNearby[]) val;
    } catch (Throwable ignored) {}
    // fallback: some builds put all in usersNearby — skip users, try any ChatNearby arrays
    try {
      for (java.lang.reflect.Field f : nearby.getClass().getFields()) {
        if (f.getType().isArray() && f.getType().getComponentType() == TdApi.ChatNearby.class) {
          Object val = f.get(nearby);
          if (val instanceof TdApi.ChatNearby[] && !"usersNearby".equals(f.getName())) {
            return (TdApi.ChatNearby[]) val;
          }
        }
      }
    } catch (Throwable ignored) {}
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

      YaRyadomFormat.ListingData parsed = null;
      try {
        String about = null;
        // description often in full info — optional
        parsed = YaRyadomFormat.parse(about);
      } catch (Throwable ignored) {}

      out.add(new Listing(n.chatId, title, address, lat, lng, n.distance, parsed));
    }
  }

  /**
   * Создать канал-объявление. При ошибке geo — fallback без location.
   */
  public static void createListingChannel (Tdlib tdlib,
                                           YaRyadomFormat.ListingData data,
                                           LongCallback onChatId,
                                           ErrorCallback onError) {
    if (tdlib == null) {
      if (onError != null) onError.onError("Telegram не готов");
      return;
    }

    String title = YaRyadomFormat.buildChannelTitle(data);
    String description = YaRyadomFormat.buildDescription(data);

    TdApi.ChatLocation chatLocation = null;
    if (data.lat != 0 || data.lng != 0) {
      chatLocation = new TdApi.ChatLocation(
        new TdApi.Location(data.lat, data.lng, 0.0),
        StringUtils.isEmpty(data.address) ? "Адрес не указан" : data.address
      );
    }

    sendCreate(tdlib, title, description, chatLocation, onChatId, error -> {
      // Fallback без гео
      if (chatLocation != null) {
        sendCreate(tdlib, title, description, null, onChatId, onError);
      } else if (onError != null) {
        onError.onError(humanError(error));
      }
    });
  }

  private static void sendCreate (Tdlib tdlib, String title, String description,
                                  @Nullable TdApi.ChatLocation location,
                                  LongCallback onChatId, ErrorCallback onError) {
    final TdApi.CreateNewSupergroupChat req;
    try {
      req = new TdApi.CreateNewSupergroupChat(title, false, description, location, 0, false);
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
        clearNearbyCache();
        if (onChatId != null) onChatId.onResult(((TdApi.Chat) object).id);
      } else if (onError != null) {
        onError.onError("Не удалось создать канал");
      }
    }));
  }

  // ——— Избранное (одно сообщение, debounce, edit) ———

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

    tdlib.client().send(new TdApi.GetChatHistory(savedChatId, 0, 0, 40, false),
      object -> tdlib.ui().post(() -> {
        List<Long> ids = new ArrayList<>();
        long msgId = 0;
        if (object instanceof TdApi.Messages) {
          for (TdApi.Message msg : ((TdApi.Messages) object).messages) {
            if (msg == null || !(msg.content instanceof TdApi.MessageText)) continue;
            String text = ((TdApi.MessageText) msg.content).text.text;
            if (text != null && (text.startsWith(YaRyadomFormat.FAV_PREFIX) || text.startsWith("YR_FAV"))) {
              ids = YaRyadomFormat.parseFavPayload(text);
              msgId = msg.id;
              break;
            }
          }
        }
        synchronized (favLock) {
          favIds.clear();
          favIds.addAll(ids);
          favMessageId = msgId;
          favChatId = savedChatId;
        }
        if (onResult != null) onResult.onResult(new ArrayList<>(ids));
      }));
  }

  public static List<Long> getFavoriteIdsSnapshot () {
    synchronized (favLock) {
      return new ArrayList<>(favIds);
    }
  }

  public static boolean isFavorite (long chatId) {
    synchronized (favLock) {
      return favIds.contains(chatId);
    }
  }

  public static void toggleFavorite (Tdlib tdlib, long chatId, @Nullable Runnable onUiUpdated) {
    synchronized (favLock) {
      if (favIds.contains(chatId)) favIds.remove(chatId);
      else favIds.add(chatId);
    }
    if (onUiUpdated != null) onUiUpdated.run();
    scheduleFavPersist(tdlib);
  }

  private static void scheduleFavPersist (Tdlib tdlib) {
    if (!favWriteScheduled.compareAndSet(false, true)) return;
    if (tdlib == null) {
      favWriteScheduled.set(false);
      return;
    }
    tdlib.ui().postDelayed(() -> {
      favWriteScheduled.set(false);
      persistFavoritesNow(tdlib);
    }, FAV_DEBOUNCE_MS);
  }

  private static void persistFavoritesNow (Tdlib tdlib) {
    final List<Long> ids;
    final long existingMsgId;
    final long savedChatId;
    synchronized (favLock) {
      ids = new ArrayList<>(favIds);
      existingMsgId = favMessageId;
      savedChatId = favChatId != 0 ? favChatId : ChatId.fromUserId(tdlib.myUserId());
    }
    if (savedChatId == 0) return;

    String payload = YaRyadomFormat.buildFavPayload(ids);
    TdApi.FormattedText ft = new TdApi.FormattedText(payload, null);

    if (existingMsgId != 0) {
      tdlib.client().send(
        new TdApi.EditMessageText(savedChatId, existingMsgId, null,
          new TdApi.InputMessageText(ft, null, false)),
        object -> tdlib.ui().post(() -> {
          if (object instanceof TdApi.Error) {
            // если edit не удался — отправим новое
            sendNewFavMessage(tdlib, savedChatId, ft);
          } else if (object instanceof TdApi.Message) {
            synchronized (favLock) {
              favMessageId = ((TdApi.Message) object).id;
              favChatId = savedChatId;
            }
          }
        })
      );
    } else {
      sendNewFavMessage(tdlib, savedChatId, ft);
    }
  }

  private static void sendNewFavMessage (Tdlib tdlib, long savedChatId, TdApi.FormattedText ft) {
    tdlib.client().send(
      new TdApi.SendMessage(savedChatId, null, null, null, null,
        new TdApi.InputMessageText(ft, null, false)),
      object -> tdlib.ui().post(() -> {
        if (object instanceof TdApi.Message) {
          synchronized (favLock) {
            favMessageId = ((TdApi.Message) object).id;
            favChatId = savedChatId;
          }
        }
      })
    );
  }

  private static String humanError (String raw) {
    if (raw == null) return "Ошибка";
    String r = raw.toLowerCase();
    if (r.contains("flood")) return "Слишком много запросов, подождите";
    if (r.contains("location")) return "Геолокация недоступна для этого действия";
    if (r.contains("chat_title")) return "Некорректное название";
    return raw;
  }
}
