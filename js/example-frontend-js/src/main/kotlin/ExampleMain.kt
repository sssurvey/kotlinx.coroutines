/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import kotlinx.coroutines.experimental.*
import kotlinx.html.*
import kotlinx.html.div
import kotlinx.html.dom.*
import kotlinx.html.js.onClickFunction
import org.w3c.dom.*
import kotlin.browser.*
import kotlin.math.*

fun main(args: Array<String>) {
    println("Starting example application...")
    document.addEventListener("DOMContentLoaded", {
        Application().start()
    })
}

val Double.px get() = "${this}px"

private fun HTMLElement.setSize(w: Double, h: Double) {
    with(style) {
        width = w.px
        height = h.px
    }
}

private fun HTMLElement.setPosition(x: Double, y: Double) {
    with(style) {
        left = x.px
        top = y.px
    }
}

@Suppress("DEPRECATION")
private fun random() = kotlin.js.Math.random()

class Application {
    private val body get() = document.body!!
    private val scene get() = document.getElementById("scene") as HTMLElement
    private val sw = 800.0
    private val sh = 600.0
    private var animationIndex = 0
    private val animations = mutableSetOf<Job>()

    fun start() {
        body.append.div("content") {
            h1 {
                +"Kotlin Coroutines JS Example"
            }
            div {
                button {
                    +"Rect"
                    onClickFunction = { onRect() }
                }
                button {
                    +"Circle"
                    onClickFunction = { onCircle() }
                }
                button {
                    +"Clear"
                    onClickFunction = { onClear() }
                }
            }
            div {
                id = "scene"
            }
        }
        scene.setSize(sw, sh)
    }

    private fun animation(cls: String, size: Double, block: suspend CoroutineScope.(HTMLElement) -> Unit) {
        val elem = scene.append.div(cls)
        elem.setSize(size, size)
        val job = launch {
            block(elem)
        }
        animations += job
        job.invokeOnCompletion {
            animations -= job
            scene.removeChild(elem)
        }
    }

    private fun onRect() {
        val index = ++animationIndex
        val speed = 0.3
        val rs = 20.0
        val turnAfter = 5000.0 // seconds
        animation("rect", rs) { rect ->
            println("Started new 'rect' coroutine #$index")
            val timer = AnimationTimer()
            var turnAt = timer.time + turnAfter
            var vx = speed
            var vy = speed
            var x = 0.0
            var y = 0.0
            while (true) {
                val dt = timer.await()
                x += vx * dt
                y += vy * dt
                val xRange = 0.0 .. sw - rs
                val yRange = 0.0 .. sh - rs
                if (x !in xRange ) {
                    x = x.coerceIn(xRange)
                    vx = -vx
                }
                if (y !in yRange) {
                    y = y.coerceIn(yRange)
                    vy = -vy
                }
                rect.setPosition(x, y)
                if (timer.time >= turnAt) {
                    delay(1000) // pause a bit
                    // flip direction
                    val t = vx
                    if (random() > 0.5) {
                        vx = vy
                        vy = -t
                    } else {
                        vx = -vy
                        vy = t
                    }
                    // reset time
                    turnAt = timer.reset() + turnAfter
                    println("Delayed #$index for a while at ${timer.time}, resumed and turned")
                }
            }
        }
    }

    private fun onCircle() {
        val index = ++animationIndex
        val acceleration = 5e-4
        val initialRange = 0.7
        val maxSpeed = 0.4
        val initialSpeed = 0.1
        val radius = 20.0
        animation("circle", radius) { circle ->
            println("Started new 'circle' coroutine #$index")
            val timer = AnimationTimer()
            val initialAngle = random() * 2 * PI
            var vx = sin(initialAngle) * initialSpeed
            var vy = cos(initialAngle) * initialSpeed
            var x = (random() * initialRange + (1 - initialRange) / 2) * sw
            var y = (random() * initialRange + (1 - initialRange) / 2) * sh
            while (true) {
                val dt = timer.await()
                val dx = sw / 2 - x
                val dy = sh / 2 - y
                val dn = sqrt(dx * dx + dy * dy)
                vx += dx / dn * acceleration * dt
                vy += dy / dn * acceleration * dt
                val vn = sqrt(vx * vx + vy * vy)
                val trim = vn.coerceAtMost(maxSpeed)
                vx = vx / vn * trim
                vy = vy / vn * trim
                x += vx * dt
                y += vy * dt
                circle.setPosition(x, y)
            }
        }

    }

    private fun onClear() {
        animations.forEach { it.cancel() }
    }
}

class AnimationTimer {
    var time = window.performance.now()

    suspend fun await(): Double {
        val newTime = window.awaitAnimationFrame()
        val dt = newTime - time
        time = newTime
        return dt.coerceAtMost(500.0) // at most half a second
    }

    fun reset(): Double {
        time = window.performance.now()
        return time
    }
}