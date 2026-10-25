package dev.mending.languageapi;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main class of LanguageAPI.
 */
public final class LanguageAPIPlugin extends JavaPlugin {

    private LanguageManager languageManager;
    private LanguageExpansion expansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        languageManager = new LanguageManager(this);
        languageManager.reload();

        // PlaceholderAPI is a required dependency (paper-plugin.yml), but registering can still fail
        expansion = new LanguageExpansion(this, languageManager);
        if (!expansion.register()) {
            getLogger().severe("Could not register the PlaceholderAPI expansion, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Paper plugins have no commands section, commands are registered via the lifecycle API
        LanguageCommand command = new LanguageCommand(languageManager);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        "languageapi",
                        "LanguageAPI admin command",
                        List.of("langapi"),
                        command
                )
        );
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister();
        }
    }

    /**
     * Gives other plugins access to the language system.
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}