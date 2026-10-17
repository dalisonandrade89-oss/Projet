# 🚀 Projet

![Build](https://github.com/dalisonandrade89-oss/projet/actions/workflows/build_and_release.yml/badge.svg)
![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-green)

**Projet** (PROfissional + JET) é um app Android nativo para acompanhar projetos e seus passos, com
progresso em tempo real, auto-save e backup/sincronização no Google Drive.

## ✨ Funcionalidades
- Cards de projeto com nome, categoria, descrição e barra de progresso animada
  (`Progresso % = passos concluídos / total × 100`).
- Passos com auto-save instantâneo; concluídos ficam acinzentados/tachados e deslizam para o fim da lista.
- Ordenação por nome (A-Z/Z-A), categoria, progresso (0-100%/100-0%) e posição manual; filtro por categoria.
- Offline-first (Room) + sincronização incremental com o Google Drive (`appDataFolder`), Last-Write-Wins por `updatedAt`.
- Auto-update in-app a partir das Releases do GitHub.

## 🧱 Stack
Kotlin · Jetpack Compose + Material 3 (Dynamic Color) · Clean Architecture + MVVM · Koin ·
Room + Flow · WorkManager · Drive REST v3 (OkHttp) · kotlinx.serialization · GitHub Actions.

```
com.projet.app
├── data/{local,remote,repository}   # Room, Drive, SyncRepository, ProjectRepositoryImpl
├── domain/{model,repository}        # Project, Step, ProjectRepository
├── ui/{list,detail,components,theme}
├── work/                            # SyncWorker, SyncScheduler
├── update/                          # AppUpdaterService
└── di/                              # módulo Koin
```

## 📦 Instalação / Setup
1. Copie esta árvore para uma pasta e rode `git init`.
2. **Gradle Wrapper** (o `gradle-wrapper.jar` é binário e não vem aqui): com Gradle 8.9 instalado, rode
   `gradle wrapper --gradle-version 8.9` e faça commit de `gradlew`, `gradlew.bat` e `gradle/wrapper/`.
   (O workflow do CI não depende disso.)
3. **Licença:** o `LICENSE` já contém o texto integral da GPL 3.0 (adicione seu nome nos cabeçalhos dos arquivos, se quiser).
4. O repositório já está configurado como `dalisonandrade89-oss/projet` (em `gradle.properties` e nos badges). Se usar outro nome de repositório, altere nesses dois lugares.
5. Suba para o GitHub. Cada push em `main` gera um APK debug (aba *Actions → Artifacts*).

### Google Drive (sincronização)
No [Google Cloud Console](https://console.cloud.google.com): ative a **Google Drive API**, configure a tela de
consentimento OAuth e crie um **OAuth Client ID do tipo Android** com o package `com.projet.app` e o SHA-1 do
keystore que assina o APK. Escopo usado: `drive.appdata`.

### Releases assinadas + auto-update
O Android só atualiza "por cima" se o APK novo tiver a **mesma assinatura** do instalado. Gere um keystore uma vez:

```bash
keytool -genkeypair -v -keystore projet.jks -alias projet -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 projet.jks   # copie a saída
```
Cadastre em *Settings → Secrets and variables → Actions*: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Depois publique:

```bash
git tag v1.0.0 && git push origin v1.0.0
```
O workflow compila o APK assinado e cria a Release `Projet-v1.0.0.apk`. O app checa `releases/latest`
ao abrir e oferece a atualização.

## 📄 Licença
GPL-3.0 — veja [LICENSE](LICENSE).
