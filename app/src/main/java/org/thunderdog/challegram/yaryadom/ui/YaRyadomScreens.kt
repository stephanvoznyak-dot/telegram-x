package org.thunderdog.challegram.yaryadom.ui

import android.content.Context
import android.graphics.Typeface
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
 * Простые UI-экраны модуля «Я рядом».
 * Используют стандартные Android View (без зависимости от внутренних View Telegram X).
 * При интеграции можно заменить на View Telegram X (CustomTextView и т.д.) для полного соответствия стилю.
 */
object YaRyadomScreens {

    fun createHomeView(
        context: Context,
        onNeedClick: () -> Unit,
        onCanClick: () -> Unit,
        onMyOrdersClick: () -> Unit
    ): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 24), dp(context, 32), dp(context, 24), dp(context, 24))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(context).apply {
            text = "Я рядом"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(context).apply {
            text = "Заявки рядом с вами"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 8), 0, dp(context, 32))
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(makeButton(context, "Мне нужно", onNeedClick))
        layout.addView(space(context, 12))
        layout.addView(makeButton(context, "Я могу", onCanClick))
        layout.addView(space(context, 12))
        layout.addView(makeButton(context, "Мои заявки", onMyOrdersClick, secondary = true))

        return ScrollView(context).apply { addView(layout) }
    }

    fun createOrderFormView(
        context: Context,
        onSubmit: (category: String, description: String, destination: String?, radius: Int, expires: Int) -> Unit,
        onBack: () -> Unit
    ): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 24))
        }

        layout.addView(makeTitle(context, "Новая заявка"))

        layout.addView(makeLabel(context, "Категория"))
        val categorySpinner = Spinner(context)
        val categoryAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            Categories.ALL.map { it.second }
        )
        categorySpinner.adapter = categoryAdapter
        layout.addView(categorySpinner)
        layout.addView(space(context, 16))

        layout.addView(makeLabel(context, "Что нужно"))
        val description = EditText(context).apply {
            hint = "Кратко опишите задачу"
            minLines = 3
            setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
        }
        layout.addView(description)
        layout.addView(space(context, 16))

        layout.addView(makeLabel(context, "Куда / адрес (необязательно)"))
        val destination = EditText(context).apply {
            hint = "Например: метро Сокольники"
            setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
        }
        layout.addView(destination)
        layout.addView(space(context, 16))

        layout.addView(makeLabel(context, "Радиус поиска"))
        val radiusSpinner = Spinner(context)
        radiusSpinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            Radii.ALL.map { it.second }
        )
        radiusSpinner.setSelection(2)
        layout.addView(radiusSpinner)
        layout.addView(space(context, 16))

        layout.addView(makeLabel(context, "Актуально (минуты)"))
        val expiresSpinner = Spinner(context)
        val expiresOptions = listOf(15, 30, 60, 120, 240, 480)
        expiresSpinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            expiresOptions.map { "$it мин" }
        )
        expiresSpinner.setSelection(1)
        layout.addView(expiresSpinner)
        layout.addView(space(context, 24))

        layout.addView(makeButton(context, "Создать заявку") {
            val catCode = Categories.ALL[categorySpinner.selectedItemPosition].first
            val desc = description.text.toString().trim()
            val dest = destination.text.toString().trim().ifEmpty { null }
            val radius = Radii.ALL[radiusSpinner.selectedItemPosition].first
            val expires = expiresOptions[expiresSpinner.selectedItemPosition]
            if (desc.length < 3) {
                Toast.makeText(context, "Опишите, что нужно (минимум 3 символа)", Toast.LENGTH_SHORT).show()
                return@makeButton
            }
            onSubmit(catCode, desc, dest, radius, expires)
        })
        layout.addView(space(context, 12))
        layout.addView(makeButton(context, "Назад", onBack, secondary = true))

        return ScrollView(context).apply { addView(layout) }
    }

    fun createNearbyListView(
        context: Context,
        items: List<NearbyItem>,
        onTake: (String) -> Unit,
        onRefresh: () -> Unit,
        onBack: () -> Unit
    ): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
        }

        layout.addView(makeTitle(context, "Рядом с вами"))

        if (items.isEmpty()) {
            layout.addView(TextView(context).apply {
                text = "Нет открытых заявок рядом.\nПопробуйте увеличить радиус или обновить."
                setPadding(0, dp(context, 24), 0, dp(context, 24))
                gravity = Gravity.CENTER
            })
        } else {
            items.forEach { item ->
                layout.addView(createOrderCard(context, item, onTake))
                layout.addView(space(context, 10))
            }
        }

        layout.addView(space(context, 16))
        layout.addView(makeButton(context, "Обновить", onRefresh))
        layout.addView(space(context, 8))
        layout.addView(makeButton(context, "Назад", onBack, secondary = true))

        return ScrollView(context).apply { addView(layout) }
    }

    fun createMyOrdersView(
        context: Context,
        items: List<MineItem>,
        onComplete: (String) -> Unit,
        onBack: () -> Unit
    ): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
        }

        layout.addView(makeTitle(context, "Мои заявки"))

        if (items.isEmpty()) {
            layout.addView(TextView(context).apply {
                text = "У вас нет активных взятых заявок."
                setPadding(0, dp(context, 24), 0, dp(context, 24))
                gravity = Gravity.CENTER
            })
        } else {
            items.forEach { item ->
                val card = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14))
                    setBackgroundColor(0xFFF5F5F5.toInt())
                }
                card.addView(TextView(context).apply {
                    text = Categories.label(item.category)
                    setTypeface(null, Typeface.BOLD)
                })
                card.addView(TextView(context).apply {
                    text = item.description
                    setPadding(0, dp(context, 6), 0, 0)
                })
                item.destinationText?.let {
                    card.addView(TextView(context).apply {
                        text = "→ $it"
                        setPadding(0, dp(context, 4), 0, 0)
                    })
                }
                card.addView(TextView(context).apply {
                    text = "Заказчик: ${item.creatorName}${item.creatorUsername?.let { " @$it" } ?: ""}"
                    setPadding(0, dp(context, 6), 0, 0)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                })
                card.addView(space(context, 10))
                card.addView(makeButton(context, "Завершить") { onComplete(item.id) })
                layout.addView(card)
                layout.addView(space(context, 10))
            }
        }

        layout.addView(space(context, 16))
        layout.addView(makeButton(context, "Назад", onBack, secondary = true))

        return ScrollView(context).apply { addView(layout) }
    }

    private fun createOrderCard(context: Context, item: NearbyItem, onTake: (String) -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14))
            setBackgroundColor(0xFFF5F5F5.toInt())

            addView(TextView(context).apply {
                text = Categories.label(item.category)
                setTypeface(null, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = item.description
                setPadding(0, dp(context, 6), 0, 0)
            })
            item.destinationText?.let {
                addView(TextView(context).apply {
                    text = "→ $it"
                    setPadding(0, dp(context, 4), 0, 0)
                })
            }
            val dist = item.distanceMeters?.let {
                if (it < 1000) "$it м" else String.format("%.1f км", it / 1000.0)
            } ?: ""
            addView(TextView(context).apply {
                text = "$dist · ${item.creatorName}"
                setPadding(0, dp(context, 6), 0, 0)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            })
            addView(space(context, 10))
            addView(makeButton(context, "Взять") { onTake(item.id) })
        }
    }

    private fun makeTitle(context: Context, text: String) = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        setTypeface(null, Typeface.BOLD)
        setPadding(0, 0, 0, dp(context, 20))
    }

    private fun makeLabel(context: Context, text: String) = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(0, 0, 0, dp(context, 6))
    }

    private fun makeButton(
        context: Context,
        text: String,
        onClick: () -> Unit,
        secondary: Boolean = false
    ): Button {
        return Button(context).apply {
            this.text = text
            setOnClickListener { onClick() }
            if (secondary) {
                setBackgroundColor(0xFFE0E0E0.toInt())
                setTextColor(0xFF333333.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun space(context: Context, dp: Int) = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(context, dp)
        )
    }

    private fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
}
