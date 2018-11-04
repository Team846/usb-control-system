package com.lynbrookrobotics.usbcontrolsystem.graph

import java.io.Closeable
import java.io.File
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.*
import kotlin.concurrent.thread

class CsvGrapher(
        fileName: String = Date().toString(),
        override val timeUnits: String,
        override val feedbackUnits: String,
        override val derivativeOfFeedbackUnits: String,
        override val outputUnits: String
) : Grapher, Closeable {

    private val file = File("$fileName.csv")
    private val writer = file.printWriter(US_ASCII) // kotlin.io.DEFAULT_BUFFER_SIZE

    init {
        writer.println("$timeUnits,$feedbackUnits,$derivativeOfFeedbackUnits,$outputUnits")
        Runtime.getRuntime().addShutdownHook(
                thread(start = false, block = this::close)
        )
    }

    override operator fun invoke(timeStamp: Double, feedback: Double, derivativeOfFeedback: Double, output: Double) {
        writer.println("$timeStamp,$feedback,$derivativeOfFeedback,$output")
    }

    override fun flush() = writer.flush()
    override fun close() = writer.close()
}