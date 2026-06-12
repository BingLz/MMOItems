package net.Indyuce.mmoitems.comp.rpg;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillUseInfo;
import com.herocraftonline.heroes.api.events.ClassChangeEvent;
import com.herocraftonline.heroes.api.events.HeroChangeLevelEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.skill.SkillType;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.StatInstance;
import io.lumine.mythic.lib.damage.AttackHandler;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemRestriction;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.RequiredLevelStat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HeroesHook implements Listener, AttackHandler {
    private final Map<SkillType, DamageType> damages = new HashMap<>();
    private final Map<UUID, Integer> lastMaxMana = new HashMap<>();
    private final Map<UUID, Integer> lastMaxStamina = new HashMap<>();

    public static final ItemStat<?, ?>
            MAX_STAMINA = new DoubleStat("MAX_STAMINA", Material.EMERALD, "Max Stamina", new String[]{"Adds stamina to your max stamina bar"}),
            REQUIRED_SECONDARY_HERO_LEVEL = new RequiredSecondaryLevel();

    public HeroesHook() {
        MythicLib.plugin.getDamage().registerHandler(this);

        damages.put(SkillType.ABILITY_PROPERTY_PHYSICAL, DamageType.PHYSICAL);
        damages.put(SkillType.ABILITY_PROPERTY_MAGICAL, DamageType.MAGIC);
        damages.put(SkillType.ABILITY_PROPERTY_PROJECTILE, DamageType.PROJECTILE);

        MythicLib.plugin.getStats().computeStat("MAX_MANA").addUpdateListener(this::updateMaxManaModifier);
        MythicLib.plugin.getStats().computeStat("MAX_STAMINA").addUpdateListener(this::updateMaxStaminaModifier);
    }

    private void updateMaxManaModifier(StatInstance instance) {
        final var uuid = instance.getMap().getPlayerData().getUniqueId();
        final Integer lastMana = lastMaxMana.get(uuid);
        if (lastMana == null) return;

        final var hero = Heroes.getInstance().getCharacterManager().getHero(instance.getMap().getPlayer());
        final var currentMana = (int) instance.getFinal();

        if (currentMana != lastMana) {
            lastMaxMana.put(uuid, currentMana);
            hero.removeMaxMana("MMOItems");
            hero.addMaxMana("MMOItems", currentMana);
        }
    }

    @NotNull
    private List<DamageType> mapSkillTypes(Set<SkillType> types) {
        var list = new ArrayList<DamageType>();
        for (SkillType type : types) {
            final DamageType found = this.damages.get(type);
            if (found != null) list.add(found);
        }

        // Always, at least, the SKILL damage type
        list.add(DamageType.SKILL);

        return list;
    }

    @Override
    @Nullable
    public AttackMetadata getAttack(EntityDamageEvent event) {
        SkillUseInfo info = Heroes.getInstance().getDamageManager().getSpellTargetInfo(event.getEntity());
        if (info == null || !(info.getCharacter().getEntity() instanceof Player)) return null;

        final Player player = (Player) info.getCharacter().getEntity();
        final DamageMetadata damageMeta = new DamageMetadata(event.getDamage(), mapSkillTypes(info.getSkill().getTypes()));
        return new AttackMetadata(damageMeta, (LivingEntity) event.getEntity(), MMOPlayerData.get(player).getStatMap().cache(EquipmentSlot.MAIN_HAND));
    }

    private void updateMaxStaminaModifier(StatInstance instance) {
        final var uuid = instance.getMap().getPlayerData().getUniqueId();
        final Integer lastStamina = lastMaxStamina.get(uuid);
        if (lastStamina == null) return;

        final var hero = Heroes.getInstance().getCharacterManager().getHero(instance.getMap().getPlayer());
        final var currentStamina = (int) instance.getFinal();

        if (currentStamina != lastStamina) {
            lastMaxStamina.put(uuid, currentStamina);
            hero.removeMaxStamina("MMOItems");
            hero.addMaxStamina("MMOItems", currentStamina);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        final var uuid = event.getPlayer().getUniqueId();
        lastMaxStamina.put(uuid, 0);
        lastMaxMana.put(uuid, 0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final var uuid = event.getPlayer().getUniqueId();
        lastMaxStamina.remove(uuid);
        lastMaxMana.remove(uuid);
    }

    /**
     * Update the player's inventory whenever he levels up
     * since it could change his current stat requirements.
     */
    @EventHandler
    public void a(HeroChangeLevelEvent event) {
        PlayerData.get(event.getHero().getPlayer()).resolveModifiersLater();
    }

    /**
     * Update the player's inventory whenever he changes class
     * since it could change his current stat requirements.
     */
    @EventHandler
    public void b(ClassChangeEvent event) {
        PlayerData.get(event.getHero().getPlayer()).resolveModifiersLater();
    }

    private static class RequiredSecondaryLevel extends RequiredLevelStat implements ItemRestriction {
        public RequiredSecondaryLevel() {
            super("SECONDARY_HERO_LEVEL", Material.EXPERIENCE_BOTTLE, "Secondary Hero Level", new String[]{"Secondary hero level requirement for your item."});
        }

        @Override
        public boolean canUse(RPGPlayer player, NBTItem item, boolean message) {
            final int requirement = item.getInteger(this.getNBTPath());
            if (requirement <= 0) return true;

            final Hero hero = Heroes.getInstance().getCharacterManager().getHero(player.getPlayer());
            final @Nullable HeroClass class2 = hero.getSecondaryClass();
            final int heroSecLevel = class2 == null ? 1 : hero.getHeroLevel(hero.getSecondaryClass());
            if (heroSecLevel >= requirement || player.getPlayer().hasPermission("mmoitems.bypass.level")) return true;

            if (message) {
                Message.NOT_ENOUGH_LEVELS.format(ChatColor.RED).send(player.getPlayer());
                player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.ENTITY_VILLAGER_NO, 1, 1.5f);
            }
            return false;
        }
    }
}