package com.tavrida.counter_scanner.utils

import java.io.File
import java.nio.file.FileSystems

fun glob(pathPattern: String): Array<File> {
    // https://stonesoupprogramming.com/2017/12/07/kotlin-glob/
    return glob(File(pathPattern))
}

fun glob(pathPattern: File): Array<File> {
    // https://stonesoupprogramming.com/2017/12/07/kotlin-glob/
    if (pathPattern.isDirectory) {
        return pathPattern.listFiles() ?: arrayOf()
    }
    val pattern = pathPattern.name
    val parentDir = pathPattern.parentFile
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
    val listFiles = parentDir.listFiles { f -> matcher.matches(f.toPath().fileName) }
    return listFiles ?: arrayOf()
}