package net.Indyuce.mmoitems.comp.rpg;

import me.robin.battlelevels.events.PlayerLevelUpEvent;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BattleLevelsHook implements  Listener {

	@EventHandler
	public void a(PlayerLevelUpEvent event) {
		PlayerData.get(event.getPlayer()).resolveModifiersLater();
	}
}