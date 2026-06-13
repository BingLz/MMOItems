package net.Indyuce.mmoitems.stat.type;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.util.annotation.BackwardsCompatibility;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.category.StatCategory;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class ItemStat<R extends RandomStatData<S>, S extends StatData> {
    @NotNull
    private final String id, name, configPath, nbtPath;
    @NotNull
    private final Material material;

    private final String[] lore;
    private final List<String> compatibleTypes;
    private final List<Material> compatibleMaterials;

    @NotNull
    private String[] aliases = {};

    /**
     * The stat can be enabled or not, depending on the server version to
     * prevent from displaying useless editable stats in the edition menu.
     */
    private boolean enabled = true;

    private StatCategory category;

    protected static final int LORE_LINE_WIDTH = 50;

    /**
     * Initializes an item stat
     *
     * @param id        The item stat ID, used internally. Also determines the
     *                  lower case path for config files
     * @param material  The material used to display the stat in the item edition GUI
     * @param name      The stat name which has a translation in the language files
     * @param lore      The stat description used in the edition GUI
     * @param types     Compatible types. See {@link #isCompatible(Type)}
     * @param materials Materials compatible with the item stat (e.g Shield Pattern), any if empty
     */
    public ItemStat(@NotNull String id, @NotNull Material material, @NotNull String name, @Nullable String[] lore, @Nullable String[] types, Material... materials) {
        this.id = id;
        this.material = material;
        this.lore = lore == null ? new String[0] : lore;
        this.compatibleTypes = types == null ? new ArrayList<>() : Arrays.asList(types);
        this.name = name;
        this.compatibleMaterials = Arrays.asList(materials);

        this.configPath = id.toLowerCase().replace("_", "-");
        this.nbtPath = "MMOITEMS_" + id;

        // Version dependency
        if (getClass().isAnnotationPresent(VersionDependant.class)) {
            final VersionDependant implVersion = getClass().getAnnotation(VersionDependant.class);
            if (MythicLib.plugin.getVersion().isUnder(implVersion.version())) disable();
        }

        // [Backwards compatibility]
        // TODO remove with MI7
        if (getClass().isAnnotationPresent(net.Indyuce.mmoitems.util.VersionDependant.class)) {
            final net.Indyuce.mmoitems.util.VersionDependant implVersion = getClass().getAnnotation(net.Indyuce.mmoitems.util.VersionDependant.class);
            if (MythicLib.plugin.getVersion().isUnder(implVersion.version())) disable();
        }
    }

    /**
     * When random stat data is being read from a config file
     *
     * @param object Could be a config section, a string, a string list, etc.
     * @return Random stat data read from config, or throws an IAE
     */
    public abstract R whenInitialized(Object object);

    /**
     * Called when applying a stat onto an mmoitem builder instance. Applies
     * item tags, adds required lines to the item lore, etc.
     *
     * @param item MMOItem builder which must be completed
     * @param data Stat data being applied
     */
    public abstract void whenApplied(@NotNull ItemStackBuilder item, @NotNull S data);

    /**
     * Usually called within <code>whenApplied</code>, this generates the
     * actual NBT tag that will be written onto the item. Reverses
     * <code>fromAppliedNBT()</code> actually.
     * <p></p>
     * Note that only the following types are supported:
     * <p><b>+ </b> Number
     * </p><b>+ </b> String
     * <p><b>+ </b> Boolean
     * </p><b>+ </b> List of any of these
     *
     * @author gunging
     */
    @NotNull
    public abstract ArrayList<ItemTag> getAppliedNBT(@NotNull S data);

    /**
     * Called when the stat item is clicked in the item edition menu
     *
     * @param inv   Inventory clicked
     * @param event Click event
     */
    public abstract void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event);

    /**
     * When inputting data using player input in order to edit the item using
     * the GUI editor. IAE's are handled and exception messages are sent back
     * to the player. Stat edition is not canceled until a right input is given
     * or the player inputs "cancel".
     * <p>
     * This method is called async inside of an AsyncPlayerChatEvent
     *
     * @param inv     Previously opened edition menu
     * @param message Player input
     * @param info    Extra information given by the stat when instanciating
     *                StatEdition given to this method to identify what is being
     *                edited
     */
    public abstract void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info);

    /**
     * Called when stat data is read from an ItemStack in a player inventory
     *
     * @param mmoitem NBTItem being read and transformed into a MMOItem instance
     */
    public abstract void whenLoaded(@NotNull ReadMMOItem mmoitem);

    /**
     * Usually called within <code>whenLoaded</code>, this generates the
     * actual Stat Data from the NBT stored within the item. Reverses
     * <code>getAppliedNBT()</code> actually.
     * <p></p>
     * Shall return null if the tags passed don't provide enough
     * information to generate a usable StatData.
     *
     * @author gunging
     */
    @Nullable
    public abstract S getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags);

    /**
     * Called when stat data is displayed in the edition GUI. We cannot use
     * MMOItemTemplate as input here because we need to know if we are editing a
     * modifier or base item data. It is much easier to display RandomStatData
     * if it exists
     *
     * @param lore     Current item lore which must be completed
     * @param statData Stat data being displayed, optional is empty if there is
     *                 no stat data
     */
    public abstract void whenDisplayed(List<String> lore, Optional<R> statData);

    protected String generalStatFormat;

    @BackwardsCompatibility(version = "6.10")
    public void loadConfiguration(@NotNull ConfigurationSection legacyLanguageFile, @NotNull Object configObject) {
        loadConfiguration(configObject);
    }

    public void loadConfiguration(@NotNull Object configObject) {
        generalStatFormat = configObject.toString();
    }

    @BackwardsCompatibility(version = "6.10")
    public String getLegacyTranslationPath() {
        return getPath();
    }

    public String getGeneralStatFormat() {
        return generalStatFormat;
    }

    @Nullable
    public StatCategory getCategory() {
        return category;
    }

    public void setCategory(@Nullable StatCategory category) {
        this.category = category;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getEditorName() {
        String path = "stat-labels." + getPath() + ".name";
        if (MMOItems.plugin.getLanguage().getAdminLanguage().contains(path))
            return MMOItems.plugin.getLanguage().getAdminLanguage().text(path, name, false);

        String statFormat = MMOItems.plugin.getLanguage().getStatFormat(getPath());
        return statFormat.startsWith("<TranslationNotFound:") ? localizeEditorName(name) : cleanEditorName(statFormat);
    }

    @NotNull
    public String[] getEditorLore() {
        String path = "stat-labels." + getPath() + ".lore";
        return MMOItems.plugin.getLanguage().getAdminLanguage().isList(path) ? MMOItems.plugin.getLanguage().getAdminLanguage().list(path, Arrays.asList(lore), false).toArray(new String[0]) : new String[]{"编辑 " + getEditorName() + " 属性。"};
    }

    @NotNull
    private String localizeEditorName(@NotNull String input) {
        Map<String, String> words = new LinkedHashMap<>();
        words.put("Required", "需求");
        words.put("Additional", "额外");
        words.put("Damage", "伤害");
        words.put("Reduction", "减免");
        words.put("Critical", "暴击");
        words.put("Strike", "");
        words.put("Chance", "几率");
        words.put("Power", "强度");
        words.put("Attack", "攻击");
        words.put("Speed", "速度");
        words.put("Health", "生命");
        words.put("Mana", "法力");
        words.put("Stamina", "耐力");
        words.put("Regeneration", "恢复");
        words.put("Armor", "护甲");
        words.put("Toughness", "韧性");
        words.put("Knockback", "击退");
        words.put("Resistance", "抗性");
        words.put("Movement", "移动");
        words.put("Range", "范围");
        words.put("Cooldown", "冷却");
        words.put("Block", "格挡");
        words.put("Dodge", "闪避");
        words.put("Parry", "招架");
        words.put("Projectile", "弹射物");
        words.put("Physical", "物理");
        words.put("Magic", "魔法");
        words.put("Weapon", "武器");
        words.put("Skill", "技能");
        words.put("Undead", "不死生物");
        words.put("Level", "等级");
        words.put("Class", "职业");
        words.put("Type", "类型");
        words.put("Item", "物品");
        words.put("Effect", "效果");
        words.put("Effects", "效果");
        words.put("Potion", "药水");
        words.put("Permanent", "永久");
        words.put("Permission", "权限");
        words.put("Commands", "指令");
        words.put("Abilities", "技能");
        words.put("Gem", "宝石");
        words.put("Sockets", "槽");
        words.put("Custom", "自定义");
        words.put("Model", "模型");
        words.put("Data", "数据");
        words.put("Display", "显示");
        words.put("Name", "名称");
        words.put("Lore", "描述");
        words.put("Durability", "耐久");
        words.put("Repair", "修复");
        words.put("Consume", "消耗");
        words.put("Restore", "恢复");
        words.put("Food", "饥饿值");
        words.put("Saturation", "饱和度");
        words.put("Soulbound", "灵魂绑定");
        words.put("Hide", "隐藏");
        words.put("Disable", "禁用");
        words.put("Arrow", "箭矢");
        words.put("Particles", "粒子");
        words.put("Tooltip", "工具提示");
        words.put("Style", "样式");
        words.put("Material", "材质");
        words.put("Tier", "品质");
        words.put("Set", "套装");
        words.put("Max", "最大");
        words.put("Min", "最小");
        words.put("Fall", "摔落");
        words.put("Fire", "火焰");
        words.put("Recoil", "后坐力");
        words.put("Lifesteal", "生命窃取");
        words.put("Vampirism", "吸血");
        words.put("Autosmelt", "自动熔炼");
        words.put("Handworn", "手持佩戴");
        words.put("Inedible", "不可食用");
        words.put("Success", "成功");
        words.put("Rate", "几率");
        words.put("Reference", "引用");
        words.put("Restriction", "限制");
        String output = input;
        for (Map.Entry<String, String> entry : words.entrySet())
            output = output.replace(entry.getKey(), entry.getValue());
        output = output.replaceAll("\\s+", "").trim();
        return output.isEmpty() ? input : output;
    }

    @NotNull
    private String cleanEditorName(@NotNull String statFormat) {
        String clean = statFormat.replaceAll("(?i)&[0-9A-FK-ORX]", "")
                .replace("<plus>", "")
                .replaceAll("\\{[^}]+}", "")
                .replaceAll("#([^#]+)#", "")
                .replaceAll("[➸■✠❤◆✖✔>|\\[\\]*]", "")
                .trim();
        int colonIndex = Math.max(clean.indexOf(':'), clean.indexOf('：'));
        if (colonIndex > 0)
            clean = clean.substring(0, colonIndex).trim();
        clean = clean.replaceAll("^\\p{Punct}+", "").trim();
        return clean.isEmpty() ? name : clean;
    }

    /**
     * The internal name of this ItemStat.
     * <p></p>
     * Example, from attack damage: <b>ATTACK_DAMAGE</b>
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Mainly for backwards compatibility. Aliases are basically
     * other string identifiers that point to the same item stat.
     * Useful when changing stat keys inside the item configs.
     * <p>
     * Aliases have to follow the UPPER_CASE stat identifier format.
     */
    @NotNull
    public String[] getAliases() {
        return aliases;
    }

    /**
     * @see #getAliases()
     */
    public void setAliases(String... aliases) {
        this.aliases = aliases;
    }

    /**
     * @return The stat ID
     * @deprecated Use getId() instead. Type is no longer an util since they can
     * now be registered from external plugins
     */
    @Deprecated
    @NotNull
    public String name() {
        return id;
    }

    /**
     * @return Path being used to reference this item stat in MMOItems config files. It's the stat path users are familiar with
     */
    @NotNull
    public String getPath() {
        return configPath;
    }

    /**
     * @return The NBT path used by the stat to save data in an item's NBTTags.
     * The format is 'MMOITEMS_' followed by the stat name in capital
     * letters only using _
     */
    @NotNull
    public String getNBTPath() {
        return nbtPath;
    }

    public Material getDisplayMaterial() {
        return material;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String[] getLore() {
        return lore;
    }

    @NotNull
    public List<String> getCompatibleTypes() {
        return compatibleTypes;
    }

    /**
     * @param type The item type to check
     * @return If a certain item type is compatible with this item stat
     */
    public boolean isCompatible(@NotNull Type type) {

        if (compatibleTypes.isEmpty()) return true;

        // Main rules
        if (type.getModifierSource().isWeapon() && compatibleTypes.contains("weapon")) return true;
        if (type.getModifierSource().isEquipment() && compatibleTypes.contains("equipment")) return true;
        if (type.getModifierSource().isHandheld() && compatibleTypes.contains("handheld")) return true;

        // Supertype/parent/root type
        final String lower = type.getSupertype().getId().toLowerCase();
        return !compatibleTypes.contains("!" + lower) && (compatibleTypes.contains("all") || compatibleTypes.contains(lower));
    }

    public boolean hasValidMaterial(ItemStack item) {
        return compatibleMaterials.isEmpty() || compatibleMaterials.contains(item.getType());
    }

    public void disable() {
        enabled = false;
    }

    /**
     * @deprecated See {@link DoubleStat#formatPath(String, String, boolean, boolean, double, double)}
     */
    @Deprecated
    public String formatNumericStat(double value, String... replace) {
        String format = getGeneralStatFormat().replace("<plus>", value > 0 ? "+" : "");
        for (int j = 0; j < replace.length; j += 2)
            format = format.replace(replace[j], replace[j + 1]);
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStat<?, ?> itemStat = (ItemStat<?, ?>) o;
        return id.equals(itemStat.id);
    }



    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Deprecated
    public static String translate(String path) {
        return MMOItems.plugin.getLanguage().getStatFormat(path);
    }

    /**
     * The default value of this ItemStat.
     * <p></p>
     * Must be blank, easiest examples are <code>DoubleStat</code>s which
     * usually return a <code>new DoubleData(0.0)</code>.
     * <p></p>
     * Used when a gem stone is applied onto an item, but this item did
     * not have the stat provided by the gem stone. In this case, the 'original'
     * value of the item will be given by this.
     * <p></p>
     * Also used to know what kind of data to expect, because it may not be super obvious.
     * <p>Take <code>Commands</code> stat that has a data of <code>CommandListData</code>
     * where one may thing it takes a <code>StringListData</code></p>
     *
     * @author gunging
     */
    @NotNull
    public abstract S getClearStatData();
}