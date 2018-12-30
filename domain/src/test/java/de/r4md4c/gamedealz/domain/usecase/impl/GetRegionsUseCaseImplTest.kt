package de.r4md4c.gamedealz.domain.usecase.impl

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.*
import de.r4md4c.gamedealz.data.entity.RegionWithCountries
import de.r4md4c.gamedealz.data.repository.PlainsRepository
import de.r4md4c.gamedealz.network.model.Currency
import de.r4md4c.gamedealz.network.model.Region
import de.r4md4c.gamedealz.network.repository.PlainsRemoteRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import de.r4md4c.gamedealz.data.repository.RegionsRepository as LocalRegionsRepository
import de.r4md4c.gamedealz.network.repository.RegionsRemoteRepository as RemoteRegionsRepository

class GetRegionsUseCaseImplTest {

    @Mock
    private lateinit var localRepository: LocalRegionsRepository

    @Mock
    private lateinit var remoteRepository: RemoteRegionsRepository

    @Mock
    private lateinit var plainsRepository: PlainsRepository

    @Mock
    private lateinit var plainsRemoteRepository: PlainsRemoteRepository

    private lateinit var subject: GetRegionsUseCaseImpl

    @Before
    fun beforeEach() {
        MockitoAnnotations.initMocks(this)

        subject = GetRegionsUseCaseImpl(localRepository, remoteRepository, plainsRepository, plainsRemoteRepository)
    }

    @Test
    fun `it should retrieve from local repository when it returns non empty data`() {
        runBlocking {
            ArrangeBuilder()
                .withReturnedRegionWithCountries(listOf(regionLocal()))

            val result = subject()

            assertThat(result).hasSize(1)
            verify(localRepository).all()
            verify(remoteRepository, never()).regions()
        }
    }

    @Test
    fun `it should retrieve from remote repository then stores to local when local returns empty data`() {
        runBlocking {
            ArrangeBuilder()
                .withReturnedRegionWithCountries(emptyList(), emptyList())
                .withRemoteRegion(regionRemoteResponse())

            subject()

            verify(localRepository, times(2)).all()
            verify(localRepository).save(listOf(regionLocal()))
            verify(remoteRepository).regions()
        }
    }

    private fun regionRemoteResponse() = mapOf("eu" to Region(emptyList(), Currency("", "", "")))

    private fun regionLocal() = RegionWithCountries(
        de.r4md4c.gamedealz.data.entity.Region("eu", ""),
        de.r4md4c.gamedealz.data.entity.Currency("", ""), emptySet()
    )


    private inner class ArrangeBuilder {
        init {
            runBlocking {
                whenever(plainsRepository.count()).thenReturn(0)
                whenever(plainsRemoteRepository.plainsList(any())).thenReturn(emptyMap())
            }
        }

        fun withReturnedRegionWithCountries(
            regionWithCountries: List<RegionWithCountries>,
            secondValue: List<RegionWithCountries>? = null
        ) = apply {
            runBlocking {
                whenever(localRepository.all()).thenReturn(produce(capacity = Channel.UNLIMITED) {
                    offer(regionWithCountries)
                }, produce(capacity = Channel.UNLIMITED) { secondValue?.let { offer(it) } })
            }
        }

        fun withRemoteRegion(response: Map<String, Region>) = apply {
            runBlocking {
                whenever(remoteRepository.regions()).thenReturn(response)
            }
        }
    }
}