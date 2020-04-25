package org.spigotmc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import services.BlockUpdateService;
import services.BlockUpdateServiceImpl;
import services.Fileservice;
import services.FileserviceImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


public final class plugin extends JavaPlugin implements CommandExecutor {

    private Player player;
    private Economy economy;
    private ConfigurationSection configurationSection;
    private Fileservice fileservice;
    private BlockUpdateService blockUpdateService;

    private boolean totalBlockLimit;
    private int totalBlockAmountLimit;
    private int claimBlockPrice;
    private List<String> worlds;

    @Override
    public void onEnable() {
        final Logger logger = getLogger();
        fileservice = new FileserviceImpl();
        blockUpdateService = new BlockUpdateServiceImpl();
        logger.info("Loaded services");
        final File f = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder() + "/");
        if(!f.exists()) {
            fileservice.setupPluginDir(f);
            logger.info("Created plugin directories and files");
        }
        else
            logger.info("Loaded plugin directories");
        loadConfig();
        logger.info("Loaded configuration!");
        setupEconomy();
        logger.info("Loaded economy");
        getServer().getPluginManager().registerEvents(new PlayerEventHandler(getWorldedit(), configurationSection), this);
        logger.info("Loaded listeners");
        logger.info("Claimplugin loaded and ready to use!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static WorldEditPlugin getWorldedit(){
        final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if(plugin instanceof WorldEditPlugin) {
            return (WorldEditPlugin) plugin;
        } else {
            return null;
        }
    }

