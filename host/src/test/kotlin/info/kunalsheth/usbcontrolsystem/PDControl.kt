package info.kunalsheth.usbcontrolsystem

import com.fazecast.jSerialComm.SerialPort
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

    val graph = ControlSystemGrapher("sec", "kiloticks", "ticks / sec", "dc")

    var targetPosition = 0
    var kP = 0.8
    var kD = 0.4

    runPeriodic(10 * 1000) {
        val feedback = mcu[Encoder1, Ticks]
        val speed = microGearMotor.getSpeed(feedback, mcu[Encoder1, Period])

        val error = targetPosition - feedback
        val output = kP * error - kD * speed

        mcu[Motor1] = cap(output.toInt())
        mcu.flush()

        graph(mcu.microsTimeStamp / 1E6, feedback / 1E3, speed, mcu[Motor1].toDouble())
    }

    while (true) {
        try {
            println("Where to?")
            val ln = readLine()!!

            when {
                ln.startsWith("kP ") -> kP = ln.removePrefix("kP ").toDouble()
                ln.startsWith("kD ") -> kD = ln.removePrefix("kD ").toDouble()
                else -> targetPosition = ln.toInt()
            }

            Thread.sleep(100)
        } catch (t: Throwable) {
            System.err.println(t.message)
        }
    }
}