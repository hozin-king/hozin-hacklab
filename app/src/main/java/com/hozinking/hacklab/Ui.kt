package com.hozinking.hacklab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.TextView

/**
 * The whole visual language in one place. Flat colors only: no gradients,
 * no decorative blobs, no image assets. One accent color (terminal green)
 * used only for prompts, success states, and progress.
 */
object Ui {

    val BG: Int = Color.parseColor("#0F1115")
    val CARD: Int = Color.parseColor("#1C1F27")
    val TEXT: Int = Color.WHITE
    val SECONDARY: Int = Color.parseColor("#9AA0A6")
    val ACCENT: Int = Color.parseColor("#00C853")
    private val ON_ACCENT: Int = Color.BLACK

    fun dp(view: View, value: Int): Int {
        return (value * view.resources.displayMetrics.density).toInt()
    }

    fun title(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
        }
    }

    fun heading(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
        }
    }

    fun body(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 15f
            setTextColor(TEXT)
        }
    }

    fun small(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(SECONDARY)
        }
    }

    /** The single primary action on a screen. */
    fun primaryButton(context: Context, label: String): Button {
        return Button(context).apply {
            this.text = label
            textSize = 16f
            setTextColor(ON_ACCENT)
            setBackgroundColor(ACCENT)
        }
    }

    /** A secondary action. Never competes visually with the primary one. */
    fun secondaryButton(context: Context, label: String): Button {
        return Button(context).apply {
            this.text = label
            textSize = 15f
            setTextColor(TEXT)
            setBackgroundColor(CARD)
        }
    }
}
