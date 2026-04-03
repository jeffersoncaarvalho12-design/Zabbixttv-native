package com.technet.olttv

data class MapResponse(
    val success: Boolean,
    val updated_at: String,
    val map: TvMapData
)

data class TvMapData(
    val name: String,
    val background: String,
    val nodes: List<TvNode>,
    val links: List<TvLink>
)

data class TvNode(
    val id: String,
    val title: String,
    val type: String,
    val x: Float,
    val y: Float,
    val rx: String?,
    val tx: String?,
    val download: String?,
    val upload: String?,
    val info: String?,
    val status: String
)

data class TvLink(
    val from: String,
    val to: String,
    val status: String
)