    private void loadConfig() {
        configurationSection = getConfig();
        fileservice.setConfigurationSection(configurationSection);
        worlds = (List<String>) configurationSection.getList("worlds");
        totalBlockLimit = configurationSection.getBoolean("claimblock.totalBlockLimit");
        totalBlockAmountLimit = configurationSection.getInt("claimblock.totalBlockAmountLimit");
        claimBlockPrice = configurationSection.getInt("claimblock.blockPrice");
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        final RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }


    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (label.equalsIgnoreCase("claimadmin")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if(player.hasPermission("claimplugin.admin")) {
                    final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
                    final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

                    if (args.length > 0 && args[0] != null) {
                        if (args[0].equalsIgnoreCase("fixowners")) {
                            player.sendMessage("a");
                            for (final ProtectedRegion region : regionManager.getRegions().values()) {
                                if (region.getId().startsWith("claim_")) {
                                    final String uuid = region.getId().split("_")[1];
                                    final DefaultDomain owners = region.getOwners();
                                    player.sendMessage("uuid: " + uuid + ", region: " + region.getId().split("_")[2]);
                                    owners.addPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
                                    region.setOwners(owners);
                                }
                            }
                        }
                        /**
                         * To get a list of admincommand use the folllowing command
                         * /claimadmin help
                         **/
                        if (args[0].equalsIgnoreCase("help")) {
                            player.sendMessage(ChatColor.YELLOW+"-------------------- " + Configuration.ADMINPREFIX + "--------------------");
                            player.sendMessage(ChatColor.YELLOW+"/claimadmin reload " + ChatColor.WHITE + "- Reloads configuration.");
                            player.sendMessage(ChatColor.YELLOW+"/claimadmin list <playername>" + ChatColor.WHITE + "- List of players claims.");
                            player.sendMessage(ChatColor.YELLOW+"/claimadmin giveclaimblocks <player> <amount>" + ChatColor.WHITE + "- Gives player claimblocks.");
                            player.sendMessage(ChatColor.YELLOW+"/claimadmin removelaimblocks <player> <amount>" + ChatColor.WHITE + "- Removes player claimblocks.");
                            player.sendMessage(ChatColor.YELLOW+"/claimadmin setowner <playername>" + ChatColor.WHITE + "- Sets owner of claim. Must be stood in the claim.");
                            player.sendMessage(ChatColor.YELLOW+"/claimadmin removeowner <playername>" + ChatColor.WHITE + "- Removes owner of claim. Must be stood in the claim.");
                        }
                        /**
                         * Reload configuration
                         **/
                        if (args[0].equalsIgnoreCase("reload")) {
                            reloadConfig();
                            loadConfig();
                            player.sendMessage(Configuration.ADMINPREFIX + "Reloaded.");
                        }
                        /**
                         * List of a player claims
                         * /claimadmin list <playername>
                         **/
                        if (args[0].equalsIgnoreCase("list")) {
                            if(args.length > 0 && args[1] != null) {
                                final List <String> claims = Lists.newArrayList();
                                if(Bukkit.getPlayer(args[1]) != null){
                                    player.sendMessage(Configuration.ADMINPREFIX + args[1] + "'s claims:");
                                    for (final ProtectedRegion region : regionManager.getRegions().values()) {
                                        if (region.getId().contains("claim_" + Bukkit.getPlayer(args[1]).getUniqueId().toString())) {
                                            claims.add(region.getId());
                                            player.sendMessage(Configuration.PREFIX + "Claim: " + region.getId().split(Bukkit.getPlayer(args[1]).getUniqueId().toString() + "_")[1] +
                                                    ": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 256 + " blocks)");
                                        }
                                    }
                                }
                                else
                                    player.sendMessage(Configuration.ADMINPREFIX+"Player " + args[1] + " is not online!");
                            }
                        }
                        /**
                         * Give player claimblocks by using the following command:
                         * /claimadmin giveclaimblocks <playername> <amount>
                         *  Example: /claimadmin giveclaimblocks goppi 500
                         **/
                        if (args[0].equalsIgnoreCase("giveclaimblocks")) {
                            if(args.length > 0 && args[1] != null && args[2] != null) {
                                if(Bukkit.getPlayer(args[1]) != null){
                                    if(StringUtils.isNumeric(args[2])){
                                        final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(Bukkit.getPlayer(args[1])));
                                        final int totalClaimBlocks = (Integer) playerConfig.get("player.totalClaimBlocks");
                                        final int blocks = Integer.parseInt(args[2]);
                                        playerConfig.set("player.totalClaimBlocks", totalClaimBlocks+blocks);
                                        try {
                                            playerConfig.save(new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin")
                                                    .getDataFolder()+"/players/"+Bukkit.getPlayer(args[1]).getUniqueId().toString()+".yml"));
                                            player.sendMessage(Configuration.ADMINPREFIX+args[2]+" blocks added to " + args[1] + ".");
                                        } catch (final IOException e) {
                                            player.sendMessage(Configuration.ADMINPREFIX+"Something went wrong while saving user file. Please investigate.");
                                            e.printStackTrace();
                                        }
                                    } else
                                        player.sendMessage(Configuration.ADMINPREFIX+"Amount must be numeric!");
                                }
                                else
                                    player.sendMessage(Configuration.ADMINPREFIX+"Player " + args[1] + " is not online!");
                            } else
                                player.sendMessage(Configuration.ADMINPREFIX+"To get a list of admin commands use /claimadmin help");
                        }
                        /**
                         * Remove claimblocks from player by using the following command:
                         * /claimadmin removeclaimblocks <playername> <amount>
                         *  Example: /claimadmin removeclaimblocks goppi 500
                         **/
                        if (args[0].equalsIgnoreCase("removeclaimblocks")) {
                            if(args.length > 0 && args[1] != null && args[2] != null) {
                                if(Bukkit.getPlayer(args[1]) != null){
                                    if(StringUtils.isNumeric(args[2])){
                                        final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(Bukkit.getPlayer(args[1])));
                                        final int totalClaimBlocks = (Integer) playerConfig.get("player.totalClaimBlocks");
                                        final int blocks = Integer.parseInt(args[2]);
                                        playerConfig.set("player.totalClaimBlocks", totalClaimBlocks-blocks);
                                        try {
                                            playerConfig.save(new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin")
                                                    .getDataFolder()+"/players/"+Bukkit.getPlayer(args[1]).getUniqueId().toString()+".yml"));
                                            player.sendMessage(Configuration.ADMINPREFIX+args[2]+" blocks removed from " + args[1] + ".");
                                        } catch (final IOException e) {
                                            player.sendMessage(Configuration.ADMINPREFIX+"Something went wrong while saving user file. Please investigate.");
                                            e.printStackTrace();
                                        }
                                    } else
                                        player.sendMessage(Configuration.ADMINPREFIX+"Amount must be numeric!");
                                }
                                else
                                    player.sendMessage(Configuration.ADMINPREFIX+"Player " + args[1] + " is not online!");
                            } else
                                player.sendMessage(Configuration.ADMINPREFIX+"To get a list of admin commands use /claimadmin help");
                        }
                        /**
                         * To set owne of a claim the admin must stand in the claim
                         * and use the /claimadmin setowener <playername>
                         * Example: /claimadmin setowner goppi
                         **/
                        if (args[0].equalsIgnoreCase("setowner")) {
                            if(args[1] != null){
                                final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                                if(!regionList.getRegions().isEmpty())
                                    for (final ProtectedRegion region : regionList) {
                                        if (region.getId().startsWith("claim_")) {
                                            final DefaultDomain owners = region.getOwners();
                                            owners.addPlayer(args[1]);
                                            region.setOwners(owners);
                                            region.getOwners();
                                            player.sendMessage(Configuration.ADMINPREFIX+"Added " + args[1]+ " as owner to " + region.getId() + "!");
                                            return true;
                                        }
                                        player.sendMessage(Configuration.ADMINPREFIX+"Not standing in a claim!");
                                    }
                                else
                                    player.sendMessage(Configuration.ADMINPREFIX + "No claim here cunt. Please move on.");
                            }
                        }
                        if (args[0].equalsIgnoreCase("removeowner")) {
                            if(args[1] != null){
                                final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                                if(!regionList.getRegions().isEmpty())
                                    for (final ProtectedRegion region : regionList) {
                                        if (region.getId().startsWith("claim_")) {
                                            region.getOwners().removePlayer(args[1]);
                                            player.sendMessage(Configuration.ADMINPREFIX+"Removed owner: " + args[1]+ " from " + region.getId() + "!");
                                            return true;
                                        }
                                        player.sendMessage(Configuration.ADMINPREFIX+"Not standing in a claim!");
                                    }
                                else
                                    player.sendMessage(Configuration.ADMINPREFIX + "No claim here cunt. Please move on.");
                            }
                        }
                        if (args[0].equalsIgnoreCase("migratePlayers")) {
                            final File files = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/migrate/players/");
                            int tmp = 0;
                            for(final File file : files.listFiles()) {
                                System.out.println(tmp + "/" + files.listFiles().length + " - Working with file: " + file.getName());
                                int blocks = 0;
                                final BufferedReader reader;
                                try {
                                    reader = new BufferedReader(new FileReader(file));
                                    String line = reader.readLine();
                                    while (line != null) {
                                        if (!line.isEmpty() && line!="" && line != null) {
                                            blocks += Integer.parseInt(line);
                                        }
                                        line = reader.readLine();
                                    }
                                    reader.close();
                                } catch (final IOException e) {
                                    e.printStackTrace();
                                }

                                final File newFile = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/players/"+file.getName()+".yml");
                                try {
                                    newFile.createNewFile();
                                    final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(newFile);
                                    playerConfig.set("player.name", Bukkit.getOfflinePlayer(file.getName()).getName());
                                    playerConfig.set("player.totalClaimBlocks", blocks);
                                    playerConfig.set("player.totalClaimBlocksInUse", 0);
                                    playerConfig.set("player.claims", new ArrayList());
                                    playerConfig.save(newFile);
                                } catch (final Exception e) {
                                }
                                tmp++;
                            }
                            player.sendMessage(Configuration.PREFIX + "Migrated player data.");
                        }
                        if (args[0].equalsIgnoreCase("migrateClaims")) {
                            final File files = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/migrate/claims/");
                            for(final File file : files.listFiles()) {
                                System.out.println("Working with file: " + file.getName()) ;
                                final FileConfiguration claimFile = YamlConfiguration.loadConfiguration(file);
                                final FileConfiguration playerConfig;
                                try {
                                    final String filename = file.getName().replaceFirst("[.][^.]+$", "");
                                    final String lbc = (String) claimFile.get("Lesser Boundary Corner");
                                    final String gbc = (String) claimFile.get("Greater Boundary Corner");
                                    String owner = String.valueOf(claimFile.get("Owner"));
                                    final int parentClaim = (Integer) claimFile.get("Parent Claim ID");
                                    int totalClaimBlocksInUse = 0;
                                    if(owner.toString().equalsIgnoreCase("") && parentClaim != -1 && owner != null) {
                                        final FileConfiguration parentClaimConfig = YamlConfiguration.loadConfiguration(
                                                new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/migrate/claims/"+parentClaim+".yml"));
                                        playerConfig = YamlConfiguration.loadConfiguration(
                                                new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder() + "/players/" + parentClaimConfig.get("Owner") + ".yml"));
                                        totalClaimBlocksInUse = (Integer) playerConfig.get("player.totalClaimBlocksInUse");
                                        owner = String.valueOf(parentClaimConfig.get("Owner"));
                                        final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + parentClaimConfig.get("Owner")+ "_" + filename,
                                                BlockVector3.at(Integer.parseInt(lbc.split(";")[1]), 0, Integer.parseInt(lbc.split(";")[3])),
                                                BlockVector3.at(Integer.parseInt(gbc.split(";")[1]), 255, Integer.parseInt(gbc.split(";")[3])));
                                        totalClaimBlocksInUse += region.volume()/256;
                                        final DefaultDomain owners = region.getOwners();
                                        owners.addPlayer(UUID.fromString(owner));
                                        final DefaultDomain members = region.getMembers();
                                        region.setOwners(owners);
                                        region.setPriority(2);
                                        region.setParent(regionManager.getRegion("claim_"+parentClaimConfig.get("Owner")+"_"+parentClaim));
                                        region.setFlag(Flags.PVP, StateFlag.State.DENY);
                                        regionManager.addRegion(region);
                                    } else {
                                        playerConfig = YamlConfiguration.loadConfiguration(
                                                new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder() + "/players/" + claimFile.get("Owner") + ".yml"));
                                        totalClaimBlocksInUse = (Integer) playerConfig.get("player.totalClaimBlocksInUse");
                                        final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + owner + "_" + filename,
                                                BlockVector3.at(Integer.parseInt(lbc.split(";")[1]), 0, Integer.parseInt(lbc.split(";")[3])),
                                                BlockVector3.at(Integer.parseInt(gbc.split(";")[1]), 255, Integer.parseInt(gbc.split(";")[3])));
                                        totalClaimBlocksInUse += (region.volume()/256);
                                        final DefaultDomain owners = region.getOwners();
                                        owners.addPlayer(UUID.fromString(owner));
                                        final DefaultDomain members = region.getMembers();
                                        region.setOwners(owners);
                                        region.setPriority(1);
                                        regionManager.addRegion(region);
                                    }
                                    final List<String> claims = (List<String>) playerConfig.getList("player.claims");
                                    claims.add("claim_" + owner + "_" + filename);
                                    playerConfig.set("player.claims", claims);
                                    playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse);
                                    playerConfig.save(new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder()+"/players/"+owner+".yml"));
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }

                            }
                            player.sendMessage(Configuration.PREFIX + "Migrated claims.");
                        }
                    }
                } else
                    player.sendMessage(Configuration.PREFIX+"You do not have permission to use this command!");
            }
        }
        if (label.equalsIgnoreCase("claim")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                try {
                    if (args.length > 0 && args[0] != null) {
                        if (worlds.contains(player.getWorld().getName())) {
                            final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(player));
                            playerConfig.set("player.name", player.getName());
                            final int totalClaimBlocks = (Integer) playerConfig.get("player.totalClaimBlocks");
                            final int totalClaimBlocksInUse = (Integer) playerConfig.get("player.totalClaimBlocksInUse");

                            final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
                            final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                            final RegionSelector regionSelector = getWorldedit().getSession(player).getRegionSelector(BukkitAdapter.adapt(player.getWorld()));

                            /**
                             * To remove a claim use the command /claim remove <claimname>
                             **/
                            if (args[0].equalsIgnoreCase("remove")) {
                                if (args.length >= 2 && args[1] != null) {
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]) != null) {
                                        int childClaimsVolume = 0;
                                        for(final ProtectedRegion region : regionManager.getRegions().values())
                                            if(region.getParent() != null && region.getParent().getId().equalsIgnoreCase("claim_" + player.getUniqueId().toString() + "_" + args[1]))
                                                childClaimsVolume += region.volume()/256;
                                        final List<String> claims = (List<String>) playerConfig.getList("player.claims");
                                        final ProtectedRegion region = regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        playerConfig.set("player.claims", claims);
                                        playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - (region.volume() / 256));
                                        fileservice.saveToFile(playerConfig, player);
                                        regionManager.removeRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        blockUpdateService.resetClaimBorder(player, region);
                                        player.sendMessage(Configuration.PREFIX + "Claim " + args[1] + " has been removed!");
                                        return true;
                                    } else {
                                        player.sendMessage(Configuration.PREFIX + "No claim with that name found! " + "claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        return false;
                                    }
                                } else {
                                    player.sendMessage(Configuration.PREFIX + "You need to specify the claim name!");
                                    return false;
                                }
                            }
                            /**
                             * To create a claim, the player must first create a selection
                             * A selection must be made using the Worldedit wand.
                             * When a selection has been made, use the command /claim create <claimname>
                             * to create a new claim with the name claim_<playerName>_claimname
                             **/
                            if (args[0].equalsIgnoreCase("create")) {
                                try {
                                    if (args.length >= 2 && args[1] != null) {
                                        final BlockVector3 p1 = regionSelector.getRegion().getMinimumPoint();
                                        final BlockVector3 p2 = regionSelector.getRegion().getMaximumPoint();
                                        ProtectedRegion parentRegion = null;
                                        final BlockVector2 pos1 = BlockVector2.at(p1.toBlockVector2().getX(),p1.toBlockVector2().getZ());
                                        final BlockVector2 pos2 = BlockVector2.at(p2.toBlockVector2().getX(),p2.toBlockVector2().getZ());
                                        final BlockVector2 pos3 = BlockVector2.at(p1.toBlockVector2().getX(),p2.toBlockVector2().getZ());
                                        final BlockVector2 pos4 = BlockVector2.at(p2.toBlockVector2().getX(),p1.toBlockVector2().getZ());
                                        final List<BlockVector2> points = Lists.newArrayList(pos1, pos2, pos3, pos4);
                                        for (final ProtectedRegion region : regionManager.getRegions().values()) {
                                            if (region.containsAny(points)) {
                                                if(region.contains(points.get(0)) && region.contains(points.get(1))
                                                        && region.contains(points.get(2)) && region.contains(points.get(3))){
                                                    if (region.getOwners().contains(player.getName())) {
                                                        if(region.getParent() == null) {
                                                            parentRegion = region;
                                                            continue;
                                                        } else {
                                                            player.sendMessage(Configuration.PREFIX + "Claim is overlaping a child claim!");
                                                            return false;
                                                        }
                                                    } else {
                                                        player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim! (" + region.getId() + ")");
                                                        return false;
                                                    }
                                                } else {
                                                    player.sendMessage(Configuration.PREFIX + "Claim overlaps with another claim! (" + region.getId().split("_" + player.getUniqueId() + "_")[1] + ")");
                                                    return false;
                                                }
                                            }
                                        }
                                        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]) == (null)) {
                                            final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + player.getUniqueId().toString() + "_" + args[1],
                                                    BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
                                            player.sendMessage(region.getIntersectingRegions(regionManager.getRegions().values())+ "");
                                            final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
                                            if(parentRegion != null) {
                                                region.setParent(parentRegion);
                                                region.setPriority(2);
                                                region.setFlags(parentRegion.getFlags());
                                            } else {
                                                if (overlapingClaims.size() != 0) {
                                                    player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim!");
                                                    return false;
                                                }
                                                region.setPriority(1);
                                            }
                                            final int regionSize = region.volume() / 256;
                                            if((p2.getX() - p1.getX() >= 5 ) && (p2.getZ() - p1.getZ() >= 5)) {
                                                if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
                                                    regionManager.addRegion(region);
                                                    final DefaultDomain owner = region.getOwners();
                                                    owner.addPlayer(player.getName());
                                                    region.setOwners(owner);
                                                    final Map<Flag<?>, Object> map = Maps.newHashMap();
                                                    map.put(Flags.PVP, StateFlag.State.DENY);
                                                    map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
                                                    region.setFlags(map);
                                                    player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
                                                    final List<String> claims = (List<String>) playerConfig.getList("player.claims");
                                                    claims.add("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                                    playerConfig.set("player.claims", claims);
                                                    playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + (region.volume() / 256));
                                                    fileservice.saveToFile(playerConfig, player);
                                                } else
                                                    player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
                                            } else
                                                player.sendMessage(Configuration.PREFIX+"Claim not big enough! Claims must be atleast 6x6 wide.");
                                        } else
                                            player.sendMessage(Configuration.PREFIX + "Claim with that name already exist");
                                    } else
                                        player.sendMessage(Configuration.PREFIX + "You must specify a claim name!");
                                } catch (final Exception ex) {
                                    ex.printStackTrace();
                                }
                                return true;
                            }
                            /**
                             * Get a list of players claims
                             **/
                            if (args[0].equalsIgnoreCase("list")) {
                                final List<ProtectedRegion> playersClaim = Lists.newArrayList();
                                final List <String> claims = Lists.newArrayList();
                                player.sendMessage(Configuration.PREFIX + "Your claims:");
                                for (final ProtectedRegion region : regionManager.getRegions().values()) {
                                    if (region.getId().contains("claim_" + player.getUniqueId().toString())) {
                                        playersClaim.add(region);
                                        claims.add(region.getId());
                                        player.sendMessage(Configuration.PREFIX + "Claim: " + region.getId().split(player.getUniqueId().toString() + "_")[1] +
                                                ": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 256 + " blocks)");
                                    }
                                }
                                playerConfig.set("player.claims", claims);
                                fileservice.saveToFile(playerConfig, player);
                                return true;
                            }
                            /**
                             * To show information about a claim, the player must stand
                             * in the claim and use the /claim info command
                             **/
                            if (args[0].equalsIgnoreCase("info")) {
                                if (args.length >= 2 && args[1] != null) {
                                    final ProtectedRegion region = regionManager.getRegions().get("claim_"+player.getUniqueId()+"_"+args[1]);
                                    if ((region.getOwners().contains(player.getName()) || (region.getOwners().contains(player.getUniqueId())))) {
                                        player.sendMessage(Configuration.PREFIX + "Claim information:");
                                        player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId().split("_" + player.getUniqueId() + "_")[1]);
                                        if(region.getParent() != null)
                                            player.sendMessage(ChatColor.YELLOW + "Claim parent: " + region.getParent().getId().split("_" + player.getUniqueId() + "_")[1]);
                                        player.sendMessage(ChatColor.YELLOW + "Claim coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
                                        String tmp = "";
                                        final Map map = region.getFlags();
                                        for (final Flag flag : region.getFlags().keySet()) {
                                            map.get(flag);
                                            tmp += flag.getName() + ": " + map.get(flag) + "; ";
                                        }
                                        player.sendMessage(ChatColor.YELLOW + "Claim flags: " + tmp);
                                        player.sendMessage(ChatColor.YELLOW + "Claim owner: " + region.getOwners().getPlayers());
                                        player.sendMessage(ChatColor.YELLOW + "Claim members: " + region.getMembers().getPlayers());
                                        return true;
                                    }
                                } else {
                                    final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                                    if(!regionList.getRegions().isEmpty())
                                        for (final ProtectedRegion region : regionList) {
                                            if (region.getId().startsWith("claim_")) {
                                                player.sendMessage(Configuration.PREFIX + "Claim information:");
                                                player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId().substring(43, region.getId().length()));
                                                if (region.getParent() != null)
                                                    player.sendMessage(ChatColor.YELLOW + "Claim parent: " + region.getParent().getId().split("_" + player.getUniqueId() + "_")[1]);
                                                player.sendMessage(ChatColor.YELLOW + "Claim coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
                                                String tmp = "";
                                                final Map map = region.getFlags();
                                                for (final Flag flag : region.getFlags().keySet()) {
                                                    map.get(flag);
                                                    tmp += flag.getName() + ": " + map.get(flag) + "; ";
                                                }
                                                player.sendMessage(ChatColor.YELLOW + "Claim flags: " + tmp);
                                                player.sendMessage(ChatColor.YELLOW + "Claim owner: " + region.getOwners().getPlayers());
                                                player.sendMessage(ChatColor.YELLOW + "Claim members: " + region.getMembers().getPlayers());
                                                return true;
                                            }
                                        }
                                    else
                                        player.sendMessage(Configuration.PREFIX + "No claim here.");
                                }
                                return true;
                            }
                            /**
                             * To add or remove a member from a claim,
                             * the player must use the command
                             * /claim add/removemember <claimName> <playerToRemove>
                             *      Example: /claim addmember house goppi
                             **/
                            if (args[0].equalsIgnoreCase("addmember")) {
                                if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]) != null) {
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]).getMembers().addPlayer(args[2]);
                                        player.sendMessage(Configuration.PREFIX+ "Added " + args[2] + " to the claim!");
                                    } else {
                                        player.sendMessage(Configuration.PREFIX+ "No claim with that name exist!");
                                    }
                                }
                                else
                                    player.sendMessage(Configuration.PREFIX+"To add a member use the following command: /claim addmember <claimname> <player>.");
                                return true;
                            }
                            if (args[0].equalsIgnoreCase("removemember")) {
                                if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]) != null) {
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]).getMembers().removePlayer(args[2]);
                                        player.sendMessage(Configuration.PREFIX+ "Removed " + args[2] + " to the claim!");
                                    } else {
                                        player.sendMessage(Configuration.PREFIX+ "No claim with that name exist!");
                                    }
                                } else
                                    player.sendMessage(Configuration.PREFIX+"To remove a member use the following command: /claim removemember <claimname> <player>.");
                                return true;
                            }
                            /**
                             * To set a falg from a claim the player must
                             * use the /claim set command.
                             * Parameters: /claim setflag <claimname> <flag> <value>
                             *     Example: /claim setflag house pvp deny
                             **/
                            if (args[0].equalsIgnoreCase("setflag")) {
                                if (args.length >= 4 && (args[1] != null && args[2] != null && args[3] != null)) {
                                    final String claimName = args[1];
                                    final String flagName = args[2];
                                    final String flagValue = args[3];
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
                                        StateFlag.State stateFlag = StateFlag.State.DENY;
                                        if (flagValue.equalsIgnoreCase("true") || flagValue.equalsIgnoreCase("allow")) {
                                            stateFlag = StateFlag.State.ALLOW;
                                        }
                                        final Map<Flag<?>, Object> mapFlags = Maps.newHashMap();
                                        mapFlags.putAll(regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getFlags());
                                        if(Configuration.DONATORFLAGS.contains(flagName.toLowerCase())) {
                                            if(player.hasPermission("gpflags.ownerfly")){
                                                if(Configuration.getFlag(flagName) != null) {
                                                    mapFlags.put(Configuration.getFlag(flagName), stateFlag);
                                                } else if(Configuration.getStringFlag(flagName) != null) {
                                                    String message = args[3];
                                                    for (int i = 4; i<args.length; i++)
                                                        message = message + " " + args[i];
                                                    mapFlags.put(Configuration.getStringFlag(flagName), message);
                                                } else{
                                                    player.sendMessage(Configuration.PREFIX+"No such flag!");
                                                    return false;
                                                }
                                            }else {
                                                player.sendMessage(Configuration.PREFIX+"You do not have the fuycking permission! :) cunt sam will shank you with an acid knife m8.");
                                                return false;
                                            }
                                        } else {
                                            if(Configuration.getFlag(flagName) !=(null)) {
                                                mapFlags.put(Configuration.getFlag(flagName), stateFlag);
                                            }else if(Configuration.getStringFlag(flagName) !=(null)) {
                                                String message = args[3];
                                                for (int i = 4; i<args.length; i++)
                                                    message = message + " " + args[i];
                                                mapFlags.put(Configuration.getStringFlag(flagName), message);
                                            }
                                            else {
                                                player.sendMessage(Configuration.PREFIX+"No such flag!");
                                                return false;
                                            }
                                        }
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).setFlags(mapFlags);
                                        player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " set to " + flagValue);
                                    }
                                    else
                                        player.sendMessage(Configuration.PREFIX+"No claim with the name " + claimName + " exists!");
                                }
                                else
                                    player.sendMessage(Configuration.PREFIX+"To set flag use the followwing command: /claim setflag <claimname> <flag> <value>");
                                return true;
                            }
                            /**
                             * To remove a falg from a claim the player must
                             * use the /claim removeflag command.
                             * Parameters: /claim removeflag <claimname> <flag>
                             *     Example: /claim removeflag house pvp
                             **/
                            if (args[0].equalsIgnoreCase("removeflag")) {
                                if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                    final String claimName = args[1];
                                    final String flagName = args[2];
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
                                        Flag flag = null;
                                        if(Configuration.DONATORFLAGS.contains(flagName.toLowerCase())) {
                                            if(player.hasPermission("gpflags.ownerfly")){
                                                if(Configuration.getFlag(flagName) != null) {
                                                    flag = Configuration.getFlag(flagName);
                                                }
                                                if(Configuration.getStringFlag(flagName) != null) {
                                                    flag = Configuration.getStringFlag(flagName);
                                                }
                                            }else {
                                                player.sendMessage(Configuration.PREFIX+"You do not have permission!");
                                                return false;
                                            }
                                        } else {
                                            if(Configuration.getFlag(flagName) != null) {
                                                flag = Configuration.getFlag(flagName);
                                            } else if(Configuration.getStringFlag(flagName) != null) {
                                                flag = Configuration.getStringFlag(flagName);
                                            } else {
                                                player.sendMessage(Configuration.PREFIX+"No such flag!");
                                                return false;
                                            }
                                        }
                                        final Map<Flag<?>, Object> claimFlags = regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getFlags();
                                        claimFlags.remove(flag);
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).setFlags(claimFlags);
                                        player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " removed from: " + claimName);
                                    }
                                }
                                else
                                    player.sendMessage(Configuration.PREFIX+"To remove flag use the followwing command: /claim removeflag <claimname> <flag>");
                                return true;
                            }
                            /**
                             * To buy claimblocks the player must.
                             * Claimblocks will be added to the players total claim blocks in the players .yml file
                             * use the /claim buyclaimblocks command.
                             * Parameters: /claim buyclaimblocks <amount>
                             *     Example: /claim buyclaimblocks 50.
                             **/
                            if (args[0].equalsIgnoreCase("buyclaimblocks")) {
                                if (args.length >= 2 && (args[1] != null)) {
                                    try {
                                        final int blocks = Integer.parseInt(args[1]);
                                        if (totalBlockLimit) {
                                            if ((totalClaimBlocks + blocks) <= totalBlockAmountLimit) {
                                                if (blocks * claimBlockPrice <= economy.getBalance(player)) {
                                                    economy.withdrawPlayer(player, claimBlockPrice * blocks);
                                                    player.sendMessage(Configuration.PREFIX + "You bought " + blocks + " blocks for $" + claimBlockPrice * blocks + ". Your new balance is: $" + economy.getBalance(player));
                                                    playerConfig.set("player.totalClaimBlocks", (Integer) playerConfig.get("player.totalClaimBlocks") + blocks);
                                                    fileservice.saveToFile(playerConfig, player);
                                                } else {
                                                    player.sendMessage(Configuration.PREFIX + "Not enough money to buy that amount of blocks. You need $" + ((claimBlockPrice * blocks) - economy.getBalance(player)) + " more.");
                                                }
                                            } else {
                                                player.sendMessage(Configuration.PREFIX + "Limit reached. You can only buy " + (totalBlockAmountLimit - totalClaimBlocks) + " more blocks.");
                                            }
                                        } else if (blocks * claimBlockPrice <= economy.getBalance(player)) {
                                            economy.withdrawPlayer(player, claimBlockPrice * blocks);
                                            player.sendMessage(Configuration.PREFIX + "You bought " + blocks + " blocks for $" + claimBlockPrice * blocks + ". Your new balance is: $" + economy.getBalance(player));
                                            playerConfig.set("player.totalClaimBlocks", (Integer) playerConfig.get("player.totalClaimBlocks") + blocks);
                                            fileservice.saveToFile(playerConfig, player);
                                        } else {
                                            player.sendMessage(Configuration.PREFIX + "Not enough money to buy that amount of blocks. You need $" + ((claimBlockPrice * blocks) - economy.getBalance(player)) + " more.");
                                        }

                                    } catch (final NumberFormatException nfe) {
                                        player.sendMessage(Configuration.PREFIX + "Amount must be a number!");
                                    }
                                }
                                return true;
                            }
                            /**
                             * To see amount of claimblocks the player have.
                             * use the /claim claimblocks.
                             * Parameters: No parameters
                             *     Example: /claim claimblocks
                             **/
                            if (args[0].equalsIgnoreCase("claimblocks")) {
                                player.sendMessage(Configuration.PREFIX + "You have a total of: " + totalClaimBlocks + " claimblocks. Claimblocks left: " + (totalClaimBlocks - totalClaimBlocksInUse));
                                return true;
                            }
                            /**
                             * To expand a claim the player must
                             * use the /claim expand <amount>.
                             * This will expand the claim the player is currently
                             * standing on in the direction the player is facing.
                             * Parameters: /claim expand <amount>
                             *      Example: /claim expand 20
                             **/
                            if (args[0].equalsIgnoreCase("expand")) {
                                if (args.length >= 2 && (args[1] != null)) {

                                    final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
                                            player.getLocation().getY(), player.getLocation().getZ()));
                                    if(!regionList.getRegions().isEmpty()) {
                                        for (final ProtectedRegion region : regionList) {
                                            if (region.getId().startsWith("claim_"+player.getUniqueId().toString())) {
                                                final BlockVector3 p1 = region.getMinimumPoint();
                                                final BlockVector3 p2 = region.getMaximumPoint();
                                                ProtectedRegion newRegion = null;
                                                if(player.getFacing() == BlockFace.NORTH) {
                                                    newRegion = new ProtectedCuboidRegion(region.getId(), p1.subtract(0,0,Integer.valueOf(args[1])), p2);
                                                } else if(player.getFacing() == BlockFace.SOUTH){
                                                    newRegion = new ProtectedCuboidRegion(region.getId(), p1, p2.add(0,0, Integer.valueOf(args[1])));
                                                } else if(player.getFacing() == BlockFace.WEST){
                                                    newRegion = new ProtectedCuboidRegion(region.getId(), p1.subtract(Integer.valueOf(args[1]),0,0), p2);
                                                } else if(player.getFacing() == BlockFace.EAST) {
                                                    newRegion = new ProtectedCuboidRegion(region.getId(),p1, p2.add(Integer.valueOf(args[1]),0, 0));
                                                } else {
                                                    player.sendMessage(Configuration.PREFIX+"Something went wrong! Please contact an administrator.");
                                                    return false;
                                                }
                                                if(newRegion.getIntersectingRegions(regionManager.getRegions().values()).size() > 0) {
                                                    for (final ProtectedRegion overlapingClaim : newRegion.getIntersectingRegions(regionManager.getRegions().values())) {
                                                        if(overlapingClaim.getParent() == null && overlapingClaim.getId() != newRegion.getId()) {
                                                            if(region.getParent() != null && region.getParent().getId() == overlapingClaim.getId()) {
                                                                player.sendMessage(Configuration.PREFIX+"You can not expand a child claim!");
                                                                return false;
                                                            } else {
                                                                player.sendMessage(Configuration.PREFIX+"Expansion failed! Claim overlaps another claim.");
                                                                return false;
                                                            }
                                                        }
                                                    }
                                                }
                                                final int newVolume = (newRegion.volume()/256) - (region.volume()/256);
                                                if((totalClaimBlocksInUse + newVolume) <= totalClaimBlocks) {
                                                    playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + newVolume);
                                                    for (final ProtectedRegion claim : regionManager.getRegions().values()) {
                                                        if(claim.getParent() != null && claim.getParent().getId().equalsIgnoreCase(region.getId())){
                                                            claim.clearParent();
                                                            claim.setParent(newRegion);
                                                        }
                                                    }
                                                    blockUpdateService.resetClaimBorder(player, region);
                                                    newRegion.copyFrom(region);
                                                    regionManager.removeRegion(region.getId());
                                                    regionManager.addRegion(newRegion);
                                                    fileservice.saveToFile(playerConfig, player);
                                                    player.sendMessage(Configuration.PREFIX + "Claim expanded!");
                                                    return true;
                                                } else
                                                    player.sendMessage(Configuration.PREFIX + "You do not have enough claimblocks! You need " +
                                                            ((totalClaimBlocksInUse+newVolume)-totalClaimBlocks) + " more blocks.");
                                            }
                                        }
                                    }
                                    else {
                                        player.sendMessage(Configuration.PREFIX + "You are not standing in your claim.");
                                    }
                                }
                                return true;
                            }
                            /**
                             * To rename a claim the player must use the following
                             * command: /claim rename <claimName> <newClaimname>
                             *     Example: /claim rename house base
                             **/
                            if (args[0].equalsIgnoreCase("rename")) {
                                if (args.length >= 3 && (args[1] != null)&& (args[2] != null)) {
                                    if(regionManager.getRegion("claim_"+player.getUniqueId().toString()+"_"+args[1]) != null) {
                                        final ProtectedRegion region = regionManager.getRegion("claim_"+player.getUniqueId().toString()+"_"+args[1]);
                                        final ProtectedRegion newRegion = new ProtectedCuboidRegion("claim_" + player.getUniqueId().toString() + "_" + args[2],
                                                region.getMinimumPoint(), region.getMaximumPoint());
                                        for (final ProtectedRegion claim : regionManager.getRegions().values()) {
                                            if(claim.getParent() != null && claim.getParent().getId().equalsIgnoreCase(region.getId())){
                                                claim.clearParent();
                                                claim.setParent(newRegion);
                                            }
                                        }
                                        blockUpdateService.resetClaimBorder(player, region);
                                        newRegion.copyFrom(region);
                                        regionManager.addRegion(newRegion);
                                        regionManager.removeRegion(region.getId());
                                        player.sendMessage(Configuration.PREFIX + "Claim renamed!");
                                        return true;
                                    }
                                    else
                                        player.sendMessage(Configuration.PREFIX + "No claim with that name exists.");
                                } else
                                    player.sendMessage(Configuration.PREFIX + "Invalid command! For help use /claim help");
                                return true;
                            }
                            /**
                             * To get a list of command use the /help command.
                             **/
                            if (args[0].equalsIgnoreCase("help")) {
                                player.sendMessage(ChatColor.YELLOW + "---------------------- " + Configuration.PREFIX + "----------------------");
                                player.sendMessage(ChatColor.YELLOW+"/claim list" + ChatColor.WHITE+ " - List of your claims.");
                                player.sendMessage(ChatColor.YELLOW+"/claim info" + ChatColor.WHITE+ " - Info about the claim you are standing in.");
                                player.sendMessage(ChatColor.YELLOW+"/claim info <claimname>" + ChatColor.WHITE+ " - Info about a specific claim. Must be yours.");
                                player.sendMessage(ChatColor.YELLOW+"/claim claimblocks" + ChatColor.WHITE+ " - Display how many claimblocks you have.");
                                player.sendMessage(ChatColor.YELLOW+"/claim buyclaimblocks <amount>" + ChatColor.WHITE+ " - Buy more claimblocks.");
                                player.sendMessage(ChatColor.YELLOW+"/claim create <claimname>" + ChatColor.WHITE+ " - Create a new claim.");
                                player.sendMessage(ChatColor.YELLOW+"/claim remove <claimname>" + ChatColor.WHITE+ " - Remove a claim.");
                                player.sendMessage(ChatColor.YELLOW+"/claim addmember <claimname> <player>" + ChatColor.WHITE+ " - Add member to your claim.");
                                player.sendMessage(ChatColor.YELLOW+"/claim removemember <claimname> <player>" + ChatColor.WHITE+ " - Remove member from your claim.");
                                player.sendMessage(ChatColor.YELLOW+"/claim flags" + ChatColor.WHITE+ " - Information about flags.");
                                player.sendMessage(ChatColor.YELLOW+"/claim setflag <claimname> <flag> <value>" + ChatColor.WHITE+ " - Set flag to claim.");
                                player.sendMessage(ChatColor.YELLOW+"/claim removeflag <claimname> <flag>" + ChatColor.WHITE+ " - Remove flag from claim.");
                                return true;
                            }
                            player.sendMessage(Configuration.PREFIX+"Invalid command. Type /claim help for a list of commands.");
                        } else {
                            player.sendMessage(Configuration.PREFIX + "You must be in the right world to use this command!");
                        }
                    } else
                        player.sendMessage(Configuration.PREFIX+"Use /claim help for a list of commands!");
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}