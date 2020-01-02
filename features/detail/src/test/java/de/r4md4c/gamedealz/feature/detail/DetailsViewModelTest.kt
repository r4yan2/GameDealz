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

package de.r4md4c.gamedealz.feature.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import de.r4md4c.commonproviders.res.ResourcesProvider
import de.r4md4c.gamedealz.common.state.Event
import de.r4md4c.gamedealz.common.state.StateMachineDelegate
import de.r4md4c.gamedealz.domain.TypeParameter
import de.r4md4c.gamedealz.domain.model.CurrencyModel
import de.r4md4c.gamedealz.domain.model.HistoricalLowModel
import de.r4md4c.gamedealz.domain.model.PlainDetailsModel
import de.r4md4c.gamedealz.domain.model.PriceModel
import de.r4md4c.gamedealz.domain.model.PriceModelHistoricalLowModelPair
import de.r4md4c.gamedealz.domain.model.ScreenshotModel
import de.r4md4c.gamedealz.domain.model.ShopModel
import de.r4md4c.gamedealz.domain.usecase.GetPlainDetails
import de.r4md4c.gamedealz.domain.usecase.IsGameAddedToWatchListUseCase
import de.r4md4c.gamedealz.domain.usecase.RemoveFromWatchlistUseCase
import de.r4md4c.gamedealz.test.CoroutinesTestRule
import de.r4md4c.gamedealz.test.TestDispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.properties.Delegates

class DetailsViewModelTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var getPlainDetails: GetPlainDetails

    @Mock
    private lateinit var stateMachineDelegate: StateMachineDelegate

    @Mock
    private lateinit var isGameAddedToWatchListUseCase: IsGameAddedToWatchListUseCase

    @Mock
    private lateinit var removeFromWatchlistUseCase: RemoveFromWatchlistUseCase

    @Mock
    private lateinit var resourceProvider: ResourcesProvider

    private lateinit var testSubject: DetailsViewModel

    @Before
    fun beforeEach() {
        MockitoAnnotations.initMocks(this)

        testSubject = DetailsViewModel(
            TestDispatchers,
            getPlainDetails,
            stateMachineDelegate,
            isGameAddedToWatchListUseCase,
            removeFromWatchlistUseCase,
            resourceProvider
        )
    }

    @Test
    fun `isAddedToWatchList LiveData returns true when plainId is added to watchlist`() {
        ArrangeBuilder()
            .withGameAddedToWatchList(true)

        testSubject.loadIsAddedToWatchlist("")

        assertThat(testSubject.isAddedToWatchList.value).isTrue()
    }

    @Test
    fun `isAddedToWatchList LiveData returns false when plainId is not added to watchlist`() {
        ArrangeBuilder()
            .withGameAddedToWatchList(false)

        testSubject.loadIsAddedToWatchlist("")

        assertThat(testSubject.isAddedToWatchList.value).isFalse()
    }

    @Test
    fun `loadPlainDetails transition to OnLoading Event`() {
        testSubject.loadPlainDetails("")

        verify(stateMachineDelegate).transition(Event.OnLoadingStart)
    }

    @Test
    fun `loadPlainDetails posts to game information live data`() {
        ArrangeBuilder()
            .withShortDescription("shortDescription")
            .withHeaderImage("headerImage")

        testSubject.loadPlainDetails("")

        assertThat(testSubject.gameInformation.value).isEqualTo(
            GameInformation(
                "headerImage",
                "shortDescription"
            )
        )
    }

    @Test
    fun `loadPlainDetails does not post to game information live data when short description is missing`() {
        ArrangeBuilder()
            .withShortDescription(null)

        testSubject.loadPlainDetails("")

        assertThat(testSubject.gameInformation.value).isNull()
    }

    @Test
    fun `loadPlainDetails posts to screenshots when not empty`() {
        ArrangeBuilder()
            .withScreenshots(listOf(ScreenshotModel("http://path", "http://full_path")))

        testSubject.loadPlainDetails("")

        assertThat(testSubject.screenshots.value).hasSize(1)
    }

    @Test
    fun `loadPlainDetails does not post to screenshots live data when empty`() {
        ArrangeBuilder()
            .withScreenshots(emptyList())

        testSubject.loadPlainDetails("")

        assertThat(testSubject.screenshots.value).isNull()
    }

    @Test
    fun `loadPlainDetails posts price details live data when map is not empty`() {
        val shopModel = ShopModel("id", "", "")
        ArrangeBuilder()
            .withShopModelAndPriceModelAndHistoricalLow(shopModel, 5f, 3f)
            .withCurrencyModel(CurrencyModel("", ""))

        val ts = testSubject.prices.test()
        testSubject.loadPlainDetails("")

        assertThat(ts.value()).hasSize(1)
        assertThat(ts.value()).contains(
            PriceDetails(
                PriceModel(5f, 0f, 0, "", shopModel, emptySet()),
                shopModel,
                HistoricalLowModel(shopModel, 3f, 0, 0),
                CurrencyModel("", "")
            )
        )
    }

    @Test
    fun `loadPlainDetails does not posts price details when map empty`() {
        ArrangeBuilder()

        val ts = testSubject.prices.test()
        testSubject.loadPlainDetails("")

        ts.assertNoValue()
    }

    @Test
    fun `currentFilterItemChoice is set to current price`() {
        assertThat(testSubject.currentFilterItemChoice).isEqualTo(R.id.menu_item_current_best)
    }

    @Test
    fun `defaultFilterItemChoice sorts according to current best price by default`() {
        ArrangeBuilder()
            .withMultipleShopModelsAndHistoricalLows((1..10).map {
                Triple(
                    ShopModel("id$it", "", ""),
                    it.toFloat(),
                    10f - it
                )
            })
            .withCurrencyModel(CurrencyModel("", ""))

        val ts = testSubject.prices.test()
        testSubject.loadPlainDetails("")

        assertThat(ts.value()).hasSize(10)
        assertThat(ts.value()).isEqualTo(
            (1..10).map {
                val shopModel = ShopModel("id$it", "", "")
                PriceDetails(
                    PriceModel(it.toFloat(), 0f, 0, "", shopModel, emptySet()),
                    shopModel,
                    HistoricalLowModel(shopModel, 10f - it, 0, 0),
                    CurrencyModel("", "")
                )
            }
        )
    }

    @Test
    fun `defaultFilterItemChoice sorts according to historical low when filters changes to historical low`() {
        ArrangeBuilder()
            .withMultipleShopModelsAndHistoricalLows((1..10).map {
                Triple(
                    ShopModel("id$it", "", ""),
                    it.toFloat(),
                    10f - it
                )
            })
            .withCurrencyModel(CurrencyModel("", ""))

        val ts = testSubject.prices.test()
        testSubject.loadPlainDetails("")
        testSubject.onFilterChange(R.id.menu_item_historical_low)

        assertThat(ts.value()).hasSize(10)
        assertThat(ts.value()).isEqualTo(
            ((1..10).map {
                val shopModel = ShopModel("id$it", "", "")
                PriceDetails(
                    PriceModel(it.toFloat(), 0f, 0, "", shopModel, emptySet()),
                    shopModel,
                    HistoricalLowModel(shopModel, 10f - it, 0, 0),
                    CurrencyModel("", "")
                )
            }).reversed()
        )
    }

    @Test
    fun `removeFromWatchlist invokes remove from watchlist usecase`() {
        runBlocking {
            ArrangeBuilder()

            testSubject.removeFromWatchlist("plainId")

            verify(removeFromWatchlistUseCase).invoke(TypeParameter("plainId"))
        }
    }

    inner class ArrangeBuilder {
        private val templatePriceDetails = PlainDetailsModel(
            currencyModel = CurrencyModel("", ""),
            plainId = "",
            shopPrices = emptyMap(),
            screenshots = emptyList(),
            headerImage = "",
            aboutGame = "",
            shortDescription = "",
            drmNotice = ""
        )
        private var useCasePriceDetails by Delegates.observable(templatePriceDetails) { _, _, newValue ->
            runBlocking { whenever(getPlainDetails.invoke(any())).thenReturn(newValue) }
        }

        init {
            runBlocking {
                whenever(removeFromWatchlistUseCase.invoke(any())).thenReturn(true)
            }
        }

        fun withShortDescription(shortDescription: String?) = apply {
            useCasePriceDetails = useCasePriceDetails.copy(shortDescription = shortDescription)
        }

        fun withHeaderImage(headerImage: String?) = apply {
            useCasePriceDetails = useCasePriceDetails.copy(headerImage = headerImage)
        }

        fun withScreenshots(screenshots: List<ScreenshotModel>) = apply {
            useCasePriceDetails = useCasePriceDetails.copy(screenshots = screenshots)
        }

        fun withCurrencyModel(currencyModel: CurrencyModel) = apply {
            useCasePriceDetails = useCasePriceDetails.copy(currencyModel = currencyModel)
        }

        fun withShopModelAndPriceModelAndHistoricalLow(
            shopModel: ShopModel,
            currentBest: Float,
            historicalLow: Float?
        ) = apply {
            useCasePriceDetails = useCasePriceDetails.copy(
                shopPrices = mapOf(
                    shopModel to
                            PriceModelHistoricalLowModelPair(PriceModel(
                                currentBest,
                                0f,
                                0,
                                "",
                                shopModel,
                                emptySet()
                            ),
                                historicalLow?.run { HistoricalLowModel(shopModel, historicalLow, 0, 0) })
                )
            )
        }

        fun withMultipleShopModelsAndHistoricalLows(args: List<Triple<ShopModel, Float, Float>>) = apply {
            useCasePriceDetails = args.associateByTo(mutableMapOf()) { it.first }.mapValues {
                PriceModelHistoricalLowModelPair(
                    PriceModel(
                        it.value.second,
                        0f,
                        0,
                        "",
                        it.value.first,
                        emptySet()
                    ), HistoricalLowModel(it.value.first, it.value.third, 0, 0)
                )
            }.run { useCasePriceDetails.copy(shopPrices = this) }
        }

        fun withGameAddedToWatchList(isAdded: Boolean) = apply {
            runBlocking {
                whenever(isGameAddedToWatchListUseCase.invoke(any())).thenReturn(flowOf(isAdded))
            }
        }
    }

}