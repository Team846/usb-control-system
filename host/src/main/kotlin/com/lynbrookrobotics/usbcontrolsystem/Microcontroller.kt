package com.lynbrookrobotics.usbcontrolsystem

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING
import com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING
import com.lynbrookrobotics.usbcontrolsystem.Microcontroller.Mode.Servo
import java.io.Closeable
import java.io.File
import java.io.Flushable

class Microcontroller(
        val device: SerialPort, val desiredMode: Mode = Servo
) : Flushable, Closeable {

    init {
        device.baudRate = 115200
        device.setComPortTimeouts(
                TIMEOUT_WRITE_BLOCKING + TIMEOUT_READ_BLOCKING,
                10, 10
        )
        device.openPort()
    }

    enum class Mode(val bits: Byte) {
        Servo(1), DirectionAndDutyCycle(2)
    }

    enum class MotorOutput {
        Motor1, Motor2, Motor3
    }

    enum class AnalogFeedback {
        Analog1, Analog2, Analog3
    }

    enum class EncoderFeedback {
        Encoder1, Encoder2, Encoder3
    }

    enum class EncoderFeedbackType {
        Ticks, Period
    }

    val microsTimeStamp
        get() = 26.let {
            concat(
                    receiveBuffer[it + 0],
                    receiveBuffer[it + 1],
                    receiveBuffer[it + 2],
                    receiveBuffer[it + 3],
                    receiveBuffer[it + 4],
                    receiveBuffer[it + 5],
                    receiveBuffer[it + 6],
                    receiveBuffer[it + 7]
            )
        }

    val mode get() = receiveBuffer[25]

    private val receiveBuffer = ByteArray(34)
    private val sendBuffer = ByteArray(8)

    operator fun set(motor: MotorOutput, dutyCycle: Byte) {
        sendBuffer[motor.ordinal + 1] = dutyCycle
    }

    operator fun get(motor: MotorOutput) = (22 + motor.ordinal).let {
        receiveBuffer[it]
    }

    operator fun get(adc: AnalogFeedback) = (16 + adc.ordinal * 2).let {
        concat(receiveBuffer[it], receiveBuffer[it + 1])
    }

    operator fun get(enc: EncoderFeedback, type: EncoderFeedbackType) = (enc.ordinal * 8 + type.ordinal * 4).let {
        concat(receiveBuffer[it], receiveBuffer[it + 1], receiveBuffer[it + 2], receiveBuffer[it + 3])
    }

    override fun flush() {
        sendBuffer[0] = desiredMode.bits

        val readResult = device.readBytes(receiveBuffer, receiveBuffer.size.toLong())
        if (readResult != receiveBuffer.size) System.err.println("device.readBytes(...) returned $readResult, expected ${receiveBuffer.size}")

        val writeResult = device.writeBytes(sendBuffer, sendBuffer.size.toLong())
        if (writeResult != sendBuffer.size) System.err.println("device.writeBytes(...) returned $readResult, expected ${sendBuffer.size}")
    }

    override fun close() {
        device.closePort()
    }


    private fun concat(b0: Byte, b1: Byte): Short {
        val i0 = b0.toInt() and 0xFF
        val i1 = b1.toInt() and 0xFF
        return ((i1 shl 8) or
                (i0 shl 0)
                ).toShort()
    }

    private fun concat(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Int {
        val i0 = b0.toInt() and 0xFF
        val i1 = b1.toInt() and 0xFF
        val i2 = b2.toInt() and 0xFF
        val i3 = b3.toInt() and 0xFF
        return (i3 shl 24) or
                (i2 shl 16) or
                (i1 shl 8) or
                (i0 shl 0)
    }

    private fun concat(b0: Byte, b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte, b7: Byte): Long {
        val i0 = b0.toLong() and 0xFF
        val i1 = b1.toLong() and 0xFF
        val i2 = b2.toLong() and 0xFF
        val i3 = b3.toLong() and 0xFF
        val i4 = b4.toLong() and 0xFF
        val i5 = b5.toLong() and 0xFF
        val i6 = b6.toLong() and 0xFF
        val i7 = b7.toLong() and 0xFF

        return (i7 shl 56) or
                (i6 shl 48) or
                (i5 shl 40) or
                (i4 shl 32) or
                (i3 shl 24) or
                (i2 shl 16) or
                (i1 shl 8) or
                (i0 shl 0)
    }
}