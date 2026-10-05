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
internal fun SavingGoalEditor(rule:SmartRule?,bill:Boolean=false,onDismiss:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val noun = if (bill) "bill" else "goal"
    var name by remember {mutableStateOf(rule?.displayLabel ?: rule?.name.orEmpty())}
    var payee by remember {mutableStateOf(rule?.counterpartyExact?.ifBlank {rule.counterpartyContains}.orEmpty())}
    var target by remember {mutableStateOf(rule?.plannedAmountMinor?.let {java.math.BigDecimal.valueOf(it,2).toPlainString()}.orEmpty())}
    var error by remember {mutableStateOf<String?>(null)}
    var busy by remember {mutableStateOf(false)}
    var remove by remember {mutableStateOf(false)}
    fun save(delete:Boolean=false) {scope.launch {
        busy=true
        try {withContext(Dispatchers.IO) {
            if(delete&&rule!=null) PlanningRuleRepository(context).disable(rule.id)
            else {
                require(name.isNotBlank() && payee.isNotBlank()) {"Add a name and the payee shown in its receipts."}
                val minor=requireNotNull(parseSalaryAmount(target)) {"Enter a positive monthly target."}
                PlanningRuleRepository(context).save(name,payee,minor,bill,rule)
            }
        };onDismiss()}catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}catch(failure:Exception){error=failure.message ?: "Couldn't save. Please try again."}finally{busy=false}
    }}
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},title={Text(if(remove)"Remove this $noun?" else if(rule==null)if (bill) "New bill" else "New saving goal" else "Edit $noun")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(remove) Text("This stops the $noun and its matching rule. Existing transactions and balances stay unchanged.")
        else {
            OutlinedTextField(name,{name=it},label={Text(if (bill) "Bill name" else "Goal name")},singleLine=true)
            OutlinedTextField(target,{target=it},label={Text(if (bill) "Monthly amount (KES)" else "Monthly target (KES)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
            OutlinedTextField(payee,{payee=it},label={Text(if (bill) "Payee on receipts" else "Savings payee on receipts")},singleLine=true)
            Text(if (bill) "Matching payment receipts reduce this monthly bill automatically. This does not make payments." else "Only trusted transfers matched to this goal count toward its monthly progress. This does not move money.",style=MaterialTheme.typography.bodySmall)
            if(rule!=null)TextButton(enabled=!busy,onClick={remove=true}){Text("Remove $noun")}
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!busy,onClick={save(remove)}){Text(if(remove)"Remove" else "Save $noun")}},dismissButton={TextButton(enabled=!busy,onClick={if(remove)remove=false else onDismiss()}){Text("Cancel")}})
}
