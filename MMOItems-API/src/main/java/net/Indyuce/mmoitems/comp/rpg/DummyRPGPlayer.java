package net.Indyuce.mmoitems.comp.rpg;

import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;

/**
 * Temporarily used by MMOItems when plugins like AureliumSkills don't load player
 * data right on player startup, which means the player data instance can't
 * directly be cached by MMOItems.
 *
 * @author indyuce
 */
public class DummyRPGPlayer extends RPGPlayer {
    public DummyRPGPlayer(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public String getClassName() {
        return "";
    }

    @Override
    public double getMana() {
        return 0;
    }

    @Override
    public double getStamina() {
        return 0;
    }

    @Override
    public void setMana(double value) {

    }

    @Override
    public void setStamina(double value) {

    }
}
