package com.lynbrookrobotics.usbcontrolsystem.graph

import java.io.Flushable

interface Grapher : Flushable {
    val timeUnits: String
    val feedbackUnits: String
    val derivativeOfFeedbackUnits: String
    val outputUnits: String

    operator fun invoke(timeStamp: Double, feedback: Double, derivativeOfFeedback: Double, output: Double)
}