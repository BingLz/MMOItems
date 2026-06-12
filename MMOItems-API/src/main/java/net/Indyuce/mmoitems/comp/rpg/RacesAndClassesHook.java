package net.Indyuce.mmoitems.comp.rpg;

import de.tobiyas.racesandclasses.eventprocessing.events.leveling.LevelDownEvent;
import de.tobiyas.racesandclasses.eventprocessing.events.leveling.LevelUpEvent;
import de.tobiyas.racesandclasses.playermanagement.player.RaCPlayer;
import de.tobiyas.racesandclasses.playermanagement.player.RaCPlayerManager;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.stat.StatInstance;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RacesAndClassesHook implements Listener {
    public RacesAndClassesHook() {
        MythicLib.plugin.getStats().computeStat("MAX_MANA").addUpdateListener(this::updateMaxManaModifier);
    }

    private void updateMaxManaModifier(StatInstance instance) {
        final var statValue = instance.getFinal();

        RaCPlayer info = RaCPlayerManager.get().getPlayer(instance.getMap().getPlayer());
        info.getManaManager().removeMaxManaBonus("MMOItems");
        info.getManaManager().addMaxManaBonus("MMOItems", statValue);
    }

    /**
     * Update the player's inventory whenever he levels up
     * since it could change its current stat requirements
     */
    @EventHandler
    public void a(LevelUpEvent event) {
        PlayerData.get(event.getPlayer()).resolveModifiersLater();
    }

    @EventHandler
    public void b(LevelDownEvent event) {
        PlayerData.get(event.getPlayer()).resolveModifiersLater();
    }
}
