package de.r4md4c.gamedealz.domain

import de.r4md4c.gamedealz.data.DATA
import de.r4md4c.gamedealz.network.NETWORK
import org.koin.dsl.module.module

val DOMAIN = listOf(DATA, NETWORK, module {

})