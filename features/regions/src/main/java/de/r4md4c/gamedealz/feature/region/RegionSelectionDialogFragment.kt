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

package de.r4md4c.gamedealz.feature.region

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.r4md4c.commonproviders.di.viewmodel.ViewModelFactoryCreator
import de.r4md4c.gamedealz.common.aware.DrawerAware
import de.r4md4c.gamedealz.core.coreComponent
import de.r4md4c.gamedealz.feature.region.di.DaggerRegionsComponent
import de.r4md4c.gamedealz.feature.regions.R
import kotlinx.android.synthetic.main.dialog_region_choice.view.*
import javax.inject.Inject
import kotlin.properties.Delegates

class RegionSelectionDialogFragment : DialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactoryCreator

    private val viewModel by viewModels<RegionSelectionViewModel> { viewModelFactory.create(this) }

    private var dialogView: View by Delegates.notNull()

    // A hack to fix a bug in spinners that makes them fire onItemSelected without any user interaction.
    private var skipFirstSelection: Int = 1

    private val activeRegion by lazy {
        RegionSelectionDialogFragmentArgs.fromBundle(requireArguments()).region
    }

    override fun onAttach(context: Context) {
        onInject(context)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView =
            LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_region_choice, null)

        return MaterialAlertDialogBuilder(requireActivity())
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val regionIndex = savedInstanceState?.getInt(STATE_REGION_INDEX)
        val countryIndex = savedInstanceState?.getInt(STATE_COUNTRY_INDEX)

        setupRegions(regionIndex)
        setupCountries(countryIndex)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(dialogView) {
            outState.putInt(STATE_REGION_INDEX, region_spinner.selectedItemPosition)
            outState.putInt(STATE_COUNTRY_INDEX, country_spinner.selectedItemPosition)
        }
    }

    override fun onResume() {
        super.onResume()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            submitResult()
        }
    }

    private fun submitResult() {
        with(dialogView) {
            val selectedRegionCode = viewModel.regions.value?.let {
                it.regions[region_spinner.selectedItemPosition]
            }
            val selectedCountryCode =
                viewModel.countries.value
                    ?.takeIf { country_spinner.selectedItemPosition > -1 }
                    ?.let {
                        it.countries[country_spinner.selectedItemPosition]
                    }
            (selectedRegionCode to selectedCountryCode).takeIf {
                it.first != null && it.second != null
            }?.let {
                viewModel.onSubmitResult(it.first!!, it.second!!)
                (requireActivity() as? DrawerAware)?.closeDrawer()
                dismiss()
            }
        }
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun setupRegions(regionIndex: Int?) {
        viewModel.requestRegions(activeRegion, regionIndex)
        viewModel.regions.observe(this, Observer { regionSelectedModel ->
            with(dialogView) {
                region_spinner.adapter =
                    ArrayAdapter(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        android.R.id.text1,
                        regionSelectedModel.regions
                    )
                region_spinner.setSelection(regionSelectedModel.activeRegionIndex)

                region_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (skipFirstSelection-- <= 0) {
                            viewModel.onRegionSelected(parent.adapter.getItem(position) as String)
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun setupCountries(countryIndex: Int?) {
        viewModel.requestCountriesUnderRegion(activeRegion, countryIndex)
        viewModel.countries.observe(this, Observer { selectedCountryModel ->
            with(dialogView) {
                country_spinner.adapter =
                    ArrayAdapter(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        android.R.id.text1,
                        selectedCountryModel.countries
                    )
                selectedCountryModel.activeCountryIndex?.let {
                    country_spinner.setSelection(it)
                }
            }
        })
    }

    private fun onInject(context: Context) {
        DaggerRegionsComponent.factory()
            .create(context.coreComponent())
            .inject(this)
    }

    companion object {
        private const val STATE_REGION_INDEX = "region_index"
        private const val STATE_COUNTRY_INDEX = "country_index"
    }
}
