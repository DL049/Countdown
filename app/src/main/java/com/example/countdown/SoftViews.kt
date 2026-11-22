package com.example.countdown

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Soft UI container. Draws the dual soft shadows (white top-left, cool grey bottom-right):
 *  - RAISED: a surface/button that sits above the background
 *  - WELL:   an inset, pressed-in area for grouped content
 *  - FLAT:   nothing drawn (e.g. an unselected segment)
 * Clickable RAISED frames animate into the inset look while pressed, and draw a
 * visible focus ring when focused. Animation is skipped when animations are disabled.
 */
open class SoftFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Mode { RAISED, WELL, FLAT }

    private val dp = resources.displayMetrics.density
    private val bg = ContextCompat.getColor(context, R.color.soft_bg)
    private val shadowColor = ContextCompat.getColor(context, R.color.soft_shadow)
    private val lightColor = ContextCompat.getColor(context, R.color.soft_light)
    private val focusColor = ContextCompat.getColor(context, R.color.soft_blue)

    private var radius = 16f * dp
    private var depth = 5f * dp
    private var blur = 12f * dp
    private var pad = 0

    private var raisedA = 0f
    private var wellA = 0f
    private var animator: ValueAnimator? = null
    private var ready = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val outer = RectF()
    private val clip = Path()
    private val frame = Path()

    var softMode: Mode = Mode.RAISED
        set(value) {
            field = value
            if (ready) syncTargets(animate = isAttachedToWindow)
        }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null) // shadow layers need the software renderer on old APIs
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SoftFrame)
        softMode = Mode.values()[ta.getInt(R.styleable.SoftFrame_softMode, 0)]
        radius = ta.getDimension(R.styleable.SoftFrame_softRadius, radius)
        depth = ta.getDimension(R.styleable.SoftFrame_softDepth, depth)
        ta.recycle()
        blur = depth * 2.4f
        pad = (depth + blur * 0.7f).toInt()
        setPadding(paddingLeft + pad, paddingTop + pad, paddingRight + pad, paddingBottom + pad)
        ready = true
        syncTargets(animate = false)
    }

    private fun reducedMotion() =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    private fun syncTargets(animate: Boolean) {
        val pressed = isClickable && isPressed
        val tr = if (softMode == Mode.RAISED && !pressed) 1f else 0f
        val tw = if (softMode == Mode.WELL || (softMode == Mode.RAISED && pressed)) 1f else 0f
        animator?.cancel()
        if (!animate || reducedMotion() || (tr == raisedA && tw == wellA)) {
            raisedA = tr
            wellA = tw
            invalidate()
            return
        }
        val r0 = raisedA
        val w0 = wellA
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            addUpdateListener {
                val f = it.animatedFraction
                raisedA = r0 + (tr - r0) * f
                wellA = w0 + (tw - w0) * f
                invalidate()
            }
            start()
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (ready) syncTargets(animate = true)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        invalidate()
    }

    override fun clearChildFocus(child: View?) {
        super.clearChildFocus(child)
        invalidate()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (isClickable) info.className = Button::class.java.name
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect.set(pad.toFloat(), pad.toFloat(), (width - pad).toFloat(), (height - pad).toFloat())
        if (rect.width() <= 0f || rect.height() <= 0f) return
        if (wellA > 0f) layer(canvas, wellA) { drawWell(it) }
        if (raisedA > 0f) layer(canvas, raisedA) { drawRaised(it) }
        if (hasFocus()) drawFocus(canvas)
    }

    private inline fun layer(canvas: Canvas, alpha: Float, block: (Canvas) -> Unit) {
        val save = canvas.saveLayerAlpha(
            0f, 0f, width.toFloat(), height.toFloat(), (alpha.coerceIn(0f, 1f) * 255).toInt(),
        )
        block(canvas)
        canvas.restoreToCount(save)
    }

    private fun drawRaised(c: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = bg
        paint.setShadowLayer(blur, -depth, -depth, lightColor)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.setShadowLayer(blur, depth, depth, shadowColor)
        c.drawRoundRect(rect, radius, radius, paint)
        paint.clearShadowLayer()
    }

    private fun drawWell(c: Canvas) {
        val d = depth * 0.7f
        val b = blur * 0.7f
        paint.style = Paint.Style.FILL
        paint.color = bg
        c.drawRoundRect(rect, radius, radius, paint)

        c.save()
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        c.clipPath(clip)
        // A frame around the well casts its shadows inwards (inset look).
        outer.set(rect)
        outer.inset(-b * 3f, -b * 3f)
        frame.reset()
        frame.fillType = Path.FillType.EVEN_ODD
        frame.addRect(outer, Path.Direction.CW)
        frame.addRoundRect(rect, radius, radius, Path.Direction.CW)
        paint.setShadowLayer(b, d, d, shadowColor)
        c.drawPath(frame, paint)
        paint.setShadowLayer(b, -d, -d, lightColor)
        c.drawPath(frame, paint)
        paint.clearShadowLayer()
        c.restore()
    }

    private fun drawFocus(c: Canvas) {
        val w = 3f * dp
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w
        paint.color = focusColor
        outer.set(rect)
        outer.inset(-(w + dp), -(w + dp))
        c.drawRoundRect(outer, radius + w, radius + w, paint)
        paint.style = Paint.Style.FILL
    }
}

/** A soft raised button with a text label (reads `android:text`). Also usable as a segment. */
class SoftButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SoftFrame(context, attrs, defStyleAttr) {

    private val blue = ContextCompat.getColor(context, R.color.soft_blue)
    private val muted = ContextCompat.getColor(context, R.color.soft_muted)
    private val label = TextView(context)

    var text: CharSequence
        get() = label.text
        set(value) {
            label.text = value
        }

    init {
        isClickable = true
        isFocusable = true
        val d = resources.displayMetrics.density
        label.apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(blue)
            minHeight = (48 * d).toInt()
            setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
        }
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val ta = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
        ta.getText(0)?.let { label.text = it }
        ta.recycle()
    }

    /** Segmented-control look: selected = raised blue, unselected = flat grey. */
    fun setSegment(selected: Boolean) {
        softMode = if (selected) Mode.RAISED else Mode.FLAT
        label.setTextColor(if (selected) blue else muted)
        isSelected = selected
    }
}
