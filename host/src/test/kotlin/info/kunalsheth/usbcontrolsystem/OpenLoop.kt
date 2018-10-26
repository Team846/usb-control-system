package info.kunalsheth.usbcontrolsystem

import com.fazecast.jSerialComm.SerialPort
import info.kunalsheth.usbcontrolsystem.Microcontroller.AnalogFeedback.Analog1
import info.kunalsheth.usbcontrolsystem.Microcontroller.EncoderFeedback.Encoder1
import info.kunalsheth.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Period
import info.kunalsheth.usbcontrolsystem.Microcontroller.EncoderFeedbackType.Ticks
import info.kunalsheth.usbcontrolsystem.Microcontroller.Mode.DirectionAndDutyCycle
import info.kunalsheth.usbcontrolsystem.Microcontroller.MotorOutput.Motor1

fun main(args: Array<String>) {

    val ports = SerialPort.getCommPorts()
    ports.forEachIndexed { index, serialPort -> println("[$index]\t${serialPort.systemPortName}") }
    println("Enter index of serial port:")
    val device = ports[readLine()!!.toInt()]

    val mcu = Microcontroller(device, DirectionAndDutyCycle)

    println("Connecting...")
    mcu.flush()
    println("Connected!")

    val microGearMotor = PololuMicroGearMotor()
    val graph = ControlSystemGrapher("sec", "megaticks", "ticks / sec", "dc")

    var targetDc = 0

    runPeriodic(10 * 1000) {
        val feedback = mcu[Encoder1, Ticks]
        val speed = microGearMotor.getSpeed(feedback, mcu[Encoder1, Period])

        mcu[Motor1] = cap(targetDc)
        mcu.flush()

        graph(mcu.microsTimeStamp / 1E6, feedback / 1E6, speed, mcu[Motor1].toDouble())
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