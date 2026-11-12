# Dozamigos

Atualizador de modpack **client-side** para Minecraft **1.20.1** / Forge **47.1.47**.

Baixa o `dozamigos-manifest.json` de um repositorio GitHub (ou qualquer URL HTTPS), compara com a pasta `mods/` e mostra o que foi **adicionado**, **atualizado** ou **removido**.

## Build

```bat
gradlew.bat build
```

Jar em `build/libs/dozamigos-1.0.0.jar` — copie para a pasta `mods` da instance.

## Config

Depois do primeiro launch: `config/dozamigos-client.toml`

```toml
manifestUrl = "https://raw.githubusercontent.com/USER/REPO/main/dozamigos-manifest.json"
githubToken = ""
checkOnTitleScreen = true
showTitleButton = true
```

## FancyMenu

No botao do menu custom:

1. **Open Screen or Custom GUI** → `com.dozamigos.dozamigos.client.gui.UpdaterScreen`
2. ou execute o comando cliente `/dozamigos`

O botao vanilla do title screen e opcional (`showTitleButton`) e pode ser escondido pelo FancyMenu — o FQCN / comando sao o caminho certo.

## Popup

Com `checkOnTitleScreen = true` e `manifestUrl` preenchido, ao abrir o menu principal o mod checa o manifest e abre um popup se houver mudancas.

## Aplicar updates

Na tela do atualizador: **Verificar** → revise a lista colorida → **Aplicar** → **reinicie o Minecraft** (jars novos so carregam no boot).

## Gerar o manifest

Script em `tools/generate-manifest.ps1` — varre `mods/` + pastas do `manifest-include.txt` (config, resourcepacks, options.txt, etc.), imprime `+ ~ - SKIP` e grava o JSON. Detalhes em `example-manifest/README.md`.
