package com.dozamigos.dozamigos.client.fancymenu;

import com.dozamigos.dozamigos.DozamigosMod;
import com.dozamigos.dozamigos.client.gui.UpdaterScreen;
import net.minecraftforge.fml.ModList;

/**
 * FancyMenu: Open Screen {@link UpdaterScreen}, requisito de manutencao e placeholder.
 * Sem import estatico das classes FancyMenu — so Class.forName quando o mod esta presente.
 */
public final class FancyMenuBridge {
    public static final String SCREEN_ID = UpdaterScreen.class.getName();
    private static final String REQ_CLASS =
            "com.dozamigos.dozamigos.client.fancymenu.ServerMaintenanceRequirement";
    private static final String PH_CLASS =
            "com.dozamigos.dozamigos.client.fancymenu.MaintenanceTextPlaceholder";

    private static boolean fancyMenuPresent;

    private FancyMenuBridge() {}

    public static boolean isPresent() {
        return fancyMenuPresent;
    }

    public static void tryRegister() {
        fancyMenuPresent = ModList.get().isLoaded("fancymenu");
        if (!fancyMenuPresent) {
            DozamigosMod.LOGGER.info("FancyMenu ausente; banner vanilla + /dozamigos seguem ok");
            return;
        }
        try {
            Class<?> reqReg = Class.forName(
                    "de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirementRegistry");
            Class<?> reqBase = Class.forName(
                    "de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement");
            Object req = Class.forName(REQ_CLASS).getConstructor().newInstance();
            reqReg.getMethod("register", reqBase).invoke(null, req);

            Class<?> phReg = Class.forName(
                    "de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry");
            Class<?> phBase = Class.forName(
                    "de.keksuccino.fancymenu.customization.placeholder.Placeholder");
            Object ph = Class.forName(PH_CLASS).getConstructor().newInstance();
            phReg.getMethod("register", phBase).invoke(null, ph);

            DozamigosMod.LOGGER.info(
                    "FancyMenu: requisito dozamigos_server_maintenance + placeholder dozamigos_maint_text. Open Screen: {}",
                    SCREEN_ID);
        } catch (Throwable t) {
            DozamigosMod.LOGGER.warn("FancyMenu presente mas registro Dozamigos falhou: {}", t.toString());
        }
    }
}
