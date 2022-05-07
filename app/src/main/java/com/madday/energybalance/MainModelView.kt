package com.madday.energybalance

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*

data class TransferNode(
    var value: Int,
    var plannedValue: Int,
    var parallel: Boolean,
    var id: String
)

data class Summary(
    var water: Int,
    var wind: Int,
    var PV: Int,
    var generated: Int,
    var needed: Int,
    var frequency: Float,
    var others: Int,
    var fossil: Int,
    var time: Long
)

data class MainModuleData(
    val transfers: List<TransferNode>,
    val summary: Summary

)

const val VALUE_POS_KEY = "value_pos_key"
const val PLANNED_VALUE_POS_KEY = "planned_value_pos_key"
const val VALUE_KEY = "value"
const val PLANNED_VALUE_KEY = "plannedValue"
const val PARALLEL_KEY = "parallel"
const val ID_KEY = "id"
const val WATER_KEY = "water"
const val WIND_KEY = "wind"
const val PV_KEY = "pv"
const val GENERATED_KEY = "generated"
const val NEEDED_KEY = "needed"
const val BALANCE_KEY = "balance"
const val FREQUENCY_KEY = "frequency"
const val OTHERS_KEY = "others"
const val FOSSIL_KEY = "fossil"
const val TIME_KEY = "time"

data class BusinessData(val nodesData: Map<String,String>, val summaryData: Map<String,String>)

class LabelProvider : KoinComponent {
    private val context: Context by inject()

    fun import() = context.getString(R.string.IMPORT)
    fun export() = context.getString(R.string.EXPORT)
}

class MainModelView : ViewModel(), KoinComponent {
    private val liveDataImpl: MutableLiveData<BusinessData> by inject()
    private val labelProvider: LabelProvider by inject()
    private val dataSourceModule: DataSourceModule by inject()
    val liveData: LiveData<BusinessData>
    get() = liveDataImpl

    fun testData() {
        updateData(
            MainModuleData(
                listOf(
                    TransferNode(200, 200, false,"SE_HVDC"),
                    TransferNode(200, 200, false,"DE"),
                    TransferNode(200, 200, false,"CZ"),
                    TransferNode(200, 200, false,"SK"),
                    TransferNode(200, 200, false,"UA"),
                    TransferNode(200, 200, false,"LT"),
                ),
                Summary(10, 20, 30, 40, 50, 60.0f, 70, 80, 90)
            )
        )
    }

    fun start() {
        dataSourceModule.addObserver {
            GlobalScope.launch(Dispatchers.Main) {
                updateData(it)
            }
        }

        dataSourceModule.start()
    }

    fun updateData(data: MainModuleData) {
        val uiDataNodes = mutableMapOf<String, String>()
        for (transfer in data.transfers) {
            uiDataNodes["${transfer.id}_${ID_KEY}"] = transfer.id
            uiDataNodes["${transfer.id}_${PLANNED_VALUE_KEY}"] = kotlin.math.abs(transfer.plannedValue).toString()
            uiDataNodes["${transfer.id}_${PARALLEL_KEY}"] = transfer.parallel.toString()
            uiDataNodes["${transfer.id}_${VALUE_KEY}"] = kotlin.math.abs(transfer.value).toString()
            uiDataNodes["${transfer.id}_${PLANNED_VALUE_POS_KEY}"] = (transfer.plannedValue > 0).toString()
            uiDataNodes["${transfer.id}_${VALUE_POS_KEY}"] = (transfer.value > 0).toString()
        }

        val uiDataSummary = mapOf(
            WATER_KEY to data.summary.water.toString(),
            WIND_KEY to data.summary.wind.toString(),
            PV_KEY to data.summary.PV.toString(),
            GENERATED_KEY to data.summary.generated.toString(),
            NEEDED_KEY to data.summary.needed.toString(),
            FREQUENCY_KEY to data.summary.frequency.toString(),
            OTHERS_KEY to data.summary.others.toString(),
            FOSSIL_KEY to data.summary.fossil.toString(),
            BALANCE_KEY to getBalanceValue(data.summary.needed, data.summary.generated),
            TIME_KEY to convertEpochTime(data.summary.time)
        )

        liveDataImpl.postValue(BusinessData(uiDataNodes, uiDataSummary))
    }

    private fun convertEpochTime(epoch: Long): String {
        return Date(epoch).let {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY).apply {
                timeZone = TimeZone.getTimeZone("Europe/Warsaw")
            }.format(it)
        }
    }

    private fun getBalanceValue(needed: Int, generated: Int) =
        if (needed > generated) "${needed - generated} ${labelProvider.import()}"
        else "${generated - needed} ${labelProvider.export()}"

}