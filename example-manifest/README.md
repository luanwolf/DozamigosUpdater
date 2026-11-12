# Manifest Dozamigos

Publique este JSON no GitHub e coloque a URL em `config/dozamigos-client.toml` -> `manifestUrl`.

## O que entra no sync

Definido em `tools/manifest-include.txt` (padrao do pack Dozamigos):

- `mods/` (secao propria)
- `config/`
- `emotes/`
- `fancymenu_data/`
- `journeymap/`
- `local/`
- `mod_data/`
- `resourcepacks/`
- `shaderpacks/`
- `options.txt`
- `servers.dat`
- `icon.png`

Nao entram: `saves/`, logs, crash-reports, e o proprio config do updater (`config/dozamigos*`).

## Gerar automatico

```powershell
cd e:\Github\Minecraft\dozamigos\tools

.\generate-manifest.ps1 `
  -GameDir "C:\Users\Luan Wolf\curseforge\minecraft\Instances\asdsasdadsadsadas" `
  -PackVersion "1.0.0" `
  -ReleaseBase "https://github.com/USER/REPO/releases/download/v1.0.0" `
  -FilesUrlBase "https://raw.githubusercontent.com/USER/REPO/main" `
  -PreviousUrl "https://raw.githubusercontent.com/USER/REPO/main/dozamigos-manifest.json" `
  -DryRun
```

Fluxo:

1. Roda o script e revisa o diff (`+ ~ - SKIP`)
2. Sobe jars no GitHub Release
3. Commit da arvore de files (`config/`, `resourcepacks/`, `options.txt`, ...) + `dozamigos-manifest.json`
4. Clientes baixam sozinhos

## Formato

| Campo | Significado |
|-------|-------------|
| `packVersion` | Versao do pack na UI |
| `mods[]` | Jars em `mods/` |
| `files[].path` | Path relativo ao jogo |
| `files[].url` | Download direto |
| `files[].sha256` | Integridade |

`configs[]` antigo ainda e lido, mas o gerador grava em `files[]`.
