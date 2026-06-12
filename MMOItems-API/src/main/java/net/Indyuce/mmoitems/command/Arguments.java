package net.Indyuce.mmoitems.command;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.command.argument.Argument;
import io.lumine.mythic.lib.command.argument.ArgumentParseException;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.util.RandomAmount;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Arguments {

    public static final Argument<Type> ITEM_TYPE = new Argument<>("type",
            (explorer, list) -> MMOItems.plugin.getTypes().getAll().forEach(type -> list.add(type.getId())),
            (explorer, input) -> {
                final var type = Type.get(UtilityMethods.enumName(input));
                if (type == null) throw new ArgumentParseException("No such item type '" + input + "'");
                return type;
            });

    public static final Argument<MMOItemTemplate> ITEM_ID_2 = new Argument<>("id", (explorer, list) -> {
        var type = Type.get(explorer.getArguments()[1]);
        if (type != null) MMOItems.plugin.getTemplates().getTemplates(type).forEach(template -> list.add(template.getId()));
    }, (explorer, input) -> {
        var type = Type.get(explorer.getArguments()[1]);
        if (type == null) throw new IllegalStateException("Error, could not parse previous arg");
        var itemId = UtilityMethods.enumName(input);
        var template = MMOItems.plugin.getTemplates().getTemplate(type, itemId);
        if (template == null) throw new ArgumentParseException("Could not find template with ID '" + itemId + "'");
        return template;
    });

    public static final Argument<ItemStat<?, ?>> ITEM_STAT = new Argument<>("STAT_ID",
            (explorer, list) -> MMOItems.plugin.getStats().getAll().forEach(stat -> list.add(stat.getId())),
            (explorer, input) -> {
                final var stat = MMOItems.plugin.getStats().get(UtilityMethods.enumName(input));
                if (stat == null) throw new ArgumentParseException("Could not find stat with ID '" + input + "'");
                return stat;
            });

    public static final Argument<World> WORLD = new Argument<>("world",
            (explorer, list) -> Bukkit.getWorlds().forEach(world -> list.add(world.getName())),
            (explorer, input) -> {
                var world = Bukkit.getWorld(input);
                if (world == null) throw new ArgumentParseException("World " + input + " not found");
                return world;
            });

    public static final Argument<RandomAmount> RANGE = new Argument<>("min-max",
            (explore, list) -> list.addAll(Arrays.asList("1", "10", "64", "1-3", "1-10")),
            (explore, input) -> new RandomAmount(input));

    /**
     * Defaults to 0%
     */
    public static final Argument<Double> CHANCE = Argument.AMOUNT_DOUBLE
            .withFallback(explore -> 0d)
            .withAutoComplete((explore, list) -> list.addAll(Arrays.asList("0", "25", "50", "75", "100")));

    @NotNull
    public static MMOItemTemplate getTemplate(@NotNull Type type, @NotNull String id) {
        try {
            return MMOItems.plugin.getTemplates().getTemplateOrThrow(type, id);
        } catch (Exception exception) {
            throw new ArgumentParseException("No item with ID '" + id + "' for type '" + type.getId() + "'");
        }
    }
}
