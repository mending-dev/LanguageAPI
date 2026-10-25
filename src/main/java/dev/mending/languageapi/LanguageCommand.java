package dev.mending.languageapi;

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

    private final LanguageManager languageManager;

    public LanguageCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
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
                int count = languageManager.reload();
                send(sender, "<#efbf04>LanguageAPI</#efbf04> <gray>Reloaded <white>" + count + "</white> language(s).");
            }
            case "info" -> {
                Player player = sender instanceof Player p ? p : null;
                send(sender, "<#efbf04>LanguageAPI</#efbf04> <gray>Information:");
                send(sender, "<gray> Fallback: <white>" + languageManager.getFallbackCode());
                send(sender, "<gray> Output format: <white>" + languageManager.getOutputFormat().name().toLowerCase(Locale.ROOT));
                send(sender, "<gray> Loaded: <white>" + String.join("<gray>, <white>", languageManager.getLoadedLanguages()));
                if (player != null) {
                    send(sender, "<gray> Your client language: <white>" + languageManager.getClientLanguageCode(player));
                    send(sender, "<gray> Language used for you: <white>" + languageManager.resolveLanguage(player).code());
                }
            }
            default -> send(sender, "<red>Unknown sub command. Use <white>/" + LABEL + " <reload|info>");
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