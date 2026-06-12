package net.Indyuce.mmoitems.comp.rpg;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.player.resource.ResourceUpdateReason;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import org.jetbrains.annotations.NotNull;

public class RPGPlayerImpl extends RPGPlayer {

    public RPGPlayerImpl(@NotNull PlayerData playerData) {
        super(playerData);
    }

    @Override
    @Deprecated
    public int getLevel() {
        return MythicLib.plugin.getLevelModule().getLevel(getPlayerData().getMMOPlayerData());
    }

    @Override
    @Deprecated
    public String getClassName() {
        return MythicLib.plugin.getClassModule().getClass(getPlayerData().getMMOPlayerData());
    }

    @Override
    @Deprecated
    public double getMana() {
        return MythicLib.plugin.getManaModule().getMana(getPlayerData().getMMOPlayerData());
    }

    @Override
    @Deprecated
    public double getStamina() {
        return MythicLib.plugin.getManaModule().getStamina(getPlayerData().getMMOPlayerData());
    }

    @Override
    @Deprecated
    public void setMana(double value) {
        MythicLib.plugin.getManaModule().setMana(getPlayerData().getMMOPlayerData(), value, ResourceUpdateReason.OTHER);
    }

    @Override
    @Deprecated
    public void setStamina(double value) {
        MythicLib.plugin.getManaModule().setStamina(getPlayerData().getMMOPlayerData(), value, ResourceUpdateReason.OTHER);
    }
}
