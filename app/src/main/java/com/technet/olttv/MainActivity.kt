package com.technet.olttv

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
                    color = Color(0xFF07111E)
                ) {
                    ZabbixSysmapTvV14()
                }
            }
        }
    }
}

@Composable
fun ZabbixSysmapTvV14() {
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val responseState = remember { mutableStateOf<MapResponse?>(null) }

    suspend fun loadMap() {
        val result = withContext(Dispatchers.IO) {
            try {
                MapRepository().fetchMap()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        result
            .onSuccess {
                responseState.value = it
                error.value = null
            }
            .onFailure {
                error.value = it.message ?: "Erro ao carregar mapa"
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
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF06101C),
                        Color(0xFF0A1830),
                        Color(0xFF07111E)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        when {
            loading.value && responseState.value == null -> LoadingView()
            error.value != null && responseState.value == null -> ErrorView(error.value ?: "Erro desconhecido")
            responseState.value != null -> {
                val map = responseState.value!!.map

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactTopBar(
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
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xAA0B1325),
                                        Color(0x880A1428)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(8.dp)
                    ) {
                        ZabbixLikeMapViewportV14(map)
                    }
                }
            }
        }
    }
}

@Composable
fun CompactTopBar(
    mapName: String,
    updatedAt: String,
    nodeCount: Int,
    linkCount: Int,
    hasError: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0E1A31))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = mapName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Atualizado: $updatedAt",
                    color = Color(0xFFBFDBFE),
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniPill("Nós", nodeCount.toString())
                MiniPill("Links", linkCount.toString())
                MiniPill("Status", if (hasError) "cache" else "online")
            }
        }
    }
}

@Composable
fun MiniPill(label: String, value: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF143055), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color(0xFF93C5FD),
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC111827))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Falha ao carregar mapa",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    color = Color(0xFFFCA5A5),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ZabbixLikeMapViewportV14(data: TvMapData) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val sourceWidth = 1300f
        val sourceHeight = 900f

        val paddingX = 26f
        val paddingTop = 8f
        val paddingBottom = 34f

        val usableWidth = (canvasWidth - paddingX * 2).coerceAtLeast(1f)
        val usableHeight = (canvasHeight - paddingTop - paddingBottom).coerceAtLeast(1f)

        val scale = minOf(usableWidth / sourceWidth, usableHeight / sourceHeight)

        val contentWidth = sourceWidth * scale
        val contentHeight = sourceHeight * scale

        val offsetX = (canvasWidth - contentWidth) / 2f
        val offsetY = ((canvasHeight - contentHeight) / 2f) - 18f

        fun sx(x: Float): Float = offsetX + (x * scale)
        fun sy(y: Float): Float = offsetY + (y * scale)

        val nodeWidth = (230f * scale.coerceIn(1.0f, 1.45f)).coerceIn(190f, 295f)
        val nodeHeight = (88f * scale.coerceIn(1.0f, 1.45f)).coerceIn(72f, 110f)
        val titleTextSize = (19f * scale.coerceIn(1.0f, 1.4f)).coerceIn(15f, 24f)
        val subTextSize = (13.5f * scale.coerceIn(1.0f, 1.4f)).coerceIn(11f, 18f)
        val typeTextSize = (11f * scale.coerceIn(1.0f, 1.3f)).coerceIn(10f, 15f)
        val lineStroke = (5.2f * scale.coerceIn(1.0f, 1.45f)).coerceIn(4f, 8f)
        val borderStroke = (2.4f * scale.coerceIn(1.0f, 1.4f)).coerceIn(2f, 4f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = titleTextSize
                isAntiAlias = true
                isFakeBoldText = true
            }

            val subPaint = Paint().apply {
                color = android.graphics.Color.rgb(221, 235, 255)
                textSize = subTextSize
                isAntiAlias = true
            }

            val typePaint = Paint().apply {
                color = android.graphics.Color.rgb(147, 197, 253)
                textSize = typeTextSize
                isAntiAlias = true
                isFakeBoldText = true
            }

            data.links.forEach { link ->
                val from = data.nodes.firstOrNull { it.id == link.from } ?: return@forEach
                val to = data.nodes.firstOrNull { it.id == link.to } ?: return@forEach

                val lineColor = when (link.status.lowercase()) {
                    "critical" -> Color(0xFFFF5B5B)
                    "warning" -> Color(0xFFFACC15)
                    else -> Color(0xFF59A7FF)
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
                val colors = nodeStyle(node)

                val cx = sx(node.x)
                val cy = sy(node.y)

                val left = cx - nodeWidth / 2f
                val top = cy - nodeHeight / 2f

                drawRoundRect(
                    color = colors.fill,
                    topLeft = Offset(left, top),
                    size = Size(nodeWidth, nodeHeight),
                    cornerRadius = CornerRadius(20f, 20f)
                )

                drawRoundRect(
                    color = colors.border,
                    topLeft = Offset(left, top),
                    size = Size(nodeWidth, nodeHeight),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = borderStroke)
                )

                val badgeWidth = nodeWidth * 0.34f
                val badgeHeight = nodeHeight * 0.22f

                drawRoundRect(
                    color = colors.badge,
                    topLeft = Offset(left + 10f, top + 8f),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    node.type.uppercase(),
                    left + 18f,
                    top + badgeHeight + 3f,
                    typePaint
                )

                drawContext.canvas.nativeCanvas.drawText(
                    shorten(node.title, adaptiveTitleLength(nodeWidth)),
                    left + 12f,
                    top + nodeHeight * 0.52f,
                    titlePaint
                )

                drawContext.canvas.nativeCanvas.drawText(
                    shorten(buildNodeSubtitle(node), adaptiveSubtitleLength(nodeWidth)),
                    left + 12f,
                    top + nodeHeight * 0.82f,
                    subPaint
                )
            }
        }
    }
}

data class NodeVisualStyle(
    val fill: Color,
    val border: Color,
    val badge: Color
)

fun nodeStyle(node: TvNode): NodeVisualStyle {
    return when {
        node.status.equals("critical", ignoreCase = true) -> NodeVisualStyle(
            fill = Color(0xCC7F1D1D),
            border = Color(0xFFFF7B7B),
            badge = Color(0x66FF7B7B)
        )

        node.status.equals("warning", ignoreCase = true) -> NodeVisualStyle(
            fill = Color(0xCC6B4F1D),
            border = Color(0xFFFDE047),
            badge = Color(0x66FDE047)
        )

        node.type.equals("olt", ignoreCase = true) -> NodeVisualStyle(
            fill = Color(0xCC0B1F3A),
            border = Color(0xFF7DD3FC),
            badge = Color(0x6638BDF8)
        )

        node.type.equals("concentrator", ignoreCase = true) -> NodeVisualStyle(
            fill = Color(0xCC10233D),
            border = Color(0xFF93C5FD),
            badge = Color(0x665DADE2)
        )

        node.type.equals("switch", ignoreCase = true) -> NodeVisualStyle(
            fill = Color(0xCC14253C),
            border = Color(0xFFA5B4FC),
            badge = Color(0x667C83FD)
        )

        else -> NodeVisualStyle(
            fill = Color(0xCC0E172A),
            border = Color(0xFF8BC5FF),
            badge = Color(0x6659A7FF)
        )
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

fun adaptiveTitleLength(width: Float): Int {
    return when {
        width >= 270f -> 34
        width >= 235f -> 30
        width >= 205f -> 27
        else -> 24
    }
}

fun adaptiveSubtitleLength(width: Float): Int {
    return when {
        width >= 270f -> 40
        width >= 235f -> 34
        width >= 205f -> 30
        else -> 26
    }
}
