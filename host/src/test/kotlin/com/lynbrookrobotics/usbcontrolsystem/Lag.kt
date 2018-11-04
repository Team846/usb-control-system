package com.lynbrookrobotics.usbcontrolsystem

import com.fazecast.jSerialComm.SerialPort.getCommPorts
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.EncoderFeedback.Encoder1
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Period
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Ticks
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.Mode.DirectionAndDutyCycle
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.MotorOutput.Motor1
import com.lynbrookrobotics.usbcontrolsystem.graph.LiveGrapher
import java.lang.Math.abs
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {

    val ports = getCommPorts()
    ports.forEachIndexed { index, serialPort -> println("[$index]\t${serialPort.systemPortName}") }
    println("Enter index of serial port:")
    val device = ports[readLine()!!.toInt()]

    val mcu = Microcontroller(device, DirectionAndDutyCycle)

    println("Connecting...")
    mcu.flush()
    println("Connected!")

    val graph = LiveGrapher("sec", "hectoticks", "ticks / sec", "dc")

    var i = 0.0
    var incr = 1
    runPeriodic(10 * 1000) {

        if (i >= 100) incr = -abs(incr)
        else if (i <= -100) incr = +abs(incr)
        i += incr

        val dc = cap(i.toInt())

        var flushes = 0
        val time = measureNanoTime {
            do {
                mcu[Motor1] = dc
                mcu.flush()
                flushes++
            } while (mcu[Motor1] != dc)
        }
        println("dc = $dc took $flushes flushes and ${time / 1000} microseconds.")

        val feedback = -mcu[Encoder1, Ticks]

        graph(
                mcu.microsTimeStamp / 1E6,
                feedback.toDouble() / 1E2,
                1.0 / -mcu[Encoder1, Period],
                mcu[Motor1].toDouble()
        )

    }
}