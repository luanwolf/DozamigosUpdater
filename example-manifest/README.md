# Manifest Dozamigos

O manifest publicado fica na raiz do repo: [`/dozamigos-manifest.json`](../dozamigos-manifest.json).

URL do cliente (`config/dozamigos-client.toml` → `manifestUrl`):

```
https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/dozamigos-manifest.json
```

Repo privado: o cliente precisa de `githubToken` (PAT com scope `repo`).

## O que entra no sync

Definido em `tools/manifest-include.txt` — paths relativos a `modpack/`:

- `mods/` (secao propria)
- `config/`
- `emotes/`
- `fancymenu_data/`
- `journeymap/`
- `local/`
- `resourcepacks/`
- `shaderpacks/`
- `options.txt`
- `servers.dat`
- `icon.png`

Nao entram: `saves/`, logs, crash-reports, e o proprio config do updater (`config/dozamigos*`).

Downloads apontam para `modpack/` no mesmo repo (`raw.githubusercontent.com/.../main/modpack/...`).

## Gerar

```powershell
cd E:\Github\DozamigosUpdater\tools

.\generate-manifest.ps1 `
  -GameDir "..\modpack" `
  -PackVersion "1.0.0" `
  -ReleaseBase "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/modpack/mods" `
  -FilesUrlBase "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/modpack" `
  -OutFile "..\dozamigos-manifest.json"
```

Fluxo:

1. Atualiza arquivos em `modpack/`
2. Roda o script e revisa o diff (`+ ~ - SKIP`)
3. Commit + push de `modpack/` + `dozamigos-manifest.json`
4. Clientes baixam sozinhos (com token se o repo for privado)

## Formato

| Campo | Significado |
|-------|-------------|
| `packVersion` | Versao do pack na UI |
| `mods[]` | Jars em `mods/` |
| `files[].path` | Path relativo ao jogo |
| `files[].url` | Download direto (URL-encoded) |
| `files[].sha256` | Integridade |

`configs[]` antigo ainda e lido, mas o gerador grava em `files[]`.
