/*
 * Telegram X + «Я рядом»
 * Форма: создаёт Telegram-канал с геометкой (ChatLocation).
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

public class YaRyadomCreateController extends ViewController<Void> {

  public interface OnChannelCreatedListener {
    void onChannelCreated (long chatId, String title);
  }

  private OnChannelCreatedListener listener;

  private EditText titleInput;
  private EditText priceInput;
  private EditText addressInput;
  private EditText latInput;
  private EditText lngInput;
  private EditText descriptionInput;
  private TextView submitBtn;
  private boolean submitting;

  public YaRyadomCreateController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  public YaRyadomCreateController setListener (OnChannelCreatedListener listener) {
    this.listener = listener;
    return this;
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

    root.addView(sectionLabel(context, "Объявление станет каналом Telegram с геометкой"));

    root.addView(sectionLabel(context, "Основное"));
    titleInput = field(context, "Название *", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    root.addView(titleInput);

    priceInput = field(context, "Цена, ₽/мес", InputType.TYPE_CLASS_NUMBER);
    root.addView(priceInput);

    root.addView(sectionLabel(context, "Геолокация"));
    addressInput = field(context, "Адрес / район *", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    root.addView(addressInput);

    LinearLayout coords = new LinearLayout(context);
    coords.setOrientation(LinearLayout.HORIZONTAL);
    latInput = field(context, "Широта *", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    coords.addView(latInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    lngInput = field(context, "Долгота *", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    LinearLayout.LayoutParams lngLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    lngLp.leftMargin = Screen.dp(8f);
    coords.addView(lngInput, lngLp);
    root.addView(coords);

    // Подсказка с примером координат (Таллинн по умолчанию можно не ставить)
    TextView geoHint = new TextView(context);
    geoHint.setText("Координаты можно скопировать из карт. Пример: 59.437 24.753");
    geoHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    geoHint.setTextColor(Theme.getColor(ColorId.textLight));
    geoHint.setPadding(0, 0, 0, Screen.dp(8f));
    root.addView(geoHint);

    root.addView(sectionLabel(context, "Описание"));
    descriptionInput = field(context, "Комнаты, площадь, условия…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    descriptionInput.setMinLines(3);
    descriptionInput.setMaxLines(6);
    descriptionInput.setSingleLine(false);
    descriptionInput.setGravity(Gravity.TOP | Gravity.START);
    descriptionInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
    root.addView(descriptionInput);

    TextView hint = new TextView(context);
    hint.setText("Будет создан канал с геолокацией. Его можно найти через поиск «рядом».");
    hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    hint.setTextColor(Theme.getColor(ColorId.textLight));
    hint.setPadding(0, Screen.dp(8f), 0, Screen.dp(16f));
    root.addView(hint);

    submitBtn = makePrimaryButton(context, "Создать канал-объявление");
    submitBtn.setOnClickListener(v -> submitForm());
    root.addView(submitBtn);

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
    if (submitting) return;

    String title = textOf(titleInput);
    String price = textOf(priceInput);
    String address = textOf(addressInput);
    String latStr = textOf(latInput);
    String lngStr = textOf(lngInput);
    String description = textOf(descriptionInput);

    if (StringUtils.isEmpty(title)) {
      UI.showToast("Укажите название", Toast.LENGTH_SHORT);
      titleInput.requestFocus();
      return;
    }
    if (StringUtils.isEmpty(address)) {
      UI.showToast("Укажите адрес", Toast.LENGTH_SHORT);
      addressInput.requestFocus();
      return;
    }

    double lat, lng;
    try {
      lat = Double.parseDouble(latStr.replace(',', '.'));
      lng = Double.parseDouble(lngStr.replace(',', '.'));
    } catch (Exception e) {
      UI.showToast("Укажите широту и долготу", Toast.LENGTH_SHORT);
      return;
    }

    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      UI.showToast("Некорректные координаты", Toast.LENGTH_SHORT);
      return;
    }

    StringBuilder desc = new StringBuilder();
    if (!StringUtils.isEmpty(price)) {
      desc.append(price.contains("₽") ? price : price + " ₽/мес");
      desc.append('\n');
    }
    if (!StringUtils.isEmpty(description)) {
      desc.append(description);
    }
    if (desc.length() == 0) {
      desc.append("Объявление «Я рядом»");
    }

    submitting = true;
    submitBtn.setEnabled(false);
    submitBtn.setText("Создание…");

    YaRyadomGeo.createListingChannel(tdlib, title, desc.toString(), lat, lng, address,
      chatId -> {
        submitting = false;
        UI.showToast("Канал создан", Toast.LENGTH_SHORT);
        if (listener != null) listener.onChannelCreated(chatId, title);
        // Открыть созданный канал
        tdlib.ui().openChat(this, chatId, null);
      },
      error -> {
        submitting = false;
        submitBtn.setEnabled(true);
        submitBtn.setText("Создать канал-объявление");
        UI.showToast("Ошибка: " + error, Toast.LENGTH_LONG);
      }
    );
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
