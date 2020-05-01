package org.spigotmc;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
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
import services.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;


public final class plugin extends JavaPlugin implements CommandExecutor {

    private Player player;
    private Economy economy;
    private ConfigurationSection configurationSection;
    private Fileservice fileservice;
    private ClientService clientService;
    private ClaimService claimService;

    private boolean totalBlockLimit;
    private int totalBlockAmountLimit;
    private int claimBlockPrice;
    private List<String> worlds;

    @Override
    public void onEnable() {
        final Logger logger = getLogger();
        fileservice = new FileserviceImpl();
        clientService = new ClientServiceImpl();
        claimService = new ClaimServiceImpl(fileservice, clientService);
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
                                    claimService.removeClaim(player, args[1], regionManager);
                                } else {
                                    player.sendMessage(Configuration.PREFIX + "You need to specify the claim name!");
                                }
                                return true;
                            }
                            /**
                             * To create a claim, the player must first create a selection
                             * A selection must be made using the Worldedit wand.
                             * When a selection has been made, use the command /claim create <claimname>
                             * to create a new claim with the name claim_<playerName>_claimname
                             **/
                            if (args[0].equalsIgnoreCase("create")) {
                                if (args.length >= 2 && args[1] != null) {
                                    claimService.createClaim(player, args[1], regionManager, regionSelector);
                                } else
                                    player.sendMessage(Configuration.PREFIX + "You must specify a claim name!");
                                return true;
                            }
                            /**
                             * Get a list of players claims
                             **/
                            if (args[0].equalsIgnoreCase("list")) {
                                claimService.listPlayerClaims(player, regionManager);
                                return true;
                            }
                            /**
                             * To show information about a claim, the player must stand
                             * in the claim and use the /claim info command
                             **/
                            if (args[0].equalsIgnoreCase("info")) {
                                if (args.length >= 2 && args[1] != null) {
                                    claimService.getClaimInfoById(player, args[1], regionManager);
                                } else {
                                    claimService.getClaimInfoFromPlayerPosition(player, regionManager);
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
                                    claimService.addMember(player, args[1], args[2], regionManager);
                                }
                                else
                                    player.sendMessage(Configuration.PREFIX+"To add a member use the following command: /claim addmember <claimname> <player>.");
                                return true;
                            }
                            if (args[0].equalsIgnoreCase("removemember")) {
                                if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                    claimService.removeMember(player, args[1], args[2], regionManager);
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
                                    String flagValue = args[3];
                                    for (int i = 4; i<args.length; i++)
                                        flagValue = flagValue + " " + args[i];
                                    claimService.setFlag(player, claimName, flagName, flagValue, regionManager);
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
                                   claimService.removeFlag(player, claimName, flagName, regionManager);
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
                                    if(args[1].matches("[1-9]\\d*")) {
                                        claimService.expandClaim(player, Integer.valueOf(args[1]), regionManager);
                                    } else
                                        player.sendMessage(Configuration.PREFIX + "Invalid amount: " + args[1] +  ".");
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
                                    claimService.renameClaim(player, args[1], args[2], regionManager);
                                } else
                                    player.sendMessage(Configuration.PREFIX + "Invalid command! For help use /claim help");
                                return true;
                            }
                            /**
                             * To get a list of nearby claims use the following command:
                             * /claim nearbyclaims
                             **/
                            if (args[0].equalsIgnoreCase("nearbyclaims")) {
                                if (args.length >= 2 && args[1] != null) {
                                    if(args[1].matches("[1-9]\\d*")) {
                                        claimService.getNearbyClaims(player, Integer.valueOf(args[1]), regionManager);
                                    }
                                     else {
                                        player.sendMessage(Configuration.PREFIX+"Radius must be a number!");
                                    }
                                }
                                return false;
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
                                player.sendMessage(ChatColor.YELLOW+"/claim rename <claimname> <newClaimName>" + ChatColor.WHITE + " - Rename a claim.");
                                player.sendMessage(ChatColor.YELLOW+"/claim expand <amount>" + ChatColor.WHITE + " - Expand a claim in the direction you are facing.");
                                player.sendMessage(ChatColor.YELLOW+"/claim nearbyclaims <radius>" + ChatColor.WHITE + " - Get a list of nearby claims.");
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