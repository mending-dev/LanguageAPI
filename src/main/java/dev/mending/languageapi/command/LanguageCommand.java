package dev.mending.languageapi.command;

import dev.mending.languageapi.core.LanguageBundle;
import dev.mending.languageapi.core.LanguageCodes;
import dev.mending.languageapi.core.LanguageService;
import dev.mending.languageapi.core.Settings;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Handles /languageapi <reload|info>, registered through Paper's command lifecycle API.
 */
public final class LanguageCommand implements BasicCommand {

    private static final String LABEL = "languageapi";
    private static final String PERMISSION = "languageapi.admin";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final List<String> SUB_COMMANDS = List.of("reload", "info");

    private final LanguageService service;

    public LanguageCommand(LanguageService service) {
        this.service = service;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            send(sender, "<#efbf04>LanguageAPI</#efbf04> <gray>- usage: <white>/" + LABEL + " <reload|info>");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                int count = service.reloadAll();
                send(sender, "<#efbf04>LanguageAPI</#efbf04> <gray>Reloaded the config and the language files of <white>"
                        + count + "</white> plugin(s).");
            }
            case "info" -> sendInfo(sender);
            default -> send(sender, "<red>Unknown sub command. Use <white>/" + LABEL + " <reload|info>");
        }
    }

    private void sendInfo(CommandSender sender) {
        Settings settings = service.settings();

        send(sender, "<#efbf04>LanguageAPI</#efbf04> <gray>Information:");
        send(sender, "<gray> Fallback: <white>" + settings.fallbackLanguage());
        send(sender, "<gray> Output format: <white>" + settings.outputFormat().name().toLowerCase(Locale.ROOT));
        send(sender, "<gray> PlaceholderAPI: <white>" + (service.hasPlaceholderApi() ? "hooked" : "not installed"));
        send(sender, "<gray> Registered plugins:");
        for (LanguageBundle bundle : service.getBundles()) {
            String papiMarker = bundle.isPlaceholderApiEnabled() ? " <#efbf04>[PAPI]" : "";
            send(sender, "<gray>  - <white>" + bundle.ownerName() + papiMarker + "<gray>: <white>"
                    + String.join("<gray>, <white>", bundle.languageCodes()));
        }

        if (sender instanceof Player player) {
            String clientCode = LanguageCodes.of(player);
            send(sender, "<gray> Your client language: <white>" + (clientCode.isEmpty() ? "unknown" : clientCode));
            send(sender, "<gray> Language used for you: <white>" + service.ownBundle().effectiveLanguage(clientCode));
        }
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        // Only the first argument has suggestions
        if (args.length > 1) {
            return List.of();
        }
        String input = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return SUB_COMMANDS.stream().filter(sub -> sub.startsWith(input)).toList();
    }

    @Override
    public @Nullable String permission() {
        return PERMISSION;
    }

    private void send(CommandSender sender, String miniMessage) {
        sender.sendMessage(MINI_MESSAGE.deserialize(miniMessage));
    }
}