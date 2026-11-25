package ua.kpi.smartgrid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.smartgrid.data.local.GridObjectEntity
import ua.kpi.smartgrid.domain.GridLabels
import ua.kpi.smartgrid.ui.theme.StatusError
import ua.kpi.smartgrid.ui.theme.StatusIdle
import ua.kpi.smartgrid.ui.theme.StatusOk
import ua.kpi.smartgrid.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Картка енергооб'єкта: назва, стан, тип, навантаження зі смугою, фідери і час оновлення. */
@Composable
fun ObjectCard(obj: GridObjectEntity, modifier: Modifier = Modifier) {
    val percent = GridLabels.loadPercent(obj)
    val state = GridLabels.state(obj)
    val color = stateColor(state)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(obj.updatedAt))
    val label = if (obj.type == "SOLAR") "Генерація" else "Навантаження"

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(obj.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(state, color = color, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "${GridLabels.type(obj.type)} · ${obj.district} район",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "$label: ${"%.0f".format(obj.loadKw)} з ${"%.0f".format(obj.capacityKw)} кВт (${"%.0f".format(percent)} %)",
                modifier = Modifier.padding(top = 4.dp)
            )
            LinearProgressIndicator(
                progress = { (percent / 100).toFloat().coerceIn(0f, 1f) },
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            Text("Фідери: ${obj.feeders}", style = MaterialTheme.typography.bodySmall)
            Text("Оновлено о $time", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Колір стану об'єкта. */
fun stateColor(state: String): Color = when (state) {
    "Норма" -> StatusOk
    "Високе навантаження" -> StatusWarning
    "Перевантаження", "Відключено" -> StatusError
    else -> StatusIdle
}
