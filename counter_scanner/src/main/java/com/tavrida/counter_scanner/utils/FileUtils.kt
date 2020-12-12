package com.tavrida.counter_scanner.utils

import java.io.File
import java.nio.file.FileSystems

fun glob(pathPattern: String): Array<File> {
    // https://stonesoupprogramming.com/2017/12/07/kotlin-glob/
    val pathPatternFile = File(pathPattern)
    if (pathPatternFile.isDirectory) {
        return pathPatternFile.listFiles() ?: arrayOf()
    }
    val pattern = pathPatternFile.name
    val parentDir = pathPatternFile.parentFile
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
    val listFiles = parentDir.listFiles { f -> matcher.matches(f.toPath().fileName) }
    return listFiles ?: arrayOf()
}