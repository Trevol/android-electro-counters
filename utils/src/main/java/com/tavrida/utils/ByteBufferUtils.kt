package com.tavrida.utils

import java.nio.ByteBuffer

fun ByteBuffer.toArray() = ByteArray(this.capacity())
    .also {
        this.rewind()
        this.get(it)
    }