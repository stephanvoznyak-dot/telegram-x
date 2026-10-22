/*
 * Telegram X + «Я рядом»
 * Экран избранных объявлений.
 */
package org.thunderdog.challegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.StringUtils;

public class YaRyadomFavoritesController extends ViewController<Void> {

  private LinearLayout listContainer;

  public YaRyadomFavoritesController (Context context, Tdlib tdlib) {
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
    return "Избранное";
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

    ScrollView scroll = new ScrollView(context);
    scroll.setLayoutParams(new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    listContainer = new LinearLayout(context);
    listContainer.setOrientation(LinearLayout.VERTICAL);
    int pad = Screen.dp(12f);
    listContainer.setPadding(pad, pad, pad, pad);
    scroll.addView(listContainer);
    root.addView(scroll);

    render();
    return root;
  }

  @Override
  public void onFocus () {
    super.onFocus();
    if (listContainer != null) {
      render();
    }
  }

  private void render () {
    listContainer.removeAllViews();
    List<YaRyadomFavorites.Item> items = YaRyadomFavorites.getAll();

    if (items.isEmpty()) {
      TextView empty = new TextView(context);
      empty.setText("Пока ничего нет.\nДобавляйте объявления сердечком ★");
      empty.setTextColor(Theme.getColor(ColorId.textLight));
      empty.setGravity(Gravity.CENTER);
      empty.setPadding(0, Screen.dp(48f), 0, 0);
      empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
      listContainer.addView(empty);
      return;
    }

    for (YaRyadomFavorites.Item item : items) {
      listContainer.addView(createCard(item));
    }
  }

  private View createCard (YaRyadomFavorites.Item item) {
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
    title.setText(item.title);
    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    title.setTypeface(Typeface.DEFAULT_BOLD);
    title.setTextColor(Theme.getColor(ColorId.text));
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView price = new TextView(context);
    price.setText(item.price);
    price.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    price.setTypeface(Typeface.DEFAULT_BOLD);
    price.setTextColor(0xFF0088CC);
    row.addView(price);
    card.addView(row);

    TextView meta = new TextView(context);
    meta.setText(item.meta + " · " + item.district);
    meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    meta.setTextColor(Theme.getColor(ColorId.textLight));
    meta.setPadding(0, Screen.dp(6f), 0, Screen.dp(10f));
    card.addView(meta);

    LinearLayout buttons = new LinearLayout(context);
    buttons.setOrientation(LinearLayout.HORIZONTAL);

    TextView writeBtn = makeBtn("Написать", true);
    writeBtn.setOnClickListener(v -> openChat(item));
    buttons.addView(writeBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    TextView removeBtn = makeBtn("Убрать", false);
    removeBtn.setOnClickListener(v -> {
      YaRyadomFavorites.remove(item.id);
      UI.showToast("Удалено из избранного", Toast.LENGTH_SHORT);
      render();
    });
    LinearLayout.LayoutParams removeLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    removeLp.leftMargin = Screen.dp(8f);
    buttons.addView(removeBtn, removeLp);

    card.addView(buttons);
    return card;
  }

  private TextView makeBtn (String text, boolean primary) {
    TextView btn = new TextView(context);
    btn.setText(text);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    btn.setTypeface(Typeface.DEFAULT_BOLD);
    btn.setGravity(Gravity.CENTER);
    btn.setPadding(Screen.dp(12f), Screen.dp(10f), Screen.dp(12f), Screen.dp(10f));

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

  private void openChat (YaRyadomFavorites.Item item) {
    if (tdlib == null) {
      UI.showToast("Telegram не готов", Toast.LENGTH_SHORT);
      return;
    }
    if (item.authorUserId != 0) {
      tdlib.ui().openPrivateChat(this, item.authorUserId, null);
      return;
    }
    if (!StringUtils.isEmpty(item.authorUsername)) {
      String username = item.authorUsername.startsWith("@")
        ? item.authorUsername.substring(1) : item.authorUsername;
      tdlib.ui().openPublicChat(this, username, null);
      return;
    }
    UI.showToast("Нет контакта автора", Toast.LENGTH_SHORT);
  }
}
