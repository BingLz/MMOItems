package net.Indyuce.mmoitems.stat.data;

import io.lumine.mythic.lib.version.api.GameProfile;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkullTextureData implements StatData, RandomStatData<SkullTextureData> {

    /**
     * Spigot 1.20.2 introduced a PlayerProfile API which requires
     * to both support PlayerProfile and GameProfile objects as
     * reflection is no longer supported by >1.20.2
     */
    private GameProfile profile;

    public SkullTextureData(GameProfile profile) {
        this.profile = profile;
    }

    @Nullable
    public GameProfile getGameProfile() {
        return profile;
    }

    public void setGameProfile(@Nullable GameProfile profile) {
        this.profile = profile;
    }

    @NotNull
    @Override
    public SkullTextureData clone() {
        return new SkullTextureData(profile);
    }

    @Override
    public boolean isEmpty() {
        return profile == null;
    }

    @Override
    public SkullTextureData randomize(MMOItemBuilder builder) {
        return this;
    }
}