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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.stfalcon.imageviewer.StfalconImageViewer
import de.r4md4c.commonproviders.date.DateFormatter
import de.r4md4c.commonproviders.di.viewmodel.components
import de.r4md4c.commonproviders.extensions.resolveThemeColor
import de.r4md4c.commonproviders.res.ResourcesProvider
import de.r4md4c.gamedealz.common.aware.LifecycleAware
import de.r4md4c.gamedealz.common.base.fragment.BaseFragment
import de.r4md4c.gamedealz.common.di.ForActivity
import de.r4md4c.gamedealz.common.exhaustive
import de.r4md4c.gamedealz.common.image.GlideApp
import de.r4md4c.gamedealz.common.mvi.MviViewModel
import de.r4md4c.gamedealz.common.mvi.UIEventsDispatcher
import de.r4md4c.gamedealz.common.navigation.Navigator
import de.r4md4c.gamedealz.common.notifications.ViewNotifier
import de.r4md4c.gamedealz.common.state.SideEffect
import de.r4md4c.gamedealz.common.state.StateVisibilityHandler
import de.r4md4c.gamedealz.core.CoreComponent
import de.r4md4c.gamedealz.core.coreComponent
import de.r4md4c.gamedealz.domain.model.ScreenshotModel
import de.r4md4c.gamedealz.feature.detail.DetailsFragmentArgs.Companion.fromBundle
import de.r4md4c.gamedealz.feature.detail.decorator.DetailsFragmentItemDecorator
import de.r4md4c.gamedealz.feature.detail.di.DaggerDetailComponent
import de.r4md4c.gamedealz.feature.detail.di.DaggerDetailsRetainedComponent
import de.r4md4c.gamedealz.feature.detail.item.AboutGameItem
import de.r4md4c.gamedealz.feature.detail.item.ExpandableScreenshotsHeader
import de.r4md4c.gamedealz.feature.detail.item.FilterHeaderItem
import de.r4md4c.gamedealz.feature.detail.item.HeaderItem
import de.r4md4c.gamedealz.feature.detail.item.ScreenshotItem
import de.r4md4c.gamedealz.feature.detail.item.toPriceItem
import de.r4md4c.gamedealz.feature.detail.model.PriceDetails
import de.r4md4c.gamedealz.feature.detail.mvi.DetailsMviEvent
import de.r4md4c.gamedealz.feature.detail.mvi.DetailsUIEvent
import de.r4md4c.gamedealz.feature.detail.mvi.DetailsViewState
import de.r4md4c.gamedealz.feature.detail.mvi.Section
import de.r4md4c.gamedealz.feature.detail.mvi.toMenuIdRes
import kotlinx.android.synthetic.main.fragment_game_detail.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("TooManyFunctions")
class DetailsFragment : BaseFragment() {

    private val title by lazy { fromBundle(requireArguments()).title }

    private val plainId by lazy { fromBundle(requireArguments()).plainId }

    @field:ForActivity
    @Inject
    lateinit var resourcesProvider: ResourcesProvider

    @Inject
    lateinit var dateFormatter: DateFormatter

    @Inject
    lateinit var viewNotifier: ViewNotifier

    @Inject
    lateinit var stateVisibilityHandler: StateVisibilityHandler

    @Inject
    lateinit var navigator: Navigator

    @Inject
    internal lateinit var detailsMviViewModel: MviViewModel<DetailsViewState, DetailsMviEvent>

    @Inject
    internal lateinit var detailsUIEventsDispatcher: UIEventsDispatcher<DetailsUIEvent>

    @Inject
    internal lateinit var lifecycleAware: LifecycleAware

    private val eventsChannel = Channel<DetailsMviEvent>()

    private val scopedComponent by components {
        DaggerDetailsRetainedComponent.factory()
            .create(
                fromBundle(requireArguments()),
                requireContext().coreComponent()
            )
    }

    private val gameDetailsAdapter by lazy { ItemAdapter<IItem<*, *>>() }
    private val pricesAdapter by lazy { ItemAdapter<IItem<*, *>>() }
    private val mainAdapter by lazy {
        FastItemAdapter.with<IItem<*, *>, ItemAdapter<IItem<*, *>>>(listOf(gameDetailsAdapter, pricesAdapter))
    }

