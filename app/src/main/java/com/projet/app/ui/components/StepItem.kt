package com.projet.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.projet.app.domain.model.Step

/**
 * Item de passo. Concluído = acinzentado + tachado (transição animada).
 *
 * O deslizar para o fim da fila é feito por quem usa o item, aplicando
 * `Modifier.animateItem()` (sucessor do animateItemPlacement) no [modifier] dentro do LazyColumn,
 * junto com a ordenação `Project.orderedSteps`.
 *
 * O título é editável inline; o texto fica em estado local e é enviado a cada tecla para
 * [onTitleChange], que faz o Auto-Save com debounce no repositório.
 */
@Composable
fun StepItem(
    step: Step,
    onToggle: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(step.id) { mutableStateOf(step.title) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (step.isCompleted) 0.5f else 1f,
        animationSpec = tween(300),
        label = "stepAlpha"
    )
    val background by animateColorAsState(
        targetValue = if (step.isCompleted) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(300),
        label = "stepBackground"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = step.isCompleted, onCheckedChange = onToggle)
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onTitleChange(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    textDecoration = if (step.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir passo")
            }
        }
    }
}
