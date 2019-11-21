package net.maxsmr.android.build.tasks.misc

import java.io.File
import java.lang.IllegalArgumentException

fun checkNotEmpty(str: String?, name: String) {
    if (str.isNullOrEmpty()) {
        throw IllegalArgumentException("Argument $name is empty")
    }
}

fun checkFilePathValid(path: String?, name: String) {
    if (path.isNullOrEmpty()) {
        throw IllegalArgumentException("Path $name is empty")
    }
    checkFileValid(File(path), name)
}

fun checkFileValid(file: File?, name: String) {
    if (file == null || !file.exists() || !file.isFile || file.length() <= 0) {
        throw IllegalArgumentException("File $file for $name is not valid")
    }
}