package com.madday.energybalance


import io.mockk.*
import okhttp3.*
import okhttp3.Call
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.junit5.KoinTestExtension
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.fail

private const val example_response = """
    {
        "status":"0", 
        "timestamp":1651589940419, 
        "data":{
            "przesyly": [
                {"wartosc": -84, "rownolegly": false, "wartosc_plan": 60, "id": "SE"}, 
                {"wartosc": 1011, "rownolegly": true, "wartosc_plan": -79, "id": "DE"}, 
                {"wartosc": -1488, "rownolegly": true, "wartosc_plan": -437, "id": "CZ"}, 
                {"wartosc": -812, "rownolegly": true, "wartosc_plan": -777, "id": "SK"}, 
                {"wartosc": 0, "rownolegly": false, "wartosc_plan": 0, "id": "UA"}, 
                {"wartosc": -412, "rownolegly": false, "wartosc_plan": -439, "id": "LT"}
                ], 
            "podsumowanie": {
                "wodne": 212, 
                "wiatrowe": 995, 
                "PV": 2458, 
                "generacja": 18164, 
                "zapotrzebowanie": 16305, 
                "czestotliwosc": 50.019, 
                "inne": 0, 
                "cieplne": 14500
                }}}
"""


internal class DataSourceModuleTest  : KoinTest {

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                single { mockk<DataSourceScheduler>() }
                single { mockk<DownloadRepeater>() }
                single { mockk<OkHttpClient>() }
                single { mockk<DataSourceObserver>() }
                single { DataSourceModule() }
            })
    }

    @Test
    fun `ignore schedule if there is no observer`() {
        // Given
        val sut: DataSourceModule = get()
        val mockDataSourceScheduler: DataSourceScheduler = get()

        // when
        sut.start()

        // then
        verify(atMost = 0, atLeast = 0) { mockDataSourceScheduler.schedule(any()) }
    }

    @Test
    fun `downloaded data are parsed and delivered to observer`() {
        // given
        val sut: DataSourceModule = get()
        val mockObserver: DataSourceObserver = get()
        val mockDataSourceScheduler: DataSourceScheduler = get()
        val mockDownloadRepeater: DownloadRepeater = get()
        val mockOkHttpClient: OkHttpClient = get()

        sut.addObserver(mockObserver)
        val download = slot<()->Unit>()
        val repeat = slot<()->Unit>()
        val takenData = slot<MainModuleData>()

        // capture and call, we want to test download not mock :D
        every {
            mockDataSourceScheduler.schedule(capture(download))
        } answers {
            download.invoke()
        }

        every {
            mockDownloadRepeater.repeatUntilNoError(capture(repeat))
        } answers {
            repeat.invoke()
        }

        val mockCall = mockk<Call>()
        val mockResponse = mockk<Response>()
        val mockResponseBody = mockk<ResponseBody>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns mockResponseBody
        every { mockResponseBody.string() } returns example_response
        every { mockObserver.invoke(capture(takenData)) } just runs

        // when
        sut.start()

        // then
        assertEquals(2458, takenData.captured.summary.PV)
        assertEquals(18164, takenData.captured.summary.generated)
        assertEquals(16305, takenData.captured.summary.needed)
        assertEquals(14500, takenData.captured.summary.fossil)
        assertEquals(0, takenData.captured.summary.others)
        assertEquals(212, takenData.captured.summary.water)
        assertEquals(995, takenData.captured.summary.wind)
        assertEquals(50.019f, takenData.captured.summary.frequency)
        assertEquals(1651589940419, takenData.captured.summary.time)
    }

    @Test
    fun `throw unable to download data when rest call failed`() {
        // given
        val sut: DataSourceModule = get()
        val mockObserver: DataSourceObserver = get()
        val mockDataSourceScheduler: DataSourceScheduler = get()
        val mockDownloadRepeater: DownloadRepeater = get()
        val mockOkHttpClient: OkHttpClient = get()

        sut.addObserver(mockObserver)
        val download = slot<()->Unit>()
        val repeat = slot<()->Unit>()

        // capture and call, we want to test download not mock :D
        every {
            mockDataSourceScheduler.schedule(capture(download))
        } answers {
            download.invoke()
        }

        every {
            mockDownloadRepeater.repeatUntilNoError(capture(repeat))
        } answers {
            repeat.invoke()
        }

        val mockCall = mockk<Call>()
        val mockResponse = mockk<Response>()
        every { mockOkHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse andThenThrows IOException()
        every { mockResponse.isSuccessful } returns false

        // when
        try {
            sut.start()
            fail()
        } catch (ex: UnableToDownloadDataError) {
            // then
        }
        // then
        verify(atLeast = 0, atMost = 0) { mockObserver.invoke(any()) }

        // when
        try {
            sut.start()
            fail()
        } catch (ex: UnableToDownloadDataError) {
            // then
        }
        // then
        verify(atLeast = 0, atMost = 0) { mockObserver.invoke(any()) }
    }
}