package com.technet.olttv

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.nativeCanvas
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
                    AutoScaleMapApp()
                }
            }
        }
    }
}

@Composable
fun AutoScaleMapApp() {
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val responseState = remember { mutableStateOf<MapResponse?>(null) }

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
                val firstNode = map.nodes.firstOrNull()

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LeftPanel(
                        mapName = map.name,
                        updatedAt = responseState.value!!.updated_at,
                        nodeCount = map.nodes.size,
                        linkCount = map.links.size,
                        selectedNode = firstNode,
                        hasError = error.value != null
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(
                                width = 1.dp,
                                color = Color(0xFF2563EB),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(
                                color = Color(0xFF0B1325),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(12.dp)
                    ) {
                        AutoScaleCanvasMap(data = map)
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
            title = "V1.1 Autoescala",
            lines = listOf(
                "Mapa ajustado para o tamanho da TV",
                "Centralização automática",
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
                fontWeight = FontWeight.Bold
            )

            lines.forEach { line ->
                Text(
                    text = line,
                    color = Color(0xFFBFDBFE),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AutoScaleCanvasMap(data: TvMapData) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val minX = data.nodes.minOfOrNull { it.x } ?: 0f
        val maxX = data.nodes.maxOfOrNull { it.x } ?: 100f
        val minY = data.nodes.minOfOrNull { it.y } ?: 0f
        val maxY = data.nodes.maxOfOrNull { it.y } ?: 100f

        val sourceWidth = (maxX - minX).coerceAtLeast(1f)
        val sourceHeight = (maxY - minY).coerceAtLeast(1f)

        val horizontalPadding = 120f
        val verticalPadding = 100f

        val availableWidth = (canvasWidth - horizontalPadding * 2).coerceAtLeast(1f)
        val availableHeight = (canvasHeight - verticalPadding * 2).coerceAtLeast(1f)

        val scaleX = availableWidth / sourceWidth
        val scaleY = availableHeight / sourceHeight
        val scale = minOf(scaleX, scaleY)

        val contentWidth = sourceWidth * scale
        val contentHeight = sourceHeight * scale

        val offsetX = (canvasWidth - contentWidth) / 2f
        val offsetY = (canvasHeight - contentHeight) / 2f

        fun sx(x: Float): Float = offsetX + ((x - minX) * scale)
        fun sy(y: Float): Float = offsetY + ((y - minY) * scale)

        val cardWidth = (190f * scale.coerceIn(0.8f, 1.8f)).coerceIn(180f, 320f)
        val cardHeight = (72f * scale.coerceIn(0.8f, 1.6f)).coerceIn(70f, 120f)
        val titleTextSize = (22f * scale.coerceIn(0.8f, 1.4f)).coerceIn(20f, 30f)
        val subTextSize = (18f * scale.coerceIn(0.8f, 1.4f)).coerceIn(16f, 24f)
        val lineStroke = (5f * scale.coerceIn(0.8f, 1.5f)).coerceIn(4f, 8f)
        val borderStroke = (2f * scale.coerceIn(0.8f, 1.5f)).coerceIn(2f, 4f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = titleTextSize
                isAntiAlias = true
                isFakeBoldText = true
            }

            val subPaint = Paint().apply {
                color = android.graphics.Color.rgb(147, 197, 253)
                textSize = subTextSize
                isAntiAlias = true
            }

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
                    start = Offset(sx(from.x), sy(from.y)),
                    end = Offset(sx(to.x), sy(to.y)),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )
            }

            data.nodes.forEach { node ->
                val fillColor = when (node.status.lowercase()) {
                    "critical" -> Color(0xFF551B1B)
                    "warning" -> Color(0xFF5A4316)
                    else -> Color(0xFF0F172A)
                }

                val borderColor = when (node.status.lowercase()) {
                    "critical" -> Color(0xFFEF4444)
                    "warning" -> Color(0xFFFACC15)
                    else -> Color(0xFF334155)
                }

                val centerX = sx(node.x)
                val centerY = sy(node.y)

                val left = centerX - cardWidth / 2f
                val top = centerY - cardHeight / 2f

                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left, top),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = borderStroke)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    shorten(node.title, adaptiveTitleLength(cardWidth)),
                    left + 12f,
                    top + cardHeight * 0.36f,
                    titlePaint
                )

                drawContext.canvas.nativeCanvas.drawText(
                    shorten(buildNodeSubtitle(node), adaptiveSubtitleLength(cardWidth)),
                    left + 12f,
                    top + cardHeight * 0.72f,
                    subPaint
                )
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

fun shorten(text: String, max: Int): String {
    if (text.length <= max) return text
    return text.take((max - 3).coerceAtLeast(1)) + "..."
}

fun adaptiveTitleLength(cardWidth: Float): Int {
    return when {
        cardWidth >= 300f -> 34
        cardWidth >= 260f -> 30
        cardWidth >= 220f -> 26
        else -> 22
    }
}

fun adaptiveSubtitleLength(cardWidth: Float): Int {
    return when {
        cardWidth >= 300f -> 38
        cardWidth >= 260f -> 34
        cardWidth >= 220f -> 30
        else -> 24
    }
}
