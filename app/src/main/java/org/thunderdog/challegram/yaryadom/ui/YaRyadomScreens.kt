package org.thunderdog.challegram.yaryadom.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.thunderdog.challegram.yaryadom.data.models.Categories
import org.thunderdog.challegram.yaryadom.data.models.MineItem
import org.thunderdog.challegram.yaryadom.data.models.NearbyItem
import org.thunderdog.challegram.yaryadom.data.models.Radii

/**
 * UI-экраны модуля «Я рядом».
 *
 * Используют стандартные Android View с адаптацией под светлую/тёмную тему.
 * При глубокой интеграции в Telegram X рекомендуется заменить
 * на компоненты TGX (CustomTextView, Theme-aware colors, etc.).
 */
object YaRyadomScreens {

    // -------------------------------------------------------------------------
    // Theme helpers
    // -------------------------------------------------------------------------

    private data class Theme(
        val bg: Int,
        val cardBg: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accent: Int,
        val accentText: Int,
        val divider: Int,
        val secondaryBtn: Int
    )

    private fun theme(context: Context): Theme {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        return if (night) {
            Theme(
                bg = Color.parseColor("#0F0F0F"),
                cardBg = Color.parseColor("#1C1C1E"),
                textPrimary = Color.parseColor("#FFFFFF"),
                textSecondary = Color.parseColor("#8E8E93"),
                accent = Color.parseColor("#2AABEE"),          // Telegram blue
                accentText = Color.WHITE,
                divider = Color.parseColor("#2C2C2E"),
                secondaryBtn = Color.parseColor("#2C2C2E")
            )
        } else {
            Theme(
                bg = Color.parseColor("#F2F2F7"),
                cardBg = Color.WHITE,
                textPrimary = Color.parseColor("#000000"),
                textSecondary = Color.parseColor("#6D6D72"),
                accent = Color.parseColor("#2AABEE"),
                accentText = Color.WHITE,
                divider = Color.parseColor("#E5E5EA"),
                secondaryBtn = Color.parseColor("#E5E5EA")
            )
        }
    }

    // -------------------------------------------------------------------------
    // Public screens
    // -------------------------------------------------------------------------

