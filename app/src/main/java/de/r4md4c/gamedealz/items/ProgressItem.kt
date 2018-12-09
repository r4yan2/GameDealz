package de.r4md4c.gamedealz.items

import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import de.r4md4c.gamedealz.R

class ProgressItem : PrimaryDrawerItem() {

    override fun getLayoutRes(): Int = R.layout.item_progress

    override fun getType(): Int = R.id.progress_item

    override fun bindView(viewHolder: ViewHolder?, payloads: MutableList<Any?>) {
    }
}