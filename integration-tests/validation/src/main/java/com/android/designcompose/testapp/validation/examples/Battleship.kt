package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Battleship
@DesignDoc(id = "RfGl9SWnBEvdg8T1Ex6ZAR")
interface Battleship {
    @DesignComponent(node = "Start Board") fun MainFrame()
}

@Composable
fun BattleshipTest() {
    BattleshipDoc.MainFrame(Modifier)
}
