package com.weighttracker.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.weighttracker.app.data.WeightRecord
import com.weighttracker.app.util.DisplayUnit
import com.weighttracker.app.util.toDisplayValue
import kotlin.math.max

@Composable
fun WeightLineChart(
    records: List<WeightRecord>,
    unit: DisplayUnit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val surface = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        if (records.isEmpty()) return@Canvas

        val values = records.map { it.weightKg.toDisplayValue(unit) }
        val minV = (values.minOrNull() ?: 0.0) - 0.8
        val maxV = (values.maxOrNull() ?: 1.0) + 0.8
        val span = max(0.5, maxV - minV)

        val padX = 18.dp.toPx()
        val padY = 22.dp.toPx()
        val w = size.width
        val h = size.height

        fun xAt(i: Int): Float {
            if (records.size == 1) return w / 2f
            val t = i / (records.size - 1f)
            return padX + t * (w - 2 * padX)
        }

        fun yAt(v: Double): Float {
            val t = ((v - minV) / span).toFloat().coerceIn(0f, 1f)
            return (h - padY) - t * (h - 2 * padY)
        }

        // baseline
        drawLine(
            color = outline,
            start = Offset(padX, h - padY),
            end = Offset(w - padX, h - padY),
            strokeWidth = 2.dp.toPx(),
        )

        if (values.size >= 2) {
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = xAt(i)
                val y = yAt(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = primary,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        values.forEachIndexed { i, v ->
            drawCircle(
                color = primary,
                radius = 6.dp.toPx(),
                center = Offset(xAt(i), yAt(v)),
            )
            drawCircle(
                color = surface,
                radius = 3.dp.toPx(),
                center = Offset(xAt(i), yAt(v)),
            )
        }
    }
}
