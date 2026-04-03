package com.technet.olttv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MapUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: TvMapData? = null,
    val updatedAt: String? = null
)

class MapViewModel : ViewModel() {

    private val repository = MapRepository()

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                loadMap()
                delay(30000)
            }
        }
    }

    fun loadMap() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            repository.fetchMap()
                .onSuccess { response ->
                    _uiState.value = MapUiState(
                        loading = false,
                        error = null,
                        data = response.map,
                        updatedAt = response.updated_at
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = e.message ?: "Erro ao carregar mapa"
                    )
                }
        }
    }
}
