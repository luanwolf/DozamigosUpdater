package com.dozamigos.dozamigos.client.fancymenu;

import com.dozamigos.dozamigos.client.ServerStatusMonitor;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import java.util.List;
import net.minecraft.client.resources.language.I18n;

/**
 * Placeholder FancyMenu com pulso leve de cor.
 * No texto: {@code {"placeholder":"dozamigos_maint_text"}}
 */
public final class MaintenanceTextPlaceholder extends Placeholder {
    public static final String ID = "dozamigos_maint_text";

    public MaintenanceTextPlaceholder() {
        super(ID);
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString deserialized) {
        ServerStatusMonitor.ensureProbing();
        if (ServerStatusMonitor.status() != ServerStatusMonitor.Status.OFFLINE) {
            return "";
        }
        // ponytail: pulso 1.2s via codigo de cor — FancyMenu re-parseia o placeholder a cada frame
        boolean bright = ((System.currentTimeMillis() / 600) % 2) == 0;
        String titleColor = bright ? "§6§l" : "§e§l";
        String hintColor = bright ? "§7" : "§8";
        String title = I18n.get("dozamigos.maintenance.title");
        String hint = I18n.get("dozamigos.maintenance.hint");
        return titleColor + title + "%n%" + hintColor + hint;
    }

    @Override
    public List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Dozamigos: texto de manutencao";
    }

    @Override
    public List<String> getDescription() {
        return List.of("Texto 'Servidor em manutencao' com pulso de cor (so quando offline).");
    }

    @Override
    public String getCategory() {
        return "Dozamigos";
    }

    @Override
    @SuppressWarnings("deprecation")
    public DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString d = new DeserializedPlaceholderString();
        d.placeholderIdentifier = getIdentifier();
        return d;
    }
}
