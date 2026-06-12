package net.Indyuce.mmoitems.api.crafting;

import io.lumine.mythic.lib.gson.JsonArray;
import io.lumine.mythic.lib.gson.JsonElement;
import io.lumine.mythic.lib.gson.JsonObject;
import io.lumine.mythic.lib.util.Lazy;
import io.lumine.mythic.lib.util.annotation.BackwardsCompatibility;
import io.lumine.mythic.lib.util.lang3.Validate;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.crafting.CraftingStatus.CraftingQueue.QueueItem;
import net.Indyuce.mmoitems.api.crafting.recipe.CraftingRecipe;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class CraftingStatus {
    private final PlayerData playerData;

    /**
     * Saves data about items being constructed in specific stations. players
     * must go back to the station GUI and claim their item once it's ready
     */
    private final Map<CraftingStation, CraftingQueue> queues = new HashMap<>();

    public CraftingStatus(PlayerData playerData) {
        this.playerData = playerData;
    }

    public CraftingQueue getQueue(@NotNull CraftingStation station) {
        return queues.computeIfAbsent(station, CraftingQueue::new);
    }

    public void flush() {
        this.queues.values().forEach(CraftingQueue::flush);
    }

    public static class CraftingQueue {

        /**
         * Using a lazy to avoid ghost references to outdated
         * crafting stations after using /mi reload
         */
        private final Lazy<CraftingStation> station;

        private final String stationId;
        private final List<QueueItem> crafts = new ArrayList<>();

        public CraftingQueue(CraftingStation station) {
            this.station = Lazy.persistent(() -> {
                final var found = MMOItems.plugin.getCrafting().getStation(station.getId());
                Validate.notNull(found, "Could not find crafting station with ID '" + station.getId() + "'");
                return found;
            });
            this.stationId = station.getId();
        }

        @NotNull
        public List<QueueItem> getCrafts() {
            return crafts;
        }

        public boolean isFull(@NotNull CraftingStation station) {
            return crafts.size() >= station.getMaxQueueSize();
        }

        public void flush() {
            this.station.flush();
            this.crafts.forEach(QueueItem::flush);
        }

        public void remove(QueueItem item) {
            final int index = crafts.indexOf(item);
            Validate.isTrue(index >= 0, "Could not find item in queue");
            crafts.remove(index);

            // Remove time left from subsequent items
            final long gain = Math.min(item.getLeft(), item.getRecipe().getCraftingTime());
            for (int j = index; j < crafts.size(); j++)
                crafts.get(j).removeDelay(gain);
        }

        @Nullable
        public QueueItem getCraft(@NotNull UUID uniqueId) {
            for (QueueItem craft : crafts)
                if (craft.getUniqueId().equals(uniqueId))
                    return craft;
            return null;
        }

        public void add(@NotNull CraftingRecipe recipe) {
            final long highestCompletion = CraftingQueue.this.crafts.isEmpty() ? System.currentTimeMillis() :
                    Math.max(System.currentTimeMillis(), CraftingQueue.this.crafts.get(CraftingQueue.this.crafts.size() - 1).completion);
            final long itemCompletion = highestCompletion + recipe.getCraftingTime();

            crafts.add(new QueueItem(recipe, System.currentTimeMillis(), itemCompletion));
        }

        @NotNull
        public CraftingStation getStation() {
            return this.station.get();
        }

        @NotNull
        public JsonElement toJson() {
            final var array = new JsonArray();
            for (QueueItem craft : crafts) array.add(craft.toJson());
            return array;
        }

        public void loadFromYml(@NotNull ConfigurationSection config) {

            @BackwardsCompatibility(version = "6.10") final Optional<String> legacyOpt = config.getConfigurationSection(stationId).getKeys(false).stream().findFirst();
            final boolean legacyLoading = legacyOpt.isPresent() && config.contains(stationId + "." + legacyOpt.get() + ".delay");

            for (String recipeConfigId : config.getConfigurationSection(stationId).getKeys(false)) {
                String recipeId = config.getString(stationId + "." + recipeConfigId + ".recipe", "none");

                // Backwards compatibility config loading for MI <6.10
                if (legacyLoading) {
                    final long started = config.getLong(stationId + "." + recipeConfigId + ".started");
                    final long delay = config.getLong(stationId + "." + recipeConfigId + ".delay");

                    this.crafts.add(new QueueItem(recipeId,
                            started,
                            started + delay));
                    continue;
                }

                this.crafts.add(new QueueItem(recipeId,
                        config.getLong(stationId + "." + recipeConfigId + ".start"),
                        config.getLong(stationId + "." + recipeConfigId + ".completion")
                ));
            }
        }

        public void loadFromJson(@NotNull JsonArray array) {
            for (var entry : array) {
                var obj = entry.getAsJsonObject();
                var recipeId = obj.get("Recipe").getAsString();
                var start = obj.get("Start").getAsLong();
                var completion = obj.get("Completion").getAsLong();

                this.crafts.add(new QueueItem(recipeId, start, completion));
            }
        }

        public class QueueItem {

            /**
             * /mi reload stations force MI to save the recipe
             * IDs instead of a direct reference to the crafting recipe
             */
            private final Lazy<CraftingRecipe> recipe;

            private final String recipeId;
            private final UUID uniqueId = UUID.randomUUID();
            private final long start;

            /**
             * A crafting queue is composed of a series of queue items
             * that each wait on the previous item in the queue for completion.
             * It is much easier to work with the timestamp of completion for
             * any queued item, in order to avoid confusion.
             */
            private long completion;

            public QueueItem(@NotNull CraftingRecipe recipe, long start, long completion) {
                this(recipe.getId(), start, completion);
            }

            public QueueItem(@NotNull String recipeId, long start, long completion) {
                this.recipe = Lazy.persistent(() -> {
                    var recipeFound = CraftingQueue.this.getStation().getRecipe(recipeId);
                    Validate.isTrue(recipeFound instanceof CraftingRecipe, "Recipe '" + recipeId + "' is not a crafting recipe");
                    return (CraftingRecipe) recipeFound;
                });
                this.recipeId = recipeId;
                this.start = start;
                this.completion = completion;
            }

            public UUID getUniqueId() {
                return uniqueId;
            }

            public CraftingRecipe getRecipe() {
                return this.recipe.get();
            }

            public boolean isReady() {
                return getLeft() == 0;
            }

            public void removeDelay(long amount) {
                this.completion -= amount;
            }

            public long getElapsed() {
                return Math.max(getRecipe().getCraftingTime(), System.currentTimeMillis() - start);
            }

            public long getLeft() {
                return Math.max(0, completion - System.currentTimeMillis());
            }

            public void flush() {
                this.recipe.flush();
            }

            public JsonElement toJson() {
                var object = new JsonObject();
                object.addProperty("Recipe", recipeId);
                object.addProperty("Start", start);
                object.addProperty("Completion", completion);
                //object.addProperty("UniqueId", uuid.toString());
                return object;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                QueueItem that = (QueueItem) o;
                return Objects.equals(uniqueId, that.uniqueId);
            }

            @Override
            public int hashCode() {
                return Objects.hash(uniqueId);
            }
        }
    }

    //region JSON

    public void loadFromJson(@NotNull JsonObject object) {
        for (var entry : object.entrySet()) {
            var station = MMOItems.plugin.getCrafting().getStation(entry.getKey());
            if (station == null) {
                MMOItems.plugin.getLogger().log(Level.SEVERE,
                        "An error occurred while trying to load crafting station recipe data of '" + playerData.getMMOPlayerData().getPlayerName() + "': "
                                + "could not find crafting station with ID '" + entry.getKey()
                                + "', make sure you backup that player data file before the user logs off.");
                continue;
            }

            var queueStatus = this.queues.computeIfAbsent(station, CraftingQueue::new);
            queueStatus.loadFromJson(entry.getValue().getAsJsonArray());
        }
    }

    @NotNull
    public JsonElement toJson() {
        var object = new JsonObject();

        for (var entry : this.queues.entrySet())
            object.add(entry.getKey().getId(), entry.getValue().toJson());

        return object;
    }

    //endregion

    //region YAML

    public void loadFromYaml(@NotNull ConfigurationSection config) {
        for (var stationId : config.getKeys(false)) {
            var station = MMOItems.plugin.getCrafting().getStation(stationId);
            if (station == null) {
                MMOItems.plugin.getLogger().log(Level.SEVERE,
                        "An error occurred while trying to load crafting station recipe data of '" + playerData.getMMOPlayerData().getPlayerName() + "': "
                                + "could not find crafting station with ID '" + stationId
                                + "', make sure you backup that player data file before the user logs off.");
                continue;
            }
            var queue = this.queues.computeIfAbsent(station, CraftingQueue::new);
            queue.loadFromYml(config);
        }
    }

    public void saveToYaml(@NotNull ConfigurationSection config) {
        queues.forEach((station, queue) -> {
            for (QueueItem craft : queue.getCrafts()) {
                config.set(station.getId() + ".recipe-" + craft.getUniqueId().toString() + ".recipe", craft.getRecipe().getId());
                config.set(station.getId() + ".recipe-" + craft.getUniqueId().toString() + ".start", craft.start);
                config.set(station.getId() + ".recipe-" + craft.getUniqueId().toString() + ".completion", craft.completion);
            }
        });
    }

    //endregion
}
