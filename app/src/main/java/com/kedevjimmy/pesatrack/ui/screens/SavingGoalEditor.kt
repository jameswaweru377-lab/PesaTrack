package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kedevjimmy.pesatrack.data.local.*
import com.kedevjimmy.pesatrack.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SavingGoalEditor(rule:SmartRule?,onDismiss:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var name by remember {mutableStateOf(rule?.displayLabel ?: rule?.name.orEmpty())}
    var payee by remember {mutableStateOf(rule?.counterpartyExact?.ifBlank {rule.counterpartyContains}.orEmpty())}
    var target by remember {mutableStateOf(rule?.plannedAmountMinor?.let {java.math.BigDecimal.valueOf(it,2).toPlainString()}.orEmpty())}
    var error by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    var remove by remember {mutableStateOf(false)}
    fun save(delete:Boolean=false) {scope.launch {
        busy=true
        try {withContext(Dispatchers.IO) {
            if(delete&&rule!=null) RuleStore.get(context).setEnabled(rule.id,false)
            else {
                require(name.isNotBlank() && payee.isNotBlank()) {"Add a goal name and the payee shown in its savings receipts."}
                val minor=requireNotNull(parseSalaryAmount(target)) {"Enter a positive monthly target."}
                RuleStore.get(context).save(name=name.trim(),counterpartyContains="",counterpartyExact=payee.trim(),category=TransactionCategory.SAVINGS,direction=MoneyDirection.TRANSFER,displayLabel=name.trim(),preset=RulePreset.SAVINGS,plannedAmountMinor=minor,accountId=rule?.accountId,linkedAccountId=rule?.linkedAccountId,id=rule?.id ?: java.util.UUID.randomUUID().toString())
            }
            PayCycleStore.get(context).rebuild();LedgerUpdateBus.notifyChanged()
        };onDismiss()}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(failure:Exception){error=failure.message ?: "Couldn't save. Please try again."}finally{busy=false}
    }}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text(if(remove)"Remove this goal?" else if(rule==null)"New saving goal" else "Edit saving goal")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(remove) Text("This stops the savings target and its matching rule. Existing transactions and balances stay unchanged.")
        else {
            OutlinedTextField(name,{name=it},label={Text("Goal name")},singleLine=true)
            OutlinedTextField(target,{target=it},label={Text("Monthly target (KES)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            OutlinedTextField(payee,{payee=it},label={Text("Savings payee on receipts")},singleLine=true)
            Text("Only trusted transfers matched to this goal count toward its monthly progress. This does not move money.",style=MaterialTheme.typography.bodySmall)
            if(rule!=null)TextButton(enabled=!busy,onClick={remove=true}){Text("Remove goal")}
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!busy,onClick={save(remove)}){Text(if(remove)"Remove" else "Save goal")}},dismissButton={TextButton(enabled=!busy,onClick={if(remove)remove=false else onDismiss()}){Text("Cancel")}})
}
