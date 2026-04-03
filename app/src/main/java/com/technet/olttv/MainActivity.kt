package com.technet.olttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF08111F)
                ) {
                    SafeStartupScreen()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun SafeStartupScreen() {
    val status = remember { mutableStateOf("Iniciando app...") }
    val details = remember { mutableStateOf("Aguarde...") }
    val loaded = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        status.value = "Testando conexão com API..."
        details.value = "http://200.106.207.64:5009/olttv/api/mapa-tv.php"

        val result = withContext(Dispatchers.IO) {
            try {
                val repository = MapRepository()
                repository.fetchMap()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        result
            .onSuccess { response ->
                loaded.value = true
                status.value = "API conectada com sucesso"
                details.value =
                    "Mapa: ${response.map.name}\nNós: ${response.map.nodes.size}\nLinks: ${response.map.links.size}\nAtualizado: ${response.updated_at}"
            }
            .onFailure { e ->
                loaded.value = false
                status.value = "Falha ao abrir API"
                details.value = e.message ?: "Erro desconhecido"
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!loaded.value) {
                CircularProgressIndicator(color = Color.White)
            }

            Text(
                text = status.value,
                color = Color.White,
                fontSize = 24.sp
            )

            Text(
                text = details.value,
                color = Color(0xFFBFD7FF),
                fontSize = 16.sp
            )
        }
    }
}
