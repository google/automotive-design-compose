// Copyright 2024 Google LLC

package com.android.designcompose.testapp.clusterdemo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val textSize = 20.sp

@Composable
@OptIn(ExperimentalMaterialApi::class)
internal inline fun <reified T : Enum<T>> LabelledDropDown(
    label: String,
    state: T,
    crossinline onStateChange: (T) -> Unit,
) {
    var dropdownState by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = dropdownState,
        onExpandedChange = { dropdownState = !dropdownState },
        modifier = Modifier.width(200.dp),
    ) {
        TextField(
            value = state.toString(),
            readOnly = true,
            onValueChange = {},
            label = { Text(label, fontSize = textSize) },
            textStyle = TextStyle(fontSize = textSize),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownState) },
        )
        ExposedDropdownMenu(
            expanded = dropdownState,
            onDismissRequest = { dropdownState = false },
        ) {
            for (entry in enumValues<T>()) {
                DropdownMenuItem(
                    onClick = {
                        onStateChange(entry)
                        dropdownState = false
                    }
                ) {
                    Text(entry.toString(), fontSize = textSize)
                }
            }
        }
    }
}

@Composable
internal fun LabelledToggle(label: String, value: Boolean, onStateChange: (Boolean) -> Unit) {
    Row(Modifier.clickable(enabled = true) { onStateChange(!value) }) {
        Text(label, fontSize = textSize)
        Switch(
            checked = value,
            onCheckedChange = { onStateChange(it) },
            modifier = Modifier.offset((3).dp),
        )
    }
}

@Composable
internal fun SliderControl(
    label: String,
    stateVar: Float,
    onValueChange: (Float) -> Unit,
    topValue: Float, // I should decide on Ints or Floats........
    minValue: Float = 0f,
) {
    Row(modifier = Modifier.border(width = 2.dp, color = Color.Gray)) {
        Text(label, Modifier.width(120.dp), color = Color.Black, fontSize = textSize)
        Slider(
            value = stateVar,
            modifier = Modifier.width(120.dp),
            valueRange = minValue..topValue,
            onValueChange = { onValueChange(it) },
        )
    }
}
