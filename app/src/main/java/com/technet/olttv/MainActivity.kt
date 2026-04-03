package com.technet.olttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                    StableNativeMapApp()
                }
            }
        }
    }
}

@Composable
fun StableNativeMapApp() {
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val responseState = remember { mutableStateOf<MapResponse?>(null) }
    val selectedNodeId = remember { mutableStateOf<String?>(null) }

    suspend fun loadMap() {
        loading.value = true
        error.value = null

        val result = withContext(Dispatchers.IO) {
            try {
                MapRepository().fetchMap()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        result
            .onSuccess { response ->
                responseState.value = response
                if (selectedNodeId.value == null) {
                    selectedNodeId.value = response.map.nodes.firstOrNull()?.id
                }
            }
            .onFailure { e ->
                error.value = e.message ?: "Erro ao carregar mapa"
            }

        loading.value = false
    }

    LaunchedEffect(Unit) {
        loadMap()
        while (true) {
            delay(30_000)
            loadMap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111F))
            .padding(18.dp)
    ) {
        when {
            loading.value && responseState.value == null -> {
                LoadingView()
            }

            error.value != null && responseState.value == null -> {
                ErrorView(error.value ?: "Erro desconhecido")
            }

            responseState.value != null -> {
                val map = responseState.value!!.map
                val selectedNode = map.nodes.firstOrNull { it.id == selectedNodeId.value }
                    ?: map.nodes.firstOrNull()

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LeftPanel(
                        mapName = map.name,
                        updatedAt = responseState.value!!.updated_at,
                        nodeCount = map.nodes.size,
                        linkCount = map.links.size,
                        selectedNode = selectedNode,
                        hasError = error.value != null
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(
                                width = 1.dp,
                                color = Color(0xFF1E3A8A),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(
                                color = Color(0xFF0B1325),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(12.dp)
                    ) {
                        StableMapView(
                            data = map,
                            selectedNodeId = selectedNode?.id,
                            onSelectNode = { selectedNodeId.value = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = "Carregando mapa...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(700.dp)
                .background(Color(0xFF111827), RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Falha ao carregar mapa",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun LeftPanel(
    mapName: String,
    updatedAt: String,
    nodeCount: Int,
    linkCount: Int,
    selectedNode: TvNode?,
    hasError: Boolean
) {
    Column(
        modifier = Modifier.width(400.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        InfoCard(
            title = mapName,
            lines = listOf(
                "Atualizado: $updatedAt",
                "Nós: $nodeCount",
                "Links: $linkCount",
                if (hasError) "Status: exibindo últimos dados" else "Status: online"
            )
        )

        if (selectedNode != null) {
            InfoCard(
                title = selectedNode.title,
                lines = listOfNotNull(
                    "Tipo: ${selectedNode.type}",
                    selectedNode.rx?.let { "RX: $it" },
                    selectedNode.tx?.let { "TX: $it" },
                    selectedNode.download?.let { "Download: $it" },
                    selectedNode.upload?.let { "Upload: $it" },
                    selectedNode.info?.let { "Info: $it" },
                    "Status: ${selectedNode.status}"
                )
            )
        }

        InfoCard(
            title = "Controles V1",
            lines = listOf(
                "Toque/clique em um card do mapa",
                "Seleciona o nó e mostra detalhes",
                "Atualização automática a cada 30s"
            )
        )
    }
}

@Composable
fun InfoCard(title: String, lines: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101A2E)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )

            lines.forEach { line ->
                Text(
                    text = line,
                    color = Color(0xFFBFDBFE),
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun StableMapView(
    data: TvMapData,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            data.links.forEach { link ->
                val from = data.nodes.firstOrNull { it.id == link.from } ?: return@forEach
                val to = data.nodes.firstOrNull { it.id == link.to } ?: return@forEach

                val lineColor = when (link.status.lowercase()) {
                    "critical" -> Color(0xFFEF4444)
                    "warning" -> Color(0xFFFACC15)
                    else -> Color(0xFF3B82F6)
                }

                drawLine(
                    color = lineColor,
                    start = Offset(from.x, from.y),
                    end = Offset(to.x, to.y),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            data.nodes.forEach { node ->
                val isSelected = node.id == selectedNodeId

                val fillColor = when (node.status.lowercase()) {
                    "critical" -> Color(0xFF551B1B)
                    "warning" -> Color(0xFF5A4316)
                    else -> Color(0xFF0F172A)
                }

                val borderColor = when {
                    isSelected -> Color(0xFF38BDF8)
                    node.status.equals("critical", ignoreCase = true) -> Color(0xFFEF4444)
                    node.status.equals("warning", ignoreCase = true) -> Color(0xFFFACC15)
                    else -> Color(0xFF334155)
                }

                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(node.x - 95f, node.y - 36f),
                    size = Size(190f, 72f),
                    cornerRadius = CornerRadius(18f, 18f)
                )

                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(node.x - 95f, node.y - 36f),
                    size = Size(190f, 72f),
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = if (isSelected) 4f else 2f)
                )
            }
        }

        data.nodes.forEach { node ->
            Box(
                modifier = Modifier
                    .padding(start = (node.x - 95).dp, top = (node.y - 36).dp)
                    .width(190.dp)
                    .height(72.dp)
                    .clickable { onSelectNode(node.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = node.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                        maxLines = 2,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = buildNodeSubtitle(node),
                        color = when (node.status.lowercase()) {
                            "critical" -> Color(0xFFFCA5A5)
                            "warning" -> Color(0xFFFDE68A)
                            else -> Color(0xFF93C5FD)
                        },
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

fun buildNodeSubtitle(node: TvNode): String {
    if (!node.rx.isNullOrBlank() && !node.tx.isNullOrBlank()) {
        return "RX ${node.rx} | TX ${node.tx}"
    }

    if (!node.info.isNullOrBlank()) {
        return node.info
    }

    if (!node.download.isNullOrBlank() || !node.upload.isNullOrBlank()) {
        val down = node.download ?: "-"
        val up = node.upload ?: "-"
        return "D $down | U $up"
    }

    return node.type
}
