package com.projet.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projet.app.R
import com.projet.app.domain.model.Project
import com.projet.app.domain.model.Step

/** Barra de progresso cuja largura "preenche" suavemente até o valor-alvo (inclusive ao entrar na tela). */
@Composable
fun AnimatedProgress(progress: Float, modifier: Modifier = Modifier) {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) { target = progress }
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

/**
 * Card de projeto: nome, categoria, descrição curta e progresso.
 * [onMoveUp]/[onMoveDown] só são passados na ordenação manual.
 *
 * Na pesquisa, [highlightTokens] destaca o trecho encontrado no nome/categoria/descrição e
 * [matchedSteps] lista os passos onde o texto aparece (assim se vê ONDE o texto está).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    highlightTokens: List<String> = emptyList(),
    matchedSteps: List<Step> = emptyList()
) {
    val searching = highlightTokens.isNotEmpty()
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = rememberHighlighted(project.name.ifBlank { "Sem nome" }, highlightTokens),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (project.category.isNotBlank()) {
                        Text(
                            text = rememberHighlighted(project.category, highlightTokens),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Default.KeyboardArrowUp, "Mover para cima") }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) { Icon(Icons.Default.KeyboardArrowDown, "Mover para baixo") }
                }
            }
            if (project.description.isNotBlank()) {
                Text(
                    text = rememberHighlighted(project.description, highlightTokens),
                    style = MaterialTheme.typography.bodyMedium,
                    // Na pesquisa mostra mais linhas, para o trecho encontrado não ficar escondido.
                    maxLines = if (searching) 4 else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (matchedSteps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    matchedSteps.take(MAX_MATCHED_STEPS).forEach { step ->
                        MatchedStepLine(step, highlightTokens)
                    }
                    val extra = matchedSteps.size - MAX_MATCHED_STEPS
                    if (extra > 0) {
                        Text(
                            text = stringResource(R.string.search_more_steps, extra),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            AnimatedProgress(project.progress)
            Text(
                text = "${project.completedSteps}/${project.totalSteps} passos · ${project.progressPercent}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val MAX_MATCHED_STEPS = 3

/** "↳ Lavar o motor · Mecânica" com o trecho encontrado em destaque. */
@Composable
private fun MatchedStepLine(step: Step, tokens: List<String>) {
    val label = buildString {
        append("↳ ").append(step.title)
        if (step.subcategory.isNotBlank()) append("  ·  ").append(step.subcategory)
    }
    Text(
        text = rememberHighlighted(label, tokens),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
    )
}
