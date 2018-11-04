package com.lynbrookrobotics.usbcontrolsystem.graph

import com.lynbrookrobotics.usbcontrolsystem.runPeriodic
import org.knowm.xchart.QuickChart
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import java.util.*

class LiveGrapher(
        override val timeUnits: String,
        override val feedbackUnits: String,
        override val derivativeOfFeedbackUnits: String,
        override val outputUnits: String,
        val bufferSize: Int = 1000
) : Grapher {

    private val timeData = LinkedList<Double>()
    private val feedbackData = LinkedList<Double>()
    private val derivativeOfFeedbackData = LinkedList<Double>()
    private val outputData = LinkedList<Double>()

    lateinit var chart: XYChart
    lateinit var gui: SwingWrapper<XYChart>

    init {
        runPeriodic(20 * 1000, "grapher", this::flush)
    }

    @Synchronized
    override operator fun invoke(timeStamp: Double, feedback: Double, derivativeOfFeedback: Double, output: Double) {
        if (timeData.size > bufferSize) {
            timeData.removeFirst()
            feedbackData.removeFirst()
            derivativeOfFeedbackData.removeFirst()
            outputData.removeFirst()
        }

        timeData += timeStamp
        feedbackData += feedback
        derivativeOfFeedbackData += derivativeOfFeedback
        outputData += output
    }

    @Synchronized
    override fun flush() {
        if (timeData.isEmpty()) return

        if (!this::chart.isInitialized) {
            chart = QuickChart.getChart(
                    "Control System Graph",
                    "Time ($timeUnits)",
                    "Input/Output",
                    arrayOf(feedbackUnits, derivativeOfFeedbackUnits, outputUnits),
                    timeData.toDoubleArray(),
                    listOf(
                            feedbackData,
                            derivativeOfFeedbackData,
                            outputData
                    ).map(List<Double>::toDoubleArray).toTypedArray()
            )

            gui = SwingWrapper(chart)
            gui.displayChart()
        }


        chart.updateXYSeries(feedbackUnits, timeData, feedbackData, null)
        chart.updateXYSeries(derivativeOfFeedbackUnits, timeData, derivativeOfFeedbackData, null)
        chart.updateXYSeries(outputUnits, timeData, outputData, null)
        gui.repaintChart()
    }
}