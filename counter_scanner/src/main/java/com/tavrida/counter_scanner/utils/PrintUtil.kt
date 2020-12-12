package com.tavrida.counter_scanner.utils

fun <T> println(message: Iterable<T>) {
    for (m in message) {
        print(m)
    }
    println()
}

fun print(arg: Any, vararg args: Any) {
    kotlin.io.print(arg)
    kotlin.io.print(" ")
    for (arg in args) {
        kotlin.io.print(arg)
        kotlin.io.print(" ")
    }
}

fun println(arg: Any, vararg args: Any) {
    print(arg, *args)
    println()
}