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
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public final class plugin extends JavaPlugin implements CommandExecutor {

    private static final String prefix = ChatColor.WHITE+"["+ChatColor.YELLOW+"Claim"+ChatColor.WHITE+"] "+ChatColor.YELLOW;
    private Player player;
    private Economy economy;
    private ConfigurationSection configurationSection;

    private boolean totalBlockLimit;
    private int totalBlockAmountLimit;
    private int claimBlockPrice;
    private List<String> worlds;

    private static final List<String> donatorFlags = Lists.newArrayList("fly", "greeting-title", "farewell-titel", "time-lock");

    @Override
    public void onEnable() {
        // Plugin startup logic
        final File f = new File(Bukkit.getServer().getPluginManager().getPlugin("Claimplugin").getDataFolder() + "/");
        if(!f.exists()){
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
        loadConfig();
        setupEconomy();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (label.equalsIgnoreCase("claimadmin")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if(player.hasPermission("claimplugin.admin")) {
                    if (args[0] != null) {
                        /**
                         * Reload configuration
                         **/
                        if (args[0].equalsIgnoreCase("reload")) {
                            reloadConfig();
                            loadConfig();
                            player.sendMessage(prefix + "Reloaded.");
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
                            player.sendMessage(prefix + "Migrated player data.");
                        }
                        if (args[0].equalsIgnoreCase("migrateClaims")) {
                            final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
                            final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
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
                                        region.setFlag(Flags.BUILD, StateFlag.State.DENY);
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
                            player.sendMessage(prefix + "Migrated claims.");
                        }
                    }
                } else
                    player.sendMessage(prefix+"You do not have permission to use this command!");
            }
        }
        if (label.equalsIgnoreCase("claim")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                try {
                    if (args[0] != null) {
                        if (worlds.contains(player.getWorld().getName())) {
                            final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(getPlayerFile(player));
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
                                        final List<String> claims = (List<String>) playerConfig.getList("player.claims");
                                        if (claims.contains("claim_" + player.getUniqueId().toString() + "_" + args[1])) {
                                            claims.remove("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        }
                                        final ProtectedRegion region = regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        playerConfig.set("player.claims", claims);
                                        playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - (region.volume() / 256));
                                        saveToFile(playerConfig, player);
                                        regionManager.removeRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        player.sendMessage(prefix + "Claim " + args[1] + " has been removed!");
                                        return true;
                                    } else {
                                        player.sendMessage(prefix + "No claim with that name found! " + "claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                        return false;
                                    }
                                } else {
                                    player.sendMessage(prefix + "You need to specify the claim name!");
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
                                        for (final ProtectedRegion region : regionManager.getRegions().values()) {
                                            final BlockVector2 pos1 = BlockVector2.at(p1.toBlockVector2().getX(),p1.toBlockVector2().getZ());
                                            final BlockVector2 pos2 = BlockVector2.at(p2.toBlockVector2().getX(),p2.toBlockVector2().getZ());
                                            final BlockVector2 pos3 = BlockVector2.at(p1.toBlockVector2().getX(),p2.toBlockVector2().getZ());
                                            final BlockVector2 pos4 = BlockVector2.at(p2.toBlockVector2().getX(),p1.toBlockVector2().getZ());
                                            final List<BlockVector2> points = Lists.newArrayList(pos1, pos2, pos3, pos4);
                                            if (region.containsAny(points)) {
                                                if(region.contains(points.get(0)) && region.contains(points.get(1))
                                                        && region.contains(points.get(2)) && region.contains(points.get(3))){
                                                    if (region.getOwners().contains(player.getName())) {
                                                        if(region.getParent() == null) {
                                                            parentRegion = region;
                                                            continue;
                                                        } else {
                                                            player.sendMessage(prefix + "Claim is overlaping a child claim!");
                                                            return false;
                                                        }
                                                    } else {
                                                        player.sendMessage(prefix + "Claim is overlaping with another claim! (" + region.getId() + ")");
                                                        return false;
                                                    }
                                                } else {
                                                    player.sendMessage(prefix + "Claim overlaps with another claim! (" + region.getId().split("_" + player.getUniqueId() + "_")[1] + ")");
                                                    return false;
                                                }
                                            }
                                        }
                                        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[1]) == (null)) {
                                            final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + player.getUniqueId().toString() + "_" + args[1],
                                                    BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
                                            if(parentRegion != null) {
                                                region.setParent(parentRegion);
                                                region.setPriority(2);
                                                region.setFlags(parentRegion.getFlags());
                                            } else
                                                region.setPriority(1);
                                            final int regionSize = region.volume() / 256;
                                            if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
                                                regionManager.addRegion(region);
                                                final DefaultDomain owner = region.getOwners();
                                                owner.addPlayer(player.getName());
                                                region.setOwners(owner);
                                                player.sendMessage(prefix + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
                                                final List<String> claims = (List<String>) playerConfig.getList("player.claims");
                                                claims.add("claim_" + player.getUniqueId().toString() + "_" + args[1]);
                                                playerConfig.set("player.claims", claims);
                                                playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + (region.volume() / 256));
                                                saveToFile(playerConfig, player);
                                            } else {
                                                player.sendMessage(prefix + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse+regionSize)-totalClaimBlocks) + " blocks more!");
                                            }
                                        } else {
                                            player.sendMessage(prefix + "Claim with that name already exist");
                                        }
                                    } else {
                                        player.sendMessage(prefix + "You must specify a claim name!");
                                    }
                                } catch (final Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            /**
                             * Get a list of players claims
                             **/
                            if (args[0].equalsIgnoreCase("list")) {
                                final List<ProtectedRegion> playersClaim = Lists.newArrayList();
                                final List <String> claims = Lists.newArrayList();
                                player.sendMessage(prefix + "Your claims:");
                                for (final ProtectedRegion region : regionManager.getRegions().values()) {
                                    if (region.getId().contains("claim_" + player.getUniqueId().toString())) {
                                        playersClaim.add(region);
                                        claims.add(region.getId());
                                        player.sendMessage(prefix + "Claim: " + region.getId().split(player.getUniqueId().toString() + "_")[1] +
                                                ": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 256 + " blocks)");
                                    }
                                }
                                playerConfig.set("player.claims", claims);
                                saveToFile(playerConfig, player);
                            }
                            /**
                             * To show information about a claim, the player must stand
                             * in the claim and use the /claim info command
                             **/
                            if (args[0].equalsIgnoreCase("info")) {
                                if (args.length >= 2 && args[1] != null) {
                                    final ProtectedRegion region = regionManager.getRegions().get("claim_"+player.getUniqueId()+"_"+args[1]);
                                    if ((region.getOwners().contains(player.getName()) || (region.getOwners().contains(player.getUniqueId())))) {
                                        player.sendMessage(prefix + "Claim information:");
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
                                    for (final ProtectedRegion region : regionList) {
                                        if (region.getOwners().contains(player.getName())) {
                                            player.sendMessage(prefix + "Claim information:");
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
                                    }
                                }
                            }
                            /**
                             * To add or remove a member from a claim,
                             * the player must use the command
                             * /claim add/removemember <playerToRemove> <claimName>
                             *      Example: /claim addmember goppi house
                             **/
                            if (args[0].equalsIgnoreCase("addmember")) {
                                if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[2]) != null) {
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[2]).getMembers().addPlayer(args[1]);
                                        player.sendMessage(ChatColor.YELLOW + "Added " + args[1] + " to the claim!");
                                    } else {
                                        player.sendMessage(ChatColor.YELLOW + "No claim with that name exist!");
                                    }
                                }
                            }
                            if (args[0].equalsIgnoreCase("removemember")) {
                                if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                    if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[2]) != null) {
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + args[2]).getMembers().removePlayer(args[1]);
                                        player.sendMessage(ChatColor.YELLOW + "Removed " + args[1] + " to the claim!");
                                    } else {
                                        player.sendMessage(ChatColor.YELLOW + "No claim with that name exist!");
                                    }
                                }
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
                                        if(donatorFlags.contains(flagName.toLowerCase())) {
                                            if(player.hasPermission("gpflags.ownerfly")){
                                                if(getDonatorFlag(flagName) != null) {
                                                    mapFlags.put(getDonatorFlag(flagName), stateFlag);
                                                } else if(getDonatorStringFlag(flagName) != null) {
                                                    String message = args[3];
                                                    for (int i = 4; i<args.length; i++)
                                                        message = message + " " + args[i];
                                                    mapFlags.put(getDonatorStringFlag(flagName), message);
                                                } else{
                                                    player.sendMessage(prefix+"No such flag!");
                                                    return false;
                                                }
                                            }else {
                                                player.sendMessage(prefix+"You do not have the fuycking permission! :) cunt sam will shank you with an acid knife m8.");
                                                return false;
                                            }
                                        } else {
                                            if(getFlag(flagName) !=(null)) {
                                                mapFlags.put(getFlag(flagName), stateFlag);
                                            }else if(getStringFlag(flagName) !=(null)) {
                                                String message = args[3];
                                                for (int i = 4; i<args.length; i++)
                                                    message = message + " " + args[i];
                                                mapFlags.put(getStringFlag(flagName), message);
                                            }
                                            else{
                                                player.sendMessage(prefix+"No such flag!");
                                                return false;
                                            }
                                        }
                                        player.sendMessage("stateflag;: " + stateFlag.toString());
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).setFlags(mapFlags);
                                        player.sendMessage(prefix + "Flag " + flagName + " set to " + flagValue);
                                    }
                                }
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
                                        if(donatorFlags.contains(flagName.toLowerCase())) {
                                            if(player.hasPermission("gpflags.ownerfly")){
                                                if(getDonatorFlag(flagName) != null) {
                                                    flag = getDonatorFlag(flagName);
                                                }
                                                if(getDonatorStringFlag(flagName) != null) {
                                                    flag = getDonatorStringFlag(flagName);
                                                }
                                            }else {
                                                player.sendMessage(prefix+"You do not have the fuycking permission! :) cunt sam will shank you with an acid knife m8.");
                                                return false;
                                            }
                                        } else {
                                            if(getFlag(flagName) != null) {
                                                flag = getFlag(flagName);
                                            } else if(getStringFlag(flagName) != null) {
                                                flag = getStringFlag(flagName);
                                            } else {
                                                player.sendMessage(prefix+"No such flag!");
                                                return false;
                                            }
                                        }
                                        final Map<Flag<?>, Object> claimFlags = regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getFlags();
                                        claimFlags.remove(flag);
                                        regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).setFlags(claimFlags);
                                        player.sendMessage(prefix + "Flag " + flagName + " removed from: " + claimName);
                                    }
                                }
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
                                                    player.sendMessage(prefix + "You bought " + blocks + " blocks for $" + claimBlockPrice * blocks + ". Your new balance is: $" + economy.getBalance(player));
                                                    playerConfig.set("player.totalClaimBlocks", (Integer) playerConfig.get("player.totalClaimBlocks") + blocks);
                                                    saveToFile(playerConfig, player);
                                                } else {
                                                    player.sendMessage(prefix + "Not enough money to buy that amount of blocks. You need $" + ((claimBlockPrice * blocks) - economy.getBalance(player)) + " more.");
                                                }
                                            } else {
                                                player.sendMessage(prefix + "Limit reached. You can only buy " + (totalBlockAmountLimit - totalClaimBlocks) + " more blocks.");
                                            }
                                        } else if (blocks * claimBlockPrice <= economy.getBalance(player)) {
                                            economy.withdrawPlayer(player, claimBlockPrice * blocks);
                                            player.sendMessage(prefix + "You bought " + blocks + " blocks for $" + claimBlockPrice * blocks + ". Your new balance is: $" + economy.getBalance(player));
                                            playerConfig.set("player.totalClaimBlocks", (Integer) playerConfig.get("player.totalClaimBlocks") + blocks);
                                            saveToFile(playerConfig, player);
                                        } else {
                                            player.sendMessage(prefix + "Not enough money to buy that amount of blocks. You need $" + ((claimBlockPrice * blocks) - economy.getBalance(player)) + " more.");
                                        }

                                    } catch (final NumberFormatException nfe) {
                                        player.sendMessage(prefix + "Amount must be a number!");
                                    }
                                }
                            }
                            /**
                             * To see amount of claimblocks the player have.
                             * use the /claim claimblocks.
                             * Parameters: No parameters
                             *     Example: /claim claimblocks
                             **/
                            if (args[0].equalsIgnoreCase("claimblocks")) {
                                player.sendMessage(prefix + "You have a total of: " + totalClaimBlocks + " claimblocks. Claimblocks left: " + (totalClaimBlocks - totalClaimBlocksInUse));
                            }
                            /**
                             * To get a list of command use the /help command.
                             **/
                            if (args[0].equalsIgnoreCase("help")) {
                                player.sendMessage(prefix+"List of commands:");
                                player.sendMessage(ChatColor.YELLOW+"/claim create <claimname>");
                                player.sendMessage(ChatColor.YELLOW+"/claim remove <claimname>");
                                player.sendMessage(ChatColor.YELLOW+"/claim list");
                                player.sendMessage(ChatColor.YELLOW+"/claim setflag <claimname> <flag> <value>");
                                player.sendMessage(ChatColor.YELLOW+"/claim removeflag <claimname> <flag>");
                                player.sendMessage(ChatColor.YELLOW+"/claim info");
                                player.sendMessage(ChatColor.YELLOW+"/claim info <claimname>");
                                player.sendMessage(ChatColor.YELLOW+"/claim addmember <player> <claimname>");
                                player.sendMessage(ChatColor.YELLOW+"/claim removemember <player> <claimname>");
                                player.sendMessage(ChatColor.YELLOW+"/claim buyclaimblocks <amount>");
                            }
                        } else {
                            player.sendMessage(prefix + "You must be in the right world to use this command!");
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static WorldEditPlugin getWorldedit(){
        final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if(plugin instanceof WorldEditPlugin) {
            return (WorldEditPlugin) plugin;
        } else {
            return null;
        }
    }

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


    public void saveToFile(final FileConfiguration playerConfig, final Player player) {
        try {
            playerConfig.save(getPlayerFile(player));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static StringFlag getDonatorStringFlag(final String flag) {
        switch (flag.toLowerCase()) {
            case "greeting-title":
                return Flags.GREET_TITLE;
            case "farwell-title":
                return Flags.FAREWELL_TITLE;
            case "time-lock":
                return Flags.TIME_LOCK;
        }
        return null;
    }


    private static StringFlag getStringFlag(final String flag) {
        switch (flag.toLowerCase()) {
            case "greeting":
                return Flags.GREET_MESSAGE;
            case "farewell":
                return Flags.FAREWELL_MESSAGE;
        }
        return null;
    }
    private static StateFlag getDonatorFlag(final String flag) {
        switch (flag.toLowerCase()){
            case "fly":
                return net.goldtreeservers.worldguardextraflags.flags.Flags.FLY;
        }
        return null;
    }

    private static StateFlag getFlag(final String flag) {
        switch (flag.toLowerCase()) {
            case "build":
                return Flags.BUILD;
            case "use":
                return Flags.USE;
            case "interact":
                return Flags.INTERACT;
            case "damage-animals":
                return Flags.DAMAGE_ANIMALS;
            case "pvp":
                return Flags.PVP;
            case "mob-spawning":
                return Flags.MOB_SPAWNING;
            case "mob-damage":
                return Flags.MOB_DAMAGE;
            case "creeper-explosion":
                return Flags.CREEPER_EXPLOSION;
            case "other-explosion":
                return Flags.OTHER_EXPLOSION;
            case "water-flow":
                return Flags.WATER_FLOW;
            case "lava-flow":
                return Flags.LAVA_FLOW;
            case "snow-melt":
                return Flags.SNOW_MELT;
            case "snow-fall":
                return Flags.SNOW_FALL;
            case "ice-form":
                return Flags.ICE_FORM;
            case "ice-melt":
                return Flags.ICE_MELT;
            case "frosted-ice-form":
                return Flags.FROSTED_ICE_FORM;
            case "frosted-ice-melt":
                return Flags.FROSTED_ICE_MELT;
            case "leaf-decay":
                return Flags.LEAF_DECAY;
            case "grass-spread":
                return Flags.GRASS_SPREAD;
            case "mycelium-spread":
                return Flags.MYCELIUM_SPREAD;
            case "vine-growth":
                return Flags.VINE_GROWTH;
            case "crop-growth":
                return Flags.CROP_GROWTH;
            case "entry":
                return Flags.ENTRY;
            case "enderpearl":
                return Flags.ENDERPEARL;
            case "chorus-fruit-teleport":
                return Flags.CHORUS_TELEPORT;
            case "vehicle-place":
                return Flags.PLACE_VEHICLE;
            case "vechicle-destroy":
                return Flags.DESTROY_VEHICLE;
            case "fall-damage":
                return Flags.FALL_DAMAGE;
        }
        return null;
    }

    private void loadConfig() {
        configurationSection = getConfig();
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
}