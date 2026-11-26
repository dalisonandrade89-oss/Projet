package com.projet.app.domain.search

import com.projet.app.domain.model.Project
import com.projet.app.domain.model.Step
import java.text.Normalizer

/**
 * Pesquisa de projetos: texto completo OU parcial ("mec" acha "Mecânica"), sem diferenciar
 * maiúsculas/minúsculas nem acentos ("mecanica" acha "Mecânica").
 *
 * Várias palavras funcionam em "E": "lavar roda" acha o projeto que tenha "lavar" e "roda" em
 * qualquer lugar (nome, categoria, descrição, passos ou subcategorias).
 */
object ProjectSearch {

    private val whitespace = Regex("\\s+")

    /**
     * Minúsculas e sem acento, **caractere a caractere**: o resultado tem o MESMO comprimento do
     * original, então um índice achado no texto "dobrado" vale também no texto original
     * (necessário para destacar o trecho encontrado).
     */
    fun fold(text: String): String = buildString(text.length) {
        for (ch in text) {
            append(
                if (ch.code < 128) ch.lowercaseChar()
                else Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)[0].lowercaseChar()
            )
        }
    }

    /** Palavras da busca já "dobradas"; vazio = sem filtro. */
    fun tokens(query: String): List<String> =
        fold(query.trim()).split(whitespace).filter { it.isNotEmpty() }

    fun matches(project: Project, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true
        val haystack = fold(
            buildString {
                append(project.name).append('\n')
                append(project.category).append('\n')
                append(project.description)
                project.steps.forEach { append('\n').append(it.title).append('\n').append(it.subcategory) }
            }
        )
        return tokens.all { haystack.contains(it) }
    }

    /** Passos (título ou subcategoria) que contêm alguma das palavras — para mostrar no card. */
    fun matchingSteps(project: Project, tokens: List<String>): List<Step> {
        if (tokens.isEmpty()) return emptyList()
        return project.orderedSteps.filter { step ->
            val title = fold(step.title)
            val sub = fold(step.subcategory)
            tokens.any { title.contains(it) || sub.contains(it) }
        }
    }

    /** Trechos de [text] a destacar (índices do texto original), sem sobreposição. */
    fun highlightRanges(text: String, tokens: List<String>): List<IntRange> {
        if (tokens.isEmpty() || text.isEmpty()) return emptyList()
        val folded = fold(text)
        val found = mutableListOf<IntRange>()
        for (token in tokens) {
            var from = 0
            while (true) {
                val index = folded.indexOf(token, from)
                if (index < 0) break
                found += index until index + token.length
                from = index + token.length
            }
        }
        val merged = mutableListOf<IntRange>()
        for (range in found.sortedBy { it.first }) {
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.last + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                merged += range
            }
        }
        return merged
    }
}
