package com.tavrida.utils

fun Boolean.assert() {
    this.assert("Assertion failed")
}

fun Boolean.assert(message: Any) {
    if (!this) {
        throw AssertionError(message)
    }
}

fun Boolean.assert(lazyMessage: () -> Any) {
    this.assert(lazyMessage())
}