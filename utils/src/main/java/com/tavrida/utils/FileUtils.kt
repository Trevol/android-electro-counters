package com.tavrida.utils

import java.io.File

fun File(parent: File, child1: String, child2: String, vararg children: String) = listOf(child2, *children)
    .fold(File(parent, child1)) { result, child -> File(result, child) }

fun File(parent: String, child1: String, child2: String, vararg children: String) = listOf(child2, *children)
    .fold(File(parent, child1)) { result, child -> File(result, child) }