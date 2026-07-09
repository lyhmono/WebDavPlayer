package com.webdav.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webdav.player.data.model.ServerConfig
import com.webdav.player.data.repository.ServerConfigRepository
import com.webdav.player.data.repository.WebDavRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerConfigViewModel @Inject constructor(
    private val serverConfigRepository: ServerConfigRepository,
    private val webDavRepository: WebDavRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerConfigUiState())
    val uiState: StateFlow<ServerConfigUiState> = _uiState.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        viewModelScope.launch {
            serverConfigRepository.observeAll().collect { servers ->
                _uiState.value = _uiState.value.copy(
                    savedServers = servers,
                    isLoading = false
                )
            }
        }
    }

    fun updateFormField(field: FormField, value: String) {
        _uiState.value = _uiState.value.copy(
            form = when (field) {
                FormField.NAME -> _uiState.value.form.copy(name = value)
                FormField.URL -> _uiState.value.form.copy(url = value)
                FormField.USERNAME -> _uiState.value.form.copy(username = value)
                FormField.PASSWORD -> _uiState.value.form.copy(password = value)
            }
        )
    }

    fun testConnection() {
        val form = _uiState.value.form
        if (!form.isValid()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请填写所有必填字段"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTesting = true,
                errorMessage = null,
                testResult = null
            )

            val config = ServerConfig(
                name = form.name,
                url = form.url.trimEnd('/'),
                username = form.username,
                password = form.password
            )

            val result = webDavRepository.testConnection(config)
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = if (result.isSuccess) "连接成功" else null,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun saveServer() {
        val form = _uiState.value.form
        if (!form.isValid()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请填写所有必填字段"
            )
            return
        }

        viewModelScope.launch {
            val config = ServerConfig(
                name = form.name,
                url = form.url.trimEnd('/'),
                username = form.username,
                password = form.password
            )
            serverConfigRepository.insert(config)
            _uiState.value = _uiState.value.copy(
                form = ServerForm(),
                errorMessage = null,
                testResult = "服务器已保存"
            )
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            serverConfigRepository.delete(id)
            webDavRepository.clearClientCache(id)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissTestResult() {
        _uiState.value = _uiState.value.copy(testResult = null)
    }
}

enum class FormField { NAME, URL, USERNAME, PASSWORD }

data class ServerForm(
    val name: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = ""
) {
    fun isValid(): Boolean =
        name.isNotBlank() && url.isNotBlank() && username.isNotBlank()
}

data class ServerConfigUiState(
    val isLoading: Boolean = true,
    val savedServers: List<ServerConfig> = emptyList(),
    val form: ServerForm = ServerForm(),
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val errorMessage: String? = null
)
