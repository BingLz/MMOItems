package net.Indyuce.mmoitems.comp.rpg;

import com.gmail.nossr50.events.experience.McMMOPlayerLevelDownEvent;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.stat.type.DisableStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class McMMOHook implements Listener {

    /**
     * McMMO is a special plugin, it can be used along with other RPG plugins
     * like MMOCore. That stat must be registered even if McMMO is not the main
     * RPG core plugin, therefore the register() method is on the onEnable() and
     * not in the constructor of that class
     */
    public static final ItemStat disableMcMMORepair = new DisableStat("MCMMO_REPAIR", Material.IRON_BLOCK, "Disable McMMO Repair",
            "Players can't repair this with McMMO.");

    @EventHandler(ignoreCancelled = true)
    public void a(McMMOPlayerLevelUpEvent event) {
        PlayerData.get(event.getPlayer()).resolveModifiersLater();
    }

    @EventHandler(ignoreCancelled = true)
    public void b(McMMOPlayerLevelDownEvent event) {
        PlayerData.get(event.getPlayer()).resolveModifiersLater();
    }
}