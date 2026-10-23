/*
 * Telegram X + «Я рядом»
 * Поиск каналов с гео (SearchChatsNearby), фильтр YR, избранное, размещение.
 */
package org.thunderdog.challegram.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.tool.UI;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.StringUtils;

public class YaRyadomController extends ViewController<Void> {

  private static final double DEFAULT_LAT = 59.437;
  private static final double DEFAULT_LNG = 24.7535;

  private final List<YaRyadomGeo.Listing> listings = new ArrayList<>();
  private LinearLayout listContainer;
  private EditText latInput, lngInput;
  private TextView statusText, favBtn, filterBtn, searchBtn;
  private boolean onlyYaRyadom = true;
  private boolean searching;
  private boolean showFavoritesOnly;

  public YaRyadomController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override public int getId () { return R.id.controller_yaryadom; }
  @Override protected int getBackButton () { return BackHeaderButton.TYPE_BACK; }
  @Override public CharSequence getName () { return "Я рядом"; }

  @Override
  protected View onCreateView (Context context) {
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    root.setBackgroundColor(Theme.backgroundColor());

    int pad = Screen.dp(12f);

    TextView geoLabel = new TextView(context);
    geoLabel.setText("Каналы с геометкой рядом с точкой");
    geoLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    geoLabel.setTextColor(Theme.getColor(ColorId.textLight));
    geoLabel.setPadding(pad, pad, pad, Screen.dp(4f));
    root.addView(geoLabel);

    LinearLayout coords = new LinearLayout(context);
    coords.setOrientation(LinearLayout.HORIZONTAL);
    coords.setPadding(pad, 0, pad, Screen.dp(6f));
    coords.setGravity(Gravity.CENTER_VERTICAL);

    latInput = coordField(context, "Широта");
    lngInput = coordField(context, "Долгота");
    tryFillDeviceLocation();
    if (StringUtils.isEmpty(text(latInput))) latInput.setText(String.valueOf(DEFAULT_LAT));
    if (StringUtils.isEmpty(text(lngInput))) lngInput.setText(String.valueOf(DEFAULT_LNG));

    coords.addView(latInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    LinearLayout.LayoutParams lngLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    lngLp.leftMargin = Screen.dp(6f);
    coords.addView(lngInput, lngLp);

    TextView myLoc = makeButton(context, "GPS", false);
    myLoc.setOnClickListener(v -> {
      if (tryFillDeviceLocation()) {
        UI.showToast("Координаты обновлены", Toast.LENGTH_SHORT);
        runSearch(true);
      } else {
        UI.showToast("Нет доступа к геолокации", Toast.LENGTH_SHORT);
      }
    });
    LinearLayout.LayoutParams gpsLp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    gpsLp.leftMargin = Screen.dp(6f);
    coords.addView(myLoc, gpsLp);
    root.addView(coords);

    LinearLayout actions = new LinearLayout(context);
    actions.setOrientation(LinearLayout.HORIZONTAL);
    actions.setPadding(pad, 0, pad, Screen.dp(6f));

    searchBtn = makeButton(context, "Найти", true);
    searchBtn.setOnClickListener(v -> runSearch(true));
    actions.addView(searchBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    filterBtn = makeButton(context, onlyYaRyadom ? "YR: вкл" : "YR: все", false);
    filterBtn.setOnClickListener(v -> {
      onlyYaRyadom = !onlyYaRyadom;
      filterBtn.setText(onlyYaRyadom ? "YR: вкл" : "YR: все");
      runSearch(false); // из кэша если есть
    });
    LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    fLp.leftMargin = Screen.dp(6f);
    actions.addView(filterBtn, fLp);
    root.addView(actions);

    LinearLayout actions2 = new LinearLayout(context);
    actions2.setOrientation(LinearLayout.HORIZONTAL);
    actions2.setPadding(pad, 0, pad, pad);

    TextView placeBtn = makeButton(context, "+ Разместить", false);
    placeBtn.setOnClickListener(v -> openCreateForm());
    actions2.addView(placeBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    favBtn = makeButton(context, "Избранное", false);
    favBtn.setOnClickListener(v -> {
      showFavoritesOnly = !showFavoritesOnly;
      if (showFavoritesOnly) renderFavoritesView();
      else runSearch(false);
    });
    LinearLayout.LayoutParams favLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    favLp.leftMargin = Screen.dp(6f);
    actions2.addView(favBtn, favLp);
    root.addView(actions2);

    statusText = new TextView(context);
    statusText.setText("Загрузка…");
    statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    statusText.setTextColor(Theme.getColor(ColorId.textLight));
    statusText.setPadding(pad, 0, pad, pad);
    root.addView(statusText);

    ScrollView scroll = new ScrollView(context);
    scroll.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    listContainer = new LinearLayout(context);
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setPadding(pad, 0, pad, pad);
    scroll.addView(listContainer);
    root.addView(scroll);

    YaRyadomGeo.loadFavoriteChatIds(tdlib, ids -> {
      updateFavButton();
      runSearch(true);
    });

    return root;
  }

  @Override
  public void onFocus () {
    super.onFocus();
    updateFavButton();
    if (listContainer != null && showFavoritesOnly) renderFavoritesView();
  }

  private boolean tryFillDeviceLocation () {
    try {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
      LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      if (lm == null) return false;
      Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      if (loc == null) loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (loc == null) return false;
      if (latInput != null) latInput.setText(String.format(java.util.Locale.US, "%.6f", loc.getLatitude()));
      if (lngInput != null) lngInput.setText(String.format(java.util.Locale.US, "%.6f", loc.getLongitude()));
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  private void runSearch (boolean force) {
    if (searching) return;
    showFavoritesOnly = false;

    double lat, lng;
    try {
      lat = Double.parseDouble(text(latInput).replace(',', '.'));
      lng = Double.parseDouble(text(lngInput).replace(',', '.'));
    } catch (Exception e) {
      UI.showToast("Укажите координаты", Toast.LENGTH_SHORT);
      return;
    }

    searching = true;
    if (searchBtn != null) searchBtn.setEnabled(false);
    statusText.setText(force ? "Ищем…" : "Обновляем…");

    YaRyadomGeo.searchNearby(tdlib, lat, lng, onlyYaRyadom, force,
      result -> {
        searching = false;
        if (searchBtn != null) searchBtn.setEnabled(true);
        listings.clear();
        listings.addAll(result);
        statusText.setText(result.isEmpty()
          ? (onlyYaRyadom
            ? "Нет каналов YR рядом. Выключите фильтр «YR» или разместите объявление."
            : "Рядом нет каналов с геометкой")
          : ("Найдено: " + result.size() + (onlyYaRyadom ? " (YR)" : "")));
        renderListings(listings);
      },
      error -> {
        searching = false;
        if (searchBtn != null) searchBtn.setEnabled(true);
        statusText.setText("Ошибка: " + error);
        UI.showToast(error, Toast.LENGTH_LONG);
      }
    );
  }

  private void renderListings (List<YaRyadomGeo.Listing> data) {
    if (listContainer == null) return;
    listContainer.removeAllViews();
    if (data.isEmpty()) {
      TextView empty = new TextView(context);
      empty.setText("Пусто.\n«+ Разместить» — создать канал-объявление.");
      empty.setTextColor(Theme.getColor(ColorId.textLight));
      empty.setGravity(Gravity.CENTER);
      empty.setPadding(0, Screen.dp(32f), 0, 0);
      listContainer.addView(empty);
      return;
    }
    for (YaRyadomGeo.Listing listing : data) {
      listContainer.addView(createCard(listing));
    }
  }

  private void renderFavoritesView () {
    List<Long> ids = YaRyadomGeo.getFavoriteIdsSnapshot();
    List<YaRyadomGeo.Listing> fav = new ArrayList<>();
    for (YaRyadomGeo.Listing l : listings) {
      if (ids.contains(l.chatId)) fav.add(l);
    }
    for (Long id : ids) {
      boolean found = false;
      for (YaRyadomGeo.Listing l : fav) {
        if (l.chatId == id) { found = true; break; }
      }
      if (!found && id != null && id != 0) {
        String title = tdlib.chatTitle(id);
        if (StringUtils.isEmpty(title)) title = "Канал " + id;
        fav.add(new YaRyadomGeo.Listing(id, title, "", 0, 0, -1, null));
      }
    }
    statusText.setText("Избранное: " + fav.size());
    renderListings(fav);
    updateFavButton();
  }

  private View createCard (YaRyadomGeo.Listing listing) {
    LinearLayout card = new LinearLayout(context);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(Screen.dp(14f), Screen.dp(14f), Screen.dp(14f), Screen.dp(14f));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(12f));
    bg.setColor(Theme.getColor(ColorId.filling));
    card.setBackground(bg);
    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardLp.bottomMargin = Screen.dp(10f);
    card.setLayoutParams(cardLp);

    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);

    TextView title = new TextView(context);
    title.setText(listing.title);
    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    title.setTypeface(Typeface.DEFAULT_BOLD);
    title.setTextColor(Theme.getColor(ColorId.text));
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    if (listing.distanceMeters >= 0) {
      TextView dist = new TextView(context);
      dist.setText(listing.distanceLabel());
      dist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
      dist.setTextColor(0xFF0088CC);
      dist.setTypeface(Typeface.DEFAULT_BOLD);
      row.addView(dist);
    }
    card.addView(row);

    String sub = listing.subtitle();
    if (!StringUtils.isEmpty(sub)) {
      TextView meta = new TextView(context);
      meta.setText(sub);
      meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
      meta.setTextColor(Theme.getColor(ColorId.textLight));
      meta.setPadding(0, Screen.dp(6f), 0, Screen.dp(10f));
      card.addView(meta);
    }

    LinearLayout buttons = new LinearLayout(context);
    buttons.setOrientation(LinearLayout.HORIZONTAL);

    boolean isFav = YaRyadomGeo.isFavorite(listing.chatId);
    TextView starBtn = makeButton(context, isFav ? "★" : "☆", false);
    starBtn.setOnClickListener(v -> {
      YaRyadomGeo.toggleFavorite(tdlib, listing.chatId, () -> {
        updateFavButton();
        if (showFavoritesOnly) renderFavoritesView();
        else renderListings(listings);
      });
      UI.showToast(YaRyadomGeo.isFavorite(listing.chatId)
        ? "В избранном" : "Удалено", Toast.LENGTH_SHORT);
    });
    buttons.addView(starBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.4f));

    TextView openBtn = makeButton(context, "Открыть", true);
    openBtn.setOnClickListener(v -> tdlib.ui().openChat(this, listing.chatId, null));
    LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    openLp.leftMargin = Screen.dp(8f);
    buttons.addView(openBtn, openLp);

    card.addView(buttons);
    return card;
  }

  private void openCreateForm () {
    double lat = DEFAULT_LAT, lng = DEFAULT_LNG;
    try {
      lat = Double.parseDouble(text(latInput).replace(',', '.'));
      lng = Double.parseDouble(text(lngInput).replace(',', '.'));
    } catch (Exception ignored) {}
    YaRyadomCreateController form = new YaRyadomCreateController(context, tdlib)
      .prefillCoords(lat, lng)
      .setListener((chatId, title) -> runSearch(true));
    navigateTo(form);
  }

  private void updateFavButton () {
    if (favBtn == null) return;
    int n = YaRyadomGeo.getFavoriteIdsSnapshot().size();
    favBtn.setText(n > 0 ? ("Избранное (" + n + ")") : "Избранное");
  }

  private EditText coordField (Context context, String hint) {
    EditText edit = new EditText(context);
    edit.setHint(hint);
    edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    edit.setTextColor(Theme.getColor(ColorId.text));
    edit.setHintTextColor(Theme.getColor(ColorId.textLight));
    edit.setSingleLine(true);
    edit.setPadding(Screen.dp(10f), Screen.dp(10f), Screen.dp(10f), Screen.dp(10f));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(8f));
    bg.setColor(Theme.getColor(ColorId.filling));
    edit.setBackground(bg);
    return edit;
  }

  private TextView makeButton (Context context, String text, boolean primary) {
    TextView btn = new TextView(context);
    btn.setText(text);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    btn.setTypeface(Typeface.DEFAULT_BOLD);
    btn.setGravity(Gravity.CENTER);
    btn.setPadding(Screen.dp(12f), Screen.dp(12f), Screen.dp(12f), Screen.dp(12f));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(10f));
    if (primary) {
      bg.setColor(0xFF0088CC);
      btn.setTextColor(0xFFFFFFFF);
    } else {
      bg.setColor(Theme.getColor(ColorId.filling));
      btn.setTextColor(Theme.getColor(ColorId.text));
    }
    btn.setBackground(bg);
    return btn;
  }

  private static String text (EditText edit) {
    return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
  }
}
