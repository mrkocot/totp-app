package edu.mk.code.crypto

data class GeneratorSettings(
    val digits: Int = 6,
    val seconds: Int = 30,
    val beginning: Long = 0,
) {
    val millis: Int
        get() = seconds * 1000
}
