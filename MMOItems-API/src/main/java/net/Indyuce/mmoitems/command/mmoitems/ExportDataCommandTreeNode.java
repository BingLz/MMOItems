package net.Indyuce.mmoitems.command.mmoitems;

import io.lumine.mythic.lib.command.CommandTreeExplorer;
import io.lumine.mythic.lib.command.CommandTreeNode;
import io.lumine.mythic.lib.data.DataExport;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.manager.data.SQLDatabaseImpl;
import net.Indyuce.mmoitems.manager.data.YAMLDatabaseImpl;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ExportDataCommandTreeNode extends CommandTreeNode {
	public ExportDataCommandTreeNode(CommandTreeNode parent) {
		super(parent, "exportdata");
	}

	@Override
	@NotNull
	public CommandResult execute(CommandTreeExplorer explorer, CommandSender sender, String[] args) {

		// Export YAML to SQL
		final boolean result = new DataExport<>(MMOItems.plugin.getPlayerDataManager(), sender).start(
				YAMLDatabaseImpl::new,
				SQLDatabaseImpl::new);

		return result ? CommandResult.SUCCESS : CommandResult.FAILURE;
	}
}
