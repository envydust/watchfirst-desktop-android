package com.nuvio.app.features.access

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_logo_wordmark
import org.jetbrains.compose.resources.painterResource

private val Ground = Color(0xFF080C10)
private val Field = Color(0xFF0B1117)
private val Line = Color(0xFF25313B)
private val Green = Color(0xFF39FF14)
private val GreenSoft = Color(0xFFB8F5A8)
private val Ink = Color(0xFF071108)
private val TextMain = Color(0xFFF2F6F4)
private val Muted = Color(0xFF9BAAB7)
private val Dim = Color(0xFF71808C)
private val ErrorText = Color(0xFFFF9AA9)
private val Amber = Color(0xFFFFC857)

private val EMAIL = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

/**
 * WATCHFIRST entry for the mobile app: name and e-mail, then the code from the member site.
 * Same two steps as the Windows app; on success the app goes straight to creating the profile.
 */
@Composable
fun WatchfirstAccessScreen(modifier: Modifier = Modifier) {
    val state by WatchfirstAccess.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val uri = LocalUriHandler.current

    var name by rememberSaveable { mutableStateOf(WatchfirstAccess.savedName) }
    var email by rememberSaveable { mutableStateOf(WatchfirstAccess.savedEmail) }
    var code by rememberSaveable { mutableStateOf("") }
    var codeStep by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(state.notice) { if (!state.notice.isNullOrBlank()) error = state.notice.orEmpty() }

    val identityValid = name.isNotBlank() && EMAIL.matches(email.trim())
    val codeValid = code.count { it.isLetterOrDigit() } >= 8

    fun continueToCode() {
        if (!identityValid) return
        name = name.trim()
        email = email.trim().lowercase()
        error = ""
        codeStep = true
    }

    fun submitCode() {
        if (!codeValid || busy) return
        focus.clearFocus(force = true)
        busy = true
        error = ""
        scope.launch {
            try {
                WatchfirstAccess.connect(name, email, code)
                code = ""
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                error = failure.message ?: "Não foi possível conectar. Tente novamente."
            } finally {
                busy = false
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextMain,
        unfocusedTextColor = TextMain,
        disabledTextColor = Muted,
        focusedContainerColor = Field,
        unfocusedContainerColor = Field,
        disabledContainerColor = Field,
        focusedBorderColor = Green,
        unfocusedBorderColor = Line,
        disabledBorderColor = Line,
        focusedLabelColor = GreenSoft,
        unfocusedLabelColor = Muted,
        cursorColor = Green,
        focusedPlaceholderColor = Dim,
        unfocusedPlaceholderColor = Dim,
    )
    val primaryColors = ButtonDefaults.buttonColors(
        containerColor = Green,
        contentColor = Ink,
        disabledContainerColor = Green.copy(alpha = 0.32f),
        disabledContentColor = Ink.copy(alpha = 0.7f),
    )
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
            Image(
                painter = painterResource(Res.drawable.app_logo_wordmark),
                contentDescription = "WATCHFIRST",
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier.height(40.dp),
            )
            Spacer(Modifier.height(44.dp))

            Text(
                text = if (codeStep) "Digite o código do site" else "Sua próxima sessão começa aqui.",
                color = TextMain,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (codeStep) {
                    "No site do seu plano, toque em \"Gerar código para conectar\" e digite o código aqui."
                } else {
                    "Informe seu nome e e-mail. Depois, você digita o código gerado no site do seu plano e cria o seu perfil."
                },
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(28.dp))

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = if (state.notice != null && error == state.notice) Amber else ErrorText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x14FF6B7A), shape)
                        .border(BorderStroke(1.dp, Color(0x33FF6B7A)), shape)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            if (!codeStep) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.filterNot(Char::isISOControl).take(100); error = "" },
                    label = { Text("Seu nome") },
                    singleLine = true,
                    shape = shape,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(254); error = "" },
                    label = { Text("Seu e-mail") },
                    singleLine = true,
                    shape = shape,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { continueToCode() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { continueToCode() },
                    enabled = identityValid,
                    shape = shape,
                    colors = primaryColors,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Line), shape)
                        .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(email, color = Muted, fontSize = 13.sp)
                    }
                    TextButton(onClick = { codeStep = false; error = "" }, enabled = !busy) {
                        Text("Alterar", color = GreenSoft)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { uri.openUri(WATCHFIRST_BASIC_SITE) },
                        shape = shape,
                        border = BorderStroke(1.dp, Line),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) { Text("Abrir site Básico", color = TextMain, fontSize = 14.sp) }
                    OutlinedButton(
                        onClick = { uri.openUri(WATCHFIRST_MAX_SITE) },
                        shape = shape,
                        border = BorderStroke(1.dp, Line),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) { Text("Abrir site MAX", color = TextMain, fontSize = 14.sp) }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(51); error = "" },
                    label = { Text("Código") },
                    placeholder = { Text("WF-XXXX-XXXX") },
                    singleLine = true,
                    enabled = !busy,
                    shape = shape,
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitCode() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { submitCode() },
                    enabled = codeValid && !busy,
                    shape = shape,
                    colors = primaryColors,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (busy) "Conectando…" else "Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "O código vale 10 minutos e funciona uma vez. Ele libera somente este aparelho.",
                    color = Dim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "BÁSICO · filmes, séries e animes\nMAX · tudo do Básico + conteúdo exclusivo e área ao vivo",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "O acesso exige conexão com a internet.",
                color = Dim,
                fontSize = 12.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}
