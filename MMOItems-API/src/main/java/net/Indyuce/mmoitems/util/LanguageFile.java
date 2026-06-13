package net.Indyuce.mmoitems.util;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Util class to make sure language files are up to date
 *
 * @author jules
 */
public class LanguageFile extends ConfigFile {
    private boolean change;

    public LanguageFile(String name) {
        super(MMOItems.plugin, "/language", name);
    }

    @NotNull
    public String computeTranslation(String path, Supplier<String> defaultTranslation) {
        return computeTranslation(path, defaultTranslation, true);
    }

    @NotNull
    public String computeTranslation(String path, Supplier<String> defaultTranslation, boolean logMissing) {
        @Nullable String found = getConfig().getString(path);
        if (found == null) {
            change = true;
            getConfig().set(path, found = defaultTranslation.get());
            if (logMissing)
                MMOItems.plugin.getLogger().log(Level.SEVERE, "Could not find translation for '" + path + "', generating it");
        }

        return found;
    }

    @NotNull
    public List<String> computeList(String path, Supplier<List<String>> defaultTranslation) {
        return computeList(path, defaultTranslation, true);
    }

    @NotNull
    public List<String> computeList(String path, Supplier<List<String>> defaultTranslation, boolean logMissing) {
        if (!getConfig().isList(path)) {
            change = true;
            getConfig().set(path, defaultTranslation.get());
            if (logMissing)
                MMOItems.plugin.getLogger().log(Level.SEVERE, "Could not find translation list for '" + path + "', generating it");
        }

        return getConfig().getStringList(path);
    }

    /**
     * Only saves if changes have been detected.
     */
    @Override
    public void save() {
        if (change)
            super.save();
    }
}
