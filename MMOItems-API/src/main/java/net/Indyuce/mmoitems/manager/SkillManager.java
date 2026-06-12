package net.Indyuce.mmoitems.manager;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import net.Indyuce.mmoitems.skill.RegisteredSkill;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A HUB for skills for them to be readily available within the plugin
 */
@Deprecated
public class SkillManager {

    @Deprecated
    private static SkillManager INSTANCE;

    @Deprecated
    public static SkillManager getInstance() {
        if (INSTANCE == null) INSTANCE = new SkillManager();
        return INSTANCE;
    }

    @Deprecated
    public RegisteredSkill getSkill(String id) {
        var f = MythicLib.plugin.getSkills().getHandler(id);
        if (f == null) return null;
        return new RegisteredSkill(f);
    }

    @Deprecated
    public RegisteredSkill getSkillOrThrow(String id) {
        return new RegisteredSkill(MythicLib.plugin.getSkills().getHandlerOrThrow(id));
    }

    @Deprecated
    public void registerSkill(RegisteredSkill skill) {
        MythicLib.plugin.getSkills().registerSkillHandler(skill.getHandler());
    }

    @Deprecated
    public boolean hasSkill(String id) {
        return this.getSkill(id) != null;
    }

    @Deprecated
    public Collection<RegisteredSkill> getAll() {
        var list = new ArrayList<RegisteredSkill>();
        for (SkillHandler<?> handler : MythicLib.plugin.getSkills().getHandlers())
            list.add(new RegisteredSkill(handler));
        return list;
    }

    @Deprecated
    public void initialize(boolean clearBefore) {
        // Nothing needed anymore

        // TODO make sure /mm reload reloads ML skills??!
        // TODO has this always been the case?
    }
}
