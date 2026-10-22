/*
 * This file is a part of Telegram X + «Я рядом»
 *
 * Простой контроллер-заглушка для раздела «Я рядом».
 * В дальнейшем здесь будет список объявлений, поиск и размещение жилья.
 */
package org.thunderdog.challegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
 * Экран «Я рядом» — точка входа сервиса аренды жилья внутри Telegram X.
 */
public class YaRyadomController extends ViewController<Void> {

  public YaRyadomController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  @Override
  public int getId () {
    // Используем существующий id-ресурс или временное значение.
    // После добавления id в ids.xml можно заменить на R.id.controller_yaryadom
    return R.id.controller_settings; // временно, чтобы не требовать новый id
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
    FrameLayoutFix content = new FrameLayoutFix(context);
    content.setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    ));
    content.setBackgroundColor(Theme.backgroundColor());

    TextView title = new TextView(context);
    title.setText("Я рядом");
    title.setTextSize(24f);
    title.setTextColor(Theme.getColor(ColorId.text));
    title.setGravity(Gravity.CENTER);

    TextView subtitle = new TextView(context);
    subtitle.setText("Сервис аренды жилья\n(раздел в разработке)");
    subtitle.setTextSize(16f);
    subtitle.setTextColor(Theme.getColor(ColorId.textLight));
    subtitle.setGravity(Gravity.CENTER);
    subtitle.setPadding(0, Screen.dp(40f), 0, 0);

    FrameLayoutFix.LayoutParams lp = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.CENTER
    );
    content.addView(title, lp);

    FrameLayoutFix.LayoutParams lp2 = FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
      Gravity.CENTER
    );
    lp2.topMargin = Screen.dp(40f);
    content.addView(subtitle, lp2);

    return content;
  }
}
