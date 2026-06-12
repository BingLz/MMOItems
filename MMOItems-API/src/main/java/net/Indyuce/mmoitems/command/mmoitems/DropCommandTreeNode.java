package net.Indyuce.mmoitems.command.mmoitems;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.command.argument.Argument;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.droptable.item.MMOItemDropItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.util.RandomAmount;
import net.Indyuce.mmoitems.command.Arguments;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DropCommandTreeNode extends CommandTreeNode {
    private final Argument<Type> argType;
    private final Argument<MMOItemTemplate> argId;
    private final Argument<World> argWorld;
    private final Argument<Double> argX, argY, argZ, argDropChance, argUnidentifyChance;
    private final Argument<RandomAmount> argRange;

    public DropCommandTreeNode(CommandTreeNode parent) {
        super(parent, "drop");

        argType = addArgument(Arguments.ITEM_TYPE);
        argId = addArgument(Arguments.ITEM_ID_2);
        argWorld = addArgument(Arguments.WORLD);
        argX = addArgument(Argument.AMOUNT_DOUBLE.withKey("x"));
        argY = addArgument(Argument.AMOUNT_DOUBLE.withKey("y"));
        argZ = addArgument(Argument.AMOUNT_DOUBLE.withKey("z"));
        argDropChance = addArgument(Arguments.CHANCE.withKey("drop-chance").withFallback(explorer -> 100d));
        argRange = addArgument(Arguments.RANGE.withFallback(explorer -> RandomAmount.one()));
        argUnidentifyChance = addArgument(Arguments.CHANCE.withKey("unidentify-chance").withFallback(explorer -> 0d));
    }

    @Override
    public @NotNull CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {
        var type = explorer.parse(argType);
        var template = explorer.parse(argId);
        var world = explorer.parse(argWorld);
        var x = explorer.parse(argX);
        var y = explorer.parse(argY);
        var z = explorer.parse(argZ);
        var dropChance = explorer.parse(argDropChance);
        var unidentifyChance = explorer.parse(argUnidentifyChance);
        var range = explorer.parse(argRange);

        var dropItem = new MMOItemDropItem(type, template.getId(), dropChance / 100, unidentifyChance / 100, range);
        if (!dropItem.rollDrop()) return CommandResult.SUCCESS;

        // Successfully drop item
        var item = dropItem.getItem(null);
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(MMOItems.plugin.getPrefix() + ChatColor.RED + "An error occurred while attempting to generate item " + template.getId() + ", see console for more information.");
            return CommandResult.FAILURE;
        }

        world.dropItem(new Location(world, x, y, z), item);
        return CommandResult.SUCCESS;
    }
}
