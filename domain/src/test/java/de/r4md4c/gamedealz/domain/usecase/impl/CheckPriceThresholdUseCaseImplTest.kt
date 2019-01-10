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

package de.r4md4c.gamedealz.domain.usecase.impl

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.*
import de.r4md4c.gamedealz.data.entity.*
import de.r4md4c.gamedealz.data.repository.RegionsRepository
import de.r4md4c.gamedealz.data.repository.WatchlistRepository
import de.r4md4c.gamedealz.data.repository.WatchlistStoresRepository
import de.r4md4c.gamedealz.domain.model.ActiveRegion
import de.r4md4c.gamedealz.domain.model.CountryModel
import de.r4md4c.gamedealz.domain.model.CurrencyModel
import de.r4md4c.gamedealz.domain.usecase.GetCurrentActiveRegionUseCase
import de.r4md4c.gamedealz.domain.usecase.impl.internal.PickMinimalWatcheesPricesHelper
import de.r4md4c.gamedealz.domain.usecase.impl.internal.RetrievePricesGroupedByCountriesHelper
import de.r4md4c.gamedealz.network.model.Price
import de.r4md4c.gamedealz.network.model.Shop
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class CheckPriceThresholdUseCaseImplTest {

    @Mock
    private lateinit var watchlistRepository: WatchlistRepository

    @Mock
    private lateinit var watchlistStoresRepository: WatchlistStoresRepository

    @Mock
    private lateinit var currentActiveRegionUseCase: GetCurrentActiveRegionUseCase

    @Mock
    private lateinit var pickMinimalWatcheesPricesHelper: PickMinimalWatcheesPricesHelper

    @Mock
    private lateinit var pricesGroupedByCountriesHelper: RetrievePricesGroupedByCountriesHelper

    @Mock
    private lateinit var regionsRepostiory: RegionsRepository

    private lateinit var subject: CheckPriceThresholdUseCaseImpl

    @Before
    fun beforeEach() {
        MockitoAnnotations.initMocks(this)

        subject = CheckPriceThresholdUseCaseImpl(
            watchlistRepository,
            watchlistStoresRepository,
            regionsRepostiory,
            pricesGroupedByCountriesHelper,
            pickMinimalWatcheesPricesHelper
        )
    }


    @Test
    fun `it should return emptySet when allWatcheesWithStores are empty`() {
        runBlocking {
            ArrangeBuilder()
                .withWatcheesWithStores(emptyList())

            assertThat(subject.invoke()).isEmpty()
        }
    }

    @Test
    fun `it should return emptySet when all the watchee target price is equal to current price are empty`() {
        runBlocking {
            ArrangeBuilder()
                .withWatcheesWithStores((1..5).map {
                    WATCHEES_WITH_STORES.copy(
                        watchee = WATCHEE.copy(
                            currentPrice = 5f,
                            targetPrice = 5f
                        )
                    )
                })

            assertThat(subject.invoke()).isEmpty()
        }
    }

    @Test
    fun `it should return emptySet when all the watchees target price are greater than current price are empty`() {
        runBlocking {
            ArrangeBuilder()
                .withWatcheesWithStores((1..5).map {
                    WATCHEES_WITH_STORES.copy(
                        watchee = WATCHEE.copy(
                            currentPrice = 5f,
                            targetPrice = 6f
                        )
                    )
                })

            assertThat(subject.invoke()).isEmpty()
        }
    }

    @Test
    fun `it should retrieve prices from the prices grouper and pass to the price picker helper`() {
        runBlocking {
            ArrangeBuilder()
                .withWatcheesWithStores((1..5).map {
                    WATCHEES_WITH_STORES
                })
                .withResultFromPricesGroupedByCountriesHelper(emptyMap())
                .withResultFromPricePickerHelper(emptyMap())

            subject.invoke()

            verify(pricesGroupedByCountriesHelper).prices((1..5).map {
                WATCHEES_WITH_STORES.watchee
            })
            verify(pickMinimalWatcheesPricesHelper).pick(emptyMap())
        }
    }

    @Test
    fun `it should return empty set when price picker returns empty map`() {
        runBlocking {
            ArrangeBuilder()
                .withWatcheesWithStores((1..5).map {
                    WATCHEES_WITH_STORES
                })
                .withResultFromPricesGroupedByCountriesHelper(emptyMap())
                .withResultFromPricePickerHelper(emptyMap())

            val result = subject.invoke()

            assertThat(result).isEmpty()
        }
    }

    @Test
    fun `it should return result with 1 value when findById yields 1 not found`() {
        runBlocking {
            val priceWatcheeMap = mapOf(PRICE to WATCHEE, PRICE.copy(newPrice = 10f) to WATCHEE.copy(id = 2))
            ArrangeBuilder()
                .withWatcheesWithStores((1..5).map {
                    WATCHEES_WITH_STORES
                })
                .withResultFromPricesGroupedByCountriesHelper(emptyMap())
                .withResultFromPricePickerHelper(priceWatcheeMap)
                .withResultFromFindById(1, WATCHEE)

            val result = subject.invoke()

            assertThat(result).hasSize(1)
        }
    }

    @Test
    fun `it should return result with 2 values when findById finds all`() {
        runBlocking {
            val (watchee1, watchee2) = WATCHEE to WATCHEE.copy(id = 2)
            val priceWatcheeMap = mapOf(PRICE to watchee1, PRICE.copy(newPrice = 10f) to watchee2)
            ArrangeBuilder()
                .withWatcheesWithStores((1..5).map {
                    WATCHEES_WITH_STORES
                })
                .withResultFromPricesGroupedByCountriesHelper(emptyMap())
                .withResultFromPricePickerHelper(priceWatcheeMap)
                .withResultFromFindById(1, watchee1)
                .withResultFromFindById(2, watchee2)

            val result = subject.invoke()

            assertThat(result).hasSize(2)
        }
    }

    inner class ArrangeBuilder {
        init {
            runBlocking {
                whenever(regionsRepostiory.findById(any())).thenReturn(
                    RegionWithCountries(
                        mock(),
                        CURRENCY,
                        emptySet()
                    )
                )
                whenever(currentActiveRegionUseCase.invoke(anyOrNull())).thenReturn(ACTIVE_REGION)
                whenever(watchlistRepository.findById(any<String>())).thenReturn(produce(capacity = 1) { close() })
            }
        }

        fun withWatcheesWithStores(watcheeWithStores: List<WatcheeWithStores>) = apply {
            runBlocking {
                whenever(watchlistStoresRepository.allWatcheesWithStores()).thenReturn(watcheeWithStores)
            }
        }

        fun withResultFromPricePickerHelper(result: Map<Price, Watchee>) = apply {
            runBlocking {
                whenever(pickMinimalWatcheesPricesHelper.pick(any())).thenReturn(result)
            }
        }

        fun withResultFromPricesGroupedByCountriesHelper(result: Map<String, List<Price>>) = apply {
            runBlocking {
                whenever(pricesGroupedByCountriesHelper.prices(any())).thenReturn(result)
            }
        }

        fun withResultFromFindById(rowId: Long, returnedWatchee: Watchee) = apply {
            runBlocking {
                whenever(watchlistRepository.findById(rowId)).thenReturn(returnedWatchee)
            }
        }
    }

    private companion object {
        val ACTIVE_REGION = ActiveRegion("US", CountryModel("US"), CurrencyModel("EUR", ""))

        val WATCHEE = Watchee(1, "plainId", "title", 0, 0, 50f, 15f, "", "", "")

        val STORES = (1..10).map { Store("$it", "name$it", "color$it") }

        val WATCHEES_WITH_STORES = WatcheeWithStores(WATCHEE, STORES.toSet())

        val PRICE = Price(1f, 1f, 2, "", Shop("", ""), emptySet())

        val CURRENCY = Currency("EUR", "")
    }
}