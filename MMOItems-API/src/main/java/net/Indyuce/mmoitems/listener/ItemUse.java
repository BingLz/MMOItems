package net.Indyuce.mmoitems.listener;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.event.PlayerAttackEvent;
import io.lumine.mythic.lib.api.event.PlayerClickEvent;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import io.lumine.mythic.lib.damage.MeleeAttackMetadata;
import io.lumine.mythic.lib.entity.ProjectileMetadata;
import io.lumine.mythic.lib.entity.ProjectileType;
import io.lumine.mythic.lib.skill.SimpleSkill;
import io.lumine.mythic.lib.skill.SkillMetadata;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.event.item.SpecialWeaponAttackEvent;
import net.Indyuce.mmoitems.api.interaction.*;
import net.Indyuce.mmoitems.api.interaction.projectile.ArrowParticles;
import net.Indyuce.mmoitems.api.interaction.weapon.Weapon;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemUse implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void rightClickEffects(PlayerClickEvent event) {

        // NPCs sometimes randomly calling click events
        final var playerData = PlayerData.getOrNull(event.getPlayer());
        if (playerData == null) return;

        final var eventItem = resolveEventItem(event); // [WTF BUKKIT] When hitting entities, `event.getItem()` is set to `null`
        if (UtilityMethods.isAir(eventItem)) return; // No interaction with air
        final var item = NBTItem.get(eventItem);  // Copy all NBTTags of item

        /*
         * Disabled items
         *
         * Cancel event and return if item has been removed
         * from the MMOItems registry.
         */
        if (MMOUtils.hasBeenRemoved(item)) {
            event.setCancelled(true);
            return;
        }

        /*
         * Disables both clicks if corresponding option is found on the item.
         * Also disabled interactions with unidentified items.
         *
         * This does NOT prevent further MMOItems interactions, which is why this
         * flag set is located here and not in another event listener.
         */
        if (item.getBoolean("MMOITEMS_DISABLE_INTERACTION") || item.hasTag("MMOITEMS_UNIDENTIFIED_ITEM"))
            event.setCancelled(true);

        final Type itemType = Type.get(item);
        if (itemType == null) return;

        /*
         * Some consumables must be fully eaten through the vanilla eating
         * animation and are handled there {@link #handleVanillaEatenConsumables(PlayerItemConsumeEvent)}
         */
        final Player player = event.getPlayer();
        final UseItem useItem = itemType.toUseItem(playerData, item);
        if (useItem instanceof Consumable) {

            // Vanilla eating is handled within another event
            if (((Consumable) useItem).hasVanillaEating()) return;

            // Disable when clicking on interactable blocks
            if (event.hasBlock()
                    && MMOItems.plugin.getLanguage().disableConsumableBlockClicks
                    && MMOUtils.isInteractable(event.getClickedBlock())) return;
        }

        // Disable most interactions (shield blocking, eating...)
        if (!useItem.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        // Commands & Consumables
        final boolean rightClick = !event.isLeftClick();
        if (rightClick) {
            if (useItem.getPlayerData().getMMOPlayerData().getCooldownMap().isOnCooldown(useItem.getMMOItem())) {
                final double cd = useItem.getPlayerData().getMMOPlayerData().getCooldownMap().getCooldown(useItem.getMMOItem());
                Message.ITEM_ON_COOLDOWN.format(ChatColor.RED, "#left#", MythicLib.plugin.getMMOConfig().decimal.format(cd), "#s#", cd >= 2 ? "s" : "").send(player);
                event.setCancelled(true);
                return;
            }

            if (useItem instanceof Consumable) {
                event.setCancelled(true);
                Consumable.ConsumableConsumeResult result = ((Consumable) useItem).useOnPlayer(event.getHand().toBukkit(), false);
                if (result == Consumable.ConsumableConsumeResult.CANCEL) return;

                else if (result == Consumable.ConsumableConsumeResult.CONSUME)
                    eventItem.setAmount(eventItem.getAmount() - 1);
            }

            useItem.getPlayerData().getMMOPlayerData().getCooldownMap().applyCooldown(useItem.getMMOItem(), useItem.getNBTItem().getStat("ITEM_COOLDOWN"));
            if (MMOItems.plugin.getLanguage().itemCommands) useItem.executeCommands();
        }

        // Target-free weapon effects
        if (useItem instanceof Weapon)
            ((Weapon) useItem).handleUntargetedAttack(rightClick, event.getHand());
    }

    @Nullable
    private static ItemStack resolveEventItem(PlayerClickEvent event) {
        if (event.hasItem()) return event.getItem();
        return event.getPlayer().getInventory().getItem(event.getHand().toBukkit());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void meleeAttacks(PlayerAttackEvent event) {

        // Make sure it's a melee attack
        if (!(event.getAttack() instanceof MeleeAttackMetadata)) return;

        final var attackMeta = (MeleeAttackMetadata) event.getAttack();
        final var player = event.getAttacker().getPlayer();
        final var weaponUsed = player.getInventory().getItem(event.getAttacker().getActionHand().toBukkit());
        final var nbtItem = MythicLib.plugin.getVersion().getWrapper().getNBTItem(weaponUsed);

        /*
         * Disabled items
         *
         * Cancel event and return if item has been removed
         * from the MMOItems registry.
         */
        if (MMOUtils.hasBeenRemoved(nbtItem)) {
            event.setCancelled(true);
            return;
        }

        final var itemType = Type.get(nbtItem);
        if (itemType == null || itemType == Type.BLOCK) return;

        // Prevent melee attacks with non-melee weapons
        if (!itemType.hasMeleeAttacks()) {
            event.setCancelled(true);
            return;
        }

        // Check item requirements
        final var playerData = PlayerData.get(player);
        final var weapon = new Weapon(playerData, nbtItem);
        if (!weapon.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        // Apply melee attack
        if (!weapon.handleTargetedAttack(attackMeta, event.getAttacker(), event.getEntity(), event))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void specialToolAbilities(BlockBreakEvent event) {
        if (UtilityMethods.isFake(event)) return;

        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(player.getInventory().getItemInMainHand());
        if (!item.hasType()) return;

        Tool tool = new Tool(PlayerData.get(player), item);
        if (!tool.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        if (tool.miningEffects(block)) event.setCancelled(true);
    }

    @EventHandler
    public void rightClickWeaponInteractions(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof LivingEntity)) return;

        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(player.getInventory().getItem(event.getHand()));
        final Type itemType = Type.get(item);
        if (itemType == null) return;

        final LivingEntity target = (LivingEntity) event.getRightClicked();
        if (!UtilityMethods.canTarget(player, target, InteractionType.OFFENSE_ACTION)) return;

        /*
         * Checks for usability
         *
         * This needs to be a silent check as Spigot fires an interact event simultaneously.
         * The non-silent check is implemented in another handler. Fixes MMOItems#1680
         */
        final UseItem usableItem = itemType.toUseItem(player, item);
        if (!usableItem.checkItemRequirements(false)) return;

        // Apply type-specific entity interactions
        final SkillHandler<?> onEntityInteract = usableItem.getMMOItem().getType().onEntityInteract();
        if (onEntityInteract != null) {
            SpecialWeaponAttackEvent called = new SpecialWeaponAttackEvent(usableItem.getPlayerData(), (Weapon) usableItem, target);
            Bukkit.getPluginManager().callEvent(called);
            if (!called.isCancelled())
                new SimpleSkill(onEntityInteract).cast(SkillMetadata.of(usableItem.getPlayerData().getMMOPlayerData(), target));
        }
    }

    // TODO: Rewrite this with a custom 'ApplyMMOItemEvent'?
    @EventHandler
    public void gemStonesAndItemStacks(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        if (event.getAction() != InventoryAction.SWAP_WITH_CURSOR) return;

        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCursor());
        final Type type = Type.get(item);
        if (type == null) return;

        final UseItem useItem = type.toUseItem(player, item);
        if (!useItem.checkItemRequirements()) return;

        if (useItem instanceof ItemSkin) {
            NBTItem picked = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCurrentItem());
            if (!picked.hasType()) return;

            ItemSkin.ApplyResult result = ((ItemSkin) useItem).applyOntoItem(picked, Type.get(picked.getType()));
            if (result.getType() == ItemSkin.ResultType.NONE) return;

            event.setCancelled(true);
            item.getItem().setAmount(item.getItem().getAmount() - 1);

            if (result.getType() == ItemSkin.ResultType.FAILURE) return;

            event.setCurrentItem(result.getResult());
        }

        if (useItem instanceof GemStone) {
            NBTItem picked = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCurrentItem());
            if (!picked.hasType()) return;

            GemStone.ApplyResult result = ((GemStone) useItem).applyOntoItem(picked, Type.get(picked.getType()));
            if (result.getType() == GemStone.ResultType.NONE) return;

            event.setCancelled(true);
            item.getItem().setAmount(item.getItem().getAmount() - 1);

            if (result.getType() == GemStone.ResultType.FAILURE) return;

            event.setCurrentItem(result.getResult());
        }

        if (useItem instanceof Consumable && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR)
            if (((Consumable) useItem).useOnItem(event, MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getCurrentItem()))) {
                event.setCancelled(true);
                event.getCursor().setAmount(event.getCursor().getAmount() - 1);
            }
    }

    /**
     * This handler registers arrows from custom MMOItems bows
     */
    @EventHandler
    public void handleCustomBows(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow) || !(event.getEntity() instanceof Player)) return;

        final NBTItem item = NBTItem.get(event.getBow());
        final Type type = Type.get(item.getType());

        if (type != null) {
            final var playerData = PlayerData.get((Player) event.getEntity());
            final var weapon = new Weapon(playerData, item);
            if (!weapon.checkItemRequirements() || !weapon.checkAndApplyWeaponCosts()) {
                event.setCancelled(true);
                return;
            }

            final var damageTypes = type.getAttackDamageTypes();
            final var bowSlot = EquipmentSlot.fromBukkit(MMOUtils.getHand(event, playerData.getPlayer()));
            final var shooterMeta = playerData.getMMOPlayerData().getStatMap().cache(bowSlot);
            final var proj = ProjectileMetadata.create(shooterMeta, damageTypes, ProjectileType.ARROW, event.getProjectile());
            proj.setSourceItem(item);
            proj.setCustomDamage(true);
            proj.setDamageMultiplier(MMOUtils.getForce(event));
            if (item.hasTag("MMOITEMS_ARROW_PARTICLES"))
                new ArrowParticles((AbstractArrow) event.getProjectile(), item);
            final AbstractArrow arrow = (AbstractArrow) event.getProjectile();

            // Apply arrow velocity
            final double arrowVelocity = proj.getShooter().getStat("ARROW_VELOCITY");
            if (arrowVelocity > 0) arrow.setVelocity(arrow.getVelocity().multiply(arrowVelocity));
        }
    }

    /**
     * Consumables which can be eaten using the
     * vanilla eating animation are handled here.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleVanillaEatenConsumables(PlayerItemConsumeEvent event) {
        final NBTItem item = MythicLib.plugin.getVersion().getWrapper().getNBTItem(event.getItem());
        final Type itemType = Type.get(item);
        if (itemType == null) return;

        Player player = event.getPlayer();
        UseItem useItem = itemType.toUseItem(player, item);
        if (!useItem.checkItemRequirements()) {
            event.setCancelled(true);
            return;
        }

        if (useItem instanceof Consumable) {

            if (useItem.getPlayerData().getMMOPlayerData().getCooldownMap().isOnCooldown(useItem.getMMOItem())) {
                final double cd = useItem.getPlayerData().getMMOPlayerData().getCooldownMap().getCooldown(useItem.getMMOItem());
                Message.ITEM_ON_COOLDOWN.format(ChatColor.RED, "#left#", MythicLib.plugin.getMMOConfig().decimal.format(cd), "#s#", cd >= 2 ? "s" : "").send(player);
                event.setCancelled(true);
                return;
            }

            org.bukkit.inventory.EquipmentSlot consumeSlot = MMOUtils.getHand(event);
            Consumable.ConsumableConsumeResult result = ((Consumable) useItem).useOnPlayer(consumeSlot, true);

            // No effects are applied and not consumed
            if (result == Consumable.ConsumableConsumeResult.CANCEL) {
                event.setCancelled(true);
                return;
            }

            // Item is not consumed but its effects are applied anyways
            if (result == Consumable.ConsumableConsumeResult.NOT_CONSUME) event.setCancelled(true);

            useItem.getPlayerData().getMMOPlayerData().getCooldownMap().applyCooldown(useItem.getMMOItem(), useItem.getNBTItem().getStat("ITEM_COOLDOWN"));
            if (MMOItems.plugin.getLanguage().itemCommands) useItem.executeCommands();
        }
    }
}
