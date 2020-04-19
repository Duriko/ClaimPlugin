package org.spigotmc;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;

public class StickListener implements Listener {

    private static WorldEditPlugin worldEdit;

    public StickListener(final WorldEditPlugin worldEdit) {
        this.worldEdit = worldEdit;
    }

    @EventHandler
    public static void onPlayerInteract(final PlayerInteractEvent e) {

        if(e != null && e.getPlayer() != null){
            final Player player = e.getPlayer();

            final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

            if (e.getAction() != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null && e.getItem().getType() == Material.STICK) {
                final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()));
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
                        }
                    }
                else
                    player.sendMessage(Configuration.PREFIX + "No claim here.");
            }
        }
    }
}
