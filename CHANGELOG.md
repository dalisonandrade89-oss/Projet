# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e versionamento [SemVer](https://semver.org/lang/pt-BR/).

## [1.1.0] - 2026-10-20

### Adicionado
- **Lixeira de 30 dias**: projetos e passos excluídos podem ser restaurados em *Lixeira* (ícone na barra da lista).
  - Projeto restaurado volta ao fim da lista, com seus passos; passo restaurado volta ao fim dos passos.
  - A limpeza definitiva (após 30 dias) roda ao abrir o app e a cada sincronização, também removendo os tombstones vencidos do Drive.
- Strings da Lixeira em `strings.xml` (com plurais).

### Corrigido
- Crash do auto-update no Android 7.x (API 24–25): `canRequestPackageInstalls()` só existe a partir da API 26.
- Filtro de categoria "fantasma": se a categoria filtrada deixa de existir, a lista volta para "Todas".
- Tela de detalhe: distingue carregando × não encontrado; fecha sozinha se o projeto for excluído (aqui ou em outro aparelho) e sem pop duplo.
- Reordenação manual agora é uma única gravação atômica (sem piscar, sem ordem pela metade).
- Ordem estável quando `position` empata (desempate por `id`) em projetos, passos e nas consultas do Room.
- `.gitignore` reescrito, incluindo `.kotlin/`.

### Alterado
- Mensagem de exclusão de projeto informa que ele vai para a Lixeira.
- Versão do app agora vive em `gradle.properties` (`versionName`/`versionCode`).

### Notas de migração
- Sem alteração de schema do Room (continua `version = 1`) e do backup (`schemaVersion = 1`): atualização direta, sem migração.

## [1.0.0]
- Versão inicial: projetos, passos, auto-save, progresso, Room offline-first, sync com Google Drive e auto-update via GitHub Releases.

---

## Política de versionamento

| O que mudou | Ação |
|---|---|
| Correção de bug | `PATCH` (1.1.0 → 1.1.1) |
| Funcionalidade nova compatível | `MINOR` (1.1.0 → 1.2.0) |
| Mudança incompatível (formato de dados, remoção de recurso) | `MAJOR` (1.x → 2.0.0) |
| Mudança em entidade do Room | subir `AppDatabase.version` **e** criar `Migration` (+ teste de migração) |
| Mudança no formato do backup JSON | subir `BackupFile.schemaVersion` e tratar versões antigas |

A cada mudança significativa: (1) atualizar `versionName`/`versionCode` em `gradle.properties`, (2) registrar no `CHANGELOG.md`, (3) `git tag vX.Y.Z && git push origin vX.Y.Z` para o CI publicar o APK assinado.
