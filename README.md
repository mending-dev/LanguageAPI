# LanguageAPI

A utility plugin for **Paper** that provides multi-language support. Server admins use it through **PlaceholderAPI** placeholders, other plugins use it as a **library** (see [For developers](#for-developers-plugin-api)). PlaceholderAPI is optional.

Admins create language files (`de_de.yml`, `en_us.yml`, ...), add their own keys with MiniMessage texts, and use them anywhere PlaceholderAPI placeholders work, e.g. `%language_npc_rewards%`. Every player automatically sees the text in the language of their Minecraft client.

---

## Features

- Automatically detects each player's client language (Paper / Adventure API)
- Any number of languages via YAML files in the `lang/` folder
- Custom keys, available as `%language_<key>%`
- Texts in **MiniMessage** format
- Configurable fallback language (default: `en_us`)
- If a key is missing in the player's language, the text from the fallback language is used automatically
- Language variants are merged, e.g. a client using `de_at` gets `de_de.yml` if there is no `de_at.yml`
- Selectable output format: MiniMessage, legacy (`§` colors incl. hex) or plain
- Nested placeholders inside texts, e.g. `%player_name%`
- Reload via command, no server restart required
- **Developer API:** other plugins can use LanguageAPI to become multi-language with their own `lang/` folder (see [For developers](#for-developers-plugin-api))
- **PlaceholderAPI is optional:** without it, LanguageAPI works as a pure library for other plugins
- Plugins using the API automatically support PlaceholderAPI placeholders in their texts (if PlaceholderAPI is installed and the plugin declares it as a dependency)

---

## Requirements

| Software | Version |
|---|---|
| Minecraft / Paper | 26.1 – 26.3 |
| Java | 25 |
| [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | Optional (see below) |

PlaceholderAPI is needed for the `%language_...%` placeholders and for placeholders inside the texts of other plugins. Without it, LanguageAPI still runs as a library for other plugins; only these two features are unavailable.

---

## Installation

1. Download `LanguageAPI-<version>.jar` or build it yourself (see [Building](#building)).
2. Install **PlaceholderAPI** if you want to use the `%language_...%` placeholders (optional if you only need LanguageAPI as a library for other plugins).
3. Copy the JAR into your server's `plugins/` folder.
4. Restart the server.

On the first start, the following files are created:

```
plugins/LanguageAPI/
├─ config.yml
└─ lang/
   └─ en_us.yml
```

A restart is only needed for the installation. Apply later changes to the config and language files with `/languageapi reload`.

---

## Quick start

1. Open `plugins/LanguageAPI/lang/en_us.yml` and add a key:

   ```yaml
   npc_rewards: "<#ffffff>Rewards"
   ```

2. Create `plugins/LanguageAPI/lang/de_de.yml` and add the same key in German:

   ```yaml
   npc_rewards: "<#ffffff>Belohnungen"
   ```

3. Run `/languageapi reload`.

4. Use the placeholder in any plugin that supports PlaceholderAPI:

   ```
   %language_npc_rewards%
   ```

   Players with a German client see *Belohnungen*, everyone else sees *Rewards*.

---

## Language files

All language files are located in `plugins/LanguageAPI/lang/`.

### File names

The file name (without `.yml`) matches the language code of the Minecraft client:

| File | Language |
|---|---|
| `en_us.yml` | English (US), **default fallback** |
| `de_de.yml` | German (Germany) |
| `fr_fr.yml` | French |
| `es_es.yml` | Spanish |

Capitalization does not matter: `de-DE.yml`, `De_de.yml` and `de_de.yml` all become `de_de`. The `.yaml` extension works as well. A list of all codes can be found on the Minecraft Wiki under *Language*.

### Structure

```yaml
# Simple key
npc_rewards: "<#ffffff>Rewards"

# Nested keys are addressed with a dot
welcome:
   title: "<gradient:#efbf04:#ffffff>Welcome!</gradient>"
   subtitle: "<gray>Hello <white>%player_name%</white>, have fun!"
   # Available as %language_welcome.title% and %language_welcome.subtitle%

# Lists are joined into one text with line breaks
help_lines:
   - "<gray>Line one"
   - "<gray>Line two"
```

### Rules and tips

- **Always put texts in quotes.** Characters like `#`, `:` and `%` can otherwise be misread by YAML.
- Keys are **not case-sensitive**: `%language_NPC_Rewards%` and `%language_npc_rewards%` return the same text.
- Do not use spaces or `%` in keys.
- Save files as **UTF-8**, otherwise special characters such as umlauts are displayed incorrectly.
- The reserved keys `player`, `active` and `fallback` must not be used. They are ignored with a warning.
- A faulty file is skipped and reported in the log. The other languages keep loading.

### How is the language chosen?

1. A file with the exact code of the client exists (e.g. `de_de`). It is used.
2. Otherwise, a file of the same language exists (e.g. `de_at` maps to `de_de`), provided `language-prefix-matching` is enabled.
3. Otherwise, the fallback language is used.

If only the **single key** is missing in the chosen language, that key is taken from the fallback language. You do not have to translate new languages completely right away.

---

## Placeholders

The expansion uses the identifier `language` and is only registered if PlaceholderAPI is installed. It exposes only the texts in `plugins/LanguageAPI/lang/`; messages of other plugins are never available through placeholders.

| Placeholder | Description | Example |
|---|---|---|
| `%language_player%` | Language code of the player's client | `de_de` |
| `%language_active%` | Language file actually used for the player | `en_us` |
| `%language_fallback%` | Configured fallback language | `en_us` |
| `%language_<key>%` | Text of the key in the player's language | `%language_npc_rewards%` |
| `%language_<a>.<b>%` | Text of a nested key | `%language_welcome.title%` |

For offline players and console requests, the fallback language is used.

If a key exists in no language file, the text from `missing-key-format` is returned (default: `[key]`). This makes typos easy to spot instead of showing the raw placeholder.

---

## Configuration (`config.yml`)

```yaml
# Fallback language (file name in lang/ without .yml)
# Also used as fallback for all plugins that use the LanguageAPI
fallback-language: en_us

# Map a client "de_at" without its own file to another file of the same language (de_de)
language-prefix-matching: true

# Output format of the placeholders: minimessage | legacy | plain
output-format: minimessage

# Resolve other placeholders (e.g. %player_name%) inside language texts
parse-nested-placeholders: true

# Text for unknown keys, {key} is replaced by the key
missing-key-format: "[{key}]"

# Only for plugins that use the LanguageAPI and declare PlaceholderAPI as a dependency:
# Values of placeholders from other expansions are inserted as plain text, so MiniMessage tags
# in values (e.g. a player-chosen nickname) cannot be abused. %language_...% keeps its formatting.
# Set to false if an expansion intentionally returns MiniMessage text.
escape-placeholder-values: true
```

### `output-format`

The plugin only delivers the text. Whether MiniMessage is actually rendered is decided by the plugin that uses the placeholder.

| Value | Output | Use when ... |
|---|---|---|
| `minimessage` | Raw MiniMessage text, e.g. `<#ffffff>Rewards` | the other plugin parses MiniMessage itself |
| `legacy` | Ready-made `§` color codes incl. hex colors | the other plugin only understands legacy colors |
| `plain` | Text without any formatting | you only need the plain text |

The format is applied once while loading. After changing it, run `/languageapi reload`.

### `parse-nested-placeholders`

With `true`, texts like this work:

```yaml
welcome_message: "<gray>Welcome back, <white>%player_name%</white>!"
```

Nested placeholders are resolved at most three levels deep, so keys cannot call each other endlessly.

### `escape-placeholder-values`

Texts of plugins that use the LanguageAPI can contain PlaceholderAPI placeholders (see [For developers](#for-developers-plugin-api)). The placeholder values are inserted **before** the text is parsed as MiniMessage. With `true` (default), values of other expansions are escaped, so a value like a nickname containing `<click:run_command:...>` is shown as plain text instead of being interpreted. Placeholders of the `language` expansion are always trusted and keep their MiniMessage formatting.

---

## Commands and permissions

| Command | Description | Permission |
|---|---|---|
| `/languageapi reload` | Reloads `config.yml` and the language files of LanguageAPI and all registered plugins | `languageapi.admin` |
| `/languageapi info` | Shows fallback, output format, PlaceholderAPI status, all registered plugins with their languages (`[PAPI]` marks plugins with placeholder support) and your client language | `languageapi.admin` |

Alias: `/langapi`

The permission `languageapi.admin` is only granted to operators by default.

---

## Usage examples

The placeholder works in any plugin that supports PlaceholderAPI, such as menus, scoreboards, chat or hologram plugins. Simply put the placeholder where the text should appear:

```yaml
# Example: display name of a menu item in a plugin with PlaceholderAPI support
display_name: "%language_npc_rewards%"
```

You can test a placeholder in-game with the PlaceholderAPI command:

```
/papi parse me %language_npc_rewards%
```

If the other plugin does not render MiniMessage texts from placeholders, set `output-format: legacy`.

---

## For developers (plugin API)

LanguageAPI can make **any other plugin multi-language** with a few lines of code. Your plugin gets its own `lang/` folder in its data folder with exactly the same structure, rules and fallback logic as LanguageAPI itself. LanguageAPI creates the folder and files, reads them and picks the right language for each player.

Messages of other plugins are **not** available through PlaceholderAPI. Only the texts in `plugins/LanguageAPI/lang/` are.

### 1. Add the dependency

`build.gradle.kts` (the JAR is only needed at compile time, LanguageAPI itself is installed on the server):

```kotlin
repositories {
   mavenLocal() // after running ./gradlew publishToMavenLocal in the LanguageAPI project
}

dependencies {
   compileOnly("dev.mending:languageapi:1.1.0")
   // or: compileOnly(files("libs/LanguageAPI-1.1.0.jar"))
}
```

`paper-plugin.yml` of your plugin:

```yaml
dependencies:
   server:
      LanguageAPI:
         load: BEFORE
         required: true
         join-classpath: true
```

### 2. Ship default language files

Put your default texts into your plugin's resources. Every file in `lang/` is picked up automatically:

```
src/main/resources/lang/
├─ en_us.yml
└─ de_de.yml
```

```yaml
# en_us.yml
shop:
   bought: "<green>You bought <white><item></white> for <gold><price></gold>!"
   title: "<gradient:#efbf04:#ffffff>Shop</gradient>"
no_permission: "<red>You are not allowed to do that."
```

### 3. Register and use

```java
public final class ShopPlugin extends JavaPlugin {

   private Messages messages;

   @Override
   public void onEnable() {
      // Creates plugins/ShopPlugin/lang/, copies the default files and loads all languages
      messages = LanguageAPI.register(this);
   }

   public void onPurchase(Player player, String item, int price) {
      // Sent in the player's client language, with MiniMessage placeholders
      messages.send(player, "shop.bought",
              Placeholder.unparsed("item", item),
              Placeholder.unparsed("price", String.valueOf(price)));
   }
}
```

That is all. The server owner can now add `plugins/ShopPlugin/lang/fr_fr.yml` and every French client sees it.

### What LanguageAPI does for you

- Creates `plugins/<YourPlugin>/lang/` and copies your bundled language files on the first start
- Reads all files, normalizes the names (`de-DE.yml` becomes `de_de`) and survives broken files
- Detects the client language of every player and finds the best file (exact match, same language, fallback)
- Uses your bundled files as a safety net: if you add new keys in an update, they work immediately, even if the server owner's files on disk do not contain them yet. Texts on disk always win over bundled texts.
- Reloads everything with `/languageapi reload`
- Removes your registration automatically when your plugin is disabled
- Resolves PlaceholderAPI placeholders in your texts if PlaceholderAPI is installed and your plugin declares it as a dependency (see below)

### PlaceholderAPI in your texts

If PlaceholderAPI is installed **and** your plugin declares it as a dependency, LanguageAPI resolves PlaceholderAPI placeholders in your texts automatically. There is nothing to call.

Declare PlaceholderAPI in your `paper-plugin.yml` (use `required: false` if your plugin also works without it):

```yaml
dependencies:
   server:
      LanguageAPI:
         load: BEFORE
         required: true
         join-classpath: true
      PlaceholderAPI:
         load: BEFORE
         required: false
         join-classpath: true
```

Then use placeholders in your language files:

```yaml
welcome: "<gray>Welcome <white>%player_name%</white>!"
```

- Placeholders are resolved for players. For the console (no player) they are resolved without player context.
- By default, values of other expansions are escaped (see `escape-placeholder-values`). Use `<tag>` placeholders with `Placeholder.unparsed(...)` for your own dynamic values.
- `%language_...%` placeholders of LanguageAPI itself work inside your texts and keep their formatting.
- `messages.isPlaceholderApiEnabled()` tells you whether placeholders are resolved for your plugin.
- LanguageAPI never exposes the texts of your plugin through PlaceholderAPI.

### Language resolution order

For every message, the first hit wins:

1. The player's language (exact match, or another file of the same language)
2. The **fallback language** from the LanguageAPI `config.yml`
3. The **default language** of your plugin (`en_us`, or the one you pass to `register(plugin, "xx_xx")`)
4. The `missing-key-format` text, plus a one-time warning in the console

### `Messages` reference

| Method | Description |
|---|---|
| `send(audience, key, resolvers...)` | Sends the message in the audience's language. Empty texts are not sent. |
| `sendActionBar(audience, key, resolvers...)` | Same, as action bar |
| `sendTitle(audience, titleKey, subtitleKey, resolvers...)` | Sends a title and subtitle (`subtitleKey` may be `null`). An overload accepts custom `Title.Times`. |
| `showBossBar(audience, key, progress, color, overlay, resolvers...)` | Creates a boss bar with the translated name, shows it to the audience and returns it |
| `broadcast(key, resolvers...)` | Sends to all online players (each in their language) and the console |
| `get(audience, key, resolvers...)` | Returns the message as a `Component` |
| `getLines(audience, key, resolvers...)` | Returns a list of components (for YAML lists, e.g. item lore) |
| `getRaw(audience, key)` | Returns the unparsed MiniMessage text |
| `has(key)` | Checks whether a key exists in any language |
| `getLanguage(audience)` | The language code that is actually used for this audience |
| `isPlaceholderApiEnabled()` | Whether PlaceholderAPI placeholders are resolved in your texts |
| `reload()` | Reloads the files of your plugin |

`audience` can be a `Player`, the console or any other Adventure `Audience`. Everything except players uses the fallback language.

Static helpers in `LanguageAPI`: `register(plugin)`, `register(plugin, defaultLanguage)`, `get(plugin)` and `getFallbackLanguage()`.

### Titles, boss bars, scoreboards and more

`Messages` has helpers for the most common cases:

```java
// Title and subtitle (the subtitle key may be null)
messages.sendTitle(player, "welcome.title", "welcome.subtitle",
        Placeholder.unparsed("player", player.getName()));

// Boss bar: returns the bar, so you can update or hide it later
BossBar bar = messages.showBossBar(player, "event.bossbar", 1.0f,
        BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
player.hideBossBar(bar);
```

For everything else, use `get(...)` and `getLines(...)`. They return Adventure components, which work wherever Paper accepts components:

```java
// Scoreboard
objective.displayName(messages.get(player, "scoreboard.title"));
team.prefix(messages.get(player, "scoreboard.coins", Placeholder.unparsed("coins", String.valueOf(coins))));

// Item name and lore (YAML lists become one component per line)
meta.displayName(messages.get(player, "shop.item.name"));
meta.lore(messages.getLines(player, "shop.item.lore"));

// Inventory title
Inventory inventory = Bukkit.createInventory(null, 27, messages.get(player, "shop.title"));
```

Keep these points in mind:

- **Use separate objects for different languages.** The language depends on the player's client. A boss bar, scoreboard or hologram that is shared by several players shows everyone the same text. Create one object per player, or one per language:

  ```java
  Map<String, BossBar> barsByLanguage = new HashMap<>();

  BossBar bar = barsByLanguage.computeIfAbsent(messages.getLanguage(player),
          code -> BossBar.bossBar(messages.get(player, "event.bossbar"),
                  1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
  player.showBossBar(bar);
  ```

- **Do not store translated components for later.** Texts and PlaceholderAPI placeholders are resolved on every call. For anything dynamic (scoreboards, boss bar names), call `get(...)` again on each update, e.g. `bar.name(messages.get(player, "event.bossbar"))`.
- **Rebuild displays when the language changes.** A player can change the client language while online. React to Paper's `PlayerClientOptionsChangeEvent` (check `hasLocaleChanged()`) and rebuild their scoreboard, boss bar and open menus.
- **Item names and lore are italic by default** in Minecraft. Write `<!italic>` at the start of the text or call `.decoration(TextDecoration.ITALIC, false)`.
- **Empty texts:** `send`, `sendActionBar` and `sendTitle` do nothing for an empty text (`key: ""`). `showBossBar` still shows the bar, just without a name, because the bar itself carries information.
- **Menus and holograms** are created once and then viewed by many players. Build the title/lines per viewer (or per language) when opening them, not once at startup.

### Tips

- Use Adventure's `Placeholder.unparsed(...)`, `Placeholder.parsed(...)` and `Placeholder.component(...)` for dynamic values. Use `unparsed` for player-controlled input so nobody can inject MiniMessage tags.
- An **empty text** (`key: ""`) disables a message. Server owners can use this to silence messages.
- YAML lists are joined with line breaks. `getLines(...)` splits them again.
- Keys are case-insensitive, nested sections are addressed with dots (`shop.bought`).
- Calling `register` again with the same plugin returns the existing instance, so it is safe to call after reloads.
- All methods are thread-safe.

---

## Building

An installed JDK 25 is required.

```bash
./gradlew build
```

The finished JAR is located in `build/libs/`.

Project structure:

```
src/main/
├─ java/dev/mending/languageapi/
│  ├─ LanguageAPIPlugin.java            # Main class
│  ├─ api/
│  │  ├─ LanguageAPI.java               # Static entry point for other plugins
│  │  └─ Messages.java                  # Per-plugin message API
│  ├─ core/
│  │  ├─ LanguageService.java           # Registry of all plugins, global settings, reload
│  │  ├─ LanguageBundle.java            # Loads one lang/ folder, resolves texts
│  │  ├─ Language.java                  # Immutable language data
│  │  ├─ Settings.java                  # Values from config.yml
│  │  ├─ LanguageCodes.java             # Code normalization, client language detection
│  │  ├─ OutputFormat.java              # MiniMessage / legacy / plain conversion
│  │  └─ PlaceholderResolver.java       # Abstraction so core works without PlaceholderAPI
│  ├─ placeholder/                      # Only loaded if PlaceholderAPI is installed
│  │  ├─ PlaceholderHook.java           # Registers expansion and resolver
│  │  ├─ LanguageExpansion.java         # PlaceholderAPI expansion
│  │  └─ PapiPlaceholderResolver.java   # Resolves placeholders in texts of other plugins
│  └─ command/
│     └─ LanguageCommand.java           # /languageapi command
└─ resources/
   ├─ paper-plugin.yml
   ├─ config.yml
   └─ lang/en_us.yml
```

To use the plugin as a compile-time dependency in other projects, publish it to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

---

## Troubleshooting

**The placeholder is not replaced and shows up as `%language_...%`.**
Check with `/papi list` whether the `language` expansion is registered. The plugin in which you use the placeholder must support PlaceholderAPI.

**`[my_key]` is shown instead of the text.**
The key exists in no language file, not even in the fallback language. Check the spelling and run `/languageapi reload`.

**The English text is shown although the client is set to German.**
Use `/languageapi info` to see which client language is detected and which file is used. Most likely `de_de.yml` or the key inside it is missing.

**A language file is not loaded.**
Check the server console. Faulty files are reported with a warning, usually caused by a YAML syntax error (missing quotes, wrong indentation, tabs instead of spaces).

**Colors are not rendered, `<#ffffff>` appears as text.**
The calling plugin does not parse MiniMessage. Set `output-format: legacy` and reload.

**Special characters such as umlauts are broken.**
Save the language file as UTF-8.

**The plugin does not start.**
Check that the server runs Java 25 and Paper 26.1 or newer. PlaceholderAPI is optional and not the cause.

**PlaceholderAPI placeholders are not replaced in the texts of another plugin.**
PlaceholderAPI must be installed, and the other plugin must declare it as a dependency in its `paper-plugin.yml` or `plugin.yml`. Run `/languageapi info`: plugins with placeholder support are marked with `[PAPI]`.