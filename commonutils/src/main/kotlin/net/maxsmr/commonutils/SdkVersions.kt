package net.maxsmr.commonutils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

fun isPreKitkat(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.KITKAT)
fun isAtLeastKitkat(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

fun isPreLollipop(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.LOLLIPOP)
fun isAtLeastLollipop(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

fun isPreMarshmallow(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.M)
fun isAtLeastMarshmallow(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.N)
fun isAtLeastNougat(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.O)
fun isAtLeastOreo(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.P)
fun isAtLeastPie(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.Q)
fun isAtLeastQ(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.R)
fun isAtLeastR(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.S)
fun isAtLeastS(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.TIRAMISU)
fun isAtLeastTiramisu(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun isAtLeastUpsideDownCake(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

@ChecksSdkIntAtLeast(api=Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun isAtLeastVanillaIceCream(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM