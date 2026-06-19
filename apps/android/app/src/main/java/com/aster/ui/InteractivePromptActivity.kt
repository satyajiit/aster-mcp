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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    BackHandler { controller.deliver(prompt.cancelResult()) }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
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

@Composable
private fun ChooserBody(
    prompt: InteractivePrompt.Chooser,
    onChoice: (id: String, text: String?) -> Unit,
) {
    var text by remember { mutableStateOf(prompt.textInput?.initial ?: "") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = prompt.aiName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = prompt.prompt,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
                shape = RoundedCornerShape(12.dp),
                colors = if (opt.id == prompt.default) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = prompt.aiName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = prompt.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        prompt.variants.forEachIndexed { i, variant ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Button(
                        onClick = { onApprove(variant.id, if (prompt.editable) edited[i] else variant.text) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Use this", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onReject,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Reject", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