    private val spanCount
        get() = resourcesProvider.getInteger(R.integer.screenshots_span_count)

    private val itemsDecorator by lazy {
        DetailsFragmentItemDecorator(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(lifecycleAware)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_game_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NavigationUI.setupWithNavController(collapsing_toolbar, toolbar, findNavController())
        stateVisibilityHandler.onViewCreated()
        stateVisibilityHandler.onRetryClick =
            { eventsChannel.offer(DetailsMviEvent.RetryClickEvent) }
        setupTitle()
        setupFab()
        setupRecyclerView()

        detailsMviViewModel.onViewEvents(
            eventsChannel.consumeAsFlow(),
            viewLifecycleOwner.lifecycleScope
        )
        detailsMviViewModel.modelState
            .onEach { renderState(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
        detailsUIEventsDispatcher.uiEvents
            .onEach { handleUIDetailsEvent(it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupRecyclerView() {
        content.apply {
            addItemDecoration(itemsDecorator)
            layoutManager =
                GridLayoutManager(context, spanCount).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            mainAdapter.getItem(position) ?: return spanCount
                            return when (mainAdapter.getItemViewType(position)) {
                                R.layout.layout_screenshot_item -> 1
                                else -> spanCount
                            }
                        }
                    }.apply { isSpanIndexCacheEnabled = true }
                }
            adapter = mainAdapter
        }
    }

    private fun setupFab() {
        addToWatchList.hide()
        addToWatchList.setOnClickListener {
            eventsChannel.offer(DetailsMviEvent.WatchlistFabClickEvent)
        }
    }

    private fun handleUIDetailsEvent(detailsUIEvent: DetailsUIEvent) {
        when (detailsUIEvent) {
            is DetailsUIEvent.AskUserToRemoveFromWatchlist -> askToRemove()
            is DetailsUIEvent.NavigateToAddToWatchlistScreen ->
                navigateToAddToWatchlistDialog(detailsUIEvent.priceDetails)
            is DetailsUIEvent.NotifyRemoveFromWatchlistSuccessfully ->
                viewNotifier.notify(
                    getString(
                        R.string.watchlist_remove_successfully,
                        detailsUIEvent.gameTitle
                    )
                )
        }.exhaustive
    }

    private fun setupTitle() {
        collapsing_toolbar.title = title
    }

    private fun renderState(state: DetailsViewState) = with(state) {
        if (loading) {
            stateVisibilityHandler.onSideEffect(SideEffect.ShowLoading)
        } else {
            stateVisibilityHandler.onSideEffect(SideEffect.HideLoading)
        }

        if (state.errorMessage.isNullOrEmpty().not()) {
            stateVisibilityHandler.onSideEffect(
                SideEffect.ShowError(Throwable(state.errorMessage))
            )
        }

        renderSections(state.sections)

        renderAddToWatchlistButton(isWatched)
    }

    private fun renderAddToWatchlistButton(isWatched: Boolean?) {
        if (isWatched == null) {
            addToWatchList.hide()
            return
        }

        addToWatchList.show()
        addToWatchList.setImageResource(
            if (isWatched) R.drawable.ic_added_to_watch_list
            else R.drawable.ic_add_to_watch_list
        )
    }

    private fun renderSections(sections: List<Section>) {
        val gameDetailsAdapterItems = mutableListOf<AbstractItem<*, *>>()
        val pricesAdapterItems = mutableListOf<AbstractItem<*, *>>()

        sections.forEach { section ->
            when (section) {
                is Section.GameInfoSection -> {
                    gameDetailsAdapterItems += HeaderItem(getString(section.titleRes))
                    gameDetailsAdapterItems += AboutGameItem(section.imageUrl, section.description)
                }
                is Section.ScreenshotSection -> {
                    gameDetailsAdapterItems += ExpandableScreenshotsHeader(
                        section.allScreenshots.size > spanCount,
                        section.isExpanded
                    ) { eventsChannel.offer(DetailsMviEvent.ExpandIconClicked) }

                    gameDetailsAdapterItems += section.visibleScreenshots
                        .mapIndexed { index, aScreenshot ->
                            ScreenshotItem(aScreenshot, index) { position ->
                                onScreenShotClick(
                                    section.allScreenshots,
                                    position
                                )
                            }
                        }
                }
                is Section.PriceSection -> {
                    val filterHeaderItem = FilterHeaderItem(
                        getString(section.titleRes),
                        R.menu.details_prices_sort_menu,
                        section.currentSortOrder.toMenuIdRes()
                    ) { sortType ->
                        eventsChannel.offer(DetailsMviEvent.PriceFilterChangeEvent(sortType))
                    }

                    val prices = section.priceDetails.map { priceDetails ->
                        priceDetails.toPriceItem(
                            resourcesProvider,
                            dateFormatter,
                            section.currentSortOrder,
                            navigator::navigateToUrl
                        )
                    }
                    pricesAdapterItems += filterHeaderItem
                    pricesAdapterItems += prices
                }
            }.exhaustive
        }.takeIf { sections.isNotEmpty() }?.let {
            gameDetailsAdapter.setNewList(gameDetailsAdapterItems.toList())
            pricesAdapter.setNewList(pricesAdapterItems.toList())
        }
    }

    private fun askToRemove() {
        viewLifecycleOwner.lifecycleScope.launch {
            val yes = ask()
            if (yes) {
                eventsChannel.offer(DetailsMviEvent.RemoveFromWatchlistYes)
            }
        }
    }

    private suspend fun ask() = suspendCoroutine<Boolean> { continuation ->
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(
                HtmlCompat.fromHtml(
                    getString(R.string.dialog_ask_remove_from_watch_list, title),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
            .setPositiveButton(android.R.string.yes) { dialog, _ -> continuation.resume(true); dialog.dismiss() }
            .setNegativeButton(android.R.string.no) { dialog, _ -> continuation.resume(false); dialog.dismiss() }
            .show()
    }

    private fun navigateToAddToWatchlistDialog(priceDetails: PriceDetails) {
        val directions = DetailsFragmentDirections.actionGamedetailsToAddToWatchlistDialog(
            title,
            plainId,
            priceDetails.priceModel
        )
        findNavController().navigate(directions)
    }

    private fun onScreenShotClick(screenshots: List<ScreenshotModel>, screenshotPosition: Int) {
        StfalconImageViewer.Builder<ScreenshotModel>(requireContext(), screenshots) { view, image ->
            val circularProgressDrawable = CircularProgressDrawable(requireContext()).apply {
                strokeWidth = resourcesProvider.getDimension(R.dimen.progress_stroke_size)
                centerRadius = resourcesProvider.getDimension(R.dimen.progress_size)
                setColorSchemeColors(requireContext().resolveThemeColor(R.attr.colorSecondary))
                start()
            }
            GlideApp.with(view)
                .load(image.full)
                .placeholder(circularProgressDrawable)
                .into(view)
        }.also {
            it.withStartPosition(screenshotPosition)
                .show()
        }
    }

    /*private fun applyRestOfScreenshots(
        screenshotsSection: Section.ScreenshotSection,
        remove: Boolean
    ) {
        val allScreenshots = screenshotsSection.screenshots
        val restScreenshots = screenshotsSection.restOfScreenshots(resourcesProvider)
        val thirdScreenshot = allScreenshots.getOrNull(spanCount - 1) ?: return

        val lastScreenshotPivot =
            gameDetailsAdapter.getAdapterPosition(thirdScreenshot.hashCode().toLong())

        if (remove) {
            gameDetailsAdapter.removeRange(lastScreenshotPivot + 1, restScreenshots.size)
        } else {
            gameDetailsAdapter.add(restScreenshots.mapIndexed { index, item ->
                ScreenshotItem(
                    item,
                    index + spanCount
                ) { position -> onScreenShotClick(screenshotsSection.screenshots, position) }
            })
        }
    }*/

    override fun onInject(coreComponent: CoreComponent) {
        super.onInject(coreComponent)
        if (::detailsMviViewModel.isInitialized.not()) {
            DaggerDetailComponent.factory()
                .create(requireActivity(), this, scopedComponent, coreComponent)
                .inject(this)
        }
    }
}
