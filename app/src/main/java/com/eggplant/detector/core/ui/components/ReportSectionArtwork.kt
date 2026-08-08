package com.eggplant.detector.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.eggplant.detector.R

enum class ReportArtworkKind {
    DESCRIPTION,
    SYMPTOMS,
    SIGNS,
    CAUSES,
    ACTIONS,
    PREVENTION,
    GUIDANCE,
    WHEN_TO_ACT,
    DISCLAIMER,
    REFERENCES,
}

@Composable
fun ReportSectionArtwork(
    kind: ReportArtworkKind,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(kind.drawableRes()),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@DrawableRes
private fun ReportArtworkKind.drawableRes(): Int = when (this) {
    ReportArtworkKind.DESCRIPTION -> R.drawable.report_description
    ReportArtworkKind.SYMPTOMS -> R.drawable.report_symptoms
    ReportArtworkKind.SIGNS -> R.drawable.report_signs
    ReportArtworkKind.CAUSES -> R.drawable.report_causes
    ReportArtworkKind.ACTIONS -> R.drawable.report_actions
    ReportArtworkKind.PREVENTION -> R.drawable.report_prevention
    ReportArtworkKind.GUIDANCE -> R.drawable.report_guidance
    ReportArtworkKind.WHEN_TO_ACT -> R.drawable.report_when_to_act
    ReportArtworkKind.DISCLAIMER -> R.drawable.report_disclaimer
    ReportArtworkKind.REFERENCES -> R.drawable.report_references
}
