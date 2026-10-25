# LanguageAPI

A simple utility plugin for **Paper** that provides multi-language support through **PlaceholderAPI**.

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

---

## Requirements

| Software | Version |
|---|---|
| Minecraft / Paper | 26.1 – 26.3 |
| Java | 25 |
| [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | **Required** |

The plugin will not start without PlaceholderAPI.

---

## Installation

1. Download `LanguageAPI-<version>.jar` or build it yourself (see [Building](#building)).
2. Make sure **PlaceholderAPI** is installed on your server.
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

The expansion uses the identifier `language`.

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
fallback-language: en_us

# Map a client "de_at" without its own file to another file of the same language (de_de)
language-prefix-matching: true

# Output format of the placeholders: minimessage | legacy | plain
output-format: minimessage

# Resolve other placeholders (e.g. %player_name%) inside language texts
parse-nested-placeholders: true

# Text for unknown keys, {key} is replaced by the key
missing-key-format: "[{key}]"
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

---

## Commands and permissions

| Command | Description | Permission |
|---|---|---|
| `/languageapi reload` | Reloads `config.yml` and all language files | `languageapi.admin` |
| `/languageapi info` | Shows fallback, output format, loaded languages and your client language | `languageapi.admin` |

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

## For developers

Other plugins can also use the language system directly, without going through PlaceholderAPI.

Add LanguageAPI as a dependency in your plugin's `paper-plugin.yml`:

```yaml
dependencies:
  server:
    LanguageAPI:
      load: BEFORE
      required: true
      join-classpath: true
```

Usage in code:

```java
LanguageAPIPlugin languageApi = (LanguageAPIPlugin) Bukkit.getPluginManager().getPlugin("LanguageAPI");
LanguageManager manager = languageApi.getLanguageManager();

// Returns null if no language contains the key
String text = manager.translate(player, "npc_rewards");

// Language used for this player (after fallback handling)
String code = manager.resolveLanguage(player).code();
```

`translate(...)` returns the text in the configured `output-format`. With `minimessage`, you can convert it to a component using `MiniMessage.miniMessage().deserialize(text)`. All methods are thread-safe.

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
│  ├─ LanguageAPIPlugin.java    # Main class, registers expansion and command
│  ├─ LanguageManager.java      # Loads languages, resolves texts
│  ├─ Language.java             # Immutable language data
│  ├─ OutputFormat.java         # MiniMessage / legacy / plain conversion
│  ├─ LanguageExpansion.java    # PlaceholderAPI expansion
│  └─ LanguageCommand.java      # /languageapi command
└─ resources/
   ├─ paper-plugin.yml
   ├─ config.yml
   └─ lang/en_us.yml
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
Check that PlaceholderAPI is installed and that the server runs Java 25 and Paper 26.1 or newer.

---

## License

Define the license of your project here.