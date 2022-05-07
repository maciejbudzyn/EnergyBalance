package com.madday.energybalance


import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException

typealias DataSourceObserver = (newData: MainModuleData) -> Unit

class DataSourceScheduler {
    fun schedule (runner: ()->Unit) {
        GlobalScope.launch {
            doBackground(runner)
        }
    }

    private fun doBackground(runner: ()->Unit) {
        runner()
    }
}

data class PSETransmission(
    @SerializedName("wartosc") val value: Int,
    @SerializedName("rownolegly") val parallel: Boolean,
    @SerializedName("wartosc_plan") val value_plan: Int,
    @SerializedName("id") val id: String
)

data class PSESummary(
    @SerializedName("wodne") val water: Int,
    @SerializedName("wiatrowe") val wind: Int,
    @SerializedName("PV") val pv: Int,
    @SerializedName("generacja") val generated: Int,
    @SerializedName("zapotrzebowanie") val needed: Int,
    @SerializedName("czestotliwosc") val frequency: Float,
    @SerializedName("inne") val others: Int,
    @SerializedName("cieplne") val thermal: Int
)

data class TransmissionData(
    @SerializedName("przesyly") val transmission: List<PSETransmission>,
    @SerializedName("podsumowanie") val summary: PSESummary
)

data class PSEData(
    @SerializedName("status") val status: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("data") val data: TransmissionData
)

class UnableToDownloadDataError : Exception("Unable to download PSE data!")

class DownloadRepeater {
    fun repeatUntilNoError(runner: ()->Unit) {
        while (true) {
            try {
                runner.invoke()
                return
            } catch (ex: UnableToDownloadDataError) {
            }
        }
    }
}

class DataSourceModule : KoinComponent {
    private val dataSourceScheduler: DataSourceScheduler by inject()
    private val downloadRepeater: DownloadRepeater by inject()
    private val client: OkHttpClient by inject()
    private var dataSourceObserver: DataSourceObserver? = null

    fun addObserver(dataSourceObserver: DataSourceObserver) {
        this.dataSourceObserver = dataSourceObserver
    }

    fun start() {
        if (dataSourceObserver != null) {
            dataSourceScheduler.schedule {
                downloadRepeater.repeatUntilNoError {
                    try {
                        val data = client.newCall(
                            Request.Builder().method("GET", null).url(
                                "https://www.pse.pl/transmissionMapService"
                            ).build()
                        ).execute().let {
                            if (it.isSuccessful) {
                                val json = it.body()?.string() ?: ""
                                Gson().fromJson(json, PSEData::class.java)
                            } else {
                                throw UnableToDownloadDataError()
                            }
                        }
                        dataSourceObserver?.invoke(data.bind(data))
                    } catch(ex: IOException) {
                        throw UnableToDownloadDataError()
                    }
                }
            }
        }
    }
}

fun PSEData.bind(input: PSEData) =
    MainModuleData(
        input.data.transmission.map {
            TransferNode(
                it.value,
                it.value_plan,
                it.parallel,
                it.id
            )
        },
        Summary(
            input.data.summary.water,
            input.data.summary.wind,
            input.data.summary.pv,
            input.data.summary.generated,
            input.data.summary.needed,
            input.data.summary.frequency,
            input.data.summary.others,
            input.data.summary.thermal,
            input.timestamp
        )
    )