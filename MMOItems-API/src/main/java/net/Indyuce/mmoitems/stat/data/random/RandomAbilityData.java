package net.Indyuce.mmoitems.stat.data.random;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import io.lumine.mythic.lib.util.lang3.Validate;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.skill.RegisteredSkill;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RandomAbilityData {
	private final SkillHandler<?> ability;
	private final TriggerType triggerType;
	private final Map<String, NumericStatFormula> modifiers = new HashMap<>();
	private final boolean hide;

	public RandomAbilityData(ConfigurationSection config) {
		Validate.isTrue(config.contains("type"), "Missing ability type");
		Validate.isTrue(config.contains("mode"), "Missing ability trigger type/casting mode");

		ability = MythicLib.plugin.getSkills().getHandlerOrThrow(UtilityMethods.enumName(config.getString("type")));
		hide = config.getBoolean("hide");

		String modeFormat = config.getString("mode").toUpperCase().replace("-", "_").replace(" ", "_");
		triggerType = MMOUtils.backwardsCompatibleTriggerType(modeFormat);

		for (String key : config.getKeys(false))
			if (!key.equalsIgnoreCase("mode") && !key.equalsIgnoreCase("type") && ability.getModifiers().contains(key))
				modifiers.put(key, new NumericStatFormula(config.get(key)));
	}

	@Deprecated
	public RandomAbilityData(RegisteredSkill ability, TriggerType triggerType) {
		this(ability.getHandler(), triggerType);
	}

	public RandomAbilityData(SkillHandler<?> ability, TriggerType triggerType) {
		this.ability = ability;
		this.triggerType = triggerType;
		this.hide = false;
	}

	@Deprecated
	public RegisteredSkill getAbility() {
		return new RegisteredSkill(ability);
	}

    @NotNull
    public SkillHandler<?> getSkill() {
        return ability;
    }

	public TriggerType getTriggerType() {
		return triggerType;
	}

	public Set<String> getModifiers() {
		return modifiers.keySet();
	}

	public void setModifier(String path, NumericStatFormula value) {
		modifiers.put(path, value);
	}

	public boolean hasModifier(String path) {
		return modifiers.containsKey(path);
	}

	public NumericStatFormula getModifier(String path) {
		return modifiers.get(path);
	}

	public AbilityData randomize(MMOItemBuilder builder) {
		AbilityData data = new AbilityData(ability, triggerType, hide);
		modifiers.forEach((key, formula) -> data.setModifier(key, formula.calculate(builder.getLevel())));
		return data;
	}
}