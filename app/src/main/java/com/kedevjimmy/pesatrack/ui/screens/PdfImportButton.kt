package com.kedevjimmy.pesatrack.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.kedevjimmy.pesatrack.documents.PdfTextReviewReader
import com.kedevjimmy.pesatrack.domain.evidence.*
import kotlinx.coroutines.*

/** Owned by the sheet, never by a disposable scrolling item. Nothing here is saveable. */
internal class PdfImportState {
    var pending by mutableStateOf<Uri?>(null)
    var password by mutableStateOf("")
    var reading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var retry by mutableStateOf(false)
    var job: Job?=null
    var choose: ()->Unit={}
    var read: (Uri,String?)->Unit={_,_->}
    fun cancel() {pending=null;password="";retry=false;error=null;job?.cancel()}
}

@Composable internal fun rememberPdfImport(onSelected: ()->Unit,onBusy: (Boolean)->Unit,onResult: (PdfTextPreview)->Unit): PdfImportState {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val state=remember {PdfImportState()}
    val selected by rememberUpdatedState(onSelected);val busy by rememberUpdatedState(onBusy);val result by rememberUpdatedState(onResult)
    // Android's document picker can stop the host Activity temporarily.
    // Cancelling on ON_STOP dropped the selected document and hid the result.
    // Only dispose the job when the owning screen leaves composition.
    DisposableEffect(state) { onDispose { state.cancel() } }
    state.read={uri,secret->
        if(!state.reading) {
            state.reading=true;busy(true);state.error=null;state.pending=null;state.password=""
            state.job=scope.launch {
                try {val pdf=PdfTextReviewReader.read(context,uri,secret);state.retry=false;result(pdf)}
                catch(e: CancellationException){throw e}
                catch(e: PdfReadException) {
                    state.error=e.failure.explanation
                    if(e.failure==PdfFailure.PASSWORD){state.pending=uri;state.retry=secret!=null}
                }
                catch(_: Exception){state.error=PdfFailure.PARSE.explanation}
                finally {state.reading=false;busy(false)}
            }
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null&&!state.reading){selected();state.retry=false;state.read(uri,null)}
    }
    state.choose={state.error=null;picker.launch(arrayOf("application/pdf"))}
    return state
}

@Composable internal fun PdfImportButton(label: String,enabled: Boolean,state: PdfImportState) {
    OutlinedButton(enabled=enabled&&!state.reading&&state.pending==null&&Build.VERSION.SDK_INT>=35,
        onClick=state.choose,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(label)}
    state.error?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
}

/** Masked password dialog also lives outside the LazyColumn. */
@Composable internal fun PdfPasswordDialog(state: PdfImportState) {
    state.pending?.let {uri->AlertDialog(
        onDismissRequest=state::cancel,
        properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn),
        title={Text("Unlock PDF")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(state.retry) "The password did not unlock this PDF, or its encryption is unsupported. Check the password and try again." else "Enter the password supplied with this document. Unlocking happens on this phone.")
            OutlinedTextField(value=state.password,onValueChange={state.password=it.take(256)},label={Text("Document password")},
                singleLine=true,visualTransformation=PasswordVisualTransformation(),
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false))
            Text("The password is not saved. Cancelling discards it.",style=MaterialTheme.typography.bodySmall)
        }},
        dismissButton={TextButton(onClick=state::cancel){Text("Cancel")}},
        confirmButton={TextButton(enabled=state.password.isNotEmpty(),onClick={val secret=state.password;state.password="";state.read(uri,secret)}){Text("Unlock and extract")}}
    )}
}
