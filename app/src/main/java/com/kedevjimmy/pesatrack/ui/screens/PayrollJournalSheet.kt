package com.kedevjimmy.pesatrack.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.PayrollJournalStore

import com.kedevjimmy.pesatrack.documents.PdfTextReviewReader
import com.kedevjimmy.pesatrack.domain.payroll.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class PayrollLineInput(val key: String=UUID.randomUUID().toString(),val label: String="",
    val amount: String="",val type: PayrollLineType=PayrollLineType.OTHER,
    val role: PayrollLineRole=PayrollLineRole.EMPLOYEE_DEDUCTION)

@Composable internal fun PayrollJournalSheet(onDismiss: ()->Unit, asPage: Boolean = false,
    scaffoldPadding: PaddingValues = PaddingValues()) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val store=remember {PayrollJournalStore(context)}
    var records by remember {mutableStateOf<List<PayrollRecord>>(emptyList())}
    var editing by remember {mutableStateOf<PayrollRecord?>(null)}
    var deleting by remember {mutableStateOf<PayrollRecord?>(null)}
    var employer by remember {mutableStateOf("")};var period by remember {mutableStateOf("")}
    var gross by remember {mutableStateOf("")};var net by remember {mutableStateOf("")}
    var source by remember {mutableStateOf(PayrollSource.MANUAL)}
    var lines by remember {mutableStateOf<List<PayrollLineInput>>(emptyList())}
    var suggestions by remember {mutableStateOf<PayslipSuggestions?>(null)}
    var reviewForm by remember {mutableStateOf(false)}
    var reviewed by remember {mutableStateOf(false)};var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)};var message by remember {mutableStateOf<String?>(null)}
    fun clear() {editing=null;employer="";period="";gross="";net="";lines=emptyList();source=PayrollSource.MANUAL;reviewed=false;suggestions=null;reviewForm=false}
    fun edit(record: PayrollRecord) {
        editing=record;employer=record.draft.employer;period=record.draft.period
        gross=PayrollJournalPolicy.money(record.draft.grossMinor);net=PayrollJournalPolicy.money(record.draft.netMinor)
        source=record.draft.source;lines=record.draft.lines.map {PayrollLineInput(label=it.label,amount=PayrollJournalPolicy.money(it.amountMinor),type=it.type,role=it.role)}
        reviewed=false;suggestions=null;reviewForm=true;error=null;message=null
    }
    suspend fun refresh() {records=withContext(Dispatchers.IO) {store.records(200)}}
    fun perform(action: suspend ()->Unit) {
        if(busy)return
        busy=true;error=null;message=null
        scope.launch {try {action()} catch(e: CancellationException) {throw e}
            catch(e: IllegalArgumentException) {error=e.message ?: "Check the payroll fields."}
            catch(_: Exception) {error="Could not update payroll details. No cash receipt was changed."}
            finally {busy=false}}
    }
    LaunchedEffect(Unit) {busy=true;try {refresh()} catch(e:CancellationException){throw e}
        catch(_:Exception){error="Could not load payroll details. Your cash history is unchanged."} finally {busy=false}}
    fun applySuggestions(s: PayslipSuggestions) {
        // Only reviewed financial fields can enter the draft. Never accept employee IDs or raw text.
        s.employer?.let {employer=it}
        s.period?.let {period=it}
        gross=s.grossMinor?.let {PayrollJournalPolicy.money(it)} ?: ""
        net=s.netMinor?.let {PayrollJournalPolicy.money(it)} ?: ""
        lines=s.lines.map {PayrollLineInput(label=it.label,amount=PayrollJournalPolicy.money(it.amountMinor),type=it.type,role=it.role)}
        source=PayrollSource.PDF_TEXT;reviewed=false;reviewForm=true
    }
    val pdfImport=rememberPdfImport(
        onSelected={
            suggestions=null;error=null;message=null
            if(editing==null){employer="";period="";gross="";net="";lines=emptyList();reviewForm=false;reviewed=false}
        },
        onBusy={busy=it},
        onResult={pdf->
            val extracted=PayslipFieldReview.read(pdf.pages)
            suggestions=extracted
            if(editing==null)applySuggestions(extracted)
        }
    )
    PdfPasswordDialog(pdfImport)
    val details: LazyListScope.() -> Unit = {
        item {
            Text(if(editing==null) "New payroll record" else "Edit payroll record",style=MaterialTheme.typography.titleMedium)
            Text("Payroll facts only: saving does not record money received.")
            Text("Only employer, period and reviewed monetary payroll fields are saved. Staff number, name, email and address are not collected.",style=MaterialTheme.typography.bodySmall)
            if(editing!=null) TextButton(enabled=!busy,onClick={clear()}) {Text("New record instead")}
        }
        item {
            PdfImportButton("Import payslip PDF",!busy,pdfImport)
            Text(if(Build.VERSION.SDK_INT>=35) "Read locally, then review only relevant payroll fields. Up to 3 MB/40 pages; scanned images require OCR. No raw PDF text, password or unrelated staff information is saved." else "PDF text needs Android 15 or newer. Enter the payroll fields manually on this phone.",style=MaterialTheme.typography.bodySmall)
        }
        suggestions?.let {s ->item {
            s.warnings.forEach {Text(it,style=MaterialTheme.typography.bodySmall)}
            Text("Extracted payroll preview · No personal identifiers shown",style=MaterialTheme.typography.titleMedium)
            Text("Employer: ${s.employer ?: "Needs confirmation"} · month: ${s.period ?: "Needs confirmation"}")
            Text("Gross: ${s.grossMinor?.let {"KES "+PayrollJournalPolicy.money(it)} ?: "Needs review"}${s.grossSource?.let {" · from "+it} ?: ""}")
            Text("Net payable: ${s.netMinor?.let {"KES "+PayrollJournalPolicy.money(it)} ?: "Needs review"}")
            Text("${s.lines.size} identified deduction/contribution lines")
            s.reportedDeductionsMinor?.let {Text("Payslip total deductions: KES ${PayrollJournalPolicy.money(it)}")}
            val deductionSum=runCatching {s.lines.filter {it.role==PayrollLineRole.EMPLOYEE_DEDUCTION}
                .fold(0L){sum,line->Math.addExact(sum,line.amountMinor)}}.getOrNull()
            if(s.grossMinor!=null && s.netMinor!=null && deductionSum!=null && s.grossMinor>=s.netMinor){
                val remainder=s.grossMinor-s.netMinor-deductionSum
                if(remainder!=0L)Text("Unexplained gross-to-net difference: KES ${PayrollJournalPolicy.money(remainder)}. Identify missing payroll lines before saving.",
                    color=MaterialTheme.colorScheme.error)
            }
            s.earnings.forEach {earning->Text("${earning.label}: KES ${PayrollJournalPolicy.money(earning.amountMinor)}",style=MaterialTheme.typography.bodySmall)}
            Text("Earnings breakdown is shown for checking, but this version stores only gross/net and deduction/contribution lines.",style=MaterialTheme.typography.bodySmall)
            if(editing!=null) OutlinedButton(enabled=!busy,onClick={applySuggestions(s)}) {
                Text("Apply imported fields to this record")
            }
            TextButton(enabled=!busy,onClick={suggestions=null; if(editing==null)clear()}) {Text("Discard extracted preview")}
        } }
        if(!reviewForm) item {TextButton(onClick={reviewForm=true},enabled=!busy) {Text("Enter paycheck manually")}}
        if(reviewForm) {
        item {Text("Review and correct",style=MaterialTheme.typography.titleMedium)}
        item {OutlinedTextField(employer,{employer=it;reviewed=false},label={Text("Employer")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("payroll-employer"))}
        item {OutlinedTextField(period,{period=it;reviewed=false},label={Text("Payroll month · yyyy-MM")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("payroll-month"))}
        item {OutlinedTextField(gross,{gross=it;reviewed=false},label={Text("Gross earnings · KES")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("payroll-gross"))}
        item {OutlinedTextField(net,{net=it;reviewed=false},label={Text("Net payable · KES")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("payroll-net"))}
        item {
            Text("Employee deductions reduce net payable. Employer contributions are separate.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled=!busy&&lines.size<PayrollJournalPolicy.MAX_LINES,onClick={lines=lines+PayrollLineInput();reviewed=false}) {Text("Add deduction / contribution")}
        }
        items(lines,key={it.key}) {input ->
            fun change(updated: PayrollLineInput) {lines=lines.map {if(it.key==input.key) updated else it};reviewed=false}
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input.label,{change(input.copy(label=it))},label={Text("Original source label")},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())
                ChoiceField("Purpose",input.type.title,PayrollLineType.entries.map {it.name to it.title},!busy) {change(input.copy(type=PayrollLineType.valueOf(it)))}
                ChoiceField("Who contributes?",input.role.title,PayrollLineRole.entries.map {it.name to it.title},!busy) {change(input.copy(role=PayrollLineRole.valueOf(it)))}
                OutlinedTextField(input.amount,{change(input.copy(amount=it))},label={Text("Line amount · KES")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),enabled=!busy,modifier=Modifier.fillMaxWidth())
                TextButton(enabled=!busy,onClick={lines=lines.filterNot {it.key==input.key};reviewed=false}) {Text("Remove line")}
                HorizontalDivider()
            }
        }
        item {
            Row {Checkbox(checked=reviewed,onCheckedChange={reviewed=it},enabled=!busy,modifier=Modifier.testTag("payroll-reviewed"));Text("I checked the employer, month, currency, amounts and every line against my payroll information.",Modifier.weight(1f))}
            Button(enabled=!busy&&reviewed,onClick={perform {
                val draft=PayrollDraft(employer,period,requireNotNull(PayrollJournalPolicy.parseAmount(gross)){"Enter valid gross earnings."},
                    requireNotNull(PayrollJournalPolicy.parseAmount(net)){"Enter valid net payable."},source,lines.map {
                        PayrollLine(it.label,it.type,it.role,requireNotNull(PayrollJournalPolicy.parseAmount(it.amount)){"Enter valid line amounts."})
                    })
                val current=editing
                withContext(Dispatchers.IO) {store.save(draft,reviewed,current)}
                clear();refresh();message="Payroll details saved. No cash receipt or balance was added."
            }},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("payroll-save")) {Text("Save payroll details")}
            ToolError(error);message?.let {Text(it)};if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        } // end reviewable payroll form
        item {Text("Saved payroll · up to 200 recent records",style=MaterialTheme.typography.titleMedium)
            Text("Reported figures only. Actual salary receipts appear separately in Salary and Activity. These amounts are not added to cash totals.",style=MaterialTheme.typography.bodySmall)}
        items(records,key={it.id}) {record ->
            val summary=PayrollJournalPolicy.validate(record.draft)
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("${record.draft.employer} · ${record.draft.period}",style=MaterialTheme.typography.titleSmall)
                Text("Reported gross: KES ${PayrollJournalPolicy.money(record.draft.grossMinor)}")
                Text("Employee deductions: KES ${PayrollJournalPolicy.money(summary.employeeDeductionMinor)}")
                Text("Reported net payable: KES ${PayrollJournalPolicy.money(summary.netPayableMinor)}")
                Text("Employer contributions: KES ${PayrollJournalPolicy.money(summary.employerContributionMinor)}")
                summary.employeeByType.forEach {(type,amount)->Text("${type.title}: KES ${PayrollJournalPolicy.money(amount)}",style=MaterialTheme.typography.bodySmall)}
                Text(if(record.draft.source==PayrollSource.MANUAL) "User-entered payroll" else "User-reviewed PDF fields",style=MaterialTheme.typography.bodySmall)
                Text("Contributions were reported, not verified as remitted or held in an account.",style=MaterialTheme.typography.bodySmall)
                Row {TextButton(enabled=!busy,onClick={edit(record)}) {Text("Edit")};TextButton(enabled=!busy,onClick={deleting=record}) {Text("Delete")}}
            } }
        }
    }
    if (asPage) {
        LazyColumn(
            modifier=Modifier.fillMaxSize().padding(scaffoldPadding).testTag("paycheck-analysis-page"),
            contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Column {
                        Text("Paycheck Analysis",style=MaterialTheme.typography.headlineMedium)
                        Text("Your payslip figures, privately reviewed",style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick=onDismiss,enabled=!busy) {Text("Back")}
                }
            }
            if(records.isNotEmpty()) item {
                val recent=records.first()
                val totalGross=runCatching {records.fold(0L) {amount,row->Math.addExact(amount,row.draft.grossMinor)}}.getOrNull()
                val totalNet=runCatching {records.fold(0L) {amount,row->Math.addExact(amount,row.draft.netMinor)}}.getOrNull()
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Latest paycheck · ${recent.draft.period}",style=MaterialTheme.typography.titleMedium)
                        Text("Gross: KES ${PayrollJournalPolicy.money(recent.draft.grossMinor)}")
                        Text("Net payable: KES ${PayrollJournalPolicy.money(recent.draft.netMinor)}")
                        Text("Saved payroll totals (${records.size}): gross ${totalGross?.let { "KES "+PayrollJournalPolicy.money(it) } ?: "Unavailable"}, net ${totalNet?.let { "KES "+PayrollJournalPolicy.money(it) } ?: "Unavailable"}",style=MaterialTheme.typography.bodySmall)
                        val sacco=runCatching {records.fold(0L) {amount,row->
                            val deductions=row.draft.lines.filter {
                                it.type in setOf(PayrollLineType.SACCO_SHARES,PayrollLineType.SACCO_DEPOSIT)
                            }.fold(0L) {total,line->Math.addExact(total,line.amountMinor)}
                            Math.addExact(amount,deductions)
                        }}.getOrNull()
                        Text("Reported SACCO deductions: ${sacco?.let { "KES "+PayrollJournalPolicy.money(it) } ?: "Unavailable"}",style=MaterialTheme.typography.bodySmall)
                        Text("Contributions are reported, not verified SACCO deposits or available balances.",style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            details()
        }
    } else MoneyToolSheet("Payroll details","Record employer-reported earnings, deductions and contributions separately from money received. KES only. A payslip does not prove a bank deposit, remittance, savings balance or loan balance. One record per employer/month; edit existing records for corrections. Receipt matching and multiple-payslip aggregation are not available here.",busy,onDismiss,details)
    deleting?.let {record ->AlertDialog(onDismissRequest={if(!busy)deleting=null},title={Text("Delete payroll details?")},
        text={Text("Only this employer/month payroll record is removed. Salary receipts and financial history stay unchanged.")},
        dismissButton={TextButton(enabled=!busy,onClick={deleting=null}) {Text("Cancel")}},
        confirmButton={TextButton(enabled=!busy,onClick={perform {
            withContext(Dispatchers.IO) {store.remove(record)}
            if(editing?.id==record.id)clear();deleting=null;refresh();message="Payroll details deleted. Cash history is unchanged."
        }}) {Text("Delete")}})}
}
