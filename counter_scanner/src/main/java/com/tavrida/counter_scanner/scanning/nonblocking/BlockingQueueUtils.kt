package com.tavrida.counter_scanner.scanning.nonblocking

import java.util.concurrent.BlockingQueue

inline fun <E> BlockingQueue<E>.takeAll(): List<E> {
    val first = take()
    return mutableListOf<E>(first).also { this.drainTo(it) }
}

inline fun <E> BlockingQueue<E>.takeLast() = takeAll().last()

inline fun <E> BlockingQueue<E>.keepLast(): E? {
    val c = mutableListOf<E>()
    drainTo(c)
    return c.lastOrNull()
}