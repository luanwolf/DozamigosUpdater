<div align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=flat-square" />
  <img alt="Forge" src="https://img.shields.io/badge/Forge-47.1.47-E04E39?style=flat-square" />
  <img alt="Java" src="https://img.shields.io/badge/Java-mod-ED8B00?style=flat-square&logo=openjdk&logoColor=white" />
  <img alt="Versão" src="https://img.shields.io/badge/versão-1.0.12-2b3140?style=flat-square" />
</div>

# Dozamigos Updater

Atualizador de modpack **client-side** pra Minecraft **1.20.1** / Forge **47.1.47**.

Baixa o `dozamigos-manifest.json` de um repositório GitHub (ou qualquer URL HTTPS), compara com a pasta `mods/` e mostra o que foi **adicionado**, **atualizado** ou **removido**.

## Build

```bat
gradlew.bat build
```

Jar em `build/libs/dozamigos-1.0.12.jar` — copia pra pasta `mods` da instance.

Resource packs e shader packs do jogador **não são removidos** pelo updater (só ADDED/UPDATED do pack).

## Config

Depois do primeiro launch: `config/dozamigos-client.toml`

```toml
manifestUrl = "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/dozamigos-manifest.json"
githubToken = ""   # obrigatório enquanto o repo for privado (PAT com scope repo)
checkOnTitleScreen = true
showTitleButton = true
```

## FancyMenu

No botão do menu custom:

1. **Open Screen or Custom GUI** → `com.dozamigos.dozamigos.client.gui.UpdaterScreen`
2. ou execute o comando cliente `/dozamigos`

O botão vanilla do title screen é opcional (`showTitleButton`) e pode ser escondido pelo FancyMenu — o FQCN / comando são o caminho certo.

## Popup

Com `checkOnTitleScreen = true` e `manifestUrl` preenchido, ao abrir o menu principal o mod checa o manifest e abre um popup se houver mudanças.

## Manutenção do servidor

Com `showMaintenanceBanner = true`, o probe (Server List Ping) roda no menu. TCP sozinho não basta — o túnel Playit aceita connect com o MC offline.

**FancyMenu:** o aviso é um elemento de texto no layout (`dozamigos.txt`), editável no editor. Visibilidade = loading requirement `Dozamigos: servidor em manutencao`. Texto = placeholder `dozamigos_maint_text` (pulso de cor). Arrasta/redimensiona no editor como qualquer outro elemento.

**Sem FancyMenu:** banner vanilla à direita dos botões, com pulso leve (só desenho, sem widget).

## Aplicar updates

Na tela do atualizador: **Verificar** → revisa a lista colorida → **Aplicar** → **reinicia o Minecraft** (jars novos só carregam no boot).

## Gerar o manifest

A árvore publicada fica em `modpack/`. O JSON na raiz (`dozamigos-manifest.json`) aponta downloads para `raw.githubusercontent.com/.../main/modpack/...`.

```powershell
.\tools\generate-manifest.ps1 `
  -GameDir ".\modpack" `
  -PackVersion "1.0.8" `
  -ReleaseBase "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/modpack/mods" `
  -FilesUrlBase "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/modpack" `
  -OutFile ".\dozamigos-manifest.json"
```

Detalhes em `example-manifest/README.md`.
