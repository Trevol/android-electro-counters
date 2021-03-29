package com.tavrida.utils

inline fun <T> iif(cond: Boolean, ifVal: T, elseVal: T) = cond.iif(ifVal, elseVal)

@JvmName("Boolean_iif")
inline fun <T> Boolean.iif(ifVal: T, elseVal: T) = if (this) ifVal else elseVal

inline fun Boolean.toggle() = this.not()