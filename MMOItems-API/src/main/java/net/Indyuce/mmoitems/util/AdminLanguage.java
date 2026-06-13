package net.Indyuce.mmoitems.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AdminLanguage {
    private final LanguageFile file;

    public AdminLanguage() {
        file = new LanguageFile("admin");
    }

    @NotNull
    public String text(@NotNull String path, @NotNull String fallback, @NotNull String... replacements) {
        String translated = replace(ChatColor.translateAlternateColorCodes('&', file.computeTranslation(path, () -> fallback)), replacements);
        file.save();
        return translated;
    }

    @NotNull
    public List<String> list(@NotNull String path, @NotNull List<String> fallback, @NotNull String... replacements) {
        List<String> translated = new ArrayList<>();
        for (String line : file.computeList(path, () -> fallback))
            translated.add(textColors(replace(line, replacements)));
        file.save();
        return translated;
    }

    public void save() {
        file.save();
    }

    @NotNull
    private String textColors(@NotNull String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    @NotNull
    private String replace(@NotNull String input, @NotNull String... replacements) {
        String output = input;
        for (int index = 0; index + 1 < replacements.length; index += 2)
            output = output.replace(replacements[index], replacements[index + 1]);
        return output;
    }
}
