/*
 * Telegram X + «Я рядом»
 * Форма создания канала-объявления (YR1 + ChatLocation).
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
  private EditText titleInput, priceInput, roomsInput, areaInput;
  private EditText addressInput, latInput, lngInput, descriptionInput;
  private TextView submitBtn;
  private boolean submitting;

  public YaRyadomCreateController (Context context, Tdlib tdlib) {
    super(context, tdlib);
  }

  public YaRyadomCreateController setListener (OnChannelCreatedListener listener) {
    this.listener = listener;
    return this;
  }

  public YaRyadomCreateController prefillCoords (double lat, double lng) {
    // applied after view create if inputs exist
    this.prefillLat = lat;
    this.prefillLng = lng;
    return this;
  }

  private double prefillLat = Double.NaN, prefillLng = Double.NaN;

  @Override public int getId () { return R.id.controller_yaryadom; }
  @Override protected int getBackButton () { return BackHeaderButton.TYPE_BACK; }
  @Override public CharSequence getName () { return "Разместить объявление"; }

  @Override
  protected View onCreateView (Context context) {
    ScrollView scroll = new ScrollView(context);
    scroll.setLayoutParams(FrameLayoutFix.newParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    scroll.setBackgroundColor(Theme.backgroundColor());
    scroll.setFillViewport(true);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = Screen.dp(16f);
    root.setPadding(pad, pad, pad, pad);

    root.addView(hint(context,
      "Создаётся канал с меткой «YR» и форматом YR1. " +
      "Его можно найти через поиск рядом."));

    root.addView(section(context, "Основное"));
    titleInput = field(context, "Название *", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    root.addView(titleInput);
    priceInput = field(context, "Цена, ₽/мес", InputType.TYPE_CLASS_NUMBER);
    root.addView(priceInput);

    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    roomsInput = field(context, "Комнат", InputType.TYPE_CLASS_NUMBER);
    row.addView(roomsInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    areaInput = field(context, "Площадь, м²", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    aLp.leftMargin = Screen.dp(8f);
    row.addView(areaInput, aLp);
    root.addView(row);

    root.addView(section(context, "Геолокация"));
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

    if (!Double.isNaN(prefillLat)) latInput.setText(String.valueOf(prefillLat));
    if (!Double.isNaN(prefillLng)) lngInput.setText(String.valueOf(prefillLng));

    root.addView(hint(context, "Координаты из карт. Если geo недоступен — канал создастся без метки, данные останутся в описании YR1."));

    root.addView(section(context, "Описание"));
    descriptionInput = field(context, "Условия, этаж, подробности…",
      InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    descriptionInput.setMinLines(3);
    descriptionInput.setMaxLines(6);
    descriptionInput.setSingleLine(false);
    descriptionInput.setGravity(Gravity.TOP | Gravity.START);
    descriptionInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
    root.addView(descriptionInput);

    submitBtn = primaryBtn(context, "Опубликовать");
    submitBtn.setOnClickListener(v -> submit());
    root.addView(submitBtn);

    TextView cancel = secondaryBtn(context, "Отмена");
    cancel.setOnClickListener(v -> navigateBack());
    LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cLp.topMargin = Screen.dp(8f);
    root.addView(cancel, cLp);

    scroll.addView(root);
    return scroll;
  }

  private void submit () {
    if (submitting) return;

    YaRyadomFormat.ListingData d = new YaRyadomFormat.ListingData();
    d.title = text(titleInput);
    d.price = text(priceInput);
    d.rooms = text(roomsInput);
    d.area = text(areaInput);
    d.address = text(addressInput);
    d.description = text(descriptionInput);

    if (StringUtils.isEmpty(d.title)) {
      UI.showToast("Укажите название", Toast.LENGTH_SHORT);
      titleInput.requestFocus();
      return;
    }
    if (StringUtils.isEmpty(d.address)) {
      UI.showToast("Укажите адрес", Toast.LENGTH_SHORT);
      addressInput.requestFocus();
      return;
    }
    try {
      d.lat = Double.parseDouble(text(latInput).replace(',', '.'));
      d.lng = Double.parseDouble(text(lngInput).replace(',', '.'));
    } catch (Exception e) {
      UI.showToast("Укажите широту и долготу", Toast.LENGTH_SHORT);
      return;
    }
    if (d.lat < -90 || d.lat > 90 || d.lng < -180 || d.lng > 180) {
      UI.showToast("Некорректные координаты", Toast.LENGTH_SHORT);
      return;
    }

    submitting = true;
    submitBtn.setEnabled(false);
    submitBtn.setText("Публикация…");

    YaRyadomGeo.createListingChannel(tdlib, d,
      chatId -> {
        submitting = false;
        UI.showToast("Объявление опубликовано", Toast.LENGTH_SHORT);
        if (listener != null) listener.onChannelCreated(chatId, YaRyadomFormat.buildChannelTitle(d));
        tdlib.ui().openChat(this, chatId, null);
      },
      error -> {
        submitting = false;
        submitBtn.setEnabled(true);
        submitBtn.setText("Опубликовать");
        UI.showToast(error, Toast.LENGTH_LONG);
      }
    );
  }

  private static String text (EditText e) {
    return e.getText() != null ? e.getText().toString().trim() : "";
  }

  private TextView section (Context c, String t) {
    TextView v = new TextView(c);
    v.setText(t);
    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    v.setTypeface(Typeface.DEFAULT_BOLD);
    v.setTextColor(Theme.getColor(ColorId.textLight));
    v.setPadding(0, Screen.dp(12f), 0, Screen.dp(6f));
    return v;
  }

  private TextView hint (Context c, String t) {
    TextView v = new TextView(c);
    v.setText(t);
    v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    v.setTextColor(Theme.getColor(ColorId.textLight));
    v.setPadding(0, Screen.dp(4f), 0, Screen.dp(8f));
    return v;
  }

  private EditText field (Context c, String hint, int type) {
    EditText e = new EditText(c);
    e.setHint(hint);
    e.setInputType(type);
    e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    e.setTextColor(Theme.getColor(ColorId.text));
    e.setHintTextColor(Theme.getColor(ColorId.textLight));
    e.setSingleLine(true);
    e.setPadding(Screen.dp(14f), Screen.dp(12f), Screen.dp(14f), Screen.dp(12f));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(10f));
    bg.setColor(Theme.getColor(ColorId.filling));
    e.setBackground(bg);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.bottomMargin = Screen.dp(8f);
    e.setLayoutParams(lp);
    return e;
  }

  private TextView primaryBtn (Context c, String t) {
    TextView b = new TextView(c);
    b.setText(t);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    b.setTypeface(Typeface.DEFAULT_BOLD);
    b.setGravity(Gravity.CENTER);
    b.setTextColor(0xFFFFFFFF);
    b.setPadding(Screen.dp(16f), Screen.dp(14f), Screen.dp(16f), Screen.dp(14f));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(12f));
    bg.setColor(0xFF0088CC);
    b.setBackground(bg);
    return b;
  }

  private TextView secondaryBtn (Context c, String t) {
    TextView b = new TextView(c);
    b.setText(t);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
    b.setGravity(Gravity.CENTER);
    b.setTextColor(Theme.getColor(ColorId.text));
    b.setPadding(Screen.dp(16f), Screen.dp(12f), Screen.dp(16f), Screen.dp(12f));
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(Screen.dp(12f));
    bg.setColor(Theme.getColor(ColorId.filling));
    b.setBackground(bg);
    return b;
  }
}
