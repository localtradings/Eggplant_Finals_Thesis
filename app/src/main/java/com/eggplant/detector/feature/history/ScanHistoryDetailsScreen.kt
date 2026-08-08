package com.eggplant.detector.feature.history

import androidx.compose.runtime.Composable
import com.eggplant.detector.domain.model.ScanResult
import com.eggplant.detector.domain.model.Disease
import com.eggplant.detector.feature.result.ResultReport
import androidx.compose.ui.res.stringResource
import com.eggplant.detector.R

@Composable
fun ScanHistoryDetailsScreen(
    result: ScanResult?,
    disease: Disease?,
    onBack: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    ResultReport(
        result = result,
        disease = disease,
        title = stringResource(R.string.history_detail),
        onBack = onBack,
        onToggleFavorite = onToggleFavorite,
        onDelete = onDelete,
    )
}
