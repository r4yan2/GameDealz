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

package de.r4md4c.gamedealz.network.service

import de.r4md4c.gamedealz.network.BuildConfig
import de.r4md4c.gamedealz.network.model.DataWrapper
import de.r4md4c.gamedealz.network.model.Deal
import de.r4md4c.gamedealz.network.model.HistoricalLowDTO
import de.r4md4c.gamedealz.network.model.IdToPlainMap
import de.r4md4c.gamedealz.network.model.ListWrapper
import de.r4md4c.gamedealz.network.model.Plain
import de.r4md4c.gamedealz.network.model.PriceDTO
import de.r4md4c.gamedealz.network.model.Region
import de.r4md4c.gamedealz.network.model.Stores
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

typealias RegionCodes = Map<String, Region>
typealias ShopPlains = Map<String, IdToPlainMap>
typealias PlainPriceList = Map<String, ListWrapper<PriceDTO>>

/**
 * A retrofit interface for accessing IsThereAnyDeal's API.
 * Check https://itad.docs.apiary.io/#reference/ for documentation.
 */
@Suppress("LongParameterList")
interface IsThereAnyDealService {

    @GET("v01/web/regions/")
    fun regions(): Deferred<DataWrapper<RegionCodes>>

    @GET("v02/web/stores")
    fun stores(@Query("region") region: String, @Query("country") country: String?): Deferred<Stores>

    @GET("v02/game/plain")
    fun plain(
        @Query("key") key: String = BuildConfig.API_KEY,
        @Query("shop") shop: String,
        @Query("game_id") gameId: String
    ): Deferred<DataWrapper<Plain>>

    @GET("v01/game/plain/list")
    fun allPlains(
        @Query("key") key: String = BuildConfig.API_KEY,
        @Query("shops") shops: String
    ): Deferred<DataWrapper<ShopPlains>>

    @GET("v01/game/prices")
    fun prices(
        @Query("key") key: String = BuildConfig.API_KEY,
        @Query("plains") plains: String,
        @Query("region") region: String? = null,
        @Query("country") country: String? = null,
        @Query("shops") shops: String? = null,
        @Query("added") added: Long? = null
    ):
            Deferred<DataWrapper<PlainPriceList>>

    @GET("v01/game/lowest")
    fun historicalLow(
        @Query("key") key: String = BuildConfig.API_KEY,
        @Query("plains") plains: String,
        @Query("region") region: String? = null,
        @Query("country") country: String? = null,
        @Query("shops") shops: String? = null
    ):
            Deferred<DataWrapper<Map<String, HistoricalLowDTO>>>

    @GET("v01/deals/list")
    fun deals(
        @Query("key") key: String = BuildConfig.API_KEY,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("region") region: String?,
        @Query("country") country: String?,
        @Query("shops") shops: String
    ): Deferred<DataWrapper<ListWrapper<Deal>>>

    @GET("v01/user/info")
    suspend fun userInfo(
        @Query("access_token") accessToken: String
    ): DataWrapper<Map<String, String>>
}
