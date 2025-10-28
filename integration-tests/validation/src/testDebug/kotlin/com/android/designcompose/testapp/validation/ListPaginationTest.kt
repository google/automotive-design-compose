package com.android.designcompose.testapp.validation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.DynamicListPaginationTest
import com.android.designcompose.testapp.validation.examples.StaticListPaginationTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ListPagination {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    @Test
    fun testItemTapsReturnCorrectIndex() {
        val clickedItem = mutableStateOf(-1)

        with(composeTestRule) {
            setContent { StaticListPaginationTest(onTap = { index -> clickedItem.value = index }) }
            waitForIdle()

            captureRootRoboImage("static_list")

            onNodeWithTag("Item0").performClick()
            assertEquals(0, clickedItem.value)

            onNodeWithTag("Item1").performClick()
            assertEquals(1, clickedItem.value)

            onNodeWithTag("Item2").performClick()
            assertEquals(2, clickedItem.value)

            onNodeWithTag("Item3").performClick()
            assertEquals(3, clickedItem.value)

            onNodeWithTag("Item4").performClick()
            assertEquals(4, clickedItem.value)
        }
    }

    @Test
    fun testDynamicListScrollAndTap() {
        val clickedItem = mutableStateOf(-1)
        val itemCount = 20
        val lastIndex = itemCount - 1

        with(composeTestRule) {
            setContent {
                DynamicListPaginationTest(
                    itemCount = itemCount,
                    onTap = { index -> clickedItem.value = index },
                )
            }
            waitForIdle()
            captureRootRoboImage("dynamic_list_initial")

            // Scroll to the bottom
            repeat(5) {
                onNodeWithTag("List").performTouchInput { swipeUp(durationMillis = 200) }
                waitForIdle()
            }
            captureRootRoboImage("dynamic_list_scrolled_down")
            onNodeWithTag("Item$lastIndex").assertExists()
            onNodeWithTag("Item$lastIndex").performClick()
            assertEquals(lastIndex, clickedItem.value)

            // Scroll to the top
            repeat(5) {
                onNodeWithTag("List").performTouchInput { swipeDown(durationMillis = 200) }
                waitForIdle()
            }
            captureRootRoboImage("dynamic_list_scrolled_up")
            onNodeWithTag("Item0").assertExists()
            onNodeWithTag("Item0").performClick()
            assertEquals(0, clickedItem.value)
        }
    }
}
