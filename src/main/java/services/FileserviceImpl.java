package services;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FileserviceImpl implements Fileservice {

    private ConfigurationSection configurationSection;

    @Override
    public void setConfigurationSection(final ConfigurationSection configurationSection) {
        this.configurationSection = configurationSection;
    }

    @Override
    public File getPlayerFile(final Player player){
        final File file = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/players/"+player.getUniqueId()+".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
                final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(file);
                playerConfig.set("player.name", player.getName());
                playerConfig.set("player.totalClaimBlocks", configurationSection.getInt("player.startingBlockAmount"));
                playerConfig.set("player.totalClaimBlocksInUse", 0);
                playerConfig.set("player.claims", new ArrayList());
                playerConfig.save(file);
                return file;
            } catch (final Exception e) {
            }
        }
        return file;
    }

    @Override
    public void saveToFile(final FileConfiguration playerConfig, final Player player) {
        try {
            playerConfig.save(getPlayerFile(player));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupPluginDir(final File f) {
        f.mkdir();
        new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder() + "/players/").mkdir();
        final File file = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/config.yml");
        try {
            file.createNewFile();
            final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("player.startingBlockAmount", 0);
            config.set("claimblock.blockPrice", 0);
            config.set("claimblock.totalBlockLimit", false);
            config.set("totalBlockAmountLimit", 0);
            config.set("worlds", Lists.newArrayList("world"));
            config.save(file);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
