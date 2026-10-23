/*
 * Telegram X + «Я рядом»
 * Стабильный текстовый формат объявления (версия 1).
 *
 * YR1|title=...|price=...|rooms=...|area=...|address=...|lat=...|lng=...|desc=...
 */
package org.thunderdog.challegram.ui;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import me.vkryl.core.StringUtils;

public final class YaRyadomFormat {

  public static final String TAG = "#ЯРядом";
  public static final String PREFIX = "YR1|";
  public static final String FAV_PREFIX = "YR_FAV1";

  public static final class ListingData {
    public String title = "";
    public String price = "";
    public String rooms = "";
    public String area = "";
    public String address = "";
    public double lat = 0;
    public double lng = 0;
    public String description = "";

    public String metaLine () {
      StringBuilder sb = new StringBuilder();
      if (!StringUtils.isEmpty(rooms)) sb.append(rooms).append(" комн.");
      if (!StringUtils.isEmpty(area)) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(area).append(" м²");
      }
      if (!StringUtils.isEmpty(price)) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(price.contains("₽") ? price : price + " ₽/мес");
      }
      return sb.toString();
    }
  }

  private YaRyadomFormat () {}

  public static String buildDescription (ListingData d) {
    StringBuilder sb = new StringBuilder();
    sb.append(TAG).append('\n');
    sb.append(PREFIX);
    sb.append("title=").append(esc(d.title)).append('|');
    sb.append("price=").append(esc(d.price)).append('|');
    sb.append("rooms=").append(esc(d.rooms)).append('|');
    sb.append("area=").append(esc(d.area)).append('|');
    sb.append("address=").append(esc(d.address)).append('|');
    sb.append("lat=").append(formatCoord(d.lat)).append('|');
    sb.append("lng=").append(formatCoord(d.lng)).append('|');
    sb.append("desc=").append(esc(d.description));
    return sb.toString();
  }

  public static String buildChannelTitle (ListingData d) {
    String t = d.title != null ? d.title.trim() : "";
    if (t.isEmpty()) t = "Объявление";
    // Короткий маркер в названии для фильтра nearby
    if (!t.contains("YR")) {
      t = "YR · " + t;
    }
    if (t.length() > 128) t = t.substring(0, 128);
    return t;
  }

  public static boolean isYaRyadomTitle (@Nullable String title) {
    if (title == null) return false;
    String t = title.toLowerCase(Locale.ROOT);
    return t.contains("yr ·") || t.contains("yr·") || t.startsWith("yr ")
      || title.contains("#ЯРядом") || t.contains("я рядом");
  }

  public static boolean isYaRyadomText (@Nullable String text) {
    if (text == null) return false;
    return text.contains(PREFIX) || text.contains(TAG);
  }

  @Nullable
  public static ListingData parse (@Nullable String text) {
    if (text == null) return null;
    int idx = text.indexOf(PREFIX);
    if (idx < 0) {
      // soft: only tag
      if (text.contains(TAG)) {
        ListingData d = new ListingData();
        d.title = text.replace(TAG, "").trim();
        return d;
      }
      return null;
    }
    String payload = text.substring(idx + PREFIX.length()).trim();
    // cut after newline if user added more text
    int nl = payload.indexOf('\n');
    if (nl > 0) payload = payload.substring(0, nl);

    Map<String, String> map = new HashMap<>();
    for (String part : payload.split("\\|")) {
      int eq = part.indexOf('=');
      if (eq <= 0) continue;
      String key = part.substring(0, eq).trim();
      String val = unesc(part.substring(eq + 1).trim());
      map.put(key, val);
    }

    ListingData d = new ListingData();
    d.title = map.getOrDefault("title", "");
    d.price = map.getOrDefault("price", "");
    d.rooms = map.getOrDefault("rooms", "");
    d.area = map.getOrDefault("area", "");
    d.address = map.getOrDefault("address", "");
    d.description = map.getOrDefault("desc", "");
    try { d.lat = Double.parseDouble(map.getOrDefault("lat", "0")); } catch (Exception ignored) {}
    try { d.lng = Double.parseDouble(map.getOrDefault("lng", "0")); } catch (Exception ignored) {}
    return d;
  }

  public static String buildFavPayload (java.util.List<Long> chatIds) {
    StringBuilder sb = new StringBuilder(FAV_PREFIX);
    if (chatIds != null) {
      for (Long id : chatIds) {
        if (id != null && id != 0) sb.append('\n').append(id);
      }
    }
    return sb.toString();
  }

  public static java.util.List<Long> parseFavPayload (@Nullable String text) {
    java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
    if (text == null || !(text.startsWith(FAV_PREFIX) || text.startsWith("YR_FAV"))) return ids;
    String[] lines = text.split("\n");
    for (int i = 1; i < lines.length; i++) {
      try {
        long id = Long.parseLong(lines[i].trim());
        if (id != 0 && !ids.contains(id)) ids.add(id);
      } catch (NumberFormatException ignored) {}
    }
    return ids;
  }

  private static String formatCoord (double v) {
    return String.format(Locale.US, "%.6f", v);
  }

  private static String esc (String s) {
    if (s == null) return "";
    return s.replace("|", "/").replace("\n", " ").trim();
  }

  private static String unesc (String s) {
    return s == null ? "" : s;
  }
}
