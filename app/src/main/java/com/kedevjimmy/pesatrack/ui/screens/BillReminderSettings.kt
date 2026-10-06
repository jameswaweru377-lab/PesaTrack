package com.kedevjimmy.pesatrack.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.planning.*
import java.util.Calendar

@Composable
internal fun BillReminderSettings(bill:ScheduledBill,onDismiss:()->Unit) {
    val context=LocalContext.current
    var date by remember(bill.rule.id) {mutableLongStateOf(Calendar.getInstance().apply {set(Calendar.DAY_OF_MONTH,minOf(bill.dueDay ?: 1,getActualMaximum(Calendar.DAY_OF_MONTH)))}.timeInMillis)}
    var enabled by remember(bill.rule.id) {mutableStateOf(bill.reminders)}
    var denied by remember {mutableStateOf(false)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {granted->enabled=granted;denied=!granted}
    AlertDialog(onDismissRequest=onDismiss,title={Text(bill.rule.displayLabel ?: bill.rule.name)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Choose the monthly due day. Short months use their last day.")
        SalaryReceiptDateButton(date,true,label="Monthly due day",allowFuture=true){date=it}
        Row {Text("Remind me before it is due",Modifier.weight(1f));Switch(enabled,onCheckedChange={on->if(on&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS) else enabled=on})}
        Text("A reminder runs in the background within three days of the due date. Matching payments reduce the amount still due.",style=MaterialTheme.typography.bodySmall)
        if(denied)Text("Allow notifications in Android Settings to receive reminders.",color=MaterialTheme.colorScheme.error)
    }},confirmButton={TextButton(onClick={BillSchedules(context).save(bill.rule.id,Calendar.getInstance().apply{timeInMillis=date}.get(Calendar.DAY_OF_MONTH),enabled);onDismiss()}){Text("Save reminder")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
