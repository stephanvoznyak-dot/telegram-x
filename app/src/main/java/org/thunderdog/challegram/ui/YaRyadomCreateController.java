/*
 * Telegram X + «Я рядом»
 *
 * Форма размещения объявления об аренде жилья.
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
import android.view.inputmethod.EditorInfo;
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

import me.vkryl.android.widget.FrameLayoutFix;
import me.vkryl.core.StringUtils;

/**
 * Форма создания объявления «Я рядом».
 */
public class YaRyadomCreateController extends ViewController<Void> {

  public interface OnListingCreatedListener {
    void onListingCreated (String title, String price, String area, String floor, String district, String description);
  }

  private OnListingCreatedListener listener;

  private EditText titleInput;
  private EditText priceInput;
  private EditText areaInput;
  private EditText floorInput;
  private EditText districtInput;
  private EditText descriptionInput;

  public YaRyadomCreateController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  public YaRyadomCreateController setListener (OnListingCreatedListener listener) {
    this.listener = listener;
    return this;
  }

  @Override
  public int getId () {
    return R.id.controller_yaryadom; // можно завести отдельный id позже
  }

  @Override
  protected int getBackButton () {
    return BackHeaderButton.TYPE_BACK;
  }

  @Override
  public CharSequence getName () {
    return "Разместить объявление";
  }

  @Override
  protected View onCreateView (Context context) {
    ScrollView scroll = new ScrollView(context);
    scroll.setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    ));
    scroll.setBackgroundColor(Theme.backgroundColor());
    scroll.setFillViewport(true);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = Screen.dp(16f);
    root.setPadding(pad, pad, pad, pad);

    root.addView(sectionLabel(context, "Основное"));
    titleInput = field(context, "Заголовок *", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    root.addView(titleInput);

    priceInput = field(context, "Цена, ₽/мес *", InputType.TYPE_CLASS_NUMBER);
    root.addView(priceInput);

    root.addView(sectionLabel(context, "Параметры"));

    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);

    areaInput = field(context, "Площадь, м²", InputType.TYPE_CLASS_NUMBER);
    row.addView(areaInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

    floorInput = field(context, "Этаж", InputType.TYPE_CLASS_TEXT);
    LinearLayout.LayoutParams floorLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    floorLp.leftMargin = Screen.dp(8f);
    row.addView(floorInput, floorLp);
    root.addView(row);

    districtInput = field(context, "Район / метро", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    root.addView(districtInput);

    root.addView(sectionLabel(context, "Описание"));
    descriptionInput = field(context, "Опишите жильё, условия…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    descriptionInput.setMinLines(3);
    descriptionInput.setMaxLines(6);
    descriptionInput.setSingleLine(false);
    descriptionInput.setGravity(Gravity.TOP | Gravity.START);
    descriptionInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
    root.addView(descriptionInput);

    TextView hint = new TextView(context);
    hint.setText("После публикации с вами смогут связаться через Telegram.");
    hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    hint.setTextColor(Theme.getColor(ColorId.textLight));
    hint.setPadding(0, Screen.dp(8f), 0, Screen.dp(16f));
    root.addView(hint);

    TextView submit = makePrimaryButton(context, "Опубликовать");
    submit.setOnClickListener(v -> submitForm());
    root.addView(submit);

    TextView cancel = makeSecondaryButton(context, "Отмена");
    cancel.setOnClickListener(v -> navigateBack());
    LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cancelLp.topMargin = Screen.dp(8f);
    root.addView(cancel, cancelLp);

    scroll.addView(root);
    return scroll;
  }

  private void submitForm () {
    String title = textOf(titleInput);
    String price = textOf(priceInput);
    String area = textOf(areaInput);
    String floor = textOf(floorInput);
    String district = textOf(districtInput);
    String description = textOf(descriptionInput);

    if (StringUtils.isEmpty(title)) {
      UI.showToast("Укажите заголовок", Toast.LENGTH_SHORT);
      titleInput.requestFocus();
      return;
    }
    if (StringUtils.isEmpty(price)) {
      UI.showToast("Укажите цену", Toast.LENGTH_SHORT);
      priceInput.requestFocus();
      return;
    }

    if (listener != null) {
      listener.onListingCreated(title, price, area, floor, district, description);
    } else {
      UI.showToast("Объявление подготовлено (нет обработчика)", Toast.LENGTH_SHORT);
    }

    navigateBack();
  }

  private static String textOf (EditText edit) {
    return edit.getText() != null ? edit.getText().toString().trim() : "";
  }

  private TextView sectionLabel (Context context, String text) {
    TextView label = new TextView(context);
    label.setText(text);
    label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    label.setTypeface(Typeface.DEFAULT_BOLD);
    label.setTextColor(Theme.getColor(ColorId.textLight));
    label.setPadding(0, Screen.dp(12f), 0, Screen.dp(6f));
    return label;
  }

  private EditText field (Context context, String hint, int inputType) {
    EditText edit = new EditText(context);
    edit.setHint(hint);
    edit.setInputType(inputType);
    edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    edit.setTextColor(Theme.getColor(ColorId.text));
    edit.setHintTextColor(Theme.getColor(ColorId.textLight));
    edit.setSingleLine(true);
    edit.setPadding(Screen.dp(14f), Screen.dp(12f), Screen.dp(14f), Screen.dp(12f));

    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(10f));
    bg.setColor(Theme.getColor(ColorId.filling));
    edit.setBackground(bg);

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.bottomMargin = Screen.dp(8f);
    edit.setLayoutParams(lp);
    return edit;
  }

  private TextView makePrimaryButton (Context context, String text) {
    TextView btn = new TextView(context);
    btn.setText(text);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    btn.setTypeface(Typeface.DEFAULT_BOLD);
    btn.setGravity(Gravity.CENTER);
    btn.setTextColor(0xFFFFFFFF);
    btn.setPadding(Screen.dp(16f), Screen.dp(14f), Screen.dp(16f), Screen.dp(14f));

    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(12f));
    bg.setColor(0xFF0088CC);
    btn.setBackground(bg);
    return btn;
  }

  private TextView makeSecondaryButton (Context context, String text) {
    TextView btn = new TextView(context);
    btn.setText(text);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
    btn.setGravity(Gravity.CENTER);
    btn.setTextColor(Theme.getColor(ColorId.text));
    btn.setPadding(Screen.dp(16f), Screen.dp(12f), Screen.dp(16f), Screen.dp(12f));

    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(12f));
    bg.setColor(Theme.getColor(ColorId.filling));
    btn.setBackground(bg);
    return btn;
  }
}
