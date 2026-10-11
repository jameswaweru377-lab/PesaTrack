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
import java.util.TimeZone
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import android.os.Build
import com.kedevjimmy.pesatrack.documents.PdfTextReviewReader
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextPreview

private fun readStatement(c:Context,uri:Uri):String {
 val bytes=c.contentResolver.openInputStream(uri)?.use {input->val out=ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=StatementCsv.MAX_BYTES){"Choose a CSV smaller than 2 MB."};out.write(buffer,0,n)};out.toByteArray()} ?: error("Could not open the file.")
 return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}
@Composable internal fun StatementImportSheet(onDismiss:()->Unit,onChanged:()->Unit) {
 val c=LocalContext.current;val store=remember {EverydayMoneyStore(c)};val scope=rememberCoroutineScope()
 var accounts by remember {mutableStateOf<List<MoneyAccount>>(emptyList())};var accountId by rememberSaveable {mutableStateOf("")}
 var rows by remember {mutableStateOf<List<StatementRow>>(emptyList())};var preview by remember {mutableStateOf<List<EverydayMoneyStore.Preview>>(emptyList())}
 var pdfText by remember {mutableStateOf<PdfTextPreview?>(null)}
 val zoneId=rememberSaveable {TimeZone.getDefault().id}
 val today=remember {StatementImportDates.day(System.currentTimeMillis(),zoneId)}
 var customDates by rememberSaveable {mutableStateOf(false)}
 var fromDay by rememberSaveable {mutableStateOf(today.substring(0,8)+"01")}
 var toDay by rememberSaveable {mutableStateOf(today)}
 val selection=remember(accountId,rows,customDates,fromDay,toDay,zoneId) {
  StatementImportSelection(accountId,rows.toList(),if(customDates)StatementDateRange(fromDay,toDay)else null,zoneId)
 }
 var previewSource by remember {mutableStateOf<StatementImportSelection?>(null)}
 var dateResult by remember {mutableStateOf<StatementDateSelection?>(null)}
 var preparing by remember {mutableStateOf<StatementImportSelection?>(null)}
 var reading by remember {mutableStateOf(false)};var importing by remember {mutableStateOf(false)}
 val busy=reading||importing||preparing!=null
 val selected=remember {mutableStateMapOf<Int,Boolean>()};var error by remember {mutableStateOf<String?>(null)};var dateError by remember {mutableStateOf<String?>(null)};var done by remember {mutableStateOf<String?>(null)}
 fun invalidatePreview() {previewSource=null;preview=emptyList();dateResult=null;selected.clear();error=null;dateError=null;done=null}
 LaunchedEffect(Unit){try{accounts=withContext(Dispatchers.IO){store.ownedAccounts()}.filter {it.type==AccountType.BANK||it.type==AccountType.MOBILE_WALLET}}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load accounts."}}
 LaunchedEffect(selection){
  val source=selection
  previewSource=null;preview=emptyList();dateResult=null;selected.clear();dateError=null
  try {
   StatementImportDates.validateRange(source.range,System.currentTimeMillis(),source.zoneId)
   if(source.rows.isNotEmpty() && source.accountId.isNotEmpty()) {
    preparing=source
    val (dates,checked)=withContext(Dispatchers.IO) {
     val dates=StatementImportDates.select(source)
     dates to if(dates.rows.isEmpty())emptyList()else store.preview(source.accountId,dates.rows)
    }
    dateResult=dates;preview=checked;previewSource=source
    checked.forEachIndexed {i,p->selected[i]=!p.duplicate&&!p.possibleMatch}
   }
  }catch(e:CancellationException){throw e}catch(e:Exception){dateError=e.message ?: "Could not check entries."}
  finally {if(preparing==source)preparing=null}
 }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null&&!reading&&!importing&&preparing==null){reading=true;invalidatePreview();pdfText=null;rows=emptyList();scope.launch{try{rows=withContext(Dispatchers.IO){StatementCsv.parse(readStatement(c,uri),TimeZone.getTimeZone(zoneId))}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not read this CSV. Check its format."}finally{reading=false}}}}
 val pdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null&&!reading&&!importing&&preparing==null){reading=true;invalidatePreview();pdfText=null;rows=emptyList();scope.launch{try{pdfText=PdfTextReviewReader.read(c,uri)}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not preview this PDF. Nothing was imported."}finally{reading=false}}}}
 MoneyToolSheet("Import statement","Import entries from UTF-8 CSV, up to 2 MB and 2,000 entries, kept on this phone. Use date, description, amount and optional reference. Negative amounts are payments; positive amounts are money received. You can use debit and credit columns instead. Dates: yyyy-MM-dd or dd/MM/yyyy. PDF text preview is available on Android 15 or newer, up to 2 MB and 40 pages. It reads selectable text only and does not import transactions or interpret bank tables or payroll. Scanned/image PDFs need OCR; password-protected PDFs are unsupported. A payslip describes payroll, not proof that money reached this account. Exact duplicates are skipped. Possible matches start unchecked; select one only if it is a different payment. Imported entries need your review before reports use them.",busy,onDismiss) {
  item {ChoiceField("Account",accounts.firstOrNull{it.id==accountId}?.name ?: "Choose account",accounts.map {it.id to it.name},!busy){if(accountId!=it){invalidatePreview();accountId=it}}}
  item {
   ChoiceField("Import dates",if(customDates)"Custom date range"else"All statement dates",listOf("all" to "All statement dates","custom" to "Custom date range"),!busy){val next=it=="custom";if(customDates!=next){invalidatePreview();customDates=next}}
   Text("Custom dates include both chosen days. Only entries within those dates are offered for import; existing history is unchanged.",style=MaterialTheme.typography.bodySmall)
  }
  if(customDates) {
   item {StatementImportDateButton("From date",fromDay,!busy){if(fromDay!=it){invalidatePreview();fromDay=it}}}
   item {StatementImportDateButton("To date",toDay,!busy){if(toDay!=it){invalidatePreview();toDay=it}}}
  }
  item {OutlinedButton(onClick={picker.launch(arrayOf("text/csv","text/comma-separated-values","application/csv","text/plain","application/octet-stream"))},enabled=!busy&&accountId.isNotEmpty(),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Rounded.UploadFile,null);Spacer(Modifier.width(8.dp));Text("Choose CSV")}}
  item {
   OutlinedButton(onClick={pdfPicker.launch(arrayOf("application/pdf"))},enabled=!busy&&Build.VERSION.SDK_INT>=35,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Preview PDF text")}
   Text(if(Build.VERSION.SDK_INT>=35)"Review a bank/M-Pesa statement or payslip locally. PDF preview does not add any financial entries." else "PDF text preview needs Android 15 or newer. Use CSV or manual entry on this phone.",style=MaterialTheme.typography.bodySmall)
  }
  item {ToolError(error ?: dateError);done?.let {Text(it)};if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())}
  if(previewSource==selection)dateResult?.let {dates ->item {
   Text("${dates.rows.size} of ${rows.size} entries in selected dates · ${dates.excludedCount} outside",style=MaterialTheme.typography.titleSmall)
   Text("Entries in this file: ${dates.availableFromDay} to ${dates.availableToDay}.",style=MaterialTheme.typography.bodySmall)
   if(dates.rows.isEmpty()) Text("No transactions in the selected date range. Choose different dates or another statement.",color=MaterialTheme.colorScheme.error)
   selection.range?.let {range->if(dates.availableFromDay!=null && dates.availableToDay!=null && (range.fromDay<dates.availableFromDay || range.toDay>dates.availableToDay))
    Text("Some selected dates fall outside the dates of entries in this file. Only available entries can be imported.",style=MaterialTheme.typography.bodySmall)}
  }}
  pdfText?.let {pdf->
   item {
    Text("Text preview only · ${pdf.pages.size} pages",style=MaterialTheme.typography.titleSmall)
    Text("No transactions or salary were imported. The date chooser filters CSV transaction rows; PDF text preview is not filtered or imported. Reading text does not verify amounts, table columns, account ownership or receipt of money.")
    if(pdf.emptyPageNumbers.isNotEmpty())Text("No selectable text on pages ${pdf.emptyPageNumbers.joinToString()}. These pages may be scanned; this preview is incomplete.",color=MaterialTheme.colorScheme.error)
    TextButton(onClick={pdfText=null},enabled=!busy){Text("Clear PDF preview")}
   }
   pdf.pages.forEachIndexed {index,text->
    item {Text("Page ${index+1}",style=MaterialTheme.typography.titleSmall)}
    if(text.isBlank())item {Text("No selectable text.")}
    else items(text.chunked(4000)){part->Text(part,style=MaterialTheme.typography.bodySmall)}
   }
  }
  if(previewSource==selection && preview.isNotEmpty()){
   item {Text("${preview.count{!it.duplicate&&!it.possibleMatch}} new · ${preview.count{!it.duplicate&&it.possibleMatch}} possible matches · ${preview.count{it.duplicate}} already recorded",style=MaterialTheme.typography.titleSmall)}
   item {Button(onClick={
    val source=selection;val reviewed=previewSource
    val chosen=preview.mapIndexedNotNull {i,p->if(selected[i]==true&&!p.duplicate)p.row else null}
    importing=true;error=null
    scope.launch{try{
     val count=withContext(Dispatchers.IO){store.importRows(source.accountId,StatementImportDates.requireReviewed(source,reviewed,chosen))}
     onChanged();done="Added $count entries. Confirm them in Activity.";rows=emptyList();previewSource=null;dateResult=null;preview=emptyList();selected.clear()
    }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not finish import. Check Activity before retrying."}finally{importing=false}}
   },enabled=!busy&&dateError==null&&previewSource==selection&&selected.values.any{it},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Import ${selected.values.count{it}} entries")}}
   items(preview.size,key={it}){i->val p=preview[i];PesaCard{Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=selected[i]==true,onCheckedChange={selected[i]=it},enabled=!busy&&!p.duplicate);Column(Modifier.weight(1f)){Text(p.row.description,style=MaterialTheme.typography.titleSmall);Text(StatementImportDates.day(p.row.date,zoneId)+(if(p.duplicate)" · Already recorded"else if(p.possibleMatch)" · Possible match"else""),style=MaterialTheme.typography.bodySmall)};Text(toolMoney(p.row.signedMinor),style=MaterialTheme.typography.bodySmall)}}}
  }
 }
}

@Composable private fun StatementImportDateButton(label:String,day:String,enabled:Boolean,onChanged:(String)->Unit) {
 val context=LocalContext.current
 OutlinedButton(enabled=enabled,onClick={
  val parts=day.split('-').map {it.toInt()}
  DatePickerDialog(context,{_,year,month,date->onChanged(String.format(Locale.ROOT,"%04d-%02d-%02d",year,month+1,date))},parts[0],parts[1]-1,parts[2])
   .apply {datePicker.maxDate=System.currentTimeMillis()}.show()
 },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("$label: $day")}
}

