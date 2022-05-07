package com.madday.energybalance

import android.app.Activity
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.lang.Exception

class NonSupportedNodeError(nodeId: String) : Exception("Node id $nodeId is not supported!")
class NotSupportedSummaryKeyError(key: String) : Exception("Summary id $key is not supported!")

class NodeProvider {
    lateinit var activity: Activity
    fun getUINode(id: Int) = activity.findViewById<TextView>(id)
}

class TransferNodesModifier(private val nodeId: String) : KoinComponent {
    private val nodeProvider: NodeProvider by inject()

    companion object {
        private val TAG = TransferNodesModifier::class.java.canonicalName

        private fun node(id: String): Pair<Int, Int> {
            return when(id) {
                "SE" -> Pair(R.id.se_hvdc_value, R.id.se_hvdc_planned)
                "DE" -> Pair(R.id.de_value, R.id.de_planned)
                "CZ" -> Pair(R.id.cz_value, R.id.cz_planned)
                "SK" -> Pair(R.id.sk_value, R.id.sk_planned)
                "UA" -> Pair(R.id.ua_value, R.id.ua_planned)
                "LT" -> Pair(R.id.lt_value, R.id.lt_planned)
                else -> throw NonSupportedNodeError(id)
            }
        }
    }

    fun value(value: String, positive: Boolean) {
        try {
            nodeProvider.getUINode(node(nodeId).first).run {
                text = value
                setTextColor(if (positive) get<Context>().getColor(R.color.dkgreen) else Color.RED)
            }
        } catch (ex: NonSupportedNodeError) {
            Log.e(TAG, "Not supported node id!", ex)
        }

    }

    fun planned(planned: String, positive: Boolean) {
        try {
            nodeProvider.getUINode(node(nodeId).second).run {
                text = planned
                setTextColor(if (positive) get<Context>().getColor(R.color.dkgreen) else Color.RED)
            }
        } catch(ex: NonSupportedNodeError) {
            Log.e(TAG, "Not supported node id!", ex)
        }
    }

}

class SummaryModifier : KoinComponent {
    private val nodeProvider: NodeProvider by inject()

    companion object {
        private val TAG = SummaryModifier::class.java.canonicalName
    }

    fun update(values: Map<String, String>) {
        values.entries.forEach {
            try {
                when (it.key) {
                    GENERATED_KEY -> R.id.generated
                    NEEDED_KEY -> R.id.needed
                    FOSSIL_KEY -> R.id.fossil
                    WATER_KEY -> R.id.water
                    WIND_KEY -> R.id.wind
                    PV_KEY -> R.id.pv
                    OTHERS_KEY -> R.id.others
                    FREQUENCY_KEY -> R.id.frequency
                    BALANCE_KEY -> R.id.balance
                    TIME_KEY -> R.id.time
                    else -> throw NotSupportedSummaryKeyError(it.key)
                }.let { id ->
                    nodeProvider.getUINode(id).text = it.value
                }
            }
            catch (ex: NotSupportedSummaryKeyError) {
                Log.e(TAG, "Not Supported Summary Key!", ex)
            }
        }
    }
}

val nodes = listOf("SE", "DE", "CZ", "SK", "UA", "LT")

class MainActivity : AppCompatActivity() {
    private val mainModelView: MainModelView by viewModel()
    private val nodeProvider: NodeProvider by inject()
    private val worker = Handler(Looper.getMainLooper())
    private var waitForIntro = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)

        nodeProvider.activity = this
        mainModelView.liveData.observe(this, Observer { businessData ->

            val update = {
                setContentView(R.layout.activity_main)

                findViewById<Button>(R.id.button2)?.setOnClickListener {
                    mainModelView.start()
                }

                nodes.forEach { node ->
                    TransferNodesModifier(node).also {
                        it.value(businessData.nodesData["${node}_${VALUE_KEY}"] ?: "error",
                            businessData.nodesData["${node}_${VALUE_POS_KEY}"]?.toBoolean() ?: false)

                        it.planned(businessData.nodesData["${node}_${PLANNED_VALUE_KEY}"] ?: "error",
                            businessData.nodesData["${node}_${PLANNED_VALUE_POS_KEY}"]?.toBoolean() ?: false)
                    }
                }


                SummaryModifier().update(businessData.summaryData)
            }

            if (waitForIntro) {
                waitForIntro = false
                worker.postDelayed({ update() },
                    2000L
                )
            } else {
                update()
            }
        })

        mainModelView.start()
    }
}