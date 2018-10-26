package info.kunalsheth.usbcontrolsystem

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.math.sign
import kotlin.system.measureNanoTime

fun limitCurrent(targetDc: Int, speed: Int, currentLimit: Int): Int {
    val expected = targetDc - speed

    val limited = when {
        expected > currentLimit -> currentLimit + speed
        expected < -currentLimit -> -currentLimit + speed
        else -> targetDc
    }

    return if (limited.sign == targetDc.sign) limited
    else 0
}

fun cap(targetDc: Int) = when {
    targetDc > 100 -> 100
    targetDc < -100 -> -100
    else -> targetDc
}.toByte()

val executor = Executors.newSingleThreadScheduledExecutor()
inline fun runPeriodic(microseconds: Long, name: String = "control", crossinline f: () -> Unit) {
    executor.scheduleAtFixedRate({
        val t = measureNanoTime(f)
        if (t > microseconds * 1E3) System.err.println("overran $name loop by ${round(t / 1E3 - microseconds)} microseconds")
    }, microseconds, microseconds, TimeUnit.MICROSECONDS)
}