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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
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
                    FullScreenMapApp()
                }
            }
        }
    }
}

@Composable
fun FullScreenMapApp() {
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07111E),
                        Color(0xFF0A1630),
                        Color(0xFF08111F)
                    )
                )
            )
            .padding(12.dp)
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

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopStatusBar(
                        mapName = map.name,
                        updatedAt = responseState.value!!.updated_at,
                        nodeCount = map.nodes.size,
                        linkCount = map.links.size,
                        hasError = error.value != null
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = Color(0xFF2D6BFF),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(
                                color = Color(0xFF081425),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(12.dp)
                    ) {
                        FullScreenAutoScaleMap(data = map)
                    }
                }
            }
        }
    }
}

@Composable
fun TopStatusBar(
    mapName: String,
    updatedAt: String,
    nodeCount: Int,
    linkCount: Int,
    hasError: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC0F1B33)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = mapName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Atualizado: $updatedAt",
                    color = Color(0xFFBFDBFE),
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                StatusPill("Nós", nodeCount.toString())
                StatusPill("Links", linkCount.toString())
                StatusPill("Status", if (hasError) "cache" else "online")
            }
        }
    }
}

@Composable
fun StatusPill(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF122543),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2B5CCF),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color(0xFF93C5FD),
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
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
fun FullScreenAutoScaleMap(data: TvMapData) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val minX = data.nodes.minOfOrNull { it.x } ?: 0f
        val maxX = data.nodes.maxOfOrNull { it.x } ?: 100f
        val minY = data.nodes.minOfOrNull { it.y } ?: 0f
        val maxY = data.nodes.maxOfOrNull { it.y } ?: 100f

        val sourceWidth = (maxX - minX).coerceAtLeast(1f)
        val sourceHeight = (maxY - minY).coerceAtLeast(1f)

        val horizontalPadding = 90f
        val verticalPadding = 70f

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

        val cardWidth = (220f * scale.coerceIn(0.9f, 1.8f)).coerceIn(190f, 340f)
        val cardHeight = (82f * scale.coerceIn(0.9f, 1.6f)).coerceIn(76f, 130f)
        val titleTextSize = (24f * scale.coerceIn(0.9f, 1.4f)).coerceIn(20f, 32f)
        val subTextSize = (18f * scale.coerceIn(0.9f, 1.4f)).coerceIn(16f, 24f)
        val lineStroke = (6f * scale.coerceIn(0.9f, 1.6f)).coerceIn(4f, 10f)
        val borderStroke = (2.5f * scale.coerceIn(0.9f, 1.5f)).coerceIn(2f, 5f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = titleTextSize
                isAntiAlias = true
                isFakeBoldText = true
            }

            val subPaint = Paint().apply {
                color = android.graphics.Color.rgb(191, 219, 254)
                textSize = subTextSize
                isAntiAlias = true
            }

            data.links.forEach { link ->
                val from = data.nodes.firstOrNull { it.id == link.from } ?: return@forEach
                val to = data.nodes.firstOrNull { it.id == link.to } ?: return@forEach

                val lineColor = when (link.status.lowercase()) {
                    "critical" -> Color(0xFFFF4D4F)
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
                    "critical" -> Color(0xCC7F1D1D)
                    "warning" -> Color(0xCC6B4F1D)
                    else -> Color(0xCC0F172A)
                }

                val borderColor = when (node.status.lowercase()) {
                    "critical" -> Color(0xFFFF6B6B)
                    "warning" -> Color(0xFFFDE047)
                    else -> Color(0xFF60A5FA)
                }

                val centerX = sx(node.x)
                val centerY = sy(node.y)

                val left = centerX - cardWidth / 2f
                val top = centerY - cardHeight / 2f

                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(22f, 22f)
                )

                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left, top),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(22f, 22f),
                    style = Stroke(width = borderStroke)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    shorten(node.title, adaptiveTitleLength(cardWidth)),
                    left + 14f,
                    top + cardHeight * 0.38f,
                    titlePaint
                )

                drawContext.canvas.nativeCanvas.drawText(
                    shorten(buildNodeSubtitle(node), adaptiveSubtitleLength(cardWidth)),
                    left + 14f,
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
        cardWidth >= 320f -> 38
        cardWidth >= 280f -> 33
        cardWidth >= 240f -> 28
        else -> 24
    }
}

fun adaptiveSubtitleLength(cardWidth: Float): Int {
    return when {
        cardWidth >= 320f -> 42
        cardWidth >= 280f -> 36
        cardWidth >= 240f -> 32
        else -> 26
    }
}
