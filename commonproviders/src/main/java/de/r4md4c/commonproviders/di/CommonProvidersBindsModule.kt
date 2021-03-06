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

package de.r4md4c.commonproviders.di

import dagger.Binds
import dagger.Module
import de.r4md4c.commonproviders.appcompat.AppCompatProvider
import de.r4md4c.commonproviders.appcompat.ApplicationAppCompatProvider
import de.r4md4c.commonproviders.configuration.AndroidConfigurationImpl
import de.r4md4c.commonproviders.configuration.ConfigurationProvider
import de.r4md4c.commonproviders.date.AndroidDateFormatter
import de.r4md4c.commonproviders.date.DateFormatter
import de.r4md4c.commonproviders.date.DateProvider
import de.r4md4c.commonproviders.date.JavaDateProvider
import de.r4md4c.commonproviders.preferences.AndroidSharedPreferencesProvider
import de.r4md4c.commonproviders.preferences.SharedPreferencesProvider
import de.r4md4c.commonproviders.res.AndroidResourcesProvider
import de.r4md4c.commonproviders.res.ResourcesProvider
import de.r4md4c.gamedealz.common.di.ForApplication

@Module
abstract class CommonProvidersBindsModule {

    @Binds
    internal abstract fun bindsAppCompatProvider(it: ApplicationAppCompatProvider): AppCompatProvider

    @Binds
    internal abstract fun bindsConfigurationProvider(it: AndroidConfigurationImpl): ConfigurationProvider

    @Binds
    internal abstract fun bindsDateProvider(it: JavaDateProvider): DateProvider

    @Binds
    internal abstract fun bindsSharedPreferencesProvider(it: AndroidSharedPreferencesProvider): SharedPreferencesProvider

    @ForApplication
    @Binds
    internal abstract fun bindsApplicationResourcesProvider(it: AndroidResourcesProvider): ResourcesProvider

    @Binds
    internal abstract fun bindsDateFormatter(it: AndroidDateFormatter): DateFormatter
}
