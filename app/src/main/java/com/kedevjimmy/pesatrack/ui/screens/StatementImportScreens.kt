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
 val selected=remember {mutableStateMapOf<Int,Boolean>()};var busy by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)};var done by remember {mutableStateOf<String?>(null)}
 LaunchedEffect(Unit){try{accounts=withContext(Dispatchers.IO){store.ownedAccounts()}.filter {it.type==AccountType.BANK||it.type==AccountType.MOBILE_WALLET}}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load accounts."}}
 LaunchedEffect(rows,accountId){preview=emptyList();selected.clear();if(rows.isNotEmpty() && accountId.isNotEmpty()){busy=true;try{preview=withContext(Dispatchers.IO){store.preview(accountId,rows)};preview.forEachIndexed {i,p->selected[i]=!p.duplicate&&!p.possibleMatch}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not check entries."}finally{busy=false}}}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){busy=true;error=null;done=null;pdfText=null;preview=emptyList();rows=emptyList();scope.launch{try{rows=withContext(Dispatchers.IO){StatementCsv.parse(readStatement(c,uri))}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not read this CSV. Check its format."}finally{busy=false}}}}
 val pdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){busy=true;error=null;done=null;pdfText=null;rows=emptyList();preview=emptyList();selected.clear();scope.launch{try{pdfText=PdfTextReviewReader.read(c,uri)}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not preview this PDF. Nothing was imported."}finally{busy=false}}}}
 MoneyToolSheet("Import statement","Import entries from UTF-8 CSV, up to 2 MB and 2,000 entries, kept on this phone. Use date, description, amount and optional reference. Negative amounts are payments; positive amounts are money received. You can use debit and credit columns instead. Dates: yyyy-MM-dd or dd/MM/yyyy. PDF text preview is available on Android 15 or newer, up to 2 MB and 40 pages. It reads selectable text only and does not import transactions or interpret bank tables or payroll. Scanned/image PDFs need OCR; password-protected PDFs are unsupported. A payslip describes payroll, not proof that money reached this account. Exact duplicates are skipped. Possible matches start unchecked; select one only if it is a different payment. Imported entries need your review before reports use them.",busy,onDismiss) {
  item {ChoiceField("Account",accounts.firstOrNull{it.id==accountId}?.name ?: "Choose account",accounts.map {it.id to it.name},!busy){accountId=it;done=null}}
  item {OutlinedButton(onClick={picker.launch(arrayOf("text/csv","text/comma-separated-values","application/csv","text/plain","application/octet-stream"))},enabled=!busy&&accountId.isNotEmpty(),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Rounded.UploadFile,null);Spacer(Modifier.width(8.dp));Text("Choose CSV")}}
  item {
   OutlinedButton(onClick={pdfPicker.launch(arrayOf("application/pdf"))},enabled=!busy&&Build.VERSION.SDK_INT>=35,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Preview PDF text")}
   Text(if(Build.VERSION.SDK_INT>=35)"Review a bank/M-Pesa statement or payslip locally. PDF preview does not add any financial entries." else "PDF text preview needs Android 15 or newer. Use CSV or manual entry on this phone.",style=MaterialTheme.typography.bodySmall)
  }
  item {ToolError(error);done?.let {Text(it)};if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())}
  pdfText?.let {pdf->
   item {
    Text("Text preview only · ${pdf.pages.size} pages",style=MaterialTheme.typography.titleSmall)
    Text("No transactions or salary were imported. Reading text does not verify amounts, table columns, account ownership or receipt of money.")
    if(pdf.emptyPageNumbers.isNotEmpty())Text("No selectable text on pages ${pdf.emptyPageNumbers.joinToString()}. These pages may be scanned; this preview is incomplete.",color=MaterialTheme.colorScheme.error)
    TextButton(onClick={pdfText=null},enabled=!busy){Text("Clear PDF preview")}
   }
   pdf.pages.forEachIndexed {index,text->
    item {Text("Page ${index+1}",style=MaterialTheme.typography.titleSmall)}
    if(text.isBlank())item {Text("No selectable text.")}
    else items(text.chunked(4000)){part->Text(part,style=MaterialTheme.typography.bodySmall)}
   }
  }
  if(preview.isNotEmpty()){
   item {Text("${preview.count{!it.duplicate&&!it.possibleMatch}} new · ${preview.count{!it.duplicate&&it.possibleMatch}} possible matches · ${preview.count{it.duplicate}} already recorded",style=MaterialTheme.typography.titleSmall)}
   item {Button(onClick={busy=true;error=null;scope.launch{try{val chosen=preview.mapIndexedNotNull {i,p->if(selected[i]==true&&!p.duplicate)p.row else null};val count=withContext(Dispatchers.IO){store.importRows(accountId,chosen)};onChanged();done="Added $count entries. Confirm them in Activity.";rows=emptyList();preview=emptyList();selected.clear()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not import. Nothing was changed."}finally{busy=false}}},enabled=!busy&&selected.values.any{it},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Import ${selected.values.count{it}} entries")}}
   items(preview.size,key={it}){i->val p=preview[i];PesaCard{Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=selected[i]==true,onCheckedChange={selected[i]=it},enabled=!busy&&!p.duplicate);Column(Modifier.weight(1f)){Text(p.row.description,style=MaterialTheme.typography.titleSmall);Text(toolDate(p.row.date)+(if(p.duplicate)" · Already recorded"else if(p.possibleMatch)" · Possible match"else""),style=MaterialTheme.typography.bodySmall)};Text(toolMoney(p.row.signedMinor),style=MaterialTheme.typography.bodySmall)}}}
  }
 }
}

