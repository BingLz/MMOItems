package net.Indyuce.mmoitems.gui;

import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.gui.Navigator;
import io.lumine.mythic.lib.gui.PluginInventory;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

@Deprecated
public abstract class MMOItemsInventory extends PluginInventory {
    protected static final NamespacedKey ACTION_KEY = new NamespacedKey(MMOItems.plugin, "gui_action");

    public MMOItemsInventory(Navigator navigator) {
        super(navigator);
    }

    protected void setAction(ItemStack item, String action) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
    }

    protected String getAction(ItemStack item) {
        return MMOUtils.isMetaItem(item, false) ? item.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING) : null;
    }

    // DEPRECATED code fix when merging with MI7
    @Override
    public void onClick(InventoryClickEvent event) {
        whenClicked(event);

        ItemStack item = event.getCurrentItem();

        // if inventory is edition inventory
        // then the player can click specific items
        if (!(this instanceof EditionInventory) || event.getInventory() != event.getClickedInventory() || !MMOUtils.isMetaItem(item, false))
            return;

        String action = getAction(item);
        if ("get_item".equals(action) || item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + AltChar.fourEdgedClub + " Get the Item! " + AltChar.fourEdgedClub)) {

            // simply give the item if left click
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
                for (ItemStack drop : player.getInventory().addItem(event.getInventory().getItem(4)).values())
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);

                // this refreshes the item if it's unstackable
                if (NBTItem.get(event.getInventory().getItem(4)).getBoolean("MMOITEMS_UNSTACKABLE")) {
                    ((EditionInventory) this).updateCachedItem();
                    event.getInventory().setItem(4, ((EditionInventory) this).getCachedItem());
                }
            }

            // reroll stats if right click
            else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                for (ItemStack drop : player.getInventory().addItem(event.getInventory().getItem(4)).values())
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);

                ((EditionInventory) this).updateCachedItem();
                event.getInventory().setItem(4, ((EditionInventory) this).getCachedItem());
            }
        }

        // Back Button
        if ("back".equals(action) || item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + AltChar.rightArrow + " Back"))
            getNavigator().popOpen();
    }

    public abstract void whenClicked(InventoryClickEvent event);
}
