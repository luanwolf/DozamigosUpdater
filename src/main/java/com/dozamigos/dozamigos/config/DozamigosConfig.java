package com.dozamigos.dozamigos.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DozamigosConfig {
    public static final String DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/luanwolf/DozamigosUpdater/main/dozamigos-manifest.json";

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> MANIFEST_URL = BUILDER
            .comment(
                    "URL do manifest JSON do pack (raw GitHub, release asset, Pages, etc.).",
                    "Vazio = usa o default do DozamigosUpdater.")
            .define("manifestUrl", DEFAULT_MANIFEST_URL);

    public static final ForgeConfigSpec.ConfigValue<String> GITHUB_TOKEN = BUILDER
            .comment("Token GitHub opcional (so precisa se o repo do manifest for privado / rate limit).")
            .define("githubToken", "");

    public static final ForgeConfigSpec.BooleanValue CHECK_ON_TITLE = BUILDER
            .comment("Checa atualizacoes ao abrir o menu principal e mostra popup se houver mudanca.")
            .define("checkOnTitleScreen", true);

    public static final ForgeConfigSpec.BooleanValue SHOW_TITLE_BUTTON = BUILDER
            .comment("Mostra botao Dozamigos no Title Screen vanilla (FancyMenu pode esconder; use a action/comando).")
            .define("showTitleButton", true);

    public static final ForgeConfigSpec.IntValue CONNECT_TIMEOUT_MS = BUILDER
            .comment("Timeout de conexao HTTP em ms.")
            .defineInRange("connectTimeoutMs", 15000, 1000, 120000);

    public static final ForgeConfigSpec.IntValue READ_TIMEOUT_MS = BUILDER
            .comment("Timeout de leitura HTTP em ms.")
            .defineInRange("readTimeoutMs", 120000, 1000, 600000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private DozamigosConfig() {}

    /** Forge guarda "" se a config foi gerada por um jar antigo; cai no default nesse caso. */
    public static String manifestUrl() {
        String url = MANIFEST_URL.get();
        if (url == null || url.isBlank()) {
            return DEFAULT_MANIFEST_URL;
        }
        return url.trim();
    }
}
