package net.Indyuce.mmoitems.comp.rpg;

import net.Indyuce.mmoitems.comp.mmocore.MMOCoreHook;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public enum RPGPluginEnum {
    MMOCORE("MMOCore", MMOCoreHook.class),
    HEROES("Heroes", HeroesHook.class),
    FABLED("Fabled", FabledHook.class),
    RPGPLAYERLEVELING("RPGPlayerLeveling", RPGPlayerLevelingHook.class),
    RACESANDCLASSES("RacesAndClasses", RacesAndClassesHook.class),
    BATTLELEVELS("BattleLevels", BattleLevelsHook.class),
    MCMMO("mcMMO", McMMOHook.class),
    MCRPG("McRPG", McRPGHook.class),
    AURELIUM_SKILLS("AureliumSkills", AureliumSkillsHook.class),
    AURA_SKILLS("AuraSkills", AuraSkillsHook.class),
    SKILLS("Skills", SkillsHook.class),
    SKILLSPRO("SkillsPro", SkillsProHook.class);

    private final Class<?> pluginClass;
    private final String name;

    RPGPluginEnum(String name, Class<?> pluginClass) {
        this.pluginClass = pluginClass;
        this.name = name;
    }

    @NotNull
    public Object load() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return pluginClass.getDeclaredConstructor().newInstance();
    }

    @NotNull
    public String getName() {
        return name;
    }
}
