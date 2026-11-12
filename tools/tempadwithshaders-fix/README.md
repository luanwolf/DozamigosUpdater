# Tempad With Shaders 1.0.1 (fix)

Crash original: `ClassMetadataNotFoundException: net.irisshaders.iris.shaderpack.ShaderPack`

O mixin referencia Iris/Oculus. Sem Oculus no instance, o Mixin quebra o Tempad inteiro no `sided_setup`.

Fix: `TempadShadersMixinPlugin` so aplica o mixin se Iris/Oculus existir. Sem Oculus, Tempad roda normal (portal sem o render especial de shader).

Rebuild:

```powershell
# ver script mental: javac plugin + jar cfm em jar/
```

Saida: `modpack/mods/tempadwithshaders-1.20.1-1.0.1.jar`
