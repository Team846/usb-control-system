package com.lynbrookrobotics.usbcontrolsystem

import org.knowm.xchart.QuickChart
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChart
import java.io.Flushable
import java.util.*

class ControlSystemGrapher(
        private val timeUnits: String,
        feedbackUnits: String,
        derivativeOfFeedbackUnits: String,
        outputUnits: String,
        val bufferSize: Int = 1000
) : Flushable {

    private val timeData = LinkedList<Double>()
    private val feedbackData = LinkedList<Double>()
    private val derivativeOfFeedbackData = LinkedList<Double>()
    private val outputData = LinkedList<Double>()

    private val feedbackSeries = "Feedback ($feedbackUnits)"
    private val derivativeOfFeedbackSeries = "Δ Feedback ($derivativeOfFeedbackUnits)"
    private val outputSeries = "Output ($outputUnits)"

    lateinit var chart: XYChart
    lateinit var gui: SwingWrapper<XYChart>

    init {
        runPeriodic(20 * 1000, "grapher", this::flush)
    }

    @Synchronized
    operator fun invoke(timeStamp: Double, feedback: Double, derivativeOfFeedback: Double, output: Double) {
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
                    arrayOf(feedbackSeries, derivativeOfFeedbackSeries, outputSeries),
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


        chart.updateXYSeries(feedbackSeries, timeData, feedbackData, null)
        chart.updateXYSeries(derivativeOfFeedbackSeries, timeData, derivativeOfFeedbackData, null)
        chart.updateXYSeries(outputSeries, timeData, outputData, null)
        gui.repaintChart()
    }
}