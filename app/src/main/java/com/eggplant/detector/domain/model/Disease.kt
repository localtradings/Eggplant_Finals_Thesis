package com.eggplant.detector.domain.model

data class Disease(
    val id: String,
    val name: String,
    val type: DiseaseType,
    val symptomPreview: String,
    val description: String = symptomPreview,
    val signs: List<String>,
    val treatment: String,
    val prevention: String,
    val causes: String = "",
    val guidance: String = "",
    val whenToAct: String = "",
    val disclaimer: String = "This result is a screening aid, not a definitive diagnosis. Consult a qualified local agricultural specialist before applying treatment.",
    val references: List<DiseaseReference> = emptyList(),
    val isDetectorSupported: Boolean = true,
    val artworkPath: String? = null,
)

data class DiseaseReference(
    val publisher: String,
    val title: String,
    val url: String,
)
