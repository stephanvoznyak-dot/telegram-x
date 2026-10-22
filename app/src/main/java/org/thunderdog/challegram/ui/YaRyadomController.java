/*
 * Telegram X + «Я рядом»
 * Поиск, размещение, избранное, чат с автором через TDLib.
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

import androidx.annotation.Nullable;

import org.drinkless.tdlib.TdApi;
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
import java.util.concurrent.atomic.AtomicInteger;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.StringUtils;

public class YaRyadomController extends ViewController<Void> {

  private static final AtomicInteger ID_SEQ = new AtomicInteger(1);

  private static class Listing {
    final String id;
    final String title;
    final String price;
    final String meta;
    final String district;
    @Nullable final String authorUsername;
    final long authorUserId;

    Listing (String id, String title, String price, String meta, String district,
             @Nullable String authorUsername, long authorUserId) {
      this.id = id;
      this.title = title;
      this.price = price;
      this.meta = meta;
      this.district = district;
      this.authorUsername = authorUsername;
      this.authorUserId = authorUserId;
    }

    static Listing create (String title, String price, String meta, String district,
                           @Nullable String authorUsername, long authorUserId) {
      return new Listing("yr_" + ID_SEQ.getAndIncrement(), title, price, meta, district,
        authorUsername, authorUserId);
    }
  }

  private final List<Listing> listings = new ArrayList<>();
  private LinearLayout listContainer;
  private EditText searchInput;

  public YaRyadomController (Context context, Tdlib tdlib) {
    super(context, tdlib);
    seedDemoListings();
  }

  private void seedDemoListings () {
    listings.add(Listing.create("2-комн. квартира у метро", "45 000 ₽/мес", "45 м² · 2/9 эт.", "Центр", "telegram", 0));
    listings.add(Listing.create("Студия с ремонтом", "28 000 ₽/мес", "28 м² · 5/12 эт.", "Север", "durov", 0));
    listings.add(Listing.create("3-комн. семейная", "62 000 ₽/мес", "78 м² · 3/5 эт.", "Юг", "telegram", 0));
    listings.add(Listing.create("Комната в квартире", "18 000 ₽/мес", "16 м² · 7/9 эт.", "Восток", null, 0));
    listings.add(Listing.create("1-комн. новостройка", "38 000 ₽/мес", "36 м² · 10/16 эт.", "Запад", "telegram", 0));
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

    LinearLayout searchBar = new LinearLayout(context);
    searchBar.setOrientation(LinearLayout.HORIZONTAL);
    searchBar.setGravity(Gravity.CENTER_VERTICAL);
    searchBar.setPadding(pad, pad, pad, pad);

    searchInput = new EditText(context);
    searchInput.setHint("Город, район, метро…");
    searchInput.setInputType(InputType.TYPE_CLASS_TEXT);
    searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    searchInput.setTextColor(Theme.getColor(ColorId.text));
    searchInput.setHintTextColor(Theme.getColor(ColorId.textLight));
    searchInput.setBackgroundColor(Theme.getColor(ColorId.filling));
    searchInput.setPadding(Screen.dp(14f), Screen.dp(12f), Screen.dp(14f), Screen.dp(12f));
    searchInput.setSingleLine(true);
    searchBar.addView(searchInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView searchBtn = makeButton(context, "Найти", true);
    searchBtn.setOnClickListener(v -> filterListings());
    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    btnLp.leftMargin = Screen.dp(8f);
    searchBar.addView(searchBtn, btnLp);
    root.addView(searchBar);

    LinearLayout actions = new LinearLayout(context);
    actions.setOrientation(LinearLayout.HORIZONTAL);
    actions.setPadding(pad, 0, pad, pad);

    TextView placeBtn = makeButton(context, "+ Разместить", false);
    placeBtn.setOnClickListener(v -> openCreateForm());
    actions.addView(placeBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView favBtn = makeButton(context, favoritesButtonLabel(), false);
    favBtn.setOnClickListener(v -> navigateTo(new YaRyadomFavoritesController(context, tdlib)));
    LinearLayout.LayoutParams favLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    favLp.leftMargin = Screen.dp(8f);
    actions.addView(favBtn, favLp);
    root.addView(actions);

    ScrollView scroll = new ScrollView(context);
    scroll.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    listContainer = new LinearLayout(context);
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setPadding(pad, 0, pad, pad);
    scroll.addView(listContainer);
    root.addView(scroll);

    renderListings(listings);
    return root;
  }

  @Override
  public void onFocus () {
    super.onFocus();
    // обновить список (звёздочки) после возврата из избранного
    if (listContainer != null) {
      filterListings();
    }
  }

  private String favoritesButtonLabel () {
    int n = YaRyadomFavorites.count();
    return n > 0 ? ("Избранное (" + n + ")") : "Избранное";
  }

  private TextView makeButton (Context context, String text, boolean primary) {
    TextView btn = new TextView(context);
    btn.setText(text);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    btn.setTypeface(Typeface.DEFAULT_BOLD);
    btn.setGravity(Gravity.CENTER);
    btn.setPadding(Screen.dp(16f), Screen.dp(12f), Screen.dp(16f), Screen.dp(12f));

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

  private void filterListings () {
    String q = searchInput != null && searchInput.getText() != null
      ? searchInput.getText().toString().trim().toLowerCase() : "";
    if (q.isEmpty()) {
      renderListings(listings);
      return;
    }
    List<Listing> filtered = new ArrayList<>();
    for (Listing l : listings) {
      if (l.title.toLowerCase().contains(q)
        || l.district.toLowerCase().contains(q)
        || l.meta.toLowerCase().contains(q)
        || l.price.toLowerCase().contains(q)) {
        filtered.add(l);
      }
    }
    renderListings(filtered);
  }

  private void renderListings (List<Listing> data) {
    if (listContainer == null) return;
    listContainer.removeAllViews();
    if (data.isEmpty()) {
      TextView empty = new TextView(context);
      empty.setText("Ничего не найдено");
      empty.setTextColor(Theme.getColor(ColorId.textLight));
      empty.setGravity(Gravity.CENTER);
      empty.setPadding(0, Screen.dp(40f), 0, 0);
      listContainer.addView(empty);
      return;
    }
    for (Listing listing : data) {
      listContainer.addView(createListingCard(context, listing));
    }
  }

  private View createListingCard (Context context, Listing listing) {
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

    TextView price = new TextView(context);
    price.setText(listing.price);
    price.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    price.setTypeface(Typeface.DEFAULT_BOLD);
    price.setTextColor(0xFF0088CC);
    row.addView(price);
    card.addView(row);

    TextView meta = new TextView(context);
    meta.setText(listing.meta + " · " + listing.district);
    meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    meta.setTextColor(Theme.getColor(ColorId.textLight));
    meta.setPadding(0, Screen.dp(6f), 0, Screen.dp(10f));
    card.addView(meta);

    LinearLayout buttons = new LinearLayout(context);
    buttons.setOrientation(LinearLayout.HORIZONTAL);

    boolean fav = YaRyadomFavorites.isFavorite(listing.id);
    TextView starBtn = makeButton(context, fav ? "★ В избранном" : "☆ В избранное", false);
    starBtn.setOnClickListener(v -> {
      YaRyadomFavorites.toggle(new YaRyadomFavorites.Item(
        listing.id, listing.title, listing.price, listing.meta, listing.district,
        listing.authorUsername, listing.authorUserId
      ));
      boolean now = YaRyadomFavorites.isFavorite(listing.id);
      UI.showToast(now ? "Добавлено в избранное" : "Удалено из избранного", Toast.LENGTH_SHORT);
      filterListings();
    });
    buttons.addView(starBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView contactBtn = makeButton(context, "Написать", true);
    contactBtn.setOnClickListener(v -> openChatWithAuthor(listing));
    LinearLayout.LayoutParams contactLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    contactLp.leftMargin = Screen.dp(8f);
    buttons.addView(contactBtn, contactLp);

    card.addView(buttons);
    return card;
  }

  private void openCreateForm () {
    YaRyadomCreateController form = new YaRyadomCreateController(context, tdlib);
    form.setListener((title, price, area, floor, district, description) -> {
      String meta = buildMeta(area, floor);
      String districtVal = StringUtils.isEmpty(district) ? "—" : district;
      String priceVal = price.contains("₽") ? price : (price + " ₽/мес");

      String myUsername = null;
      long myUserId = 0;
      try {
        myUserId = tdlib.myUserId();
        TdApi.User me = tdlib.myUser();
        if (me != null && me.usernames != null) {
          if (!StringUtils.isEmpty(me.usernames.editableUsername)) {
            myUsername = me.usernames.editableUsername;
          } else if (me.usernames.activeUsernames != null && me.usernames.activeUsernames.length > 0) {
            myUsername = me.usernames.activeUsernames[0];
          }
        }
      } catch (Throwable ignored) {
      }

      Listing created = Listing.create(title, priceVal, meta, districtVal, myUsername, myUserId);
      listings.add(0, created);
      renderListings(listings);
      UI.showToast("Объявление опубликовано", Toast.LENGTH_SHORT);
    });
    navigateTo(form);
  }

  private static String buildMeta (String area, String floor) {
    String a = StringUtils.isEmpty(area) ? "—" : (area + " м²");
    String f = StringUtils.isEmpty(floor) ? "—" : (floor + " эт.");
    return a + " · " + f;
  }

  private void openChatWithAuthor (Listing listing) {
    if (tdlib == null) {
      UI.showToast("Telegram не готов", Toast.LENGTH_SHORT);
      return;
    }
    if (listing.authorUserId != 0) {
      tdlib.ui().openPrivateChat(this, listing.authorUserId, null);
      return;
    }
    if (!StringUtils.isEmpty(listing.authorUsername)) {
      String username = listing.authorUsername.startsWith("@")
        ? listing.authorUsername.substring(1) : listing.authorUsername;
      tdlib.ui().openPublicChat(this, username, null);
      return;
    }
    UI.showToast("У объявления нет контакта автора", Toast.LENGTH_SHORT);
  }
}
