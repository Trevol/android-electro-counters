package com.tavrida.counter_scanner.scanning

import java.util.concurrent.BlockingQueue

inline fun <E> BlockingQueue<E>.waitAndTakeAll(): List<E> {
    val first = take()
    return mutableListOf<E>(first).also { this.drainTo(it) }
}

inline fun <E> BlockingQueue<E>.waitAndTakeLast() = waitAndTakeAll().last()

inline fun <E> BlockingQueue<E>.keepLastOrNull() =
    mutableListOf<E>().also { this.drainTo(it) }.lastOrNull()