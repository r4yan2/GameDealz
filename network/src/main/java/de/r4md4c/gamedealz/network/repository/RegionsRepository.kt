package de.r4md4c.gamedealz.network.repository

import de.r4md4c.gamedealz.network.service.RegionCodes

interface RegionsRepository {

    /**
     * Retrieves regions from remote repository
     */
    suspend fun regions(): RegionCodes

}
