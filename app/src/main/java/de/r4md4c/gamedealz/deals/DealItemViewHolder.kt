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

package de.r4md4c.gamedealz.deals

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import de.r4md4c.gamedealz.R
import de.r4md4c.gamedealz.common.image.GlideApp
import de.r4md4c.gamedealz.deals.model.DealRenderModel
import kotlinx.android.synthetic.main.layout_deal_item.view.*

class DealItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun onBind(dealModel: DealRenderModel?, clickListener: (DealRenderModel) -> Unit) {
        with(itemView) {
            setOnClickListener { dealModel?.let { clickListener(dealModel) } }

            name.text = dealModel?.title
            price.text = dealModel?.newPrice
            stores.text = dealModel?.storesAndTime

            GlideApp.with(image)
                .load(dealModel?.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .fitCenter()
                .into(image)
        }
    }


}