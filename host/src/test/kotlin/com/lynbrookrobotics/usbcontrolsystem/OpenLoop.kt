package com.lynbrookrobotics.usbcontrolsystem

import com.fazecast.jSerialComm.SerialPort
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.AnalogFeedback.Analog1
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.EncoderFeedback.Encoder1
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Ticks
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.Mode.DirectionAndDutyCycle
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.MotorOutput.Motor1

fun main(args: Array<String>) {

    val ports = SerialPort.getCommPorts()
    ports.forEachIndexed { index, serialPort -> println("[$index]\t${serialPort.systemPortName}") }
    println("Enter index of serial port:")
    val device = ports[readLine()!!.toInt()]

    val mcu = Microcontroller(device, DirectionAndDutyCycle)

    println("Connecting...")
    mcu.flush()
    println("Connected!")

    val graph = ControlSystemGrapher("sec", "megaticks", "ticks / sec", "dc")

    var targetDc = 0

    var lastPosition = 0
    runPeriodic(10 * 1000) {
        val feedback = mcu[Encoder1, Ticks]
        val speed = feedback - lastPosition

        mcu[Motor1] = cap(targetDc)
        mcu.flush()

        graph(mcu.microsTimeStamp / 1E6, feedback / 1E6, speed.toDouble(), mcu[Motor1].toDouble())

        lastPosition = feedback
    }

    while (true) {
        try {
            println("Analog1 = ${mcu[Analog1]}, Encoder1 = ${mcu[Encoder1, Ticks]}")
            targetDc = readLine()!!.toInt()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}