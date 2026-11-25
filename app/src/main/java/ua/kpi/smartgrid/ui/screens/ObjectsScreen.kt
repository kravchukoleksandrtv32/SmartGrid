package ua.kpi.smartgrid.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.kpi.smartgrid.data.ObjectsSummary
import ua.kpi.smartgrid.domain.GridLabels
import ua.kpi.smartgrid.ui.components.InfoRow
import ua.kpi.smartgrid.ui.components.LoadingButton
import ua.kpi.smartgrid.ui.components.MessageSnackbar
import ua.kpi.smartgrid.ui.components.ObjectCard
import ua.kpi.smartgrid.ui.components.ScreenTitle
import ua.kpi.smartgrid.viewmodel.ObjectsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Енергооб'єкти мережі: дані з REST API (JSON із вкладеними об'єктами) у вигляді карток.
 * Над списком — зведені показники, фільтр за типом і сортування.
 * Якщо сервер недоступний, показуються дані з кешу Room.
 */
@Composable
fun ObjectsScreen(viewModel: ObjectsViewModel, onBack: () -> Unit) {
    val objects by viewModel.objects.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val sortByLoad by viewModel.sortByLoad.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val lastSync by viewModel.lastSync.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageSnackbar(message, snackbarHostState, onShown = viewModel::clearMessage,
        actionLabel = "Повторити", onAction = { viewModel.refresh() })

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ScreenTitle("Енергооб'єкти мережі")

            // Звідки дані: сервер чи кеш
            if (lastSync > 0) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSync))
                Text(
                    if (isOffline) "Офлайн-режим: збережені дані від $time" else "Синхронізовано о $time",
                    color = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SummaryCard(summary)

            // Фільтр за типом об'єкта
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = typeFilter == null, onClick = { viewModel.typeFilter.value = null },
                    label = { Text("Усі") })
                GridLabels.types.forEach { (type, title) ->
                    FilterChip(selected = typeFilter == type, onClick = { viewModel.typeFilter.value = type },
                        label = { Text(title) })
                }
            }

            // Сортування
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !sortByLoad, onClick = { viewModel.sortByLoad.value = false },
                    label = { Text("За назвою") })
                FilterChip(selected = sortByLoad, onClick = { viewModel.sortByLoad.value = true },
                    label = { Text("За завантаженням") })
            }

            if (objects.isEmpty() && !isLoading) {
                Text("Немає даних. Перевірте з'єднання і натисніть «Оновити».",
                    modifier = Modifier.padding(vertical = 16.dp))
            }

            // Список карток займає весь вільний простір
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(objects, key = { it.id }) { obj -> ObjectCard(obj) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    LoadingButton("Оновити з сервера", isLoading) { viewModel.refresh(force = true) }
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("Назад") }
            }
        }
    }
}

/** Агреговані показники, які обчислює репозиторій. */
@Composable
fun SummaryCard(summary: ObjectsSummary) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            InfoRow("Об'єктів", summary.count.toString())
            InfoRow("Навантаження споживачів", "%.0f з %.0f кВт (%.0f %%)".format(
                summary.loadKw, summary.capacityKw, summary.loadPercent))
            InfoRow("Генерація СЕС", "%.0f кВт".format(summary.generationKw))
            InfoRow("Перевантажені / не працюють", "${summary.overloaded} / ${summary.notWorking}")
        }
    }
}