    fun createHomeView(
        context: Context,
        onNeedClick: () -> Unit,
        onCanClick: () -> Unit,
        onMyOrdersClick: () -> Unit
    ): View {
        val t = theme(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.bg)
            setPadding(dp(context, 24), dp(context, 40), dp(context, 24), dp(context, 24))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(context).apply {
            text = "Я рядом"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(t.textPrimary)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(context).apply {
            text = "Локальные заявки рядом с вами"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(t.textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 8), 0, dp(context, 36))
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(makePrimaryButton(context, t, "Мне нужно", onNeedClick))
        layout.addView(space(context, 14))
        layout.addView(makePrimaryButton(context, t, "Я могу", onCanClick))
        layout.addView(space(context, 14))
        layout.addView(makeSecondaryButton(context, t, "Мои заявки", onMyOrdersClick))

        return ScrollView(context).apply {
            setBackgroundColor(t.bg)
            addView(layout)
        }
    }

    fun createOrderFormView(
        context: Context,
        onSubmit: (category: String, description: String, destination: String?, radius: Int, expires: Int) -> Unit,
        onBack: () -> Unit
    ): View {
        val t = theme(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.bg)
            setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 24))
        }

        layout.addView(makeHeader(context, t, "Новая заявка", onBack))

        // Категория
        layout.addView(makeLabel(context, t, "Категория"))
        val categorySpinner = Spinner(context)
        val categoryAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            Categories.ALL.map { it.second }
        )
        categorySpinner.adapter = categoryAdapter
        layout.addView(categorySpinner)
        layout.addView(space(context, 16))

        // Описание
        layout.addView(makeLabel(context, t, "Описание"))
        val description = EditText(context).apply {
            hint = "Что нужно сделать?"
            setTextColor(t.textPrimary)
            setHintTextColor(t.textSecondary)
            minLines = 3
            setBackgroundColor(t.cardBg)
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
        }
        layout.addView(description)
        layout.addView(space(context, 16))

        // Куда (опционально)
        layout.addView(makeLabel(context, t, "Куда (необязательно)")
        val destination = EditText(context).apply {
            hint = "Адрес или ориентир"
            setTextColor(t.textPrimary)
            setHintTextColor(t.textSecondary)
            setBackgroundColor(t.cardBg)
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
        }
        layout.addView(destination)
        layout.addView(space(context, 16))

        // Радиус
        layout.addView(makeLabel(context, t, "Радиус поиска"))
        val radiusSpinner = Spinner(context)
        val radiusAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            Radii.ALL.map { it.second }
        )
        radiusSpinner.adapter = radiusAdapter
        radiusSpinner.setSelection(2) // 5 км по умолчанию
        layout.addView(radiusSpinner)
        layout.addView(space(context, 16))

        // Срок
        layout.addView(makeLabel(context, t, "Срок действия (минуты)")
        val expires = EditText(context).apply {
            setText("30")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(t.textPrimary)
            setBackgroundColor(t.cardBg)
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
        }
        layout.addView(expires)
        layout.addView(space(context, 28))

        layout.addView(makePrimaryButton(context, t, "Создать заявку") {
            val catCode = Categories.ALL.getOrNull(categorySpinner.selectedItemPosition)?.first ?: "OTHER"
            val desc = description.text.toString().trim()
            if (desc.length < 3) {
                Toast.makeText(context, "Описание слишком короткое", Toast.LENGTH_SHORT).show()
                return@makePrimaryButton
            }
            val dest = destination.text.toString().trim().ifEmpty { null }
            val rad = Radii.ALL.getOrNull(radiusSpinner.selectedItemPosition)?.first ?: 5000
            val exp = expires.text.toString().toIntOrNull()?.coerceIn(5, 1440) ?: 30
            onSubmit(catCode, desc, dest, rad, exp)
        })

        return ScrollView(context).apply {
            setBackgroundColor(t.bg)
            addView(layout)
        }
    }

    fun createNearbyListView(
        context: Context,
        items: List<NearbyItem>,
        onTake: (String) -> Unit,
        onRefresh: () -> Unit,
        onBack: () -> Unit
    ): View {
        val t = theme(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.bg)
        }

        root.addView(makeHeader(context, t, "Рядом со мной", onBack, onRefresh))

        if (items.isEmpty()) {
            val empty = TextView(context).apply {
                text = "Нет открытых заявок в выбранном радиусе"
                setTextColor(t.textSecondary)
                gravity = Gravity.CENTER
                setPadding(dp(context, 24), dp(context, 48), dp(context, 24), 0)
            }
            root.addView(empty)
            return root
        }

        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 16))
        }

        for (item in items) {
            list.addView(makeOrderCard(context, t, item, showTake = true) {
                onTake(item.id)
            })
            list.addView(space(context, 10))
        }

        val scroll = ScrollView(context).apply {
            setBackgroundColor(t.bg)
            addView(list)
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        return root
    }

    fun createMyOrdersView(
        context: Context,
        items: List<MineItem>,
        onComplete: (String) -> Unit,
        onBack: () -> Unit
    ): View {
        val t = theme(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.bg)
        }

        root.addView(makeHeader(context, t, "Мои заявки", onBack))

        if (items.isEmpty()) {
            val empty = TextView(context).apply {
                text = "У вас пока нет взятых заявок"
                setTextColor(t.textSecondary)
                gravity = Gravity.CENTER
                setPadding(dp(context, 24), dp(context, 48), dp(context, 24), 0)
            }
            root.addView(empty)
            return root
        }

        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 16))
        }

        for (item in items) {
            list.addView(makeMineCard(context, t, item) {
                onComplete(item.id)
            })
            list.addView(space(context, 10))
        }

        val scroll = ScrollView(context).apply {
            setBackgroundColor(t.bg)
            addView(list)
        }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        return root
    }

    // -------------------------------------------------------------------------
    // Building blocks
    // -------------------------------------------------------------------------

    private fun makeHeader(
        context: Context,
        t: Theme,
        title: String,
        onBack: () -> Unit,
        onAction: (() -> Unit)? = null
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 8), dp(context, 12), dp(context, 8), dp(context, 12))
            setBackgroundColor(t.cardBg)
        }

        val back = TextView(context).apply {
            text = "←"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(t.accent)
            setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4))
            setOnClickListener { onBack() }
        }

        val titleView = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(t.textPrimary)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(back)
        row.addView(titleView)

        if (onAction != null) {
            val action = TextView(context).apply {
                text = "↻"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTextColor(t.accent)
                setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4))
                setOnClickListener { onAction() }
            }
            row.addView(action)
        }

        return row
    }

    private fun makeOrderCard(
        context: Context,
        t: Theme,
        item: NearbyItem,
        showTake: Boolean,
        onAction: () -> Unit
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(t.cardBg, dp(context, 12).toFloat())
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
        }

        val cat = TextView(context).apply {
            text = Categories.label(item.category)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.accent)
            setTypeface(null, Typeface.BOLD)
        }

        val desc = TextView(context).apply {
            text = item.description
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(t.textPrimary)
            setPadding(0, dp(context, 4), 0, dp(context, 6))
        }

        val meta = TextView(context).apply {
            val dist = item.distanceMeters?.let { "${it} м · " } ?: ""
            text = "$dist${item.creatorName}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.textSecondary)
        }

        card.addView(cat)
        card.addView(desc)
        card.addView(meta)

        if (showTake) {
            card.addView(space(context, 12))
            card.addView(makePrimaryButton(context, t, "Взять заявку", onAction))
        }

        return card
    }

    private fun makeMineCard(
        context: Context,
        t: Theme,
        item: MineItem,
        onComplete: () -> Unit
    ): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(t.cardBg, dp(context, 12).toFloat())
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
        }

        val cat = TextView(context).apply {
            text = Categories.label(item.category)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.accent)
            setTypeface(null, Typeface.BOLD)
        }

        val desc = TextView(context).apply {
            text = item.description
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(t.textPrimary)
            setPadding(0, dp(context, 4), 0, dp(context, 6))
        }

        val meta = TextView(context).apply {
            val uname = item.creatorUsername?.let { " @$it" } ?: ""
            text = "${item.creatorName}$uname · ${item.status}"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.textSecondary)
        }

        card.addView(cat)
        card.addView(desc)
        card.addView(meta)

        if (item.status == "TAKEN") {
            card.addView(space(context, 12))
            card.addView(makePrimaryButton(context, t, "Завершить", onComplete))
        }

        return card
    }

    private fun makePrimaryButton(context: Context, t: Theme, text: String, onClick: () -> Unit): View {
        return TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(null, Typeface.BOLD)
            setTextColor(t.accentText)
            gravity = Gravity.CENTER
            background = roundRect(t.accent, dp(context, 10).toFloat())
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            setOnClickListener { onClick() }
        }
    }

    private fun makeSecondaryButton(context: Context, t: Theme, text: String, onClick: () -> Unit): View {
        return TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(t.textPrimary)
            gravity = Gravity.CENTER
            background = roundRect(t.secondaryBtn, dp(context, 10).toFloat())
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            setOnClickListener { onClick() }
        }
    }

    private fun makeLabel(context: Context, t: Theme, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.textSecondary)
            setPadding(0, 0, 0, dp(context, 6))
        }
    }

    private fun space(context: Context, dp: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, dp)
            )
        }
    }

    private fun roundRect(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
