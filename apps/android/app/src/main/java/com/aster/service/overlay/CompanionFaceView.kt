package com.aster.service.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
 * SINGLE SOURCE OF TRUTH: this view does NOT run the face engine. OpenAlly runs the
 * ONE shared `@openally/face` rig and streams the SAME renderer-agnostic
 * `buildGeometry` output — serialized as JSON — over `IAsterService.pushCompanionFrame`.
 * This view only PAINTS an already-parsed [CompanionFaceModel], in the exact order
 * `FaceCanvas.tsx` draws (head pill → blush → hands → headphones → brows → eyes →
 * mouth → cookie → props → particles). So the overlay face is identical to
 * OpenAlly's in-app face by construction — there is no second engine to diverge.
 *
 * THREADING: [parseCompanionFace] is a pure function called on the Binder pool
 * thread that receives the frame, so the binder transaction buffer is released
 * immediately and this view's main thread only invalidates + draws. The view is
 * passive — it repaints solely when a new frame lands, so an idle face costs ≈0%
 * CPU, and `paused` gates repaints while the display is off.
 */
class CompanionFaceView(context: Context) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private var model: CompanionFaceModel? = null

    /** Gate repaints while the display is off/dozing (battery). */
    var paused: Boolean = false
        set(value) {
            field = value
            if (!value) invalidate()
        }

    /** Install one already-parsed frame. Main thread only. */
    fun setFrame(frame: CompanionFaceModel) {
        model = frame
        if (!paused) invalidate()
    }

    /** True once at least one frame has landed — an attached window with no frame
     *  would be an empty rectangle, so the controller waits for this. */
    fun hasFrame(): Boolean = model != null

    override fun onDraw(canvas: Canvas) {
        val m = model ?: return
        if (width == 0 || height == 0) return

        canvas.save()
        // Match the desktop notch: paint the rig's 200×96 feature band (y=30…126),
        // not the full 200×150 stage. Fitting the full stage made Android's former
        // square window look like a small black panel with tiny, apparently static
        // features. The rig and geometry remain shared; this is only a viewport crop.
        val cropTop = 30f
        val cropHeight = CompanionOverlayGeometry.CROP_HEIGHT.toFloat()
        val s = min(width / m.viewW, height / cropHeight)
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        canvas.translate(
            (width - m.viewW * s) / 2f,
            (height - cropHeight * s) / 2f - cropTop * s,
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
        canvas.translate(m.offsetX, m.offsetY)
        canvas.translate(cx, cy)
        canvas.rotate(m.tiltDeg)
        canvas.scale(m.scale, m.scale)
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
        for (p in m.brows) canvas.drawPath(p, fill)
        for (p in m.eyes) canvas.drawPath(p, fill)

        // 6. Mouth: lip ring (ink) + cavity (head) + clipped tongue (red) & teeth (ink).
        val mouth = m.mouth
        canvas.save()
        canvas.translate(mouth.pivotX, mouth.pivotY)
        canvas.rotate(mouth.tiltDeg)
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
