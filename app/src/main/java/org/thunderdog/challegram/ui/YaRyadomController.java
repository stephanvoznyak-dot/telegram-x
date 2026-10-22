/*
 * Telegram X + «Я рядом»
 *
 * Базовый экран сервиса аренды жилья внутри клиента Telegram X.
 */
package org.thunderdog.challegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.thunderdog.challegram.R;
import org.thunderdog.challegram.navigation.BackHeaderButton;
import org.thunderdog.challegram.navigation.ViewController;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.ColorId;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Screen;

import me.vkryl.android.widget.FrameLayoutFix;

/**
 * Экран «Я рядом» — точка входа сервиса аренды жилья.
 */
public class YaRyadomController extends ViewController<Void> {

  public YaRyadomController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    // После добавления в ids.xml замените на R.id.controller_yaryadom
    return R.id.controller_settings;
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
    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER);
    content.setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    ));
    content.setBackgroundColor(Theme.backgroundColor());
    content.setPadding(Screen.dp(24f), Screen.dp(24f), Screen.dp(24f), Screen.dp(24f));

    TextView title = new TextView(context);
    title.setText("Я рядом");
    title.setTextSize(28f);
    title.setTextColor(Theme.getColor(ColorId.text));
    title.setGravity(Gravity.CENTER);
    content.addView(title);

    TextView subtitle = new TextView(context);
    subtitle.setText("Сервис аренды жилья");
    subtitle.setTextSize(16f);
    subtitle.setTextColor(Theme.getColor(ColorId.textLight));
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setPadding(0, Screen.dp(12f), 0, Screen.dp(32f));
    content.addView(subtitle);

    TextView hint = new TextView(context);
    hint.setText("Здесь появится:\n• поиск объявлений\n• размещение жилья\n• заявки и чаты");
    hint.setTextSize(15f);
    hint.setTextColor(Theme.getColor(ColorId.textLight));
    hint.setGravity(Gravity.CENTER);
    content.addView(hint);

    return content;
  }
}
