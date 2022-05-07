package com.madday.energybalance

import androidx.lifecycle.MutableLiveData
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension


class MainModelViewTest : KoinTest {

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
            modules(
                module {
                    single { mockk<MutableLiveData<BusinessData>>() }
                    single { mockk<LabelProvider>() }
                    single { mockk<DataSourceModule>() }
                    single { MainModelView() }
                })
    }


    private val sut: MainModelView by inject()

    @Test
    fun `observer notified when data updated`() {
        // given
        val mockObserver: MutableLiveData<BusinessData> = get()
        val mockLabelProvider: LabelProvider = get()

        every { mockObserver.postValue(any()) } just runs
        every { mockLabelProvider.export() } returns "EXPORT"
        every { mockLabelProvider.import() } returns "IMPORT"

        val data = MainModuleData(
            mutableListOf(),
            Summary(
                10,
                20,
                30,
                40,
                50,
                60.0f,
                70,
                80,
                Integer.MAX_VALUE + 100L
            )
        )

        // when
        sut.updateData(data)

        // then
        val expectedData = mutableMapOf(
            "water" to "10",
            "wind" to "20",
            "pv" to "30",
            "generated" to "40",
            "needed" to "50",
            "frequency" to "60.0",
            "others" to "70",
            "fossil" to "80",
            "balance" to "10 IMPORT",
            "time" to "1970-01-25 21:31:23"
        )

        verify { mockObserver.postValue(BusinessData(mapOf(),expectedData)) }
    }

    @Test
    fun `begin observing data source when modelview started`() {
        // given
        val mockDataSourceModule: DataSourceModule = get()

        every { mockDataSourceModule.addObserver ( any() ) } just runs
        every { mockDataSourceModule.start() } just runs

        // when
        sut.start()

        // then
        verify(atMost = 1, atLeast = 1) { mockDataSourceModule.addObserver ( any() ) }
    }
}