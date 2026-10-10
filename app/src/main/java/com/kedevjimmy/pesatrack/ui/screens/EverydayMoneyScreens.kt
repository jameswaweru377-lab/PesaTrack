package com.kedevjimmy.pesatrack.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.everyday.*
import com.kedevjimmy.pesatrack.domain.model.*
import com.kedevjimmy.pesatrack.domain.recurring.RecurringPatternCandidate
import com.kedevjimmy.pesatrack.planning.*
import com.kedevjimmy.pesatrack.ui.components.PesaCard
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal fun toolMoney(minor:Long):String = "KES " + java.math.BigDecimal.valueOf(minor,2).toPlainString()
internal fun toolDate(epoch:Long):String=SimpleDateFormat("d MMM yyyy",Locale.getDefault()).format(java.util.Date(epoch))
internal fun amountText(minor:Long)=java.math.BigDecimal.valueOf(minor,2).toPlainString()

@Composable internal fun MoneyHelp(text:String) {
 var open by remember {mutableStateOf(false)}
 IconButton(onClick={open=true},modifier=Modifier.size(48.dp)) {Icon(Icons.AutoMirrored.Rounded.HelpOutline,"Help")}
 if(open) AlertDialog(onDismissRequest={open=false},title={Text("How this works")},text={Text(text)},confirmButton={TextButton(onClick={open=false}){Text("Got it")}})
}
@Composable internal fun ToolHeading(title:String,help:String) {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);MoneyHelp(help)}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun MoneyToolSheet(title:String,help:String,busy:Boolean,onDismiss:()->Unit,content:LazyListScope.()->Unit) {
 ModalBottomSheet(onDismissRequest={if(!busy)onDismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(.88f).imePadding(),contentPadding=PaddingValues(horizontal=18.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
   item {ToolHeading(title,help)};content();item {Spacer(Modifier.navigationBarsPadding().height(12.dp))}
  }
 }
}
@Composable internal fun ChoiceField(label:String,value:String,choices:List<Pair<String,String>>,enabled:Boolean=true,onChoose:(String)->Unit) {
 var open by remember {mutableStateOf(false)}
 Column {Text(label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Box {OutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(value,Modifier.weight(1f));Icon(Icons.Rounded.ExpandMore,null)}
   DropdownMenu(expanded=open,onDismissRequest={open=false}) {choices.forEach {(id,name)->DropdownMenuItem(text={Text(name)},onClick={open=false;onChoose(id)})}}
  }
 }
}
@Composable internal fun MoneyAmount(value:String,label:String="Amount (KES)",enabled:Boolean=true,onChange:(String)->Unit) {
 OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,enabled=enabled,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
}
@Composable internal fun ToolError(message:String?) {message?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium)}}
internal fun chooseDate(context:Context,date:Long,update:(Long)->Unit) {
 val c=Calendar.getInstance().apply {timeInMillis=date}
 DatePickerDialog(context,{_,y,m,d->update(Calendar.getInstance().apply{clear();set(y,m,d)}.timeInMillis)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).apply {datePicker.maxDate=System.currentTimeMillis();show()}
}

