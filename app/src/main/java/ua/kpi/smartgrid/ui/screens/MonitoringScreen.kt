package ua.kpi.smartgrid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.kpi.smartgrid.data.HourForecast
import ua.kpi.smartgrid.data.LoadForecastResult
import ua.kpi.smartgrid.ui.components.InfoRow
import ua.kpi.smartgrid.ui.components.MessageSnackbar
import ua.kpi.smartgrid.ui.components.ScreenTitle
import ua.kpi.smartgrid.ui.components.stateColor
import ua.kpi.smartgrid.viewmodel.MonitoringUiState
import ua.kpi.smartgrid.viewmodel.MonitoringViewModel

/**
 * Моніторинг мережі: стан мережі з сервера диспетчерського центру
 * і прогноз навантаження на 12 годин за даними погоди Open-Meteo.
 * Помилки показуються в Snackbar з кнопкою «Повторити».
 */
@Composable
fun MonitoringScreen(viewModel: MonitoringViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val forecast by viewModel.forecast.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageSnackbar(message, snackbarHostState, onShown = viewModel::clearMessage,
        actionLabel = "Повторити", onAction = viewModel::refresh)

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ScreenTitle("Моніторинг мережі")

            // Стан мережі: завантаження / успіх / помилка
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Стан мережі", fontWeight = FontWeight.Bold)
                    when (val s = state) {
                        is MonitoringUiState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.CenterHorizontally))
                            Text("Отримання даних з сервера…")
                        }
                        is MonitoringUiState.Success -> {
                            InfoRow("Режим", s.status.mode)
                            InfoRow("Навантаження", "%.0f кВт".format(s.status.loadKw))
                            InfoRow("Частота", "%.2f Гц".format(s.status.frequency))
                            InfoRow("Завантаження", "%.0f %%".format(s.status.percent))
                            InfoRow("Статус", s.status.status)
                            LinearProgressIndicator(
                                progress = { (s.status.percent / 100).toFloat().coerceIn(0f, 1f) },
                                color = stateColor(s.status.status),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                        is MonitoringUiState.Error -> {
                            Text("Помилка: ${s.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ForecastCard(forecast)

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.refresh() }) { Text("Оновити дані") }
                OutlinedButton(onClick = onBack) { Text("Назад") }
            }
        }
    }
}

/** Прогноз навантаження за погодою: температура, прогноз на кожну годину, попередження. */
@Composable
fun ForecastCard(forecast: LoadForecastResult?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Прогноз навантаження (погода Open-Meteo)", fontWeight = FontWeight.Bold)
            if (forecast == null) {
                Text("Прогноз ще не отримано")
                return@Column
            }
            Text("Зараз у Києві: %.1f °C, вітер %.0f км/год".format(forecast.temperature, forecast.windSpeed))

            if (forecast.overloadHours.isNotEmpty()) {
                Text(
                    "Очікується перевантаження: ${forecast.overloadHours.joinToString()}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            forecast.hours.forEach { ForecastRow(it) }
        }
    }
}

@Composable
fun ForecastRow(hour: HourForecast) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(hour.time, modifier = Modifier.weight(1f))
        Text("%.1f °C".format(hour.temperature), modifier = Modifier.weight(1f))
        Text("%.0f кВт".format(hour.loadKw), modifier = Modifier.weight(1.2f))
        Text(
            "%.0f %%".format(hour.percent),
            color = if (hour.percent > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
