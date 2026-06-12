# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the entire project (clean, install all modules, skip tests)
mvn clean install -DskipTests

# Build and copy to a local Spigot server (pass server folder paths as args)
./server_install.bat "C:\path\to\server"

# Build individual modules
cd MMOItems-API && mvn clean install -DskipTests
cd MMOItems-Dist && mvn clean package
cd MMOItems-Paper && mvn clean package

# Package produces final JAR at: mmoitems-master/target/MMOItems-<version>.jar
# (MMOItems-Dist shades all modules into the final distributable)

# Install to local Maven repository (needed for dependents)
cd MMOItems-API && mvn clean install
```

**Note**: There are no formal unit tests in this project. The primary validation is building the JAR and deploying it to a Paper/Spigot test server.

**Requirements**: Java 11, Maven 3.6+, access to the PhoenixDevelopment Nexus repo (public).

## Project Overview

MMOItems is a Minecraft RPG item plugin for Paper/Spigot 1.21.4, providing custom items with stats, abilities, crafting, and world generation features. It's a Maven multi-module project (6.10.1-SNAPSHOT) depending on **MythicLib** (required).

### Module Structure

| Module | Purpose |
|--------|---------|
| `MMOItems-API` | All plugin logic: managers, stats, API, GUIs, commands, listeners, crafting system |
| `MMOItems-Paper` | Paper-specific optimizations (PaperServerAdapter, MiniMessage, a few Paper-only stats) |
| `MMOItems-Dist` | Distribution shade module — contains `plugin.yml`, `config.yml`, default item YAML configs, language files, tooltips. Shade-bundles the other two modules into the final JAR |

All Java source code lives under `net.Indyuce.mmoitems` across modules, but the Dist module shades them together into a single JAR.

### Build Process

`MMOItems-Dist/pom.xml` uses `maven-shade-plugin` to merge `MMOItems-API` and `MMOItems-Paper` into the final `target/MMOItems-<version>.jar`. The `server_install.bat` script runs `mvn clean install` then copies the JAR to specified server folders.

### Core Dependencies

- **MythicLib** (1.7.1-SNAPSHOT) — required dependency, provides the plugin framework, NBT wrapper, skill/damage systems, cooldowns, elements, MMOPlayerData
- **Spigot API** (1.21.4-R0.1-SNAPSHOT) / **Paper API** (1.21.4-R0.1-SNAPSHOT) — provided scope
- Many optional soft-dependencies for compatibility (see `comp/` package)

## Architecture Highlights

### Manager Pattern

The main class `MMOItems` (extends `MMOPlugin` from MythicLib) initializes ~15 managers, most implementing `Reloadable`:

| Manager | Responsibility |
|---------|---------------|
| `StatManager` | Registers all item stats (loaded via reflection from `ItemStats` static fields) |
| `TemplateManager` | Manages MMOItem templates loaded from YAML config; holds all item definitions |
| `TypeManager` | Manages item types (SWORD, ARMOR, CONSUMABLE, GEM_STONE, etc.) |
| `CraftingManager` | Crafting stations, recipes, conditions, triggers, ingredients |
| `ConfigManager` | Plugin config, language files (Crowdin-synced, 10+ languages), cached options |
| `TierManager` | Item tier definitions |
| `SetManager` | Item set bonuses |
| `UpgradeManager` | Upgrade templates for item upgrading |
| `DropTableManager` | Drop tables (block drops, mob drops) |
| `WorldGenManager` | World generation custom blocks |
| `BlockManager` | Custom block management |
| `RecipeManager` | Bukkit recipe registration |
| `LoreFormatManager` | Lore formatting rules |
| `PlayerDataManager` | SQL/YAML player data persistence |

### Stats System (Core Extensibility)

Stats are the fundamental building block. Each stat is an `ItemStat<R extends RandomStatData<S>, S extends StatData>` with these lifecycle methods:

- `whenInitialized(Object)` — reads from config YAML → `RandomStatData`
- `whenApplied(ItemStackBuilder, S)` — applies stat to item during building (adds NBT tags, lore lines)
- `whenLoaded(ReadMMOItem)` — reconstructs stat data when reading an item from NBT
- `whenDisplayed(List<String>, S)` — contributes lore lines

**All built-in stats are registered as `public static final` fields in `ItemStats.java`**, discovered via reflection by `StatManager.loadBuiltins()`. Custom stats can be added via config or API.

Stat type hierarchy:
- `DoubleStat`, `BooleanStat`, `StringStat`, `StringListStat`, `ChooseStat` — simple value stats
- `WeaponBaseStat` — weapon attack damage
- `RequiredLevelStat` — level requirements
- `DisableStat` — interaction toggles
- `InternalStat`, `PlayerStat`, `GemStoneStat`, `ItemRestriction`, `ConsumableItemInteraction` — specialized

### Type System

`Type` (in `api/Type.java`) defines item categories: SWORD, DAGGER, HAMMER, BOW, STAFF, ARMOR, CONSUMABLE, GEM_STONE, ACCESSORY, BLOCK, etc. Each type:
- Has a `ModifierSource` (MELEE_WEAPON, RANGED_WEAPON, ARMOR, ACCESSORY, etc.)
- Has an interaction provider function `(PlayerData, NBTItem) -> UseItem`
- Defines which stats are available (from config YAML)

### Item Lifecycle

1. **Config YAML** → `TemplateManager` reads item YAML files → creates `MMOItemTemplate`
2. **Template** → `MMOItem` (holds `Map<ItemStat, StatData>`)
3. **Building** → `ItemStackBuilder` applies each stat (`whenApplied`) → Bukkit `ItemStack`
4. **Storage** → All stat data serialized to NBT tags via MythicLib `NBTItem`
5. **Reading** → `LiveMMOItem` (heavy) reads ALL stats from an ItemStack via NBT; `ReadMMOItem` (lighter) for partial reads

The NBT key pattern is `MMOITEMS_<STAT_ID>`, with metadata keys like `MMOITEMS_ITEM_TYPE`, `MMOITEMS_ITEM_ID`, `MMOITEMS_ITEM_TIER`, `MMOITEMS_ITEM_UUID`, `MMOITEMS_REVISION_ID`.

### API Entry Points

- `MMOItemsAPI` — public API for other plugins (register skills, get items, cast skills)
- `MMOItems.plugin.getItem(Type, String)` — generate an ItemStack
- `MMOItems.plugin.getMMOItem(Type, String)` — get the MMOItem object
- `PlayerData.get(Player)` — access player data
- `Type` static constants identify item types

### Crafting System

Complex crafting system with `CraftingStation`, conditions (level, mana, stamina, permission, placeholder), ingredients (vanilla, MMOItems, ItemsAdder, Oraxen, Nexo, Mythic items), recipe outputs, and triggers (commands, sounds, messages, skill casts). Supports smithing recipes and upgrading recipes.

### Compatibility Layer (`comp/`)

Soft-dependency hooks organized by integration type:
- **RPG**: MMOCore, mcMMO, Heroes, AuraSkills, AureliumSkills, BattleLevels, SkillsPro, McRPG, Fabled, RacesAndClasses
- **Enchants**: CrazyEnchantments, AdvancedEnchantments, MythicEnchants
- **Custom Items**: ItemsAdder, Oraxen, Nexo
- **Other**: MythicMobs (mechanics, lootsplosion), Vault, PlaceholderAPI, WorldEdit, PhatLoots, BossShopPro, Citizens

### Key Patterns

- **Reloadable interface** — Many managers implement this for `/mi reload` support
- **PluginUtils.hookDependencyIfPresent()** — Soft-dependency loading pattern throughout `MMOItems.onEnable()`
- **Reflection-based stat registration** — `StatManager.loadBuiltins()` uses reflection on `ItemStats.class`
- **PreloadedObject / PostLoadAction** — Two-phase initialization pattern from MythicLib
- **Version-dependent functionality** — Annotations (`@VersionDependant`, `@BackwardsCompatibility`) for handling MC version differences
- **Language system** — YAML-based translations per language, Crowdin-managed for community contributions
