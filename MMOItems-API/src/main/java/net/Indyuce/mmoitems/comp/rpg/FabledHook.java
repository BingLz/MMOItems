package net.Indyuce.mmoitems.comp.rpg;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import studio.magemonkey.fabled.api.event.PlayerLevelUpEvent;

public class FabledHook implements Listener {

    @EventHandler
    public void b(PlayerLevelUpEvent event) {
        net.Indyuce.mmoitems.api.player.PlayerData.get(event.getPlayerData().getPlayer()).resolveModifiersLater();
    }
}