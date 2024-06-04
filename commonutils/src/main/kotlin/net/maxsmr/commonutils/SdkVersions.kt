package net.maxsmr.commonutils

import android.os.Build

fun isPreKitkat(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT

fun isAtLeastKitkat(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

fun isPreLollipop(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP

fun isAtLeastLollipop(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

fun isPreMarshmallow(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M

fun isAtLeastMarshmallow(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

fun isAtLeastNougat(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

fun isAtLeastOreo(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

fun isAtLeastPie(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

fun isAtLeastQ(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

fun isAtLeastR(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

fun isAtLeastS(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun isAtLeastTiramisu(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun isAtLeastUpsideDownCake(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE