package com.example.monopolyzapped.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DebugOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintPointA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN; style = Paint.Style.FILL; strokeWidth = 6f
    }
    private val paintPointB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.MAGENTA; style = Paint.Style.FILL; strokeWidth = 6f
    }
    private val paintRect = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FF9800; style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val paintRectFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22FF9800; style = Paint.Style.FILL
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 32f; typeface = Typeface.MONOSPACE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW; strokeWidth = 3f
    }

    private var pA: PointF? = null
    private var pB: PointF? = null
    private var dx: Float? = null
    private var dy: Float? = null
    private var dist: Float? = null
    private var label: String? = null

    private val corners = ArrayList<Rect>()

    fun setPoints(a: PointF?, b: PointF?, dx: Float?, dy: Float?, dist: Float?, label: String?) {
        pA = a; pB = b
        this.dx = dx; this.dy = dy; this.dist = dist; this.label = label
        invalidate()
    }

    fun setCornerRects(rects: List<Rect>) {
        corners.clear()
        corners.addAll(rects)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // coins
        for (r in corners) {
            canvas.drawRect(r, paintRectFill)
            canvas.drawRect(r, paintRect)
        }

        val a = pA
        val b = pB
        if (a != null) {
            canvas.drawCircle(a.x, a.y, 12f, paintPointA)
            canvas.drawText("A(%.0f,%.0f)".format(a.x, a.y), a.x + 16f, a.y - 16f, paintText)
        }
        if (b != null) {
            canvas.drawCircle(b.x, b.y, 12f, paintPointB)
            canvas.drawText("B(%.0f,%.0f)".format(b.x, b.y), b.x + 16f, b.y - 16f, paintText)
        }

        if (a != null && b != null) {
            canvas.drawLine(a.x, a.y, b.x, b.y, linePaint)
        }

        var y = 48f
        label?.let {
            canvas.drawText(it, 24f, y, paintText); y += 40f
        }
        dx?.let { canvas.drawText("dx=%.2f".format(it), 24f, y, paintText); y += 36f }
        dy?.let { canvas.drawText("dy=%.2f".format(it), 24f, y, paintText); y += 36f }
        dist?.let { canvas.drawText("dist=%.2f".format(it), 24f, y, paintText); y += 36f }
    }
}
