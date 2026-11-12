package com.bawnorton.tempadwithshaders;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Tempad With Shaders referencia classes do Iris/Oculus no mixin.
 * Sem Oculus, o Mixin explode com ClassMetadataNotFoundException: ShaderPack.
 * Este plugin so aplica o mixin quando Iris/Oculus esta no classpath.
 */
public class TempadShadersMixinPlugin implements IMixinConfigPlugin {
    private boolean irisPresent;

    @Override
    public void onLoad(String mixinPackage) {
        irisPresent = canLoad("net.irisshaders.iris.Iris")
                && canLoad("net.irisshaders.iris.shaderpack.ShaderPack");
        System.out.println("[tempadwithshaders] Iris/Oculus "
                + (irisPresent ? "detectado — mixin ativo" : "ausente — mixin desligado (Tempad normal)"));
    }

    private static boolean canLoad(String name) {
        try {
            Class.forName(name, false, TempadShadersMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return irisPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
