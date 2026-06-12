package net.Indyuce.mmoitems;

import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.comp.PhatLootsHook;
import net.Indyuce.mmoitems.listener.*;
import net.Indyuce.mmoitems.listener.option.DroppedItems;
import net.Indyuce.mmoitems.listener.option.SoulboundNoDrop;
import net.Indyuce.mmoitems.util.PluginUtils;
import org.bukkit.Bukkit;

public class MMOItemsBukkit {

    /**
     * Called when MMOItems enables. This registers
     * all the listeners required for MMOItems to run
     */
    public MMOItemsBukkit(MMOItems plugin) {

        Bukkit.getPluginManager().registerEvents(new ItemUse(), plugin);
        Bukkit.getPluginManager().registerEvents(new ItemListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new CustomSoundListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new DurabilityListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new DisableInteractions(), plugin);
        Bukkit.getPluginManager().registerEvents(new BiomeChangeListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new CustomBlockListener(), plugin);
        PluginUtils.hookDependencyIfPresent("PhatLoots", true, pl -> Bukkit.getPluginManager().registerEvents(new PhatLootsHook(pl), plugin));

        if (plugin.getConfig().getBoolean("dropped-items.tier-glow") || plugin.getConfig().getBoolean("dropped-items.hints"))
            Bukkit.getPluginManager().registerEvents(new DroppedItems(plugin.getConfig().getConfigurationSection("dropped-items")), plugin);
        if (plugin.getLanguage().disableRemovedItems)
            Bukkit.getPluginManager().registerEvents(new DisabledItemsListener(plugin), plugin);

        if (!plugin.getConfig().getBoolean("soulbound.can-drop"))
            Bukkit.getPluginManager().registerEvents(new SoulboundNoDrop(), plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> PlayerData.getLoaded().forEach(this::tickOnline), 100, 20);
    }

    public static MMOItemsBukkit bukkitBootstrap(MMOItems plugin) {
        return new MMOItemsBukkit(plugin);
    }

    private void tickOnline(PlayerData playerData) {
        if (playerData.getMMOPlayerData().isPlaying() && !playerData.getPlayer().isDead()) playerData.tick();
    }
}
