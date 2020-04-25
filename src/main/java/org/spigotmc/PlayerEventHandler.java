package org.spigotmc;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import services.BlockUpdateService;
import services.BlockUpdateServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayerEventHandler implements Listener {

    private static WorldEditPlugin worldEdit;
    private static ConfigurationSection configurationSection;
    private static List<String> worlds;
    private static BlockUpdateService blockUpdateService;

    public PlayerEventHandler(final WorldEditPlugin worldEdit, final ConfigurationSection configurationSection) {
        this.worldEdit = worldEdit;
        this.configurationSection = configurationSection;
        worlds = (List<String>) configurationSection.getList("worlds");
        blockUpdateService = new BlockUpdateServiceImpl();
    }

    @EventHandler
    public static void onPlayerMove(final PlayerMoveEvent e) {
        final Player player = e.getPlayer();
        if (worlds.contains(player.getWorld().getName())) {
            final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

            BlockVector3.at(e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ());
            BlockVector3.at(e.getTo().getX(), e.getTo().getY(), e.getTo().getZ());
            final Set<ProtectedRegion> regionsTo = regionManager.getApplicableRegions(BlockVector3.at(e.getTo().getX(), e.getTo().getY(), e.getTo().getZ())).getRegions();
            final Set<ProtectedRegion> regionsFrom = regionManager.getApplicableRegions(BlockVector3.at(e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ())).getRegions();
            boolean fromRegionFlag = false;
            if(regionsTo.size() > regionsFrom.size()) {
                if(regionsFrom.size() != 0) {
                    for (final ProtectedRegion region : regionsFrom) {
                        if(region.getFlags().keySet().contains(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY)){
                            if(region.getFlags().get(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY).equals(StateFlag.State.ALLOW)) {
                                fromRegionFlag = true;
                                continue;
                            }
                        }
                    }
                }
                for (final ProtectedRegion region : regionsTo) {
                    if(region.getFlags().keySet().contains(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY)){
                        if(region.getFlags().get(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY).equals(StateFlag.State.ALLOW)) {
                            if(!fromRegionFlag)
                                player.sendMessage(Configuration.PREFIX+"Flying enabled");
                        }
                    }
                }
            } else if(regionsTo.size() < regionsFrom.size()) {
                if(regionsTo.size() != 0) {
                    for (final ProtectedRegion region : regionsTo) {
                        if(region.getFlags().keySet().contains(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY)){
                            if(region.getFlags().get(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY).equals(StateFlag.State.ALLOW)) {
                                fromRegionFlag = true;
                                continue;
                            }
                        }
                    }
                }
                for (final ProtectedRegion region : regionsFrom) {
                    if(region.getFlags().keySet().contains(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY)){
                        if(region.getFlags().get(net.goldtreeservers.worldguardextraflags.flags.Flags.FLY).equals(StateFlag.State.ALLOW)) {
                            if(!fromRegionFlag)
                                player.sendMessage(Configuration.PREFIX+"Flying disabled");
                        }
                    }
                    if(region.getParent() != null)
                        blockUpdateService.resetClaimBorder(player, region);
                    if(region.getParent() == null)
                        blockUpdateService.resetClaimBorder(player, region);
                }
            }
        }
    }

    @EventHandler
    public static void onPlayerInteract(final PlayerInteractEvent e) {
        if(e != null && e.getPlayer() != null){
            final Player player = e.getPlayer();
            if (worlds.contains(player.getWorld().getName())) {
                final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
                final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

                if (e.getAction() != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null && e.getItem().getType() == Material.STICK) {
                    final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()));
                    if(!regionList.getRegions().isEmpty()){
                        ProtectedRegion region = null;
                        for (final ProtectedRegion rg : regionList) {
                            if(region == null)
                                region = rg;
                            if(rg.getPriority() > region.getPriority()) {
                                region = rg;
                            }
                        }
                        if (region.getId().startsWith("claim_")) {
                            player.sendMessage(Configuration.PREFIX + "Claim information:");
                            player.sendMessage(ChatColor.YELLOW + "Claim id: " + region.getId().substring(43, region.getId().length()));
                            if (region.getParent() != null){
                                player.sendMessage(ChatColor.YELLOW + "Claim parent: " + region.getParent().getId().split("_" + player.getUniqueId() + "_")[1]);
                                blockUpdateService.displayClaimBorder(player, region, true);
                            }
                            else
                                blockUpdateService.displayClaimBorder(player, region, false);
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
                        }
                    }
                    else
                        player.sendMessage(Configuration.PREFIX + "No claim here.");
                }
            }
        }
    }

}
