package net.Indyuce.mmoitems.gui;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.api.util.ui.SilentNumbers;
import io.lumine.mythic.lib.gui.Navigator;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.edition.NewItemEdition;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.gui.edition.ItemEdition;
import net.Indyuce.mmoitems.stat.BrowserDisplayIDX;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ItemBrowser extends MMOItemsInventory {
    private final Map<String, ItemStack> cached = new LinkedHashMap<>();

    @NotNull
    private final Type type;
    private boolean deleteMode;

    // Slots used to display items based on the item type explored
    private static final int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] slotsAlt = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final String CUSTOM_RP_DOWNLOAD_LINK = "https://gitlab.com/phoenix-dvpmt/mmoitems-default-resource-pack/-/archive/main/mmoitems-default-resource-pack-main.zip";

    public ItemBrowser(Navigator navigator, Type type) {
        super(navigator);

        this.type = type;
    }

    public static ItemBrowser of(Player player, Type type) {
        TypeBrowser prev = TypeBrowser.of(player);
        return new ItemBrowser(prev.getNavigator(), type);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        var lang = MMOItems.plugin.getLanguage().getAdminLanguage();
        Inventory inv = Bukkit.createInventory(this, 54, lang.text(deleteMode ? "gui.item-browser.delete-title" : "gui.item-browser.title", deleteMode ? "Delete Mode: {type}" : "Item Explorer: {type}", "{type}", MythicLib.plugin.getAdventureParser().stripColors(type.getName())));

        /*
         * Build cool Item Stacks for buttons and sh
         */
        ItemStack error = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta errorMeta = error.getItemMeta();
        errorMeta.setDisplayName(lang.text("gui.common.error", ChatColor.RED + "- Error -"));
        List<String> errorLore = lang.list("gui.item-browser.error-lore", Arrays.asList("&7&oAn error occurred while", "&7&otrying to generate that item."));
        errorMeta.setLore(errorLore);
        error.setItemMeta(errorMeta);

        ItemStack noItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta noItemMeta = noItem.getItemMeta();
        noItemMeta.setDisplayName(lang.text("gui.common.no-item", ChatColor.RED + "- No Item -"));
        noItem.setItemMeta(noItemMeta);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(lang.text("gui.common.next-page", ChatColor.GREEN + "Next Page"));
        next.setItemMeta(nextMeta);
        setAction(next, "next_page");

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(lang.text("gui.common.back", ChatColor.GREEN + AltChar.rightArrow + " Back"));
        back.setItemMeta(backMeta);
        setAction(back, "back");

        ItemStack create = new ItemStack(new ItemStack(Material.WRITABLE_BOOK));
        ItemMeta createMeta = create.getItemMeta();
        createMeta.setDisplayName(lang.text("gui.item-browser.create-new", ChatColor.GREEN + "Create New"));
        create.setItemMeta(createMeta);
        setAction(create, "create_new");

        ItemStack delete = new ItemStack(new ItemStack(Material.CAULDRON));
        ItemMeta deleteMeta = delete.getItemMeta();
        deleteMeta.setDisplayName(lang.text(deleteMode ? "gui.item-browser.cancel-deletion" : "gui.item-browser.delete-item", ChatColor.RED + (deleteMode ? "Cancel Deletion" : "Delete Item")));
        delete.setItemMeta(deleteMeta);
        setAction(delete, deleteMode ? "cancel_deletion" : "delete_item");

        ItemStack previous = new ItemStack(Material.ARROW);
        ItemMeta previousMeta = previous.getItemMeta();
        previousMeta.setDisplayName(lang.text("gui.common.previous-page", ChatColor.GREEN + "Previous Page"));
        previous.setItemMeta(previousMeta);
        setAction(previous, "previous_page");

        if (type == Type.BLOCK) {
            ItemStack downloadPack = new ItemStack(Material.HOPPER);
            ItemMeta downloadMeta = downloadPack.getItemMeta();
            downloadMeta.setDisplayName(lang.text("gui.item-browser.download-resourcepack.name", ChatColor.GREEN + "Download Default Resourcepack"));
            downloadMeta.setLore(lang.list("gui.item-browser.download-resourcepack.lore", Arrays.asList(ChatColor.LIGHT_PURPLE + "Only seeing stone blocks?", "",
                    ChatColor.RED + "By downloading the default resourcepack you can", ChatColor.RED + "edit the blocks however you want.",
                    ChatColor.RED + "You will still have to add it to your server!")));
            downloadPack.setItemMeta(downloadMeta);
            setAction(downloadPack, "download_resourcepack");
            inv.setItem(45, downloadPack);
        }

        // Get templates of this type
        HashMap<Double, ArrayList<MMOItemTemplate>> templates = BrowserDisplayIDX.select(MMOItems.plugin.getTemplates().getTemplates(type));

        /*
         *  -----------
         *    CALCULATE GUI BOUNDS AND PAGE SIZES
         *
         *      Each display index claims the entire column of items, such that there will be
         *      empty spaces added to fill the inventories.
         *
         *      In Four GUI mode, columns are four slots tall, else they are three slots tall.
         *  -----------
         */
        int[] usedSlots = type.isFourGUIMode() ? slotsAlt : slots;
        int min = (page - 1) * usedSlots.length;
        int max = page * usedSlots.length;
        int n = 0;

        int sc = type.isFourGUIMode() ? 4 : 3;
        int totalSpaceCount = 0;

        for (Map.Entry<Double, ArrayList<MMOItemTemplate>> indexTemplates : templates.entrySet()) {

            // Claim columns
            int totalSpaceAdd = indexTemplates.getValue().size();
            while (totalSpaceAdd > 0) {
                totalSpaceCount += sc;
                totalSpaceAdd -= sc;
            }
        }

        /*
         * Over the page-range currently in use...
         */
        for (int j = min; j < Math.min(max, totalSpaceCount); j++) {
            MMOItemTemplate template = BrowserDisplayIDX.getAt(j, templates);

            // No template here?
            if (template == null) {

                // Set Item
                inv.setItem(usedSlots[n], noItem);

                /*
                 *      Calculate next n from the slots.
                 *
                 *      #1 Adding 7 will give you the slot immediately under
                 *
                 *      #2 If it overflows, subtract 7sc (column space * 7)
                 *         and add one
                 */
                n += 7;
                if (n >= usedSlots.length) {
                    n -= 7 * sc;
                    n++;
                }
                continue;
            }

            // Build item -> any errors?
            final ItemStack item = template.newBuilder(PlayerData.get(playerData).getRPG(), true).build().newBuilder().build();
            if (item == null || item.getType().isAir() || !item.getType().isItem() || item.getItemMeta() == null) {

                // Set Item
                cached.put(template.getId(), error);
                inv.setItem(usedSlots[n], error);

                /*
                 *      Calculate next n from the slots.
                 *
                 *      #1 Adding 7 will give you the slot immediately under
                 *
                 *      #2 If it overflows, subtract 7sc (column space * 7)
                 *         and add one
                 */
                n += 7;
                if (n >= usedSlots.length) {
                    n -= 7 * sc;
                    n++;
                }
                continue;
            }

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add("");

            // Deleting lore?
            if (deleteMode) {
                lore.add(lang.text("gui.item-browser.click-delete-lore", ChatColor.RED + AltChar.cross + " CLICK TO DELETE " + AltChar.cross));
                meta.setDisplayName(lang.text("gui.item-browser.delete-prefix", ChatColor.RED + "DELETE: {item}", "{item}", meta.hasDisplayName() ? meta.getDisplayName() : MMOUtils.getDisplayName(item)));

                // Editing lore?
            } else {
                lore.addAll(lang.list("gui.item-browser.item-actions-lore", Arrays.asList(
                        ChatColor.YELLOW + AltChar.smallListDash + " Left click to obtain this item.",
                        ChatColor.YELLOW + AltChar.smallListDash + " Right click to edit this item.")));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            // Set item
            cached.put(template.getId(), item);
            inv.setItem(usedSlots[n], cached.get(template.getId()));

            /*
             *      Calculate next n from the slots.
             *
             *      #1 Adding 7 will give you the slot immediately under
             *
             *      #2 If it overflows, subtract 7sc (column space * 7)
             *         and add one
             */
            n += 7;
            if (n >= usedSlots.length) {
                n -= 7 * sc;
                n++;
            }
        }

        // Put the buttons
        if (!deleteMode) {
            inv.setItem(51, create);
        }
        inv.setItem(47, delete);
        inv.setItem(49, back);
        inv.setItem(18, page > 1 ? previous : null);
        inv.setItem(26, max >= totalSpaceCount ? null : next);
        for (int i : usedSlots) {
            if (SilentNumbers.isAir(inv.getItem(i))) {
                inv.setItem(i, noItem);
            }
        }
        return inv;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getInventory() != event.getClickedInventory())
            return;

        ItemStack item = event.getCurrentItem();
        if (MMOUtils.isMetaItem(item, false)) {
            String action = getAction(item);

            // Back Button
            if ("back".equals(action))
                getNavigator().popOpen();

            else if ("next_page".equals(action)) {
                page++;
                open();
            } else if ("previous_page".equals(action)) {
                page--;
                open();
            }

            else if ("cancel_deletion".equals(action)) {
                deleteMode = false;
                open();
            } else if ("create_new".equals(action))
                new NewItemEdition(this).enable();

            else if ("delete_item".equals(action)) {
                deleteMode = true;
                open();
            } else if ("download_resourcepack".equals(action)) {
                MythicLib.plugin.getVersion().getWrapper().sendJson(getPlayer(),
                        "[{\"text\":\"Click to download!\",\"color\":\"green\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" + CUSTOM_RP_DOWNLOAD_LINK + "\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":[\"\",{\"text\":\"Click to download resource pack\",\"italic\":true,\"color\":\"white\"}]}}]");
                getPlayer().closeInventory();
            }
        }

        if (!MMOUtils.isMetaItem(item, false))
            return;

        String id = NBTItem.get(item).getString("MMOITEMS_ITEM_ID");
        if (id.equals(""))
            return;

        if (deleteMode) {
            MMOItems.plugin.getTemplates().deleteTemplate(type, id);
            deleteMode = false;
            open();

        } else {
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
                getPlayer().getInventory().addItem(MMOItems.plugin.getItem(type, id, PlayerData.get(player)));
                getPlayer().playSound(getPlayer().getLocation(), Sounds.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 2);
            }

            if (event.getAction() == InventoryAction.PICKUP_HALF)
                new ItemEdition(getNavigator(), MMOItems.plugin.getTemplates().getTemplate(type, id)).open();
        }
    }
}
