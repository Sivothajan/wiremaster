package com.example.wiremaster

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sqrt

object CatenaryCalculator {
    data class PointD(val x: Double, val y: Double)

    fun calculateArcLength(realSpanMeters: Double, p1: PointD, p2: PointD, pSag: PointD): Double {
        val pixelSpan = hypot(p2.x - p1.x, p2.y - p1.y)
        val metersPerPixel = realSpanMeters / pixelSpan

        val wx2 = (p2.x - p1.x) * metersPerPixel
        val wy2 = (p2.y - p1.y) * metersPerPixel
        val wx3 = (pSag.x - p1.x) * metersPerPixel
        val wy3 = (pSag.y - p1.y) * metersPerPixel

        val x2Sq = wx2 * wx2
        val x3Sq = wx3 * wx3

        val denominator = (x2Sq * wx3) - (x3Sq * wx2)
        if (abs(denominator) < 0.0001) return realSpanMeters

        val a = ((wy2 * wx3) - (wy3 * wx2)) / denominator
        val b = (wy2 - a * x2Sq) / wx2

        val startX = minOf(0.0, wx2)
        val endX = maxOf(0.0, wx2)

        return integrateParabola(a, b, startX, endX)
    }

    private fun integrateParabola(a: Double, b: Double, startX: Double, endX: Double): Double {
        fun f(x: Double): Double {
            val u = 2 * a * x + b
            val term1 = u * sqrt(1 + u * u)
            val term2 = ln(u + sqrt(1 + u * u))
            return (term1 + term2) / (4 * a)
        }
        return abs(f(endX) - f(startX))
    }
}