package com.dozamigos.dozamigos.client.fancymenu;

import com.dozamigos.dozamigos.DozamigosMod;
import com.dozamigos.dozamigos.client.gui.UpdaterScreen;
import net.minecraftforge.fml.ModList;

/**
 * FancyMenu nao precisa de API compile-time.
 * No editor: botao -> Open Screen or Custom GUI ->
 * {@code com.dozamigos.dozamigos.client.gui.UpdaterScreen}
 * Alternativa: executar comando cliente {@code /dozamigos}.
 */
public final class FancyMenuBridge {
    public static final String SCREEN_ID = UpdaterScreen.class.getName();

    private FancyMenuBridge() {}

    public static void tryRegister() {
        if (ModList.get().isLoaded("fancymenu")) {
            DozamigosMod.LOGGER.info(
                    "FancyMenu detectado. No botao custom use Open Screen: {} (ou comando /dozamigos)",
                    SCREEN_ID);
        } else {
            DozamigosMod.LOGGER.info("FancyMenu ausente; botao do title screen e /dozamigos seguem ok");
        }
    }
}
