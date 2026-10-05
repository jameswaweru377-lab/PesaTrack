package com.kedevjimmy.pesatrack.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kedevjimmy.pesatrack.data.local.StoredTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun ReportExportButton(rows:List<StoredTransaction>) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var message by remember {mutableStateOf<String?>(null)}
    var pending by remember {mutableStateOf(emptyList<StoredTransaction>())}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {uri->if(uri!=null)scope.launch {
        try {withContext(Dispatchers.IO) {
            val out=context.contentResolver.openOutputStream(uri) ?: error("Couldn't open destination")
            out.bufferedWriter().use {writer->
                writer.appendLine("Date,Payee,Direction,Category,Amount KES,Fee KES")
                fun cell(value:String):String {val safe=if(value.firstOrNull() in listOf('=','+','-','@','\t','\r')) "'$value" else value;return "\""+safe.replace("\"","\"\"")+"\""}
                pending.forEach {row->writer.appendLine(listOf(SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(row.occurredAtEpochMs),row.counterparty.orEmpty(),row.direction,row.category,BigDecimal.valueOf(row.amountMinor,2).toPlainString(),BigDecimal.valueOf(row.feeMinor,2).toPlainString()).joinToString(","){cell(it)})}
            }
        };message="Report exported."}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(error:Exception){message="Couldn't export. Please try again."}
    }}
    Column {
        Button(onClick={pending=rows;launcher.launch("PesaTrack-report.csv")},enabled=rows.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("Export report")}
        message?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
    }
}
