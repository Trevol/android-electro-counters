package com.tavrida.counter_scanner.aggregation

import kotlinx.serialization.Serializable

@Serializable
data class DigitCount(val digit: Int, val count: Int)