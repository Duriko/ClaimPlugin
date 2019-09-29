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

    private String prefix = ChatColor.WHITE+"["+ChatColor.YELLOW+"Claim"+ChatColor.WHITE+"] "+ChatColor.YELLOW;

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
                        if (args[0].equalsIgnoreCase("remove")) {
                            if (args.length >= 2 && args[1] != null) {
                                ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
                                for (ProtectedRegion region : regionList) {
                                    if (region.getId().equalsIgnoreCase("claim_" + player.getName() + "_" + args[1])) {
                                        regionManager.removeRegion("claim_" + player.getName() + "_" + args[1]);
                                        player.sendMessage(prefix + "Claim " + args[1] + " has been removed!");
                                        return true;
                                    } else {
                                        player.sendMessage(prefix + "No claim with that name found! " + "claim_" + player.getName() + "_" + args[1]);
                                        return false;
                                    }
                                }
                                player.sendMessage(prefix + "No claim with the name: " + args[1] + " exists!");
                                return false;
                            } else {
                                player.sendMessage(prefix + "You need to specify the claim name!");
                                return false;
                            }
                        }

                        /**
                         * Get a list of players claims
                         **/
                        if (args[0].equalsIgnoreCase("list")){
                            List<ProtectedRegion> playersClaim = new ArrayList<>();
                            player.sendMessage(prefix+"Your claims:");
                            for (ProtectedRegion region : regionManager.getRegions().values()){
                                if (region.getId().contains("claim_"+player.getName())) {
                                    playersClaim.add(region);
                                    player.sendMessage(prefix + region.getId());
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
                                    player.sendMessage(prefix + "Claim information:");
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
                                if (args.length >= 2 && args[1] != null) {
                                    BlockVector3 p1 = regionSelector.getRegion().getMinimumPoint();
                                    BlockVector3 p2 = regionSelector.getRegion().getMaximumPoint();
                                    for(ProtectedRegion region : regionManager.getRegions().values()){
                                        if(region.contains(p1) || region.contains(p2)){
                                            player.sendMessage(prefix+"Claim overlaps with another claim! (" + region.getId()+")");
                                            return false;
                                        }
                                    }
                                    if(regionManager.getRegion("claim_"+player.getName()+"_"+args[1]) == (null)){
                                        ProtectedRegion region = new ProtectedCuboidRegion("claim_"+player.getName()+"_"+args[1],
                                                BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
                                        regionManager.addRegion(region);
                                        region.setFlag(Flags.PVP, StateFlag.State.DENY);
                                        region.setFlag(Flags.BUILD, StateFlag.State.DENY);
                                        DefaultDomain owner = region.getOwners();
                                        owner.addPlayer(player.getName());
                                        region.setOwners(owner);
                                        player.sendMessage(prefix+"Claim " +region.getId()+ " created!");
                                    } else
                                        player.sendMessage(prefix+"Claim with that name already exist");
                                } else
                                    player.sendMessage(prefix+"You must specify a claim name!");
                            } catch (NullPointerException ne) { }
                            catch (Exception ex){ }
                        }

                        /**
                         * To add or remove a member from a claim,
                         * the player must use the command
                         * /claim add/removemember <playerToRemove> <claimName>
                         *      Example: /claim addmember goppi house
                         **/
                        if (args[0].equalsIgnoreCase("addmember")){
                            if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                if (regionManager.getRegion("claim_"+player.getName()+"_"+args[2]) != null){
                                    regionManager.getRegion("claim_"+player.getName()+"_"+args[2]).getMembers().addPlayer(args[1]);
                                    player.sendMessage(ChatColor.YELLOW+"Added " + args[1] + "to the claim!");
                                } else {
                                    player.sendMessage(ChatColor.YELLOW+"No claim with that name exist!");
                                }
                            }
                        }
                        if (args[0].equalsIgnoreCase("removemember")){
                            if (args.length >= 3 && (args[1] != null && args[2] != null)) {
                                if (regionManager.getRegion("claim_"+player.getName()+"_"+args[2]) != null){
                                    regionManager.getRegion("claim_"+player.getName()+"_"+args[2]).getMembers().removePlayer(args[1]);
                                    player.sendMessage(ChatColor.YELLOW+"Removed " + args[1] + " to the claim!");
                                } else
                                    player.sendMessage(ChatColor.YELLOW+"No claim with that name exist!");
                            }
                        }

                        /**
                         * To set or remove a falg from a claim the player must
                         * use the /claim set/removeflag command.
                         * Parameters: /claim setflag <flag> <value> <claimname>
                         *     Example: /claim setflag pvp deny house
                         **/
                        if (args[0].equalsIgnoreCase("setflag")){
                            if (args.length >= 4 && (args[1] != null && args[2] != null && args[3] != null)){
                                if (regionManager.getRegion("claim_"+player.getName()+"_"+args[3]) != null){
                                     boolean removeflag = false;
                                     StateFlag.State stateFlag = StateFlag.State.DENY;
                                     StateFlag flag = null;
                                     if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("allow"))
                                         stateFlag = StateFlag.State.ALLOW;
                                    switch (args[1]){
                                        case "pvp":
                                            flag = Flags.PVP;
                                            break;
                                        case "build":
                                            flag = Flags.BUILD;
                                            break;
                                        case "entry":
                                            flag = Flags.ENTRY;
                                            break;
                                        case "use":
                                            flag = Flags.USE;
                                            break;
                                    }
                                    if (removeflag){
                                        regionManager.getRegion("claim_"+player.getName()+"_"+args[3]).getFlags().remove(flag);
                                        player.sendMessage(prefix+"Removed flag: " + args[1]);
                                    }
                                    else{
                                        regionManager.getRegion("claim_"+player.getName()+"_"+args[3]).setFlag(flag, stateFlag);
                                        player.sendMessage(prefix+"Flag " + args[1] + " set to " + args[2] );
                                    }
                                }
                            }
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
