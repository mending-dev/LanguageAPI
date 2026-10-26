package dev.mending.languageapi;

import dev.mending.languageapi.command.LanguageCommand;
import dev.mending.languageapi.core.LanguageService;
import dev.mending.languageapi.placeholder.PlaceholderHook;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main class of LanguageAPI.
 * <p>
 * PlaceholderAPI is an optional dependency. All code that touches PlaceholderAPI classes lives in
 * {@link PlaceholderHook} and the classes it uses, which are only loaded if PlaceholderAPI is present.
 */
public final class LanguageAPIPlugin extends JavaPlugin {

    private LanguageService service;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        service = new LanguageService(this);
        service.load();

        // Removes the registration of other plugins when they are disabled
        getServer().getPluginManager().registerEvents(service, this);

        hookPlaceholderApi();
        registerCommand();
    }

    @Override
    public void onDisable() {
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
    }

    private void hookPlaceholderApi() {
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI not found. Running as a library only: "
                    + "the %language_...% placeholders are not available.");
            return;
        }

        // Only now are the PlaceholderAPI-dependent classes loaded
        placeholderHook = new PlaceholderHook(this, service);
        if (!placeholderHook.register()) {
            getLogger().warning("Could not register the PlaceholderAPI expansion. "
                    + "The %language_...% placeholders are not available.");
        }
    }

    private void registerCommand() {
        // Paper plugins have no commands section, commands are registered via the lifecycle API
        LanguageCommand command = new LanguageCommand(service);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        "languageapi",
                        "LanguageAPI admin command",
                        List.of("langapi"),
                        command
                )
        );
    }

    /**
     * @return the central service, used by the API facade
     */
    public LanguageService getService() {
        return service;
    }
}