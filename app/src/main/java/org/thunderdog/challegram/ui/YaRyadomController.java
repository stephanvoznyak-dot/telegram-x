/*
 * Telegram X + «Я рядом»
 *
 * Объявления = каналы с геометкой.
 * Поиск = TdApi.SearchChatsNearby.
 */
package org.thunderdog.challegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

  // Координаты поиска по умолчанию (можно менять в полях)
  private static final double DEFAULT_LAT = 59.437;
  private static final double DEFAULT_LNG = 24.7535;

  private final List<YaRyadomGeo.Listing> listings = new ArrayList<>();
  private final List<Long> favoriteIds = new ArrayList<>();

  private LinearLayout listContainer;
  private EditText latInput;
  private EditText lngInput;
  private TextView statusText;
  private TextView favBtn;

  public YaRyadomController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    return R.id.controller_yaryadom;
  }

  @Override
  protected int getBackButton () {
    return BackHeaderButton.TYPE_BACK;
  }

  @Override
  public CharSequence getName () {
    return "Я рядом";
  }

  @Override
  protected View onCreateView (Context context) {
    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    ));
    root.setBackgroundColor(Theme.backgroundColor());

    int pad = Screen.dp(12f);

    // —— Координаты поиска ——
    TextView geoLabel = new TextView(context);
    geoLabel.setText("Поиск каналов с геометкой рядом с точкой");
    geoLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    geoLabel.setTextColor(Theme.getColor(ColorId.textLight));
    geoLabel.setPadding(pad, pad, pad, Screen.dp(4f));
    root.addView(geoLabel);

    LinearLayout coords = new LinearLayout(context);
    coords.setOrientation(LinearLayout.HORIZONTAL);
    coords.setPadding(pad, 0, pad, pad);
    coords.setGravity(Gravity.CENTER_VERTICAL);

    latInput = coordField(context, "Широта");
    latInput.setText(String.valueOf(DEFAULT_LAT));
    coords.addView(latInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    lngInput = coordField(context, "Долгота");
    lngInput.setText(String.valueOf(DEFAULT_LNG));
    LinearLayout.LayoutParams lngLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    lngLp.leftMargin = Screen.dp(8f);
    coords.addView(lngInput, lngLp);

    TextView searchBtn = makeButton(context, "Найти", true);
    searchBtn.setOnClickListener(v -> runNearbySearch());
    LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    sLp.leftMargin = Screen.dp(8f);
    coords.addView(searchBtn, sLp);
    root.addView(coords);

    // —— Действия ——
    LinearLayout actions = new LinearLayout(context);
    actions.setOrientation(LinearLayout.HORIZONTAL);
    actions.setPadding(pad, 0, pad, pad);

    TextView placeBtn = makeButton(context, "+ Разместить", false);
    placeBtn.setOnClickListener(v -> openCreateForm());
    actions.addView(placeBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    favBtn = makeButton(context, "Избранное", false);
    favBtn.setOnClickListener(v -> openFavorites());
    LinearLayout.LayoutParams favLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    favLp.leftMargin = Screen.dp(8f);
    actions.addView(favBtn, favLp);
    root.addView(actions);

    statusText = new TextView(context);
    statusText.setText("Нажмите «Найти», чтобы загрузить каналы рядом");
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

    // Загрузить избранное из Saved Messages и сразу поиск
    reloadFavorites(() -> runNearbySearch());

    return root;
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
    btn.setPadding(Screen.dp(14f), Screen.dp(12f), Screen.dp(14f), Screen.dp(12f));

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

  private void runNearbySearch () {
    double lat, lng;
    try {
      lat = Double.parseDouble(textOf(latInput).replace(',', '.'));
      lng = Double.parseDouble(textOf(lngInput).replace(',', '.'));
    } catch (Exception e) {
      UI.showToast("Укажите координаты", Toast.LENGTH_SHORT);
      return;
    }

    statusText.setText("Ищем каналы с геометкой…");
    listContainer.removeAllViews();

    YaRyadomGeo.searchNearby(tdlib, lat, lng,
      result -> {
        listings.clear();
        listings.addAll(result);
        statusText.setText(result.isEmpty()
          ? "Рядом нет каналов с геометкой"
          : ("Найдено: " + result.size()));
        renderListings(listings);
      },
      error -> {
        statusText.setText("Ошибка поиска: " + error);
        UI.showToast(error, Toast.LENGTH_LONG);
      }
    );
  }

  private void renderListings (List<YaRyadomGeo.Listing> data) {
    listContainer.removeAllViews();
    if (data.isEmpty()) {
      TextView empty = new TextView(context);
      empty.setText("Ничего не найдено.\nСоздайте канал-объявление кнопкой «+ Разместить».");
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

    String subtitle = listing.address;
    if (StringUtils.isEmpty(subtitle) && (listing.latitude != 0 || listing.longitude != 0)) {
      subtitle = String.format("%.5f, %.5f", listing.latitude, listing.longitude);
    }
    if (!StringUtils.isEmpty(subtitle)) {
      TextView meta = new TextView(context);
      meta.setText(subtitle);
      meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
      meta.setTextColor(Theme.getColor(ColorId.textLight));
      meta.setPadding(0, Screen.dp(6f), 0, Screen.dp(10f));
      card.addView(meta);
    }

    LinearLayout buttons = new LinearLayout(context);
    buttons.setOrientation(LinearLayout.HORIZONTAL);

    boolean isFav = favoriteIds.contains(listing.chatId);
    TextView starBtn = makeButton(context, isFav ? "★ В избранном" : "☆ В избранное", false);
    starBtn.setOnClickListener(v -> toggleFavorite(listing));
    buttons.addView(starBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView openBtn = makeButton(context, "Открыть", true);
    openBtn.setOnClickListener(v -> tdlib.ui().openChat(this, listing.chatId, null));
    LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    openLp.leftMargin = Screen.dp(8f);
    buttons.addView(openBtn, openLp);

    card.addView(buttons);
    return card;
  }

  private void toggleFavorite (YaRyadomGeo.Listing listing) {
    if (favoriteIds.contains(listing.chatId)) {
      favoriteIds.remove(listing.chatId);
      UI.showToast("Удалено из избранного", Toast.LENGTH_SHORT);
    } else {
      favoriteIds.add(listing.chatId);
      UI.showToast("Добавлено в избранное", Toast.LENGTH_SHORT);
    }
    updateFavButton();
    YaRyadomGeo.saveFavoriteChatIds(tdlib, new ArrayList<>(favoriteIds), null);
    renderListings(listings);
  }

  private void reloadFavorites (Runnable after) {
    YaRyadomGeo.loadFavoriteChatIds(tdlib, ids -> {
      favoriteIds.clear();
      favoriteIds.addAll(ids);
      updateFavButton();
      if (after != null) after.run();
    });
  }

  private void updateFavButton () {
    if (favBtn != null) {
      int n = favoriteIds.size();
      favBtn.setText(n > 0 ? ("Избранное (" + n + ")") : "Избранное");
    }
  }

  private void openFavorites () {
    // Показать только избранные из текущего списка + попытаться открыть по id
    List<YaRyadomGeo.Listing> fav = new ArrayList<>();
    for (YaRyadomGeo.Listing l : listings) {
      if (favoriteIds.contains(l.chatId)) fav.add(l);
    }
    // Для id, которых нет в текущем поиске — карточки-заглушки
    for (Long id : favoriteIds) {
      boolean found = false;
      for (YaRyadomGeo.Listing l : fav) {
        if (l.chatId == id) { found = true; break; }
      }
      if (!found && id != null) {
        String title = tdlib.chatTitle(id);
        if (StringUtils.isEmpty(title)) title = "Канал " + id;
        fav.add(new YaRyadomGeo.Listing(id, title, "", 0, 0, -1));
      }
    }
    statusText.setText("Избранное: " + fav.size());
    renderListings(fav);
  }

  private void openCreateForm () {
    YaRyadomCreateController form = new YaRyadomCreateController(context, tdlib);
    form.setListener((chatId, title) -> {
      // После создания — обновить поиск
      runNearbySearch();
    });
    navigateTo(form);
  }

  private static String textOf (EditText edit) {
    return edit != null && edit.getText() != null ? edit.getText().toString().trim() : "";
  }
}
