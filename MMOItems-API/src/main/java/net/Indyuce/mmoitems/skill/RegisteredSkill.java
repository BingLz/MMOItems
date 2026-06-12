package net.Indyuce.mmoitems.skill;

import io.lumine.mythic.lib.skill.handler.SkillHandler;

@Deprecated
public class RegisteredSkill {
    private final SkillHandler<?> delegate;

    @Deprecated
    public RegisteredSkill(SkillHandler<?> delegate) {
        this.delegate = delegate;
    }

    @Deprecated
    public SkillHandler<?> getHandler() {
        return delegate;
    }

    @Deprecated
    public String getName() {
        return delegate.getName();
    }

    @Deprecated
    public void setDefaultValue(String modifier, double value) {
        // Nope
    }

    @Deprecated
    public void setName(String modifier, String name) {
        // Nope
    }

    @Deprecated
    public String getParameterName(String modifier) {
        throw new RuntimeException("TODO");
    }

    @Deprecated
    public double getDefaultModifier(String modifier) {
        throw new RuntimeException("TODO");
    }
}
