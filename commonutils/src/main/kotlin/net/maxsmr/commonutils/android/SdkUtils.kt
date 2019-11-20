/*
  Copyright (c) 2018-present, SurfStudio LLC.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package net.maxsmr.commonutils.android

import android.os.Build


/**
 * Утилиты для проверки версии Api
 */
object SdkUtils {

    fun isPreLollipop(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP

    fun isAtLeastLollipop(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    fun isAtLeastMarshmallow(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    fun isAtLeastNougat(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    fun isAtLeastOreo(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun isAtLeastPie(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Запускает блок кода на устройствах с андроид версии KitKat и ниже
     *
     * @param block запускаемый блок кода
     */
    fun runOnPreLollipop(block: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            block()
        }
    }

    /**
     * Запускает блок кода на устройствах с андроид версии Lollipop и выше
     *
     * @param block запускаемый блок кода
     */
    fun runOnLollipop(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            block()
        }
    }

    /**
     * Запускает блок кода на устройствах с андроид версии Marshmallow и выше
     *
     * @param block запускаемый блок кода
     */
    fun runOnMarshmallow(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            block()
        }
    }

    /**
     * Запускает блок кода на устройствах с андроид версии Noughat и выше
     *
     * @param block запускаемый блок кода
     */
    fun runOnNoughat(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            block()
        }
    }

    /**
     * Запускает блок кода на устройствах с андроид версии Oreo и выше
     *
     * @param block запускаемый блок кода
     */
    fun runOnOreo(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            block()
        }
    }

    /**
     * Запускает блок кода на устройствах с андроид версии Pie и выше
     *
     * @param block запускаемый блок кода
     */
    fun runOnPie(block: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            block()
        }
    }

    /**
     * Запускает ту, или иную лямбду в зависимости какая версия api на устройстве
     */
    fun doIfSdk(atLeast: Boolean, ifTrue: () -> Unit, ifFalse: () -> Unit) =
            if (atLeast) ifTrue() else ifFalse()
}
