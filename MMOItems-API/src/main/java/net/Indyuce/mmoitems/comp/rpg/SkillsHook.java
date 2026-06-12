package net.Indyuce.mmoitems.comp.rpg;

import me.leothepro555.skills.events.SkillLevelUpEvent;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillsHook implements Listener {

    @EventHandler
    public void a(SkillLevelUpEvent event) {
        OfflinePlayer player = event.getPlayer();
        if (player.isOnline()) PlayerData.get(player).resolveModifiersLater();
    }
}