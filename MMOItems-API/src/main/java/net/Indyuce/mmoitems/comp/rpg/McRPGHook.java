package net.Indyuce.mmoitems.comp.rpg;

import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import us.eunoians.mcrpg.api.events.mcrpg.McRPGPlayerLevelChangeEvent;

public class McRPGHook implements Listener {

    @EventHandler
    public void a(McRPGPlayerLevelChangeEvent event) {
        PlayerData.get(event.getMcRPGPlayer().getOfflineMcRPGPlayer()).resolveModifiersLater();
    }
}