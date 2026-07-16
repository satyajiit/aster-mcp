package com.aster.service.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import androidx.core.graphics.PathParser
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * CompanionFaceView — the native painter for OpenAlly's ambient companion face,
 * drawn beside the camera cutout in an over-other-apps window.
 *
 * WHY THIS LIVES IN ASTER: OpenAlly ships on Google Play and holds NO
 * draw-over-other-apps permission. Aster is sideloaded and already holds
 * SYSTEM_ALERT_WINDOW for its screen-control work, so it hosts the window and
 * OpenAlly only feeds it geometry over the existing IPC bridge.
 *
 * POSE SOURCE OF TRUTH: OpenAlly runs the shared `@openally/face` rig and streams the
 * same renderer-agnostic
 * `buildGeometry` output — serialized as JSON — over `IAsterService.pushCompanionFrame`.
 * This view only PAINTS an already-parsed [CompanionFaceModel], in the exact order
 * `FaceCanvas.tsx` draws (head pill → blush → hands → headphones → brows → eyes →
 * mouth → cookie → props → particles). So the overlay face is identical to
 * OpenAlly's in-app face by construction. A bounded native layer animates only
 * transforms and decorations (blink, breath, semantic reactions) after remote frames
 * become stale; a fresh remote frame disables ambient native transforms immediately.
 *
 * THREADING: [parseCompanionFace] is a pure function called on the Binder pool
 * thread that receives the frame, so the binder transaction buffer is released
 * immediately and this view's main thread only invalidates + draws. Its native clock
 * is capped at 30 fps and `paused` stops it entirely while the display is off.
 */
class CompanionFaceView(context: Context) : View(context), Choreographer.FrameCallback {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val contentClip = Path()
    private val backgroundPath = Path()
    private val safeContent = RectF()
    private val faceViewport = RectF()
    private val statusViewport = RectF()
    private val iconScratch = RectF()
    private val iconPath = Path()
    private val primaryText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val secondaryText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 255, 255, 255)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val defaultBold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val monospaceBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    private val animator = CompanionNativeAnimator()
    private var status: CompanionStatusModel? = null
    private var statusVisible = false
    private var lastRemoteFrameAtMs = 0L
    private var animationScheduled = false

    /** Geometry must shrink when a timed side readout expires. */
    var onStatusVisibilityChanged: (() -> Unit)? = null

    /** Symmetric island radius; the full window already sits below real hardware. */
    var bottomCornerRadiusPx: Float = 12f * resources.displayMetrics.density
        set(value) {
            if (field == value) return
            field = value
            rebuildSafePaths()
            invalidate()
        }

    private var model: CompanionFaceModel? = null

    /** Gate repaints while the display is off/dozing (battery). */
    var paused: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                Choreographer.getInstance().removeFrameCallback(this)
                animationScheduled = false
            } else {
                invalidate()
                scheduleAnimation()
            }
        }

    /** Install one already-parsed frame. Main thread only. */
    fun setFrame(frame: CompanionFaceModel) {
        model = frame
        lastRemoteFrameAtMs = SystemClock.elapsedRealtime()
        if (!paused) invalidate()
    }

    fun setStatus(next: CompanionStatusModel?) {
        status = next
        refreshStatusVisibility()
        if (!paused) invalidate()
    }

    fun hasStatus(): Boolean = statusVisible

    /** A delayed Pomodoro countdown may exist while its face-only interlude is visible. */
    fun hasStatusModel(): Boolean = status != null

    fun react(reaction: CompanionReaction, nowMs: Long = SystemClock.elapsedRealtime()) {
        if (reaction == CompanionReaction.LAND) animator.land(nowMs) else animator.react(reaction, nowMs)
        scheduleAnimation()
    }

    /** Install the aggregate semantic speech snapshot produced by OpenAlly. */
    fun setSpeaking(
        active: Boolean,
        energy: Float,
        viseme: CompanionViseme,
        expiresAtMs: Long,
        reducedMotion: Boolean,
        nowMs: Long = SystemClock.elapsedRealtime(),
    ) {
        animator.setSpeaking(
            active = active,
            nowMs = nowMs,
            energy = energy,
            viseme = viseme,
            expiresAtMs = expiresAtMs,
            reducedMotion = reducedMotion,
        )
        scheduleAnimation()
        if (!paused) invalidate()
    }

    /** True once at least one frame has landed — an attached window with no frame
     *  would be an empty rectangle, so the controller waits for this. */
    fun hasFrame(): Boolean = model != null

    /**
     * Install the geometry policy's safe band. Face features, status copy, progress,
     * decoration, and touch are all constrained to this same rectangle, so a new OEM
     * cutout shape cannot be fixed visually while remaining unsafe interactively.
     */
    fun setSafeGeometry(bounds: SafeBounds) {
        safeContent.set(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
        )
        rebuildSafePaths()
        if (!paused) invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleAnimation()
    }

    override fun onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(this)
        animationScheduled = false
        animator.clear()
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        animationScheduled = false
        if (!isAttachedToWindow || paused) return
        val now = frameTimeNanos / 1_000_000L
        expireStatusIfNeeded(now)
        refreshStatusVisibility()
        postInvalidateOnAnimation()
        scheduleAnimation()
    }

    private fun scheduleAnimation() {
        if (animationScheduled || paused || !isAttachedToWindow) return
        animationScheduled = true
        // 30 fps is ample at notch scale and halves the cost of a 60/120 Hz panel.
        Choreographer.getInstance().postFrameCallbackDelayed(this, 33L)
    }

    private fun expireStatusIfNeeded(nowMs: Long) {
        val current = status ?: return
        if (current.expiresAtMs == 0L || nowMs < current.expiresAtMs) return
        status = null
        refreshStatusVisibility()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildSafePaths()
    }

    private fun rebuildSafePaths() {
        contentClip.reset()
        backgroundPath.reset()
        if (safeContent.isEmpty) return

        val radius = bottomCornerRadiusPx.coerceAtMost(safeContent.height() / 2f)
        contentClip.addRoundRect(safeContent, radius, radius, Path.Direction.CW)
        backgroundPath.addRoundRect(safeContent, radius, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val m = model ?: return
        if (width == 0 || height == 0 || safeContent.isEmpty) return
        val now = SystemClock.elapsedRealtime()
        expireStatusIfNeeded(now)
        val currentStatus = status.takeIf { statusVisible }
        val motion = animator.sample(now, lastRemoteFrameAtMs)
        fill.color = Color.BLACK
        fill.alpha = 255
        canvas.drawPath(backgroundPath, fill)

        canvas.save()
        canvas.clipPath(contentClip)
        when (currentStatus?.mode) {
            "full" -> faceViewport.set(
                safeContent.left,
                safeContent.top,
                safeContent.left,
                safeContent.bottom,
            )
            "side" -> {
                val compactFaceWidth = min(safeContent.width() * 0.34f, safeContent.height() * 1.55f)
                faceViewport.set(
                    safeContent.left,
                    safeContent.top,
                    safeContent.left + compactFaceWidth,
                    safeContent.bottom,
                )
            }
            else -> faceViewport.set(safeContent)
        }
        if (!faceViewport.isEmpty) drawFace(canvas, m, faceViewport, motion)
        if (currentStatus != null) {
            if (currentStatus.mode == "full") {
                statusViewport.set(safeContent)
            } else {
                statusViewport.set(
                    faceViewport.right,
                    safeContent.top,
                    safeContent.right,
                    safeContent.bottom,
                )
            }
            drawStatus(canvas, currentStatus, statusViewport)
        }
        drawNativeDecoration(canvas, motion.decoration, faceViewport)
        canvas.restore()
    }

    private fun refreshStatusVisibility() {
        val nextVisible = status?.let { model ->
            val revealAt = model.visibleAfterEpochMs
            revealAt == null || System.currentTimeMillis() >= revealAt
        } ?: false
        if (statusVisible == nextVisible) return
        statusVisible = nextVisible
        onStatusVisibilityChanged?.invoke()
    }

    private fun drawFace(
        canvas: Canvas,
        m: CompanionFaceModel,
        viewport: RectF,
        motion: CompanionMotionFrame,
    ) {
        canvas.save()
        canvas.clipRect(viewport)
        // Match the desktop notch: paint the rig's 200×96 feature band (y=30…126),
        // not the full 200×150 stage. Fitting the full stage made Android's former
        // square window look like a small black panel with tiny, apparently static
        // features. The rig and geometry remain shared; this is only a viewport crop.
        val cropTop = 30f
        val cropHeight = CompanionOverlayGeometry.CROP_HEIGHT.toFloat()
        val s = min(viewport.width() / m.viewW, viewport.height() / cropHeight)
        canvas.translate(
            viewport.left + (viewport.width() - m.viewW * s) / 2f,
            viewport.top + (viewport.height() - cropHeight * s) / 2f - cropTop * s,
        )
        canvas.scale(s, s)

        // Whole-face transform (offset → tilt/scale about the view centre) + opacity.
        val opaque = m.opacity >= 0.999f
        val restoreTo =
            if (opaque) {
                canvas.save()
            } else {
                canvas.saveLayerAlpha(0f, 0f, m.viewW, m.viewH, (m.opacity * 255).roundToInt())
            }
        val cx = m.viewW / 2f
        val cy = m.viewH / 2f
        canvas.translate(m.offsetX + motion.dx, m.offsetY + motion.dy)
        canvas.translate(cx, cy)
        canvas.rotate(m.tiltDeg + motion.rotationDeg)
        canvas.scale(m.scale * motion.scaleX, m.scale * motion.scaleY)
        canvas.translate(-cx, -cy)

        // 1. Head pill.
        m.headPath?.let {
            fill.color = m.head
            fill.alpha = 255
            canvas.drawPath(it, fill)
        }

        // 2. Blush.
        if (m.blushAlpha > 0f) {
            fill.color = m.blushRed
            fill.alpha = (Color.alpha(m.blushRed) * m.blushAlpha).roundToInt().coerceIn(0, 255)
            for (b in m.blush) canvas.drawOval(b.cx - b.rx, b.cy - b.ry, b.cx + b.rx, b.cy + b.ry, fill)
        }

        // 3. Ink hands.
        for (g in m.hands) drawGroup(canvas, g, m.palette)

        // 4. Headphones (stroked band + filled cups).
        m.headphones?.let { hp ->
            val a = (255 * hp.alpha).roundToInt().coerceIn(0, 255)
            hp.band?.let {
                stroke.color = m.ink
                stroke.alpha = a
                stroke.strokeWidth = hp.bandStrokeWidth
                stroke.strokeCap = Paint.Cap.ROUND
                canvas.drawPath(it, stroke)
            }
            fill.color = m.ink
            fill.alpha = a
            for (c in hp.cups) canvas.drawRoundRect(c.x, c.y, c.x + c.w, c.y + c.h, c.r, c.r, fill)
        }

        // 5. Brows + eyes (ink fills).
        fill.color = m.ink
        fill.alpha = 255
        canvas.save()
        canvas.translate(cx, 75f)
        canvas.scale(1f, motion.eyeScaleY)
        canvas.translate(-cx, -75f)
        for (p in m.brows) canvas.drawPath(p, fill)
        for (p in m.eyes) canvas.drawPath(p, fill)
        canvas.restore()

        // 6. Mouth: lip ring (ink) + cavity (head) + clipped tongue (red) & teeth (ink).
        val mouth = m.mouth
        canvas.save()
        canvas.translate(mouth.pivotX, mouth.pivotY)
        canvas.rotate(mouth.tiltDeg + motion.mouthRotationDeg)
        canvas.scale(motion.mouthScaleX, motion.mouthScaleY)
        canvas.translate(-mouth.pivotX, -mouth.pivotY)
        mouth.outer?.let {
            fill.color = m.ink
            fill.alpha = 255
            canvas.drawPath(it, fill)
        }
        val interior = mouth.interior
        if (interior != null) {
            fill.color = m.head
            fill.alpha = 255
            canvas.drawPath(interior, fill)
            canvas.save()
            runCatching { canvas.clipPath(interior) }
            m.tongue?.let { t ->
                fill.color = m.red
                fill.alpha = 255
                canvas.drawRoundRect(t.x, t.y, t.x + t.w, t.y + t.h, t.r, t.r, fill)
            }
            mouth.teeth?.let {
                fill.color = m.ink
                fill.alpha = 255
                canvas.drawPath(it, fill)
            }
            canvas.restore()
        }
        canvas.restore()

        // 7. Cookie (base + chips; bites carve in the head colour).
        m.cookie?.let { ck ->
            val a = (255 * ck.alpha).roundToInt().coerceIn(0, 255)
            canvas.save()
            canvas.translate(ck.cx, ck.cy)
            canvas.rotate(ck.rotDeg)
            canvas.translate(-ck.cx, -ck.cy)
            fill.color = m.cookieBase
            fill.alpha = a
            canvas.drawCircle(ck.cx, ck.cy, ck.r, fill)
            fill.color = m.cookieChip
            for (c in ck.chips) canvas.drawCircle(c.cx, c.cy, c.r, fill)
            fill.color = m.head
            for (c in ck.bites) canvas.drawCircle(c.cx, c.cy, c.r, fill)
            canvas.restore()
        }

        // 8. Scene props + 9. FX particles (generic prim groups).
        for (g in m.props) drawGroup(canvas, g, m.palette)
        for (g in m.particles) drawGroup(canvas, g, m.palette)

        canvas.restoreToCount(restoreTo)
        canvas.restore()
    }

    private fun drawStatus(canvas: Canvas, status: CompanionStatusModel, bounds: RectF) {
        if (bounds.isEmpty) return
        val density = resources.displayMetrics.density
        val accent = parseStatusColor(status.hue)
        val full = status.mode == "full"
        val countdownSeconds = status.countdownEndsAtEpochMs?.let { deadline ->
            ((deadline - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1_000L
        }
        val renderedText = countdownSeconds?.let(::formatCountdown) ?: status.text
        val renderedProgress = if (countdownSeconds != null && status.countdownTotalSeconds != null) {
            (1f - countdownSeconds.toFloat() / status.countdownTotalSeconds).coerceIn(0f, 1f)
        } else {
            status.progress
        }
        val iconSize = (if (full) 22f else 17f) * density
        val iconCx = bounds.left + (if (full) 24f else 14f) * density
        val iconCy = bounds.centerY() - if (renderedProgress == null) 0f else 1.5f * density
        drawStatusIcon(canvas, status.icon, iconCx, iconCy, iconSize, accent, renderedProgress)

        val textLeft = if (status.icon == null) {
            bounds.left + 10f * density
        } else {
            iconCx + iconSize * 0.72f
        }
        val textRight = bounds.right - 10f * density
        if (textRight <= textLeft) return
        primaryText.typeface = if (full) monospaceBold else defaultBold
        primaryText.textSize = (if (full) 21f else 12.5f) * density
        secondaryText.textSize = 9f * density
        val hasDetail = !status.detail.isNullOrBlank()
        val progressLift = if (renderedProgress == null) 0f else 2f * density
        val primaryY = if (hasDetail) {
            bounds.centerY() - 2f * density - progressLift
        } else {
            bounds.centerY() - (primaryText.ascent() + primaryText.descent()) / 2f - progressLift
        }
        canvas.drawText(
            ellipsize(renderedText, primaryText, textRight - textLeft),
            textLeft,
            primaryY,
            primaryText,
        )
        status.detail?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText(
                ellipsize(it, secondaryText, textRight - textLeft),
                textLeft,
                primaryY + 11f * density,
                secondaryText,
            )
        }
        renderedProgress?.let { value ->
            val trackLeft = if (full) bounds.left + 13f * density else textLeft
            val trackRight = bounds.right - 10f * density
            val top = bounds.bottom - 6f * density
            fill.color = Color.argb(72, 255, 255, 255)
            fill.alpha = 255
            canvas.drawRoundRect(trackLeft, top, trackRight, top + 2f * density, density, density, fill)
            fill.color = accent
            canvas.drawRoundRect(
                trackLeft,
                top,
                trackLeft + (trackRight - trackLeft) * value.coerceIn(0f, 1f),
                top + 2f * density,
                density,
                density,
                fill,
            )
        }
    }

    private fun formatCountdown(totalSeconds: Long): String {
        val seconds = totalSeconds.coerceAtLeast(0L)
        val hours = seconds / 3_600L
        val minutes = (seconds % 3_600L) / 60L
        val remainder = seconds % 60L
        return if (hours > 0L) {
            "$hours:${minutes.toString().padStart(2, '0')}:${remainder.toString().padStart(2, '0')}"
        } else {
            "${minutes.toString().padStart(2, '0')}:${remainder.toString().padStart(2, '0')}"
        }
    }

    private fun drawStatusIcon(
        canvas: Canvas,
        icon: String?,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        progress: Float?,
    ) {
        if (icon == null) return
        stroke.color = color
        stroke.alpha = 255
        stroke.strokeWidth = maxOf(1.7f, size * 0.09f)
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeJoin = Paint.Join.ROUND
        fill.color = color
        fill.alpha = 255
        val r = size / 2f
        when (icon) {
            "focus" -> {
                canvas.drawCircle(cx, cy + r * 0.08f, r * 0.72f, stroke)
                canvas.drawLine(cx, cy + r * 0.08f, cx, cy - r * 0.38f, stroke)
                canvas.drawLine(cx, cy + r * 0.08f, cx + r * 0.34f, cy + r * 0.28f, stroke)
                canvas.drawLine(cx - r * 0.22f, cy - r * 0.86f, cx + r * 0.22f, cy - r * 0.86f, stroke)
            }
            "break" -> {
                canvas.drawRoundRect(cx - r * 0.72f, cy - r * 0.25f, cx + r * 0.44f, cy + r * 0.58f, r * 0.12f, r * 0.12f, stroke)
                canvas.drawArc(cx + r * 0.20f, cy - r * 0.10f, cx + r * 0.88f, cy + r * 0.48f, -80f, 170f, false, stroke)
                canvas.drawLine(cx - r * 0.32f, cy - r * 0.55f, cx - r * 0.18f, cy - r * 0.84f, stroke)
                canvas.drawLine(cx + r * 0.12f, cy - r * 0.55f, cx + r * 0.24f, cy - r * 0.84f, stroke)
            }
            "rest" -> {
                canvas.drawArc(cx - r * 0.70f, cy - r * 0.82f, cx + r * 0.68f, cy + r * 0.80f, 72f, 220f, false, stroke)
                canvas.drawArc(cx - r * 0.24f, cy - r * 0.78f, cx + r * 0.68f, cy + r * 0.56f, 92f, 180f, false, stroke)
            }
            "music" -> {
                canvas.drawLine(cx - r * 0.12f, cy - r * 0.68f, cx - r * 0.12f, cy + r * 0.48f, stroke)
                canvas.drawLine(cx - r * 0.12f, cy - r * 0.68f, cx + r * 0.66f, cy - r * 0.48f, stroke)
                canvas.drawLine(cx + r * 0.66f, cy - r * 0.48f, cx + r * 0.66f, cy + r * 0.62f, stroke)
                canvas.drawCircle(cx - r * 0.38f, cy + r * 0.52f, r * 0.25f, fill)
                canvas.drawCircle(cx + r * 0.40f, cy + r * 0.66f, r * 0.25f, fill)
            }
            "disc" -> {
                canvas.drawCircle(cx, cy, r * 0.82f, stroke)
                canvas.drawCircle(cx, cy, r * 0.19f, fill)
            }
            "battery" -> {
                iconScratch.set(cx - r, cy - r * 0.58f, cx + r * 0.78f, cy + r * 0.58f)
                canvas.drawRoundRect(iconScratch, r * 0.18f, r * 0.18f, stroke)
                canvas.drawRoundRect(cx + r * 0.82f, cy - r * 0.22f, cx + r, cy + r * 0.22f, r * 0.08f, r * 0.08f, fill)
                val level = (progress ?: 0.5f).coerceIn(0f, 1f)
                canvas.drawRoundRect(
                    iconScratch.left + r * 0.17f,
                    iconScratch.top + r * 0.17f,
                    iconScratch.left + r * 0.17f + (iconScratch.width() - r * 0.34f) * level,
                    iconScratch.bottom - r * 0.17f,
                    r * 0.08f,
                    r * 0.08f,
                    fill,
                )
            }
            "bolt" -> {
                iconPath.reset()
                iconPath.moveTo(cx + r * 0.12f, cy - r)
                iconPath.lineTo(cx - r * 0.58f, cy + r * 0.08f)
                iconPath.lineTo(cx - r * 0.04f, cy + r * 0.08f)
                iconPath.lineTo(cx - r * 0.18f, cy + r)
                iconPath.lineTo(cx + r * 0.62f, cy - r * 0.18f)
                iconPath.lineTo(cx + r * 0.08f, cy - r * 0.18f)
                iconPath.close()
                canvas.drawPath(iconPath, fill)
            }
            "move", "scroll" -> {
                canvas.drawLine(cx, cy - r, cx, cy + r, stroke)
                canvas.drawLine(cx, cy - r, cx - r * 0.35f, cy - r * 0.65f, stroke)
                canvas.drawLine(cx, cy - r, cx + r * 0.35f, cy - r * 0.65f, stroke)
                canvas.drawLine(cx, cy + r, cx - r * 0.35f, cy + r * 0.65f, stroke)
                canvas.drawLine(cx, cy + r, cx + r * 0.35f, cy + r * 0.65f, stroke)
            }
            "vibrate" -> {
                canvas.drawRoundRect(cx - r * 0.46f, cy - r * 0.78f, cx + r * 0.46f, cy + r * 0.78f, r * 0.14f, r * 0.14f, stroke)
                canvas.drawArc(cx - r, cy - r * 0.58f, cx - r * 0.48f, cy + r * 0.58f, 100f, 160f, false, stroke)
                canvas.drawArc(cx + r * 0.48f, cy - r * 0.58f, cx + r, cy + r * 0.58f, -80f, 160f, false, stroke)
            }
            "app" -> {
                val cell = r * 0.48f
                val gap = r * 0.12f
                for (row in 0..1) for (column in 0..1) {
                    val left = cx - cell - gap / 2f + column * (cell + gap)
                    val top = cy - cell - gap / 2f + row * (cell + gap)
                    canvas.drawRoundRect(left, top, left + cell, top + cell, r * 0.10f, r * 0.10f, fill)
                }
            }
            "touch" -> {
                canvas.drawCircle(cx, cy, r * 0.26f, fill)
                canvas.drawCircle(cx, cy, r * 0.62f, stroke)
                canvas.drawArc(cx - r, cy - r, cx + r, cy + r, 205f, 100f, false, stroke)
            }
            "volume", "mute" -> {
                iconPath.reset()
                iconPath.moveTo(cx - r * 0.82f, cy - r * 0.26f)
                iconPath.lineTo(cx - r * 0.42f, cy - r * 0.26f)
                iconPath.lineTo(cx, cy - r * 0.68f)
                iconPath.lineTo(cx, cy + r * 0.68f)
                iconPath.lineTo(cx - r * 0.42f, cy + r * 0.26f)
                iconPath.lineTo(cx - r * 0.82f, cy + r * 0.26f)
                iconPath.close()
                canvas.drawPath(iconPath, fill)
                if (icon == "mute") {
                    canvas.drawLine(cx + r * 0.28f, cy - r * 0.30f, cx + r * 0.80f, cy + r * 0.30f, stroke)
                    canvas.drawLine(cx + r * 0.80f, cy - r * 0.30f, cx + r * 0.28f, cy + r * 0.30f, stroke)
                } else {
                    canvas.drawArc(cx - r * 0.08f, cy - r * 0.58f, cx + r * 0.72f, cy + r * 0.58f, -54f, 108f, false, stroke)
                }
            }
            "notification" -> {
                canvas.drawArc(cx - r * 0.62f, cy - r * 0.72f, cx + r * 0.62f, cy + r * 0.62f, 195f, 150f, false, stroke)
                canvas.drawLine(cx - r * 0.68f, cy + r * 0.58f, cx + r * 0.68f, cy + r * 0.58f, stroke)
                canvas.drawCircle(cx, cy + r * 0.82f, r * 0.12f, fill)
            }
            "typing" -> {
                for (i in -1..1) canvas.drawCircle(cx + i * r * 0.62f, cy, r * 0.17f, fill)
            }
            else -> {
                canvas.drawCircle(cx, cy, r * 0.82f, stroke)
                canvas.drawCircle(cx, cy, r * 0.18f, fill)
            }
        }
    }

    private fun drawNativeDecoration(canvas: Canvas, reaction: CompanionReaction?, face: RectF) {
        if (reaction == null || face.isEmpty) return
        val density = resources.displayMetrics.density
        val alpha = 220
        fun x(fraction: Float): Float = face.left + face.width() * fraction
        fun y(fraction: Float): Float = face.top + face.height() * fraction
        fill.alpha = alpha
        stroke.alpha = alpha
        when (reaction) {
            CompanionReaction.LIFT -> {
                fill.color = Color.rgb(96, 165, 250)
                canvas.drawOval(
                    x(0.76f),
                    y(0.12f),
                    x(0.76f) + 5f * density,
                    y(0.12f) + 9f * density,
                    fill,
                )
                stroke.color = Color.WHITE
                stroke.strokeWidth = 1.5f * density
                canvas.drawLine(x(0.18f), y(0.16f), x(0.10f), y(0.05f), stroke)
                canvas.drawLine(x(0.82f), y(0.16f), x(0.90f), y(0.05f), stroke)
            }
            CompanionReaction.CHARGE, CompanionReaction.PING -> {
                fill.color = if (reaction == CompanionReaction.CHARGE) Color.rgb(52, 211, 153) else Color.rgb(251, 191, 36)
                canvas.drawCircle(x(0.17f), y(0.22f), 2.6f * density, fill)
                canvas.drawCircle(x(0.82f), y(0.16f), 2f * density, fill)
            }
            CompanionReaction.SHAKE -> {
                stroke.color = Color.rgb(251, 113, 133)
                stroke.strokeWidth = 1.5f * density
                canvas.drawArc(face.left + 2f * density, y(0.27f), face.left + 10f * density, y(0.73f), 90f, 180f, false, stroke)
                canvas.drawArc(face.right - 10f * density, y(0.27f), face.right - 2f * density, y(0.73f), -90f, 180f, false, stroke)
            }
            CompanionReaction.LAND -> {
                fill.color = Color.rgb(52, 211, 153)
                canvas.drawCircle(x(0.76f), y(0.20f), 2.4f * density, fill)
                canvas.drawCircle(x(0.82f), y(0.27f), 1.6f * density, fill)
            }
            else -> Unit
        }
        fill.alpha = 255
        stroke.alpha = 255
    }

    private fun ellipsize(value: String, paint: Paint, width: Float): String {
        if (width <= 0f || paint.measureText(value) <= width) return value
        val suffix = "…"
        val count = paint.breakText(value, true, (width - paint.measureText(suffix)).coerceAtLeast(0f), null)
        return value.take(count.coerceAtLeast(0)).trimEnd() + suffix
    }

    private fun parseStatusColor(value: String?): Int =
        value?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: Color.rgb(96, 165, 250)

    /** One prim group: a rotation about (`ox`,`oy`) + a group alpha over its prims. */
    private fun drawGroup(canvas: Canvas, g: FaceGroup, palette: Map<String, Int>) {
        canvas.save()
        if (g.rotDeg != 0f) {
            canvas.translate(g.ox, g.oy)
            canvas.rotate(g.rotDeg)
            canvas.translate(-g.ox, -g.oy)
        }
        for (p in g.prims) drawPrim(canvas, p, g.alpha, palette)
        canvas.restore()
    }

    private fun drawPrim(canvas: Canvas, p: FacePrim, groupAlpha: Float, palette: Map<String, Int>) {
        val fillColor = p.fill?.let { palette[it] }
        val strokeColor = p.stroke?.let { palette[it] }
        fun alphaOf(color: Int): Int =
            (Color.alpha(color) * p.alpha * groupAlpha).roundToInt().coerceIn(0, 255)
        when (p.kind) {
            FacePrim.CIRCLE -> {
                if (fillColor != null) {
                    fill.color = fillColor; fill.alpha = alphaOf(fillColor)
                    canvas.drawCircle(p.cx, p.cy, p.r, fill)
                }
                if (strokeColor != null) {
                    stroke.color = strokeColor; stroke.alpha = alphaOf(strokeColor)
                    stroke.strokeWidth = p.strokeWidth; stroke.strokeCap = p.cap
                    canvas.drawCircle(p.cx, p.cy, p.r, stroke)
                }
            }
            FacePrim.ELLIPSE -> if (fillColor != null) {
                fill.color = fillColor; fill.alpha = alphaOf(fillColor)
                canvas.drawOval(p.cx - p.rx, p.cy - p.ry, p.cx + p.rx, p.cy + p.ry, fill)
            }
            FacePrim.RECT -> if (fillColor != null) {
                fill.color = fillColor; fill.alpha = alphaOf(fillColor)
                canvas.drawRoundRect(p.x, p.y, p.x + p.w, p.y + p.h, p.rr, p.rr, fill)
            }
            FacePrim.PATH -> {
                val path = p.path ?: return
                if (fillColor != null) {
                    fill.color = fillColor; fill.alpha = alphaOf(fillColor)
                    canvas.drawPath(path, fill)
                }
                if (strokeColor != null) {
                    stroke.color = strokeColor; stroke.alpha = alphaOf(strokeColor)
                    stroke.strokeWidth = p.strokeWidth; stroke.strokeCap = p.cap
                    canvas.drawPath(path, stroke)
                }
            }
        }
    }
}

// ── the parsed draw model (produced off the main thread) ──────────────────────

class CompanionFaceModel internal constructor(
    val viewW: Float, val viewH: Float,
    val opacity: Float, val scale: Float, val offsetX: Float, val offsetY: Float, val tiltDeg: Float,
    val headPath: Path?, val head: Int, val ink: Int, val red: Int, val blushRed: Int,
    val cookieBase: Int, val cookieChip: Int,
    val brows: List<Path>, val eyes: List<Path>, val mouth: FaceMouth, val tongue: FaceTongue?,
    val blush: List<FaceBlush>, val blushAlpha: Float,
    val hands: List<FaceGroup>, val headphones: FaceHeadphones?, val cookie: FaceCookie?,
    val props: List<FaceGroup>, val particles: List<FaceGroup>, val palette: Map<String, Int>,
)

class FaceMouth internal constructor(val outer: Path?, val interior: Path?, val teeth: Path?, val tiltDeg: Float, val pivotX: Float, val pivotY: Float)
class FaceTongue internal constructor(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float)
class FaceBlush internal constructor(val cx: Float, val cy: Float, val rx: Float, val ry: Float)
class FaceCup internal constructor(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float)
class FaceHeadphones internal constructor(val alpha: Float, val band: Path?, val bandStrokeWidth: Float, val cups: List<FaceCup>)
class FaceCircle internal constructor(val cx: Float, val cy: Float, val r: Float)
class FaceCookie internal constructor(val cx: Float, val cy: Float, val r: Float, val rotDeg: Float, val alpha: Float, val chips: List<FaceCircle>, val bites: List<FaceCircle>)
class FaceGroup internal constructor(val rotDeg: Float, val ox: Float, val oy: Float, val alpha: Float, val prims: List<FacePrim>)

class FacePrim internal constructor(
    val kind: Int,
    val cx: Float = 0f, val cy: Float = 0f, val r: Float = 0f,
    val rx: Float = 0f, val ry: Float = 0f,
    val x: Float = 0f, val y: Float = 0f, val w: Float = 0f, val h: Float = 0f, val rr: Float = 0f,
    val path: Path? = null,
    val fill: String? = null, val stroke: String? = null,
    val strokeWidth: Float = 1f, val alpha: Float = 1f, val cap: Paint.Cap = Paint.Cap.ROUND,
) {
    companion object { const val CIRCLE = 0; const val ELLIPSE = 1; const val RECT = 2; const val PATH = 3 }
}

// ── parse (pure; safe to call off the main thread) ────────────────────────────

/**
 * Parse one serialized `buildGeometry` frame. Returns null on any malformed frame —
 * a bad frame is DROPPED, never painted half-way and never thrown across the Binder
 * boundary (the frame lane is `oneway`, so a throw here would be invisible to
 * OpenAlly anyway; the last good frame simply stays on screen).
 */
fun parseCompanionFace(json: String): CompanionFaceModel? = runCatching { parseFace(json) }.getOrNull()

private fun parseFace(json: String): CompanionFaceModel {
    val o = JSONObject(json)
    val view = o.getJSONObject("view")
    val palette = HashMap<String, Int>()
    o.optJSONObject("palette")?.let { pj ->
        for (k in pj.keys()) palette[k] = parseCssColor(pj.getString(k))
    }
    val mouth = o.getJSONObject("mouth")
    val pivot = mouth.getJSONObject("pivot")
    return CompanionFaceModel(
        viewW = view.getDouble("w").toFloat(),
        viewH = view.getDouble("h").toFloat(),
        opacity = o.optDouble("opacity", 1.0).toFloat(),
        scale = o.optDouble("scale", 1.0).toFloat(),
        offsetX = o.optDouble("offsetX", 0.0).toFloat(),
        offsetY = o.optDouble("offsetY", 0.0).toFloat(),
        tiltDeg = o.optDouble("tiltDeg", 0.0).toFloat(),
        headPath = pathOf(o.optString("headPath", "")),
        head = parseCssColor(o.optString("head", "#000000")),
        ink = parseCssColor(o.optString("ink", "#000000")),
        red = parseCssColor(o.optString("red", "#ef4444")),
        blushRed = parseCssColor(o.optString("blushRed", "#f87171")),
        cookieBase = parseCssColor(o.optString("cookieBase", "#c98a4e")),
        cookieChip = parseCssColor(o.optString("cookieChip", "#54341c")),
        brows = pathsOfD(o.optJSONArray("brows")),
        eyes = pathsOfD(o.optJSONArray("eyes")),
        mouth = FaceMouth(
            outer = pathOf(mouth.optString("outer", "")),
            interior = mouth.optString("interior").takeIf { it.isNotEmpty() && it != "null" }?.let { pathOf(it) },
            teeth = mouth.optString("teethBand").takeIf { it.isNotEmpty() && it != "null" }?.let { pathOf(it) },
            tiltDeg = mouth.optDouble("tiltDeg", 0.0).toFloat(),
            pivotX = pivot.getDouble("x").toFloat(),
            pivotY = pivot.getDouble("y").toFloat(),
        ),
        tongue = o.optJSONObject("tongue")?.let {
            FaceTongue(
                it.getDouble("x").toFloat(), it.getDouble("y").toFloat(),
                it.getDouble("w").toFloat(), it.getDouble("h").toFloat(), it.getDouble("r").toFloat(),
            )
        },
        blush = mapArray(o.optJSONArray("blush")) {
            FaceBlush(it.getDouble("cx").toFloat(), it.getDouble("cy").toFloat(), it.getDouble("rx").toFloat(), it.getDouble("ry").toFloat())
        },
        blushAlpha = o.optDouble("blushAlpha", 0.0).toFloat(),
        hands = faceGroups(o.optJSONArray("handGroups")),
        headphones = o.optJSONObject("headphones")?.let { hp ->
            FaceHeadphones(
                alpha = hp.optDouble("alpha", 1.0).toFloat(),
                band = hp.optString("band").takeIf { it.isNotEmpty() }?.let { pathOf(it) },
                bandStrokeWidth = hp.optDouble("bandStrokeWidth", 5.0).toFloat(),
                cups = mapArray(hp.optJSONArray("cups")) {
                    FaceCup(it.getDouble("x").toFloat(), it.getDouble("y").toFloat(), it.getDouble("w").toFloat(), it.getDouble("h").toFloat(), it.getDouble("r").toFloat())
                },
            )
        },
        cookie = o.optJSONObject("cookie")?.let { ck ->
            FaceCookie(
                cx = ck.getDouble("cx").toFloat(), cy = ck.getDouble("cy").toFloat(), r = ck.getDouble("r").toFloat(),
                rotDeg = ck.optDouble("rotDeg", 0.0).toFloat(), alpha = ck.optDouble("alpha", 1.0).toFloat(),
                chips = mapArray(ck.optJSONArray("chips")) { FaceCircle(it.getDouble("cx").toFloat(), it.getDouble("cy").toFloat(), it.getDouble("r").toFloat()) },
                bites = mapArray(ck.optJSONArray("bites")) { FaceCircle(it.getDouble("cx").toFloat(), it.getDouble("cy").toFloat(), it.getDouble("r").toFloat()) },
            )
        },
        props = faceGroups(o.optJSONArray("propGroups")),
        particles = faceGroups(o.optJSONArray("particleGroups")),
        palette = palette,
    )
}

private fun faceGroups(arr: JSONArray?): List<FaceGroup> = mapArray(arr) { g ->
    FaceGroup(
        rotDeg = g.optDouble("rotDeg", 0.0).toFloat(),
        ox = g.optDouble("ox", 0.0).toFloat(),
        oy = g.optDouble("oy", 0.0).toFloat(),
        alpha = g.optDouble("alpha", 1.0).toFloat(),
        prims = mapArray(g.optJSONArray("prims")) { facePrim(it) },
    )
}

private fun facePrim(p: JSONObject): FacePrim = when (p.optString("k")) {
    "circle" -> FacePrim(
        FacePrim.CIRCLE, cx = p.getDouble("cx").toFloat(), cy = p.getDouble("cy").toFloat(), r = p.getDouble("r").toFloat(),
        fill = p.optString("fill").ifEmpty { null }, stroke = p.optString("stroke").ifEmpty { null },
        strokeWidth = p.optDouble("strokeWidth", 1.0).toFloat(), alpha = p.optDouble("alpha", 1.0).toFloat(), cap = capOf(p),
    )
    "ellipse" -> FacePrim(
        FacePrim.ELLIPSE, cx = p.getDouble("cx").toFloat(), cy = p.getDouble("cy").toFloat(),
        rx = p.getDouble("rx").toFloat(), ry = p.getDouble("ry").toFloat(),
        fill = p.optString("fill").ifEmpty { null }, alpha = p.optDouble("alpha", 1.0).toFloat(),
    )
    "rect" -> FacePrim(
        FacePrim.RECT, x = p.getDouble("x").toFloat(), y = p.getDouble("y").toFloat(),
        w = p.getDouble("w").toFloat(), h = p.getDouble("h").toFloat(), rr = p.optDouble("rx", 0.0).toFloat(),
        fill = p.optString("fill").ifEmpty { null }, alpha = p.optDouble("alpha", 1.0).toFloat(),
    )
    else -> FacePrim(
        FacePrim.PATH, path = pathOf(p.optString("d", "")),
        fill = p.optString("fill").ifEmpty { null }, stroke = p.optString("stroke").ifEmpty { null },
        strokeWidth = p.optDouble("strokeWidth", 1.0).toFloat(), alpha = p.optDouble("alpha", 1.0).toFloat(), cap = capOf(p),
    )
}

private fun capOf(p: JSONObject): Paint.Cap =
    if (p.optString("cap") == "butt") Paint.Cap.BUTT else Paint.Cap.ROUND

private fun pathOf(d: String): Path? =
    if (d.isEmpty()) null else runCatching { PathParser.createPathFromPathData(d) }.getOrNull()

private fun pathsOfD(arr: JSONArray?): List<Path> = mapArray(arr) { pathOf(it.getString("d")) }.filterNotNull()

private inline fun <T> mapArray(arr: JSONArray?, f: (JSONObject) -> T): List<T> {
    if (arr == null) return emptyList()
    val out = ArrayList<T>(arr.length())
    for (i in 0 until arr.length()) out.add(f(arr.getJSONObject(i)))
    return out
}

/** Parse a CSS colour: `#rgb`/`#rrggbb`/`#aarrggbb` (via Color.parseColor) and
 *  `rgb()`/`rgba()` (the face reducer's `argbToCss` output). Defaults to opaque black. */
private fun parseCssColor(s: String): Int {
    val t = s.trim()
    if (t.isEmpty()) return Color.BLACK
    if (t.startsWith("#")) return runCatching { Color.parseColor(t) }.getOrDefault(Color.BLACK)
    if (t.startsWith("rgb")) {
        val inner = t.substringAfter('(').substringBefore(')').split(',')
        if (inner.size >= 3) {
            val r = inner[0].trim().toFloat().roundToInt().coerceIn(0, 255)
            val g = inner[1].trim().toFloat().roundToInt().coerceIn(0, 255)
            val b = inner[2].trim().toFloat().roundToInt().coerceIn(0, 255)
            val a = if (inner.size >= 4) (inner[3].trim().toFloat() * 255).roundToInt().coerceIn(0, 255) else 255
            return Color.argb(a, r, g, b)
        }
    }
    return Color.BLACK
}
