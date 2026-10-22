/*
 * Telegram X + «Я рядом»
 * Простое in-memory хранилище избранного (на сессию приложения).
 */
package org.thunderdog.challegram.ui;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Избранные объявления. Ключ — стабильный id объявления.
 */
public final class YaRyadomFavorites {

  public static final class Item {
    public final String id;
    public final String title;
    public final String price;
    public final String meta;
    public final String district;
    @Nullable public final String authorUsername;
    public final long authorUserId;

    public Item (String id, String title, String price, String meta, String district,
                 @Nullable String authorUsername, long authorUserId) {
      this.id = id;
      this.title = title;
      this.price = price;
      this.meta = meta;
      this.district = district;
      this.authorUsername = authorUsername;
      this.authorUserId = authorUserId;
    }
  }

  private static final Map<String, Item> favorites = new LinkedHashMap<>();

  private YaRyadomFavorites () {}

  public static synchronized boolean isFavorite (String id) {
    return id != null && favorites.containsKey(id);
  }

  public static synchronized void toggle (Item item) {
    if (item == null || item.id == null) return;
    if (favorites.containsKey(item.id)) {
      favorites.remove(item.id);
    } else {
      favorites.put(item.id, item);
    }
  }

  public static synchronized void add (Item item) {
    if (item == null || item.id == null) return;
    favorites.put(item.id, item);
  }

  public static synchronized void remove (String id) {
    if (id != null) favorites.remove(id);
  }

  public static synchronized List<Item> getAll () {
    return new ArrayList<>(favorites.values());
  }

  public static synchronized int count () {
    return favorites.size();
  }
}
