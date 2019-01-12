package com.anwesh.uiprojects.triangleslabstepview

/**
 * Created by anweshmishra on 12/01/19.
 */

import android.view.View
import android.content.Context
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Path
import android.app.Activity

val nodes : Int = 5
val slabs : Int = 4
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val sizeFactor : Float = 2.8f
val strokeFactor : Int = 90
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.mirrorValue(a : Int, b : Int) : Float = (1 - scaleFactor()) * a.inverse() + scaleFactor() * b.inverse()
fun Float.updateScale(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.clipTrianglePath(size : Float) {
    val path : Path = Path()
    path.moveTo(-size/2, size / 2)
    path.lineTo(size/2, size/2)
    path.lineTo(0f, -size/2)
    clipPath(path)
}
fun Canvas.drawSlab(x : Float, y : Float, w : Float, h : Float, paint : Paint) {
    save()
    translate(x, y)
    clipTrianglePath(w)
    paint.style = Paint.Style.STROKE
    drawRect(-w/2, - h/2, w/2, h/2, paint)
    paint.style = Paint.Style.FILL
    drawRect(-w/2, - h/2, w/2, h/2, paint)
    restore()
}

fun Canvas.drawTSSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sf : Float = 1f - 2 * (i % 2)
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    val x : Float = (w/2 + size/2 + paint.strokeWidth/2)
    val yGap : Float = h / slabs
    save()
    translate(w/2, gap * (i + 1))
    rotate(180f * sc2)
    for (j in 0..(slabs - 1)) {
        val sc : Float = sc1.divideScale(j, slabs)
        save()
        drawSlab(x * sc, yGap * (slabs - 1 - j) + yGap / 2, 2 * size, yGap, paint)
        restore()
    }
    restore()
}

class TriangleSlabStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateScale(dir, slabs, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            cb()
            try {
                Thread.sleep(50)
                view.invalidate()
            } catch(ex : Exception) {

            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class TSSNode(var i : Int, val state : State = State()) {

        private var next : TSSNode? = null
        private var prev : TSSNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = TSSNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawTSSNode(i, state.scale, paint)
            prev?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : TSSNode {
            var curr : TSSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class TriangleSlabStep(var i : Int) {

        private var curr : TSSNode = TSSNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    cb(i, scl)
                }
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : TriangleSlabStepView) {
        private val animator : Animator = Animator(view)
        private val tss : TriangleSlabStep = TriangleSlabStep(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            tss.draw(canvas, paint)
            animator.animate {
                tss.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            tss.startUpdating {
                animator.start()
            }
        }
    }
}