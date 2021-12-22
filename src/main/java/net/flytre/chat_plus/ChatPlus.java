package net.flytre.chat_plus;

import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.flytre.chat_plus.config.Config;
import net.flytre.flytre_lib.api.config.ConfigHandler;
import net.flytre.flytre_lib.api.config.ConfigRegistry;
import net.flytre.flytre_lib.api.config.GsonHelper;

public class ChatPlus implements ModInitializer {

    public static final ConfigHandler<Config> CONFIG;

    static {
        GsonBuilder builder = GsonHelper.GSON_BUILDER;
        CONFIG = new ConfigHandler<>(new Config(), "chat_plus", builder.create());
    }

    @Override
    public void onInitialize() {
        ConfigRegistry.registerServerConfig(CONFIG);
        CONFIG.handle();
    }
}
