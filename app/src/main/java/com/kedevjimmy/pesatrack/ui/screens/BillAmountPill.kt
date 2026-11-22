package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.ui.components.pesaToneContainer
import com.kedevjimmy.pesatrack.ui.components.pesaToneContent

/** Shared bill amount treatment used by current planning details. */
@Composable
fun BillAmountPill(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = pesaToneContainer("amber")
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = pesaToneContent("amber"),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
