package com.example.gymappv10

data class ClassModel(
    val name: String = "",
    val description: String = "",
    val date: String? = null,
    val dateAdded: String = "",
    val dayOfWeek: List<String> = emptyList(),
    val time: String = "",
    val instructor: String = "",
    val duration: String = "",
    val venue: String = "",
    val price: Double = 0.0,
    val capacity: Int = 0,
    val isOneTime: Boolean = false
)
