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

package de.r4md4c.gamedealz.feature.home.mvi.processor

import de.r4md4c.gamedealz.common.mvi.IntentProcessor
import de.r4md4c.gamedealz.domain.usecase.ToggleNightModeUseCase
import de.r4md4c.gamedealz.feature.home.mvi.HomeMviResult
import de.r4md4c.gamedealz.feature.home.mvi.HomeMviViewEvent
import de.r4md4c.gamedealz.feature.home.state.HomeMviViewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

internal class NightModeToggleIntentProcessor @Inject constructor(
    private val toggleNightModeUseCase: ToggleNightModeUseCase
) : IntentProcessor<HomeMviViewEvent, HomeMviViewState> {

    override fun process(viewEvent: Flow<HomeMviViewEvent>): Flow<HomeMviResult> =
        viewEvent.filterIsInstance<HomeMviViewEvent.NightModeToggleViewEvent>()
            .transformLatest {
                toggleNightModeUseCase()
            }
}
