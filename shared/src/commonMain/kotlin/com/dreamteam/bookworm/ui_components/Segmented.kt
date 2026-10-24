package com.dreamteam.bookworm.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.bookworm.domain.model.Fraction

/** Two or more equal-width tabs on a sunken track ("Notes" / "Flashcards"). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BookWormColors.Track)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(16.dp)) else Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) BookWormColors.Surface else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) BookWormColors.Ink else BookWormColors.InkMuted,
                )
            }
        }
    }
}

/** A fraction written the school way: numerator over a bar over denominator. */
@Composable
fun FractionText(
    fraction: Fraction,
    modifier: Modifier = Modifier,
    fontSize: Int = 40,
    color: Color = BookWormColors.Ink,
) {
    val style = MaterialTheme.typography.headlineLarge.copy(fontSize = fontSize.sp, lineHeight = fontSize.sp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${fraction.numerator}", style = style, color = color, modifier = Modifier.padding(horizontal = 8.dp))
        Box(
            Modifier
                .padding(vertical = (fontSize / 10).dp)
                .width((fontSize * 0.9f).dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text("${fraction.denominator}", style = style, color = color, modifier = Modifier.padding(horizontal = 8.dp))
    }
}
