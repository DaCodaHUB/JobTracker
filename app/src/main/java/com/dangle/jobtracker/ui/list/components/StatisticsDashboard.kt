package com.dangle.jobtracker.ui.list.components

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
import com.dangle.jobtracker.ui.theme.LocalIsDarkTheme

@Composable
fun StatisticsDashboard(
    statistics: JobStatistics,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    
    // Theme-aware color palettes: Vibrant for light mode, Muted for dark mode
    val activeColor = if (isDark) Color(0xFF1E3A5F) else Color(0xFF90CAF9)
    val interviewColor = if (isDark) Color(0xFF4A3420) else Color(0xFFFFCC80)
    val responseColor = if (isDark) Color(0xFF213A28) else Color(0xFFA5D6A7)
    val offerColor = if (isDark) Color(0xFF4A2020) else Color(0xFFEF9A9A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "ACTIVE\nAPPLICATIONS",
            value = statistics.activeCount.toString(),
            color = activeColor,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "INTERVIEWS",
            value = statistics.interviewCount.toString(),
            color = interviewColor,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "RESPONSES",
            value = "${statistics.responseRate}%",
            color = responseColor,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "OFFERS",
            value = statistics.offerCount.toString(),
            color = offerColor,
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
    val isDark = LocalIsDarkTheme.current
    val contentColor = if (isDark) Color.White else Color.Black

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
                color = contentColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
