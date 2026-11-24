package com.dozamigos.dozamigos.client.fancymenu;

import com.dozamigos.dozamigos.client.ServerStatusMonitor;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import java.util.List;

/**
 * Visibilidade no FancyMenu: elemento aparece so com o servidor OFFLINE.
 * No editor: Loading Requirements → "Dozamigos: servidor em manutencao".
 */
public final class ServerMaintenanceRequirement extends LoadingRequirement {
    public static final String ID = "dozamigos_server_maintenance";

    public ServerMaintenanceRequirement() {
        super(ID);
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(String value) {
        ServerStatusMonitor.ensureProbing();
        return ServerStatusMonitor.status() == ServerStatusMonitor.Status.OFFLINE;
    }

    @Override
    public String getDisplayName() {
        return "Dozamigos: servidor em manutencao";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "Verdadeiro quando o servidor Dozamigos nao responde ao status ping.",
                "Use em um elemento de texto pra editar posicao, cor e animacao no FancyMenu.");
    }

    @Override
    public String getCategory() {
        return "Dozamigos";
    }

    @Override
    public String getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getValueFormattingRules() {
        return null;
    }
}
