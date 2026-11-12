package com.dozamigos.dozamigos;

import com.dozamigos.dozamigos.client.ClientBootstrap;
import com.dozamigos.dozamigos.config.DozamigosConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(DozamigosMod.MOD_ID)
public class DozamigosMod {
    public static final String MOD_ID = "dozamigos";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DozamigosMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DozamigosConfig.SPEC);

        // Client-only: no server hooks. displayTest=IGNORE_ALL_VERSION keeps multiplayer happy.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.init();
        }
    }
}
