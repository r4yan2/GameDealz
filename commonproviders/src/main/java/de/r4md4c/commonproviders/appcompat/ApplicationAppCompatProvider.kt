/*
 * This file is part of GameDealz.
 *
 * GameDealz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * GameDealz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GameDealz.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.r4md4c.commonproviders.appcompat

import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicReference

internal class ApplicationAppCompatProvider : AppCompatProvider, FragmentActivityProvider {

    private val _fragmentActivity = AtomicReference<FragmentActivity?>(null)

    override var fragmentActivity: FragmentActivity?
        get() = _fragmentActivity.get()
        set(value) = _fragmentActivity.set(value)

    override var currentNightMode: NightMode
        get() = AppCompatDelegate.getDefaultNightMode().fromAppCompatNightMode()
        set(value) {
            AppCompatDelegate.setDefaultNightMode(NightMode.toAppCompatNightMode(value))
        }

    private fun Int.fromAppCompatNightMode(): NightMode =
        when (this) {
            AppCompatDelegate.MODE_NIGHT_YES -> NightMode.Enabled
            else -> NightMode.Disabled
        }
}
