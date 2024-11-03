package edu.mk.code.prompts

abstract class AlertableException : Exception() {
    abstract val titleId: Int
    abstract val messageId: Int
}
