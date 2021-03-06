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

package de.r4md4c.gamedealz.network.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ShopDTO(val id: String, val name: String)

@JsonSerializable
data class PriceDTO(
    @Json(name = "price_new") val newPrice: Float,
    @Json(name = "price_old") val oldPrice: Float,
    @Json(name = "price_cut") val priceCutPercentage: Short,
    val url: String,
    val shop: ShopDTO,
    val drm: Set<String>
)
