package com.madday.energybalance

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.TextView
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.component.get
import org.koin.test.get
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import java.util.stream.Stream


internal class TransferNodesModifierTest : KoinTest {
    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(
            module {
                single { mockk<TextView>() }
                single { mockk<NodeProvider>() }
                single { mockk<Context>(relaxed = true)}
            })
    }

    companion object {
        @JvmStatic
        fun NodeNameAndIdsParameters() = Stream.of(
            arguments("SE", Pair(R.id.se_hvdc_value, R.id.se_hvdc_planned)),
            arguments("DE", Pair(R.id.de_value, R.id.de_planned)),
            arguments("CZ", Pair(R.id.cz_value, R.id.cz_planned)),
            arguments("SK", Pair(R.id.sk_value, R.id.sk_planned)),
            arguments("UA", Pair(R.id.ua_value, R.id.ua_planned)),
            arguments("LT", Pair(R.id.lt_value, R.id.lt_planned))
        )
    }


    @ParameterizedTest
    @MethodSource("NodeNameAndIdsParameters")
    fun `the ids of transfer nodes is returned when nodes names are given`(nodeName: String, ids: Pair<Int, Int>) {
        // Given
        val sut = TransferNodesModifier(nodeName)
        val mockNodeProvider: NodeProvider = get()
        val mockTextView: TextView = get()
        val mockContext: Context = get()

        every { mockNodeProvider.getUINode(ids.first) } returns mockTextView
        every { mockNodeProvider.getUINode(ids.second) } returns mockTextView
        every { mockTextView.text = "100" } just runs
        every { mockTextView.text = "200" } just runs
        every { mockTextView.setTextColor(any<Int>()) } just runs
        every { mockContext.getColor(R.color.dkgreen) } returns Color.GREEN
        every { mockContext.getColor(R.color.red) } returns Color.RED


        // When
        sut.value("100", false)
        sut.planned("200", true)

        // Then
        verify {mockNodeProvider.getUINode(ids.first)}
        verify {mockNodeProvider.getUINode(ids.second)}
        verify(atMost = 1, atLeast = 1) { mockTextView.text = "100" }
        verify(atMost = 1, atLeast = 1)  { mockTextView.text = "200" }
        verify { mockTextView.setTextColor(Color.GREEN) }
        verify { mockTextView.setTextColor(Color.RED) }
    }

    @Test
    fun `don't modify anything when nodeName is not recognized`() {
        // Given
        mockkStatic(Log::class)
        every { Log.v(any(), any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.i(any(), any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        val sut = TransferNodesModifier("Fail")
        val mockNodeProvider: NodeProvider = get()

        // When
        sut.value("100", true)
        sut.planned("200", false)

        // Then
        verify(atMost = 0, atLeast = 0) { mockNodeProvider.getUINode(any()) }

    }

}
