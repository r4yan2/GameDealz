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

package de.r4md4c.gamedealz.home

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import de.r4md4c.gamedealz.R
import de.r4md4c.gamedealz.common.navigator.Navigator
import de.r4md4c.gamedealz.deals.DealsFragment
import de.r4md4c.gamedealz.domain.model.StoreModel
import de.r4md4c.gamedealz.domain.model.displayName
import de.r4md4c.gamedealz.home.item.ErrorDrawerItem
import de.r4md4c.gamedealz.home.item.ProgressDrawerItem
import de.r4md4c.gamedealz.regions.RegionSelectionDialogFragment
import de.r4md4c.gamedealz.search.SearchFragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class HomeActivity : AppCompatActivity(), DealsFragment.OnFragmentInteractionListener,
    SearchFragment.OnFragmentInteractionListener, RegionSelectionDialogFragment.OnRegionChangeSubmitted {

    private lateinit var drawer: Drawer

    val drawerLayout: DrawerLayout
        get() = drawer.drawerLayout

    private val viewModel: HomeViewModel by viewModel { parametersOf(this) }

    private val navigator: Navigator by inject { parametersOf(this) }

    private val accountHeader by lazy {
        AccountHeaderBuilder()
            .withActivity(this)
            .withCompactStyle(true)
            .withOnAccountHeaderSelectionViewClickListener { _, _ -> handleAccountHeaderClick(); true }
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadDrawer(savedInstanceState)

        listenToViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        drawer.saveInstanceState(outState)
    }

    override fun onFragmentInteraction(uri: Uri, extras: Parcelable?) {
        viewModel.onNavigateTo(navigator, uri.toString(), extras)
    }

    override fun onSupportNavigateUp(): Boolean =
        NavigationUI.navigateUp(findNavController(R.id.nav_host_fragment), drawer.drawerLayout)

    override fun onRegionSubmitted() {
        viewModel.closeDrawer()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen) {
            drawer.closeDrawer()
            return
        }
        super.onBackPressed()
    }

    private fun listenToViewModel() {
        observeCurrentRegion()

        observeRegionsLoading()

        observeRegionSelectionDialog()

        observeStoreSelections()

        observeCloseDrawer()

        observeErrors()

        viewModel.init()
    }

    private fun observeCloseDrawer() {
        viewModel.closeDrawer.observe(this, Observer {
            drawer.closeDrawer()
        })
    }

    private fun observeStoreSelections() {
        viewModel.stores.observe(this, Observer { storeList ->
            val drawerItems = storeList.map {
                PrimaryDrawerItem()
                    .withName(it.name)
                    .withTag(it)
                    .withIdentifier(it.id.hashCode().toLong())
                    .withSetSelected(it.selected)
                    .withOnDrawerItemClickListener { _, _, drawerItem ->
                        viewModel.onStoreSelected(drawerItem.tag as StoreModel)
                        true
                    }
            }
            drawer.setItems(drawerItems)
        })
    }

    private fun observeRegionSelectionDialog() {
        viewModel.openRegionSelectionDialog.observe(this, Observer {
            RegionSelectionDialogFragment.create(it).show(supportFragmentManager, null)
        })
    }

    private fun observeRegionsLoading() {
        viewModel.regionsLoading.observe(this, Observer {
            showProgress(it)
        })
    }

    private fun loadDrawer(savedInstanceState: Bundle?) {
        drawer = DrawerBuilder(this)
            .withAccountHeader(accountHeader)
            .withMultiSelect(true)
            .withCloseOnClick(false)
            .withHasStableIds(true)
            .apply { savedInstanceState?.let { withSavedInstance(it) } }
            .build()
    }

    private fun showProgress(show: Boolean) {
        drawer.removeAllItems()
        if (show) {
            drawer.addItem(ProgressDrawerItem())
        }
    }

    private fun handleAccountHeaderClick() {
        viewModel.onRegionChangeClicked()
    }

    private fun observeCurrentRegion() {
        viewModel.currentRegion.observe(this, Observer {
            accountHeader.setSelectionFirstLine(it.regionCode)
            accountHeader.setSelectionSecondLine(it.country.displayName())
        })
    }

    private fun observeErrors() {
        viewModel.onError.observe(this, Observer {
            drawer.removeAllItems()
            drawer.addItem(ErrorDrawerItem(it) {
                viewModel.init()
            })
        })
    }
}
