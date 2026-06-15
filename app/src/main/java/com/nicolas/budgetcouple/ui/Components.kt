package com.nicolas.budgetcouple.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicolas.budgetcouple.ui.theme.LineCol
import com.nicolas.budgetcouple.ui.theme.Muted
import com.nicolas.budgetcouple.ui.theme.Paper
import java.text.NumberFormat
import java.util.Locale

private val euroFmt: NumberFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
    maximumFractionDigits = 0
}
fun eur(n: Double): String = euroFmt.format(n)

@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = BorderStroke(1.dp, LineCol)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    LedgerCard(modifier) {
        Text(
            label.uppercase(),
            color = accent,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = accent,
            fontSize = 22.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, track: Color, height: Int = 9) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(track, RoundedCornerShape(2.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun Pill(text: String, accent: Color) {
    Box(
        Modifier
            .background(Color.White, RoundedCornerShape(2.dp))
            .border(BorderStroke(1.dp, accent), RoundedCornerShape(2.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(text, color = accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

val MutedColor = Muted
