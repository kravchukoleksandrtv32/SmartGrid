package ua.kpi.smartgrid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.kpi.smartgrid.data.GridRepository
import ua.kpi.smartgrid.data.GridStatus
import ua.kpi.smartgrid.data.LoadForecastResult

sealed class MonitoringUiState {
    object Loading : MonitoringUiState()
    data class Success(val status: GridStatus) : MonitoringUiState()
    data class Error(val message: String) : MonitoringUiState()
}

/**
 * Стан мережі з сервера і прогноз навантаження за погодою.
 * Використовується екранами «Моніторинг» (withForecast = true) і «Режими мережі».
 */
class MonitoringViewModel(
    private val repository: GridRepository,
    private val withForecast: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow<MonitoringUiState>(MonitoringUiState.Loading)
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

    private val _forecast = MutableStateFlow<LoadForecastResult?>(null)
    val forecast: StateFlow<LoadForecastResult?> = _forecast.asStateFlow()

    // Повідомлення для Snackbar (null — нічого не показувати)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = MonitoringUiState.Loading
            // Запити до двох різних сервісів виконуються паралельно
            val statusJob = async { repository.getGridStatus() }
            val forecastJob = if (withForecast) async { repository.getLoadForecast() } else null

            statusJob.await()
                .onSuccess { _uiState.value = MonitoringUiState.Success(it) }
                .onFailure {
                    _uiState.value = MonitoringUiState.Error(it.message.orEmpty())
                    _message.value = it.message
                }
            forecastJob?.await()
                ?.onSuccess { _forecast.value = it }
                ?.onFailure { _message.value = "Прогноз погоди: ${it.message}" }
        }
    }

    // Зміна режиму мережі (сервер дозволяє лише адміністратору)
    fun setMode(mode: String) {
        viewModelScope.launch {
            repository.setMode(mode)
                .onSuccess {
                    _uiState.value = MonitoringUiState.Success(it)
                    _message.value = "Режим змінено: $mode"
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
