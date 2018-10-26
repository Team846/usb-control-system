package info.kunalsheth.usbcontrolsystem

import kotlin.math.sign

class PololuMicroGearMotor(val samplesToAverage: Int = 2) {

    private var lastTicks = 0

    private val samples = DoubleArray(samplesToAverage)
    private var index = 0

    companion object {
        const val maxTicksPerSecond = 32500 /* Rev / Min */ *
                3 /* Tick / Rev */ *
                1.0 / 60 /* Min / Sec */
    }

    @Synchronized
    fun getSpeed(ticks: Int, period: Int): Double {
        val ticksPerSecond = if (lastTicks == ticks || period == 0) {
            0.0
        } else {
            1E6 / period * (ticks - lastTicks).sign
        }

        lastTicks = ticks

        val speed = 100 * ticksPerSecond / maxTicksPerSecond

        samples[index % samplesToAverage] = speed
        index++

        return samples.average()
    }
}