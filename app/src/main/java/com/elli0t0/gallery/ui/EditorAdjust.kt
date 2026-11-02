package com.elli0t0.gallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elli0t0.gallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustControls(
    adjustments: Adjustments,
    onChange: (Adjustments) -> Unit,
    onDone: () -> Unit,
    onAuto: () -> Unit
) {
    val rows = listOf(
        Triple(R.string.light_balance, { a: Adjustments -> a.lightBalance }, { a: Adjustments, v: Float -> a.copy(lightBalance = v) }),
        Triple(R.string.brightness, { a: Adjustments -> a.brightness }, { a: Adjustments, v: Float -> a.copy(brightness = v) }),
        Triple(R.string.exposure, { a: Adjustments -> a.exposure }, { a: Adjustments, v: Float -> a.copy(exposure = v) }),
        Triple(R.string.contrast, { a: Adjustments -> a.contrast }, { a: Adjustments, v: Float -> a.copy(contrast = v) }),
        Triple(R.string.highlights, { a: Adjustments -> a.highlights }, { a: Adjustments, v: Float -> a.copy(highlights = v) }),
        Triple(R.string.shadows, { a: Adjustments -> a.shadows }, { a: Adjustments, v: Float -> a.copy(shadows = v) }),
        Triple(R.string.saturation, { a: Adjustments -> a.saturation }, { a: Adjustments, v: Float -> a.copy(saturation = v) }),
        Triple(R.string.tint, { a: Adjustments -> a.tint }, { a: Adjustments, v: Float -> a.copy(tint = v) }),
        Triple(R.string.temperature, { a: Adjustments -> a.temperature }, { a: Adjustments, v: Float -> a.copy(temperature = v) }),
        Triple(R.string.sharpness, { a: Adjustments -> a.sharpness }, { a: Adjustments, v: Float -> a.copy(sharpness = v) }),
        Triple(R.string.vignette, { a: Adjustments -> a.vignette }, { a: Adjustments, v: Float -> a.copy(vignette = v) })
    )
    Column(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.adjust_tab), color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAuto) { Text(stringResource(R.string.auto), fontSize = 13.sp) }
        }
        rows.forEach { (label, get, set) ->
            val value = get(adjustments)
            val range = if (label == R.string.sharpness || label == R.string.vignette) 0f..100f else -100f..100f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(label), color = Color.White, fontSize = 13.sp, modifier = Modifier.width(100.dp))
                Slider(
                    value = value,
                    onValueChange = { onChange(set(adjustments, it)) },
                    onValueChangeFinished = onDone,
                    valueRange = range,
                    modifier = Modifier.weight(1f)
                )
                Text(value.toInt().toString(), color = Color.White, fontSize = 12.sp, modifier = Modifier.width(36.dp))
            }
        }
    }
}
