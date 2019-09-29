package org.spigotmc;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.*;
import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class plugin extends JavaPlugin implements CommandExecutor {

    private JumpingListener jp;
    private static Player player;

    public WorldEditPlugin getWorldedit(){
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if(plugin instanceof WorldEditPlugin)
            return (WorldEditPlugin) plugin;
        else
            return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (label.equalsIgnoreCase("claim")){
            if (sender instanceof Player) {
                player = (Player) sender;
                RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
                RegionSelector regionSelector = getWorldedit().getSession(player).getRegionSelector(BukkitAdapter.adapt(player.getWorld()));
                try {
                    if(args[0]!=null){

                        /**
                         * To remove a claim you must stand in the claim.
                         * To remove a claim use the command /claim remove <claimname>
                         **/

                        if (args[0].equalsIgnoreCase("remove"))
                            if (args.length >= 2 && args[1] != null) {
                                ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                                for (ProtectedRegion region : regionList) {
                                    if (region.getId().equalsIgnoreCase("claim_" + player.getName() + "_" + args[1])) {
                                        regionManager.removeRegion("claim_" + player.getName() + "_" + args[1]);
                                        player.sendMessage(ChatColor.YELLOW + "Claim " + args[1] + " has been removed!");
                                        return true;
                                    } else {
                                        player.sendMessage(ChatColor.YELLOW + "No claim with that name found! " + "claim_" + player.getName() + "_" + args[1]);
                                        return false;
                                    }
                                }
                                player.sendMessage(ChatColor.YELLOW+"No claim with the name: " + args[1] + " exists!");
                                return false;
                            } else {
                                player.sendMessage(ChatColor.YELLOW+"You need to specify the claim name!");
                                return false;
                            }
                        /**
                         * Get a list of players claims
                         **/
                        if (args[0].equalsIgnoreCase("list")){
                            List<ProtectedRegion> playersClaim = new ArrayList<>();
                            player.sendMessage(ChatColor.YELLOW+"Your claims:");
                            for (ProtectedRegion region : regionManager.getRegions().values()){
                                if (region.getId().contains("claim_"+player.getName())) {
                                    playersClaim.add(region);
                                    player.sendMessage(ChatColor.YELLOW + region.getId());
                                }
                            }
                        }

                        /**
                         * To show information about a claim, the player must stand
                         * in the claim and use the /claim info command
                         **/

                        if (args[0].equalsIgnoreCase("info")){
                            ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                            for (ProtectedRegion region : regionList) {
                                if (region.getOwners().contains(player.getName())) {
                                    player.sendMessage(ChatColor.YELLOW + "Claim information:");
                                    player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId());
                                    player.sendMessage(ChatColor.YELLOW + "Claim coords: " + region.getMinimumPoint() + " - " + region.getMaximumPoint());
                                    String tmp = "";
                                    Map<Flag<?>, Object> map = region.getFlags();
                                    for (Flag<?> flag : region.getFlags().keySet()) {
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

                        /**
                         * To create a claim, the player must first create a selection
                         * A selection must be made using the Worldedit wand.
                         * When a selection has been made, use the command /claim create <claimname>
                         * to create a new claim with the name claim_<playerName>_claimname
                         **/
                        if (args[0].equalsIgnoreCase("create")){
                            try {
                                BlockVector3 p1 = regionSelector.getRegion().getMinimumPoint();
                                BlockVector3 p2 = regionSelector.getRegion().getMaximumPoint();
                                for(ProtectedRegion region : regionManager.getRegions().values()){
                                    if(region.contains(p1) || region.contains(p2)){
                                        player.sendMessage(ChatColor.YELLOW+"Claim overlaps with another claim! (" + region.getId()+")");
                                        return false;
                                    }
                                }
                                if(regionManager.getRegion("claim_"+player.getName()+"_"+args[0]) == (null)){
                                    regionManager.getRegion("claim_"+player.getName()+"_"+args[0]);
                                    ProtectedRegion region = new ProtectedCuboidRegion("claim_"+player.getName()+"_"+args[0],
                                            BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
                                    regionManager.addRegion(region);
                                    region.setFlag(Flags.PVP, StateFlag.State.DENY);
                                    region.setFlag(Flags.BUILD, StateFlag.State.DENY);
                                    DefaultDomain owner = region.getOwners();
                                    owner.addPlayer(player.getName());
                                    region.setOwners(owner);
                                    player.sendMessage("Claim " + "claim_"+player.getName()+"_"+args[0] + " created!");
                                } else
                                    player.sendMessage("Claim with that name already exist");
                            } catch (NullPointerException ne) { }
                            catch (Exception ex){ }
                        }
                    }

                }catch (Exception e){ }
            }
        }

        if(label.equalsIgnoreCase("spawn")){
            player.teleport(new Location(player.getWorld(),player.getWorld().getSpawnLocation().getX(), player.getBedSpawnLocation().getY() ,player.getBedSpawnLocation().getZ()));
        }

        if (label.equalsIgnoreCase("day")) {
            if (sender instanceof Player){
                player = (Player) sender;
                player.getWorld().setTime(1200);
                player.getWorld().setWeatherDuration(0);
            }
        }


        return true;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        jp = new JumpingListener();
        getServer().getPluginManager().registerEvents(jp, this);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
