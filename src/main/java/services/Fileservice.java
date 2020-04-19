package services;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public interface Fileservice  {

    void setupPluginDir(File f);

    File getPlayerFile(Player player);

    void saveToFile(final FileConfiguration playerConfig, final Player player);

    void setConfigurationSection(final ConfigurationSection configurationSection);


}
