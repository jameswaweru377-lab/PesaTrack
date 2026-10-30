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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.kedevjimmy.pesatrack.documents.OfflineMpesaPdfReader
import com.kedevjimmy.pesatrack.documents.OfflineBankTableReader
import com.kedevjimmy.pesatrack.domain.statements.BankStatementAccountIdentityUiPolicy
import com.kedevjimmy.pesatrack.domain.statements.StatementAdmissionResult
import com.kedevjimmy.pesatrack.domain.statements.StatementBankAdmission
import com.kedevjimmy.pesatrack.domain.statements.StatementSourceProfileRegistry

private data class StagedBankPdf(
 val parsed: OfflineBankTableReader.Prepared,
 val admitted: StatementAdmissionResult.Accepted
)

private fun readStatement(c:Context,uri:Uri):String {
 val bytes=c.contentResolver.openInputStream(uri)?.use {input->val out=ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=StatementCsv.MAX_BYTES){"Choose a CSV smaller than 2 MB."};out.write(buffer,0,n)};out.toByteArray()} ?: error("Could not open the file.")
 return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}
@Composable internal fun StatementImportSheet(onDismiss:()->Unit,onChanged:()->Unit,onPaycheckClick:()->Unit = {},statementOnly:Boolean = false,onOpenStatements:(()->Unit)? = null) {
 val c=LocalContext.current;val store=remember {EverydayMoneyStore(c)};val scope=rememberCoroutineScope()
 var accounts by remember {mutableStateOf<List<MoneyAccount>>(emptyList())};var accountId by rememberSaveable {mutableStateOf("")}
 var rows by remember {mutableStateOf<List<StatementRow>>(emptyList())};var preview by remember {mutableStateOf<List<EverydayMoneyStore.Preview>>(emptyList())}
 var sourcePdfUri by remember {mutableStateOf<Uri?>(null)}
 var sourcePdfProviderKey by remember {mutableStateOf("SAFARICOM_MPESA")}
 var sourcePdfPassword by remember {mutableStateOf("")}
 var stagedPdf by remember {mutableStateOf<OfflineMpesaPdfReader.Prepared?>(null)}
 var stagedBankPdf by remember {mutableStateOf<StagedBankPdf?>(null)}
 var statementPdfApproved by remember {mutableStateOf(false)}
 // This constraint belongs to the current rows; changing accounts must not clear it.
 var activityRowsRequireAccountType by remember {mutableStateOf<AccountType?>(null)}
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
 var savingBankAccountNumber by remember {mutableStateOf(false)}
 var bankAccountNumberDraft by remember {mutableStateOf("")}
 var bankAccountEditingId by remember {mutableStateOf<String?>(null)}
 val busy=reading||importing||preparing!=null||savingBankAccountNumber
 val selectedBankAccount=accounts.firstOrNull{it.id==accountId&&it.type==AccountType.BANK}
 val bankAccountNumberReady=selectedBankAccount?.let {
  BankStatementAccountIdentityUiPolicy.hasFullNumber(it.identifier)
 } ?: false
 val selected=remember {mutableStateMapOf<Int,Boolean>()};var error by remember {mutableStateOf<String?>(null)};var dateError by remember {mutableStateOf<String?>(null)};var done by remember {mutableStateOf<String?>(null)}
 fun invalidatePreview() {stagedPdf=null;stagedBankPdf=null;statementPdfApproved=false;previewSource=null;preview=emptyList();dateResult=null;selected.clear();error=null;dateError=null;done=null}
 LaunchedEffect(Unit){try{accounts=withContext(Dispatchers.IO){store.ownedAccounts()}.filter {it.type==AccountType.BANK||it.type==AccountType.MOBILE_WALLET}}catch(e:CancellationException){throw e}catch(e:Exception){error="Could not load accounts."}}
  LaunchedEffect(selection,activityRowsRequireAccountType){
  val source=selection
  previewSource=null;preview=emptyList();dateResult=null;selected.clear();dateError=null
  try {
   StatementImportDates.validateRange(source.range,System.currentTimeMillis(),source.zoneId)
   if(source.rows.isNotEmpty() && source.accountId.isNotEmpty()) {
     activityRowsRequireAccountType?.let {requiredType->
      require(accounts.any{it.id==source.accountId&&it.type==requiredType}) {
       "M-Pesa statement movements must use a confirmed mobile wallet account."
      }
     }
    preparing=source
    val (dates,checked)=withContext(Dispatchers.IO) {
     val dates=StatementImportDates.select(source)
     dates to if(dates.rows.isEmpty())emptyList()else store.preview(source.accountId,dates.rows)
    }
    dateResult=dates;preview=checked;previewSource=source
    checked.forEachIndexed {i,p->
     selected[i]=!p.duplicate&&!p.possibleMatch
    }
   }
  }catch(e:CancellationException){throw e}catch(e:Exception){dateError=e.message ?: "Could not check entries."}
  finally {if(preparing==source)preparing=null}
 }
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null&&!reading&&!importing&&preparing==null){
    activityRowsRequireAccountType=null
   reading=true;invalidatePreview();rows=emptyList()
   scope.launch {
    try{rows=withContext(Dispatchers.IO){StatementCsv.parse(readStatement(c,uri),TimeZone.getTimeZone(zoneId))}}
    catch(e:CancellationException){throw e}
    catch(e:Exception){error=e.message ?: "Could not read this CSV. Check its format."}
    finally{reading=false}
   }
  }
 }
 val statementPdfPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null&&!busy&&accountId.isNotEmpty()){
   invalidatePreview();sourcePdfUri=uri;sourcePdfPassword=""
  }
 }
 StatementImportWorkspace(busy,onDismiss) {
  item {
   PesaCard {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Rounded.AccountBalanceWallet,null,tint=MaterialTheme.colorScheme.primary)
      Text("01 · Source account",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     }
     Text("Select the account that appears on your statement.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     ChoiceField("Account",accounts.firstOrNull{it.id==accountId}?.name ?: "Choose account",accounts.map {it.id to it.name},!busy&&stagedPdf==null&&stagedBankPdf==null&&sourcePdfUri==null){if(accountId!=it){invalidatePreview();bankAccountNumberDraft="";bankAccountEditingId=null;accountId=it}}
     if(accountId.isEmpty()) Text("Choose a confirmed account before reviewing transactions.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     selectedBankAccount?.let { bank ->
      if(!bankAccountNumberReady || bankAccountEditingId==bank.id) {
       Text("Complete bank account number required for PDF import. Enter the full number printed on your bank statement; the last four digits are not enough.",
        style=MaterialTheme.typography.bodySmall,color=if(bankAccountNumberReady)MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
       OutlinedTextField(
        value=bankAccountNumberDraft,onValueChange={bankAccountNumberDraft=it.take(64)},
        label={Text("Full bank account number")},singleLine=true,
        visualTransformation=PasswordVisualTransformation(),
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),
        isError=bankAccountNumberDraft.isNotEmpty() && BankStatementAccountIdentityUiPolicy.normalizeFullNumber(bankAccountNumberDraft)==null,
        supportingText={Text("8–20 digits. Saved only on this device; never uploaded for verification.")},
        modifier=Modifier.fillMaxWidth()
       )
       Button(onClick={
        val target=bank.id
        val fullNumber=BankStatementAccountIdentityUiPolicy.normalizeFullNumber(bankAccountNumberDraft)
         ?:return@Button
        savingBankAccountNumber=true;error=null
        scope.launch {
         try {
          val refreshed=withContext(Dispatchers.IO){
           AccountStore.get(c).updateIdentifier(target,fullNumber)
           store.ownedAccounts().filter{it.type==AccountType.BANK||it.type==AccountType.MOBILE_WALLET}
          }
          if(accountId==target) {
           accounts=refreshed;bankAccountNumberDraft="";bankAccountEditingId=null
           // Identity metadata does not change an already-reviewed CSV's rows or date selection.
           done="Bank account number saved on this device. You can now choose the bank PDF."
          }
         }catch(e:CancellationException){throw e}
         catch(e:Exception){error="Could not save the bank account number. Please try again."}
         finally{savingBankAccountNumber=false}
        }
       },enabled=!busy&&stagedPdf==null&&stagedBankPdf==null&&sourcePdfUri==null&&
        BankStatementAccountIdentityUiPolicy.normalizeFullNumber(bankAccountNumberDraft)!=null,
        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Save bank account number")}
      } else {
       Text("A complete bank account number is saved on this device for PDF verification.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       TextButton(onClick={bankAccountNumberDraft="";bankAccountEditingId=bank.id},
        enabled=!busy&&stagedBankPdf==null&&sourcePdfUri==null){Text("Change account number")}
      }
     }
    }
   }
  }
  if(accountId.isNotEmpty() && !statementOnly) item {
   PesaCard {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Rounded.CalendarMonth,null,tint=MaterialTheme.colorScheme.primary)
      Text("02 · Dates to include",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     }
     ChoiceField("Import dates",if(customDates)"Custom date range"else"All statement dates",listOf("all" to "All statement dates","custom" to "Custom date range"),!busy){val next=it=="custom";if(customDates!=next){invalidatePreview();customDates=next}}
     if(customDates) {
      StatementImportDateButton("From date",fromDay,!busy){if(fromDay!=it){invalidatePreview();fromDay=it}}
      StatementImportDateButton("To date",toDay,!busy){if(toDay!=it){invalidatePreview();toDay=it}}
     }
     Text("Both chosen days are included. Your recorded transactions stay unchanged.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  if(accountId.isNotEmpty()) item {
   PesaCard {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Rounded.UploadFile,null,tint=MaterialTheme.colorScheme.primary)
      Text(if(statementOnly)"02 · Choose your statement PDF"else"03 · Choose a document",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     }
     if(!statementOnly) {
      Text("Choose a supported CSV or verified PDF.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      OutlinedButton(onClick={picker.launch(arrayOf("text/csv","text/comma-separated-values","application/csv","text/plain","application/octet-stream"))},enabled=!busy&&accountId.isNotEmpty(),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Rounded.TableChart,null);Spacer(Modifier.width(8.dp));Text("Select CSV file")}
      if(onOpenStatements!=null) OutlinedButton(onClick={onDismiss();onOpenStatements()},enabled=!busy,
       modifier=Modifier.fillMaxWidth().heightIn(48.dp)){Text("Open Statements")}
     }
     OutlinedButton(onClick={sourcePdfProviderKey="SAFARICOM_MPESA";statementPdfPicker.launch(arrayOf("application/pdf"))},
      enabled=!busy&&accounts.any{it.id==accountId&&it.type==AccountType.MOBILE_WALLET},
      modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
      Icon(Icons.Rounded.PictureAsPdf,null);Spacer(Modifier.width(8.dp));Text("Choose M-Pesa PDF")
     }
     if(accounts.any{it.id==accountId&&it.type==AccountType.BANK}) {
      StatementSourceProfileRegistry.importableBankProfiles().forEach { profile ->
       OutlinedButton(onClick={sourcePdfProviderKey=profile.providerKey;statementPdfPicker.launch(arrayOf("application/pdf"))},
        enabled=!busy&&bankAccountNumberReady,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
        Icon(Icons.Rounded.AccountBalance,null);Spacer(Modifier.width(8.dp));Text("Choose ${profile.label} PDF")
       }
      }
     }
     Text("Your PDF is checked privately on this device. Only verified rows are saved to Statements; Activity balances stay separate.",
      style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
  if(error!=null || dateError!=null || done!=null || busy) item {
   PesaCard {
    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     ToolError(error ?: dateError)
     done?.let {
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       Icon(Icons.Rounded.CheckCircle,null,tint=MaterialTheme.colorScheme.primary)
       Text(it,style=MaterialTheme.typography.bodyMedium)
      }
     }
     if(busy) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
      Text("Checking your document and transactions…",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
   }
  }
  stagedPdf?.let{ready->item{
   PesaCard {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text("Local PDF verification",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     Text("${ready.pages} pages · ${ready.lines.size} source rows · ${ready.proof.cashRows} cash movements · ${ready.informationalRows} M-Shwari informational events.")
     Text("Official Paid In: ${toolMoney(ready.paidInMinor)} · Paid Out: ${toolMoney(ready.paidOutMinor)}.")
     Text("${ready.sourceCharacters} source characters checked for ownership by physical cells.",style=MaterialTheme.typography.bodySmall)
     if(ready.categoryPaidInGapMinor!=0L||ready.categoryPaidOutGapMinor!=0L)
      Text("Source PDF category subtotals disagree with the printed total: Paid In difference ${toolMoney(ready.categoryPaidInGapMinor)}; Paid Out difference ${toolMoney(ready.categoryPaidOutGapMinor)}. Detailed rows match the official TOTAL.",
       color=MaterialTheme.colorScheme.tertiary,style=MaterialTheme.typography.bodySmall)
     Text("Exact balance sequence under ${ready.fulizaCycles} provisional Fuliza exception(s), ${ready.sharedBalanceGroups} shared ending-balance groups. No independent opening/closing balance or external receipt count.",
      style=MaterialTheme.typography.bodySmall)
     Text("Every original source row appears below, including zero-value events. These are not SMS Activity postings.",
      style=MaterialTheme.typography.bodySmall)
     Row(verticalAlignment=Alignment.CenterVertically) {
      Checkbox(checked=statementPdfApproved,onCheckedChange={statementPdfApproved=it},enabled=!busy)
      Text("I reviewed the PDF and amounts. Save only in Statement Activity and leave my SMS ledger unchanged.",
       Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
     }
     Button(enabled=!busy&&statementPdfApproved&&accounts.any{it.id==accountId&&it.type==AccountType.MOBILE_WALLET},
      onClick={
       val data=ready
       val target=accountId
       importing=true;error=null
       scope.launch {
        try {
         val saved=withContext(Dispatchers.IO){
          StatementActivityStore(c).save(target,data.lines,data.proof)
         }
         done=if(saved.added)"Saved ${data.lines.size} original rows in Statement Activity. SMS Activity remains unchanged."
          else "This statement is already saved. No duplicate was created."
         stagedPdf=null;statementPdfApproved=false
         // A Statements-only launch returns to the existing dashboard after saving.
         onChanged();onDismiss()
         if(!statementOnly) onOpenStatements?.invoke()
        }catch(e:CancellationException){throw e}
        catch(e:Exception){error=e.message ?: "Statement Activity save failed. No ledger entries changed."}
        finally{importing=false}
       }
      },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
      Text("Save complete PDF to Statement Activity")
     }
     OutlinedButton(
       enabled=!busy&&ready.proof.totalsVerified&&ready.proof.cashRows>0&&accounts.any{it.id==accountId&&it.type==AccountType.MOBILE_WALLET},
      onClick={
       try {
         val accountType=accounts.firstOrNull{it.id==accountId}?.type
          ?:error("Choose a confirmed mobile wallet account first.")
         val activityRows=StatementPdfActivityPolicy.toActivityRows(ready.lines,ready.proof,accountType)
         activityRowsRequireAccountType=AccountType.MOBILE_WALLET
        previewSource=null;preview=emptyList();dateResult=null;selected.clear()
        dateError=null;error=null
        customDates=false
        rows=activityRows
        done="Review verified cash movements below. They will remain unclassified until you confirm them in Activity."
       } catch(e:Exception) {
        error=e.message ?: "These PDF rows could not be prepared for Activity review."
       }
      },
      modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)
     ) { Text("Review cash movements in Activity") }
     Text(
      "Only selected cash movements go to Activity. They remain unclassified for review; informational rows stay in the statement archive.",
      style=MaterialTheme.typography.bodySmall,
      color=MaterialTheme.colorScheme.onSurfaceVariant
     )
    }
   }
  }}
  stagedPdf?.let { ready ->
   items(ready.lines.size,key={it}){i->
    val record=ready.lines[i]
    PesaCard {
     Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
      Text(record.description,style=MaterialTheme.typography.bodyMedium)
      Text("Page ${record.page}, row ${record.row} · ${record.reference}",
       style=MaterialTheme.typography.bodySmall)
      Text(if(record.informational)"Informational only — no M-Pesa movement"
       else if(record.movementMinor>0)"Paid In ${toolMoney(record.movementMinor)}"
       else "Withdrawn ${toolMoney(-record.movementMinor)}",
       style=MaterialTheme.typography.bodySmall)
      Text("Printed balance ${toolMoney(record.balanceMinor)}",style=MaterialTheme.typography.bodySmall)
     }
    }
   }
  }
  stagedBankPdf?.let { ready ->
   item { PesaCard {
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
     Text("Verified bank statement",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     Text("${ready.parsed.document.pages} PDF pages · ${ready.admitted.sourceRows} verified cash movements")
     Text("Money in ${toolMoney(ready.admitted.moneyInMinor)} · Money out ${toolMoney(ready.admitted.moneyOutMinor)}")
     Text("${statementDate(ready.admitted.legs.minOf{it.occurredAt})} – ${statementDate(ready.admitted.legs.maxOf{it.occurredAt})}",
      style=MaterialTheme.typography.bodySmall)
     Text("The account identity, opening balance, every running balance and printed totals were checked locally. Money out is not automatically spending; unknown categories remain unknown.",
      style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     Row(verticalAlignment=Alignment.CenterVertically) {
      Checkbox(checked=statementPdfApproved,onCheckedChange={statementPdfApproved=it},enabled=!busy)
      Text("I reviewed the source rows. Save only to Statements, never to Activity.",
       Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
     }
     Button(enabled=!busy&&statementPdfApproved,onClick={
      importing=true;error=null
      scope.launch {
       try {
        val saved=withContext(Dispatchers.IO) {
         StatementBankArchiveStore(c).save(ready.admitted,ready.parsed.tables)
        }
        done=if(saved.added)"Saved this verified bank statement. Activity was not changed."
          else "This statement is already saved; no duplicate was created."
        stagedBankPdf=null;statementPdfApproved=false
        onChanged();onDismiss()
        if(!statementOnly) onOpenStatements?.invoke()
       }catch(e:CancellationException){throw e}
       catch(e:Exception){error=e.message ?: "Could not save this bank statement."}
       finally{importing=false}
      }
     },modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text("Save to Statements")}
    }
   }}
   items(ready.admitted.legs.size,key={it}){i->
    val row=ready.admitted.legs[i]
    PesaCard {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
     Text(row.description,style=MaterialTheme.typography.bodyMedium)
     Text("${statementDate(row.occurredAt)} · Page ${row.pageNumber}, row ${row.rowNumber}",style=MaterialTheme.typography.bodySmall)
     Text(if(row.movementMinor>=0)"Money in ${toolMoney(row.movementMinor)}"
      else "Money out ${toolMoney(-row.movementMinor)}",style=MaterialTheme.typography.bodySmall)
     Text("Printed balance ${toolMoney(row.runningBalanceMinor)}",style=MaterialTheme.typography.bodySmall)
    }}
   }
  }
  if(previewSource==selection)dateResult?.let {dates ->item {
   Text("${dates.rows.size} of ${rows.size} entries in selected dates · ${dates.excludedCount} outside",style=MaterialTheme.typography.titleSmall)
   Text("Entries in this file: ${dates.availableFromDay} to ${dates.availableToDay}.",style=MaterialTheme.typography.bodySmall)
   if(dates.rows.isEmpty()) Text("No transactions in the selected date range. Choose different dates or another statement.",color=MaterialTheme.colorScheme.error)
   selection.range?.let {range->if(dates.availableFromDay!=null && dates.availableToDay!=null && (range.fromDay<dates.availableFromDay || range.toDay>dates.availableToDay))
    Text("Some selected dates fall outside the dates of entries in this file. Only available entries can be imported.",style=MaterialTheme.typography.bodySmall)}
  }}
  if(previewSource==selection && preview.isNotEmpty()){
   item {PesaCard {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text("04 · Review transactions",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("${preview.count{!it.duplicate&&!it.possibleMatch}} new · ${preview.count{!it.duplicate&&it.possibleMatch}} possible matches · ${preview.count{it.duplicate}} already recorded",style=MaterialTheme.typography.bodyMedium);Text("Only selected, validated transactions will be added.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
   item {Button(onClick={
    val source=selection;val reviewed=previewSource
    val chosen=preview.mapIndexedNotNull {i,p->if(selected[i]==true&&!p.duplicate)p.row else null}
    importing=true;error=null
    scope.launch{try{
      val expectedAccountType=activityRowsRequireAccountType
        ?:accounts.firstOrNull{it.id==source.accountId}?.type
       ?:error("Choose a confirmed account and review the statement again.")
     val count=withContext(Dispatchers.IO){
      store.importRows(source.accountId,StatementImportDates.requireReviewed(source,reviewed,chosen),expectedAccountType)
     }
      onChanged();done="Added $count entries. Confirm them in Activity.";rows=emptyList();activityRowsRequireAccountType=null;previewSource=null;dateResult=null;preview=emptyList();selected.clear()
    }catch(e:CancellationException){throw e}catch(e:Exception){error=e.message ?: "Could not finish import. Check Activity before retrying."}finally{importing=false}}
     },enabled=!busy&&dateError==null&&previewSource==selection&&selected.values.any{it}&&
      (activityRowsRequireAccountType==null||accounts.any{it.id==selection.accountId&&it.type==activityRowsRequireAccountType}),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Import ${selected.values.count{it}} entries")}}
   items(preview.size,key={it}){i->
    val p=preview[i]
    PesaCard {
     Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){
      Checkbox(checked=selected[i]==true,onCheckedChange={selected[i]=it},enabled=!busy&&!p.duplicate)
      Column(Modifier.weight(1f)){
       Text(p.row.description,style=MaterialTheme.typography.titleSmall)
       Text(StatementImportDates.day(p.row.date,zoneId)+(if(p.duplicate)" · Already recorded"else if(p.possibleMatch)" · Possible match"else""),style=MaterialTheme.typography.bodySmall)
      }
      Text(toolMoney(p.row.signedMinor),style=MaterialTheme.typography.bodySmall)
     }
    }
   }
  }
 }
 sourcePdfUri?.let { uri ->
  AlertDialog(
   onDismissRequest={sourcePdfUri=null;sourcePdfPassword=""},
   properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn),
   title={Text(if(sourcePdfProviderKey=="SAFARICOM_MPESA") "Open M-Pesa statement privately" else "Open bank statement privately")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("The PDF and password remain on your phone. Enter its PDF-opening password, if required.")
    OutlinedTextField(value=sourcePdfPassword,onValueChange={sourcePdfPassword=it.take(256)},
     label={Text("PDF password (optional)")},
     visualTransformation=PasswordVisualTransformation(),singleLine=true,
     modifier=Modifier.fillMaxWidth())
   }},
   dismissButton={TextButton(onClick={sourcePdfUri=null;sourcePdfPassword=""}){Text("Cancel")}},
   confirmButton={TextButton(onClick={
    val secret=sourcePdfPassword
    sourcePdfPassword="";sourcePdfUri=null;invalidatePreview();reading=true
    scope.launch{
     try{
      if(sourcePdfProviderKey=="SAFARICOM_MPESA") {
       stagedPdf=withContext(Dispatchers.IO){OfflineMpesaPdfReader.inspect(c,uri,secret)}
       done="The full PDF passed physical extraction, official summary and provisional balance checks. Review all rows before saving."
      } else {
       val providerKey=sourcePdfProviderKey
       val selectedAccount=accounts.firstOrNull{it.id==accountId}
        ?:error("Choose the account first.")
       val prepared=withContext(Dispatchers.IO){OfflineBankTableReader.inspect(c,uri,secret,providerKey)}
       val confirmed=withContext(Dispatchers.IO){
        FinancialUnderstandingStore.get(c).confirmedAccountIds()+
          AccountOwnershipConsentStore(c).confirmedRevisions().keys
       }
       val admission=StatementBankAdmission.verify(
        StatementSourceProfileRegistry.forProvider(providerKey),selectedAccount,
        confirmed,providerKey,prepared.document,prepared.tables
       )
       val accepted=admission as? StatementAdmissionResult.Accepted
        ?:error(BankStatementAccountIdentityUiPolicy.safeBlockedMessage((admission as StatementAdmissionResult.Blocked).reason))
       stagedBankPdf=StagedBankPdf(prepared,accepted)
       done="This bank statement passed full local extraction, account, balance and totals checks. Review its rows before saving."
      }
     }catch(e:CancellationException){throw e}
     catch(e:Exception){error=e.message ?: "The PDF did not pass verification. No statement records were saved."}
     finally{reading=false}
    }
   }){Text("Verify locally")}}
  )
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
