package org.spigotmc;

import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class JumpingListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        event.setJoinMessage("Welcome, " + event.getPlayer().getName() + " to the server!");
    }

    @EventHandler
    public void onMovement(PlayerMoveEvent e) {
        double oldY;
        Player player = e.getPlayer();
        //onJump
        if(e.getFrom().getY() < e.getTo().getY() && player.getLocation().subtract(0, 1, 0).getBlock().getType() != Material.AIR) {
            oldY = player.getVelocity().getY();
            if(player.getLocation().subtract(0,1,0).getBlock().getType() == Material.GOLD_BLOCK) {
                if(player.getVelocity().getY() < oldY+5){
                    player.setVelocity(new Vector(0 ,1,0));
                }
            }
        }
    }

    @EventHandler
    public void onPressurePlate(PlayerInteractEvent e){
        Player player = e.getPlayer();
        if(e.getClickedBlock().getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE){
            int yaw = (int)player.getLocation().getYaw();
            player.sendMessage(String.valueOf(player.getLocation().getYaw()));
            //south
            if((yaw >=0 && yaw < 45) || (yaw >=315 && yaw < 360))
                player.setVelocity(new Vector(0,1,1));
            //east
            if(yaw >=225 && yaw < 315)
                player.setVelocity(new Vector(1,1,0));
            //north
            if(yaw >=135 && yaw < 225)
                player.setVelocity(new Vector(0,1,-1));
            //west
            if(yaw >=45 && yaw < 135)
                player.setVelocity(new Vector(-1,1,0));
        }
    }


}
