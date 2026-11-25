package ua.kpi.smartgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ua.kpi.smartgrid.data.GridRepository
import ua.kpi.smartgrid.data.ObjectsSummary
import ua.kpi.smartgrid.data.local.GridObjectEntity
import ua.kpi.smartgrid.domain.GridLabels

/**
 * Енергооб'єкти мережі: дані з REST API, закешовані в Room.
 * Список фільтрується за типом і сортується; зведені показники рахує репозиторій.
 */
class ObjectsViewModel(private val repository: GridRepository) : ViewModel() {

    val typeFilter = MutableStateFlow<String?>(null) // null — усі типи
    val sortByLoad = MutableStateFlow(false)          // false — за назвою

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // true — сервер недоступний, показано збережені дані
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _lastSync = MutableStateFlow(0L)
    val lastSync: StateFlow<Long> = _lastSync.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Список для екрана: перераховується, коли змінюються дані в базі або параметри фільтра
    val objects: StateFlow<List<GridObjectEntity>> =
        combine(repository.objects, typeFilter, sortByLoad) { list, type, byLoad ->
            val filtered = if (type == null) list else list.filter { it.type == type }
            if (byLoad) filtered.sortedByDescending { GridLabels.loadPercent(it) } else filtered.sortedBy { it.name }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<ObjectsSummary> =
        repository.objects.map { repository.summarizeObjects(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ObjectsSummary())

    init {
        refresh(force = false) // при відкритті екрана використовуємо кеш, якщо він свіжий
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshObjects(force)
                .onSuccess { fromServer ->
                    _isOffline.value = false
                    if (fromServer) _message.value = "Дані оновлено з сервера"
                }
                .onFailure {
                    _isOffline.value = true
                    _message.value = it.message
                }
            _lastSync.value = repository.lastSyncTime()
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
