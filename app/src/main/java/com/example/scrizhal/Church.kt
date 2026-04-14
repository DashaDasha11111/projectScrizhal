package com.example.scrizhal

data class Church(
    val id: Int,
    val name: String,
    val address: String,
    val rector: String,
    val clericCount: Int,
    /** WGS84, для карты; 0,0 — подставляются из справочника по id */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
