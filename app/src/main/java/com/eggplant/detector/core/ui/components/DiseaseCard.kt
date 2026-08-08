package com.eggplant.detector.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.eggplant.detector.R
import com.eggplant.detector.domain.model.Disease

@Composable
fun DiseaseCard(
    disease: Disease,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val description = stringResource(R.string.open_disease_details, disease.name)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.heightIn(min = 70.dp))
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (compact) 16.dp else 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 1.dp else 2.dp),
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DiseaseArtwork(
                    artworkKey = disease.id,
                    localArtworkPath = disease.artworkPath,
                    contentDescriptionOverride = "${disease.name} disease photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.45f),
                )
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        disease.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 2,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    disease.symptomPreview,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                DiseaseTypeTag(disease)
            }
        } else {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiseaseArtwork(
                    artworkKey = disease.id,
                    localArtworkPath = disease.artworkPath,
                    contentDescriptionOverride = "${disease.name} disease photo",
                    modifier = Modifier
                        .size(width = 88.dp, height = 58.dp)
                        .aspectRatio(1.52f),
                )
                Spacer(Modifier.width(11.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        disease.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.sp, lineHeight = 14.sp),
                    )
                    Text(
                        disease.symptomPreview,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp, lineHeight = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                    DiseaseTypeTag(disease)
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun DiseaseTypeTag(disease: Disease) {
    val isLeaf = disease.type.name.startsWith("LEAF")
    val tagColor = if (isLeaf) {
        com.eggplant.detector.core.ui.theme.LeafGreenDark
    } else {
        com.eggplant.detector.core.ui.theme.PrimaryGreenDark
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isLeaf) com.eggplant.detector.core.ui.theme.LeafGreenSoft
        else com.eggplant.detector.core.ui.theme.PrimaryGreenSoft,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                if (isLeaf) Icons.Outlined.Eco else Icons.Outlined.LocalFlorist,
                null,
                tint = tagColor,
                modifier = Modifier.size(10.dp),
            )
            Text(
                stringResource(if (isLeaf) R.string.leaf_disease else R.string.fruit_disease),
                color = tagColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                ),
            )
        }
    }
}
