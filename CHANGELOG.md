# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e versionamento [SemVer](https://semver.org/lang/pt-BR/).

## [1.2.0] - 2026-11-25

### Adicionado
- **Versão no rodapé** da tela inicial (`Projet v1.2.0.N`, a mesma do APK/Release).
- **Botão flutuante de ajustes (engrenagem)**, à esquerda e no mesmo estilo do botão "+". Abre uma folha com:
  - **Sincronizar com o Google Drive** (antes na barra superior; durante o sync um anel gira em volta da engrenagem);
  - **Lixeira** (antes na barra superior);
  - **Cor de fundo** e **Cor de destaque**, com paleta de 12 fundos (claros e escuros) e 10 destaques, e botão "Padrão" para voltar ao visual do sistema. A escolha fica salva no aparelho.
- Ícone de **ordenar** com três riscos (substitui as três bolinhas).

### Alterado
- Tema: com fundo personalizado, as superfícies (cards, diálogos, menus) e os ícones da barra de status acompanham a cor escolhida; o destaque clareia automaticamente em fundos escuros para manter o contraste.
- Lista de projetos ganhou folga embaixo para o último card não ficar atrás dos botões flutuantes.

### Corrigido
- CI: o job de Release só rodava em tags `v*`, então um push na `main` gerava apenas o APK debug. Agora todo push na `main` (e o disparo manual) compila o APK **Release** e publica uma GitHub Release `vX.Y.Z.<nº da execução>`, que o auto-update do app já reconhece. Tags `v*` continuam funcionando.
- CI: atualizados `actions/checkout` e `actions/setup-java` para v5 (aviso de Node 20/descontinuação).

### Adicionado
- **Ícone do app**: mochila a jato (sem piloto) em *adaptive icon* vetorial, com versão monocromática para ícones temáticos (Android 13+) e fallback para API 24–25.

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
