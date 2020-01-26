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

package de.r4md4c.gamedealz.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.r4md4c.gamedealz.common.annotation.Mockable
import hu.akarnokd.kotlin.flow.publish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

@Mockable
abstract class BaseMviViewModel<Event : MviViewEvent, State : MviState>(
    private val intentProcessors: Set<@JvmSuppressWildcards IntentProcessor<Event, State>>,
    private val store: ModelStore<State>,
    initEvent: Event? = null
) : ViewModel() {

    private val eventsChannel = Channel<Event>()

    init {
        eventsChannel.consumeAsFlow()
            .broadcastIn(viewModelScope)
            .asFlow()
            .publish { events ->
                val eventsWithInitEvent = events.onStart {
                    initEvent?.let { emit(it) }
                }
                intentProcessors.map { it.process(eventsWithInitEvent) }.merge()
            }
            .onEach { store.process(it) }
            .launchIn(viewModelScope)
    }

    val modelState = store.modelState().distinctUntilChanged()

    fun onViewEvents(viewEventFlow: Flow<Event>, viewScope: CoroutineScope) {
        viewEventFlow.onEach { eventsChannel.send(it) }.launchIn(viewScope)
    }

    override fun onCleared() {
        super.onCleared()
        eventsChannel.cancel()
        store.dispose()
    }
}