package com.technet.olttv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TvMapApp()
            }
        }
    }
}

@Composable
fun TvMapApp(viewModel: MapViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var selectedNodeId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08111F))
            .padding(20.dp)
    ) {
        when {
            state.loading && state.data == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.error != null && state.data == null -> {
                Text(
                    text = "Erro: ${state.error}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.data != null -> {
                val map = state.data!!
                val selectedNode = map.nodes.firstOrNull { it.id == selectedNodeId } ?: map.nodes.firstOrNull()

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.width(380.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        InfoCard(
                            title = map.name,
                            lines = listOf(
                                "Atualizado: ${state.updatedAt ?: "-"}",
                                "Nós: ${map.nodes.size}",
                                "Links: ${map.links.size}"
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
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(24.dp))
                            .background(Color(0xFF0B1325), RoundedCornerShape(24.dp))
                            .padding(12.dp)
                    ) {
                        TvNativeMap(
                            data = map,
                            selectedNodeId = selectedNode?.id,
                            onSelectNode = { selectedNodeId = it }
                        )
                    }
                }
            }
        }
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
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            lines.forEach {
                Text(it, color = Color(0xFFBFDBFE), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun TvNativeMap(
    data: TvMapData,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            data.links.forEach { link ->
                val from = data.nodes.firstOrNull { it.id == link.from } ?: return@forEach
                val to = data.nodes.firstOrNull { it.id == link.to } ?: return@forEach

                val color = when (link.status.lowercase()) {
                    "critical" -> Color(0xFFEF4444)
                    "warning" -> Color(0xFFFACC15)
                    else -> Color(0xFF3B82F6)
                }

                drawLine(
                    color = color,
                    start = Offset(from.x, from.y),
                    end = Offset(to.x, to.y),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            data.nodes.forEach { node ->
                val color = when (node.status.lowercase()) {
                    "critical" -> Color(0xFF7F1D1D)
                    "warning" -> Color(0xFF78350F)
                    else -> Color(0xFF0F172A)
                }

                val border = if (node.id == selectedNodeId) Color(0xFF38BDF8) else Color(0xFF334155)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(node.x - 90f, node.y - 35f),
                    size = androidx.compose.ui.geometry.Size(180f, 70f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                drawRoundRect(
                    color = border,
                    topLeft = Offset(node.x - 90f, node.y - 35f),
                    size = androidx.compose.ui.geometry.Size(180f, 70f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                    style = Stroke(width = 3f)
                )
            }
        }

        data.nodes.forEach { node ->
            Card(
                modifier = Modifier
                    .padding(start = (node.x - 90).dp, top = (node.y - 35).dp)
                    .width(180.dp)
                    .height(70.dp)
                    .focusable(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                onClick = { onSelectNode(node.id) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
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
                        text = node.rx?.let { rx ->
                            node.tx?.let { tx -> "RX $rx | TX $tx" }
                        } ?: node.info ?: listOfNotNull(node.download, node.upload).joinToString(" | "),
                        color = Color(0xFF93C5FD),
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
