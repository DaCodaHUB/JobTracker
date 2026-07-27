package com.dangle.jobtracker.ui.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangle.jobtracker.ui.list.JobStatistics

@Composable
fun StatisticsDashboard(
    statistics: JobStatistics,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "ACTIVE\nAPPLICATIONS",
            value = statistics.activeCount.toString(),
            color = Color(0xFFE3F2FD), // Light Blue
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "INTERVIEWS",
            value = statistics.interviewCount.toString(),
            color = Color(0xFFFFF3E0), // Light Orange
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "RESPONSES",
            value = "${statistics.responseRate}%",
            color = Color(0xFFE8F5E9), // Light Green
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "OFFERS",
            value = statistics.offerCount.toString(),
            color = Color(0xFFFFEBEE), // Light Pink/Red
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(90.dp),
        shape = MaterialTheme.shapes.medium,
        color = color,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp,
                color = Color.Black.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
