package com.hozinking.hacklab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The whole visual language in one place. Flat colors only: no gradients,
 * no decorative blobs, no image assets, no emoji. One accent color
 * (terminal green) used only for prompts, focus commands, success states,
 * and progress.
 *
 * The look is a lab mission board, not a generic card list: a terminal
 * prompt header, monospace focus commands, and node rows whose left status
 * bar tells the state at a glance.
 */
object Ui {

    val BG: Int = Color.parseColor("#0F1115")
    val CARD: Int = Color.parseColor("#1C1F27")
    val TEXT: Int = Color.WHITE
    val SECONDARY: Int = Color.parseColor("#9AA0A6")
    val ACCENT: Int = Color.parseColor("#00C853")
    private val ON_ACCENT: Int = Color.BLACK

    /** Node state on the mission board. */
    enum class NodeStatus {
        DONE,
        ACTIVE,
        LOCKED
    }

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

    /** Small letter-spaced section label, e.g. "PAPAN MISI" or "BRIEFING". */
    fun label(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(SECONDARY)
            letterSpacing = 0.14f
        }
    }

    /** Monospace text in the given color and size. */
    fun mono(context: Context, text: String, size: Float, color: Int): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = size
            typeface = Typeface.MONOSPACE
            setTextColor(color)
        }
    }

    /** A green terminal prompt line, e.g. "pemula@hacklab:~$ ./hacklab". */
    fun prompt(context: Context, text: String): TextView {
        return mono(context, text, 14f, ACCENT)
    }

    /** A vertical card panel with the standard padding. */
    fun panel(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CARD)
        }
    }

    /**
     * One mission-board node row. The left status bar carries the state:
     * green for done, white for the active node, dim for locked. The row
     * itself is just a view; the caller attaches the tap behavior.
     */
    fun missionNode(
        context: Context,
        status: NodeStatus,
        marker: String,
        focus: String,
        title: String,
        note: String
    ): LinearLayout {
        val barColor = when (status) {
            NodeStatus.DONE -> ACCENT
            NodeStatus.ACTIVE -> TEXT
            NodeStatus.LOCKED -> SECONDARY
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BG)
        }
        val bar = View(context).apply {
            setBackgroundColor(barColor)
            layoutParams = LinearLayout.LayoutParams(
                Ui.dp(this, 4),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        row.addView(bar)
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        val head = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        head.addView(mono(context, marker, 13f, barColor).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.rightMargin = Ui.dp(this, 8)
            layoutParams = params
        })
        head.addView(mono(context, focus, 14f, TEXT).apply {
            setTypeface(null, Typeface.BOLD)
        })
        textCol.addView(head)
        textCol.addView(body(context, title).apply {
            setPadding(0, Ui.dp(this, 2), 0, 0)
        })
        textCol.addView(small(context, note).apply {
            setPadding(0, Ui.dp(this, 2), 0, 0)
        })
        row.addView(textCol)
        return row
    }

    // One Play Store-style row: a colored icon tile on the left, the
    // lesson name (bold), one tagline line and one meta line on the
    // right. The row itself is just a view; the caller attaches the
    // tap behavior.
    fun gameRow(
        context: Context,
        iconText: String,
        iconColorHex: String,
        name: String,
        tagline: String,
        meta: String
    ): LinearLayout {
        val tileColor = try {
            Color.parseColor(iconColorHex)
        } catch (e: Exception) {
            Color.parseColor("#455A64")
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BG)
            setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8))
        }
        row.addView(TextView(context).apply {
            text = iconText
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
            setBackgroundColor(tileColor)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                Ui.dp(this, 56),
                Ui.dp(this, 56)
            )
        })
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this, 12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        textCol.addView(TextView(context).apply {
            text = name
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
        })
        textCol.addView(small(context, tagline).apply {
            setPadding(0, Ui.dp(this, 2), 0, 0)
        })
        textCol.addView(small(context, meta).apply {
            setPadding(0, Ui.dp(this, 2), 0, 0)
        })
        row.addView(textCol)
        return row
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
