package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.domain.everyday.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable internal fun PdfStatementRowDialog(row: StatementRow,zoneId: String,onDismiss: ()->Unit,onSave: (StatementRow)->Unit) {
    var date by remember(row) {mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).apply {timeZone=TimeZone.getTimeZone(zoneId)}.format(Date(row.date)))}
    var description by remember(row) {mutableStateOf(row.description)}
    var amount by remember(row) {mutableStateOf(java.math.BigDecimal.valueOf(row.signedMinor,2).toPlainString())}
    var reference by remember(row) {mutableStateOf(row.reference ?: "")}
    var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Review extracted entry")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Check against the PDF. Positive amounts are received; negative amounts are paid out. Editing rechecks dates and duplicates.")
        OutlinedTextField(date,{date=it},label={Text("Date/time · yyyy-MM-dd HH:mm:ss")},singleLine=true)
        OutlinedTextField(description,{description=it},label={Text("Description")},singleLine=true)
        OutlinedTextField(amount,{amount=it},label={Text("Signed amount · KES")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)
        OutlinedTextField(reference,{reference=it},label={Text("Receipt reference")},singleLine=true)
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}},confirmButton={TextButton(onClick={
        try {onSave(PdfStatementReview.correct(date,description,amount,reference,TimeZone.getTimeZone(zoneId)))}
        catch(e: IllegalArgumentException){error=e.message ?: "Check the entry."}
    }){Text("Apply correction")}})
}
