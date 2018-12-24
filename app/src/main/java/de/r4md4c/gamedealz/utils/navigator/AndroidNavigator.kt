package de.r4md4c.gamedealz.utils.navigator

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavController
import de.r4md4c.gamedealz.R
import de.r4md4c.gamedealz.search.ARG_SEARCH_TERM
import de.r4md4c.gamedealz.utils.deepllink.DeepLinks

class AndroidNavigator(private val navController: NavController) : Navigator {

    override fun navigate(uri: String) {
        val parsedUri = Uri.parse(uri)
        navController.navigate(R.id.action_dealsFragment_to_searchFragment, Bundle().apply {
            putString(ARG_SEARCH_TERM, parsedUri.getQueryParameter(DeepLinks.QUERY_SEARCH_TERM))
        })
    }

}