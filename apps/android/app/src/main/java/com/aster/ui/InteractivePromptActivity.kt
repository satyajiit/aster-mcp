package com.aster.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aster.service.overlay.InteractiveOverlayController
import com.aster.service.overlay.InteractiveOverlayModel
import com.aster.service.overlay.InteractiveOverlayModel.InteractivePrompt
import com.aster.ui.theme.AsterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * App Automations /goal R-C — the no-draw-overlay fallback surface for the
 * interactive prompt. The [InteractiveOverlayController] launches this when
 * `Settings.canDrawOverlays` is false; it renders the same
 * [InteractiveOverlayModel.InteractivePrompt] held by the controller and bridges
 * the owner's choice back via [InteractiveOverlayController.deliver] (the
 * controller's parked round-trip then tears this down). Mirrors
 * [IpcApprovalActivity]'s transparent-activity pattern, but the result rides the
 * singleton, not `setResult` (the caller is a Binder thread, not an Activity).
 */
@AndroidEntryPoint
class InteractivePromptActivity : ComponentActivity() {

    @Inject
    lateinit var controller: InteractiveOverlayController

    /** The epoch of the prompt this Activity rendered (review F4/F6). */
    private var promptEpoch: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        promptEpoch = intent.getLongExtra(InteractiveOverlayController.EXTRA_EPOCH, -1)
        val prompt = controller.pendingPrompt
        if (prompt == null || promptEpoch != controller.liveEpoch) {
            // The prompt resolved (timeout/kill) before we could show, or a newer
            // prompt has superseded this one — this Activity is stale.
            finish()
            return
        }
        // Let the controller's teardown finish us (single finish path). If the
        // prompt already resolved or this is a stale epoch, this finishes now.
        controller.registerActivityFinisher(promptEpoch) { finish() }

        setContent {
            AsterTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    BackHandler { controller.deliver(prompt.cancelResult()) }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = CardBg,
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .widthIn(max = 460.dp)
                            .padding(16.dp),
                    ) {
                        when (prompt) {
                            is InteractivePrompt.Chooser -> ChooserBody(prompt) { id, text ->
                                controller.deliver(InteractiveOverlayModel.chooserResult(id, text))
                            }
                            is InteractivePrompt.Approval -> ApprovalBody(
                                prompt,
                                onApprove = { id, text ->
                                    controller.deliver(
                                        InteractiveOverlayModel.approvalResult("approve", id, text),
                                    )
                                },
                                onReject = { controller.deliver(prompt.cancelResult()) },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        controller.clearActivityFinisher(promptEpoch)
        super.onDestroy()
    }
}

// Brand palette — mirrors the WindowManager overlay (InteractiveOverlayController)
// so the draw-overlay path and this fallback Activity look identical.
private val AccentTeal = Color(0xFF2DD4BF)
private val OnAccent = Color(0xFF04221F)
private val CardBg = Color(0xFF12171E)
private val OptionBg = Color(0xFF181F29)
private val TextColor = Color(0xFFECF0F6)
private val SubtleText = Color(0xB3FFFFFF)
private val FaintText = Color(0x80FFFFFF)
private val RejectRed = Color(0xFFEF4444)
private val Hairline = Color(0x1AFFFFFF)

/** Avatar + AI name + contextual subtitle — the branded header for both prompts. */
@Composable
private fun BrandHeader(aiName: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).background(AccentTeal, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = aiName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                color = OnAccent,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                aiName,
                color = TextColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = SubtleText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ChooserBody(
    prompt: InteractivePrompt.Chooser,
    onChoice: (id: String, text: String?) -> Unit,
) {
    var text by remember { mutableStateOf(prompt.textInput?.initial ?: "") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandHeader(prompt.aiName, prompt.prompt)
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        prompt.textInput?.let { field ->
            val lbl = field.label
            val ph = field.hint
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = if (lbl != null) ({ Text(lbl) }) else null,
                placeholder = if (ph != null) ({ Text(ph) }) else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        prompt.options.forEach { opt ->
            Button(
                onClick = { onChoice(opt.id, text.takeIf { it.isNotEmpty() }) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = if (opt.id == prompt.default) {
                    ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = OnAccent)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(opt.label, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ApprovalBody(
    prompt: InteractivePrompt.Approval,
    onApprove: (id: String, text: String) -> Unit,
    onReject: () -> Unit,
) {
    val edited = remember { mutableStateListOf<String>().apply { addAll(prompt.variants.map { it.text }) } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandHeader(prompt.aiName, prompt.title)
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Text(
            text = "Choose a draft to publish",
            style = MaterialTheme.typography.titleSmall,
            color = TextColor,
            fontWeight = FontWeight.Bold,
        )
        prompt.variants.forEachIndexed { i, variant ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OptionBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "OPTION ${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold,
                    )
                    if (prompt.editable) {
                        OutlinedTextField(
                            value = edited[i],
                            onValueChange = { edited[i] = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = variant.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextColor,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${(if (prompt.editable) edited[i] else variant.text).length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = FaintText,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { onApprove(variant.id, if (prompt.editable) edited[i] else variant.text) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentTeal,
                                contentColor = OnAccent,
                            ),
                        ) {
                            Text("Use this  ›")
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (prompt.editable) "Tap a draft to edit" else "Pick a draft",
                style = MaterialTheme.typography.labelMedium,
                color = FaintText,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed),
            ) {
                Text("Reject")
            }
        }
    }
}
