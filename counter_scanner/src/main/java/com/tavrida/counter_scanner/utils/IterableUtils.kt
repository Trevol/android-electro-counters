package com.tavrida.counter_scanner.utils

fun <T1, T2, T3> Iterable<T1>.zip(iterable1: Iterable<T2>, iterable2: Iterable<T3>) =
    sequence {
        val it0 = iterator()
        val it1 = iterable1.iterator()
        val it2 = iterable2.iterator()

        while (it0.hasNext() && it1.hasNext() && it2.hasNext()) {
            yield(Triple(it0.next(), it1.next(), it2.next()))
        }
    }