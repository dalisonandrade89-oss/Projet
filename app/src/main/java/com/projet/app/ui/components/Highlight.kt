package com.projet.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.projet.app.domain.search.ProjectSearch

/** [text] com os trechos que casam com a pesquisa destacados (sem acento/maiúscula, como a busca). */
@Composable
fun rememberHighlighted(text: String, tokens: List<String>): AnnotatedString {
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        fontWeight = FontWeight.Bold
    )
    return remember(text, tokens, style) {
        buildAnnotatedString {
            append(text)
            ProjectSearch.highlightRanges(text, tokens).forEach { addStyle(style, it.first, it.last + 1) }
        }
    }
}
