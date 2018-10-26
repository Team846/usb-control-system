package info.kunalsheth.usbcontrolsystem

import com.fazecast.jSerialComm.SerialPort
import info.kunalsheth.usbcontrolsystem.Microcontroller.EncoderFeedback.Encoder1
import info.kunalsheth.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Period
import info.kunalsheth.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Ticks
import info.kunalsheth.usbcontrolsystem.Microcontroller.MotorOutput.Motor1
import kotlin.math.round

fun main(args: Array<String>) {
    val ports = SerialPort.getCommPorts()
    ports.forEachIndexed { index, serialPort -> println("[$index]\t${serialPort.systemPortName}") }
    println("Enter index of serial port:")
    val device = ports[readLine()!!.toInt()]

    val mcu = Microcontroller(device, Microcontroller.Mode.DirectionAndDutyCycle)

    println("Connecting...")
    mcu.flush()
    println("Connected!")

    val graph = ControlSystemGrapher("sec", "kiloticks", "ticks / sec", "dc")

    var targetPosition = 0
    val currentLimit = 50
    val kP = 0.5

    runPeriodic(10 * 1000) {
        val feedback = -mcu[Encoder1, Ticks]
        val speed = 1.0 / -mcu[Encoder1, Period]

        val error = targetPosition - feedback
        val output = kP * error

        val limited = limitCurrent(output.toInt(), speed.toInt(), currentLimit)

//        mcu[Motor1] = cap(output.toInt())
        mcu[Motor1] = cap(limited)
        mcu.flush()

        graph(mcu.microsTimeStamp / 1E6, feedback / 1E3, speed, mcu[Motor1].toDouble())
    }

    while (true) {
        println("Where to?")
        targetPosition = readLine()!!.toInt()
        Thread.sleep(100)
    }
}