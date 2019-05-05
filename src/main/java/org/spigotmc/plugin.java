package org.spigotmc;

import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;



public final class plugin extends JavaPlugin implements CommandExecutor {

    private JumpingListener jp;
    private static Player player;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(label.equalsIgnoreCase("spawn")){
            if(sender instanceof Player) {
                player = (Player) sender;
                player.teleport(new Location(player.getWorld(),player.getWorld().getSpawnLocation().getX(), player.getBedSpawnLocation().getY() ,player.getBedSpawnLocation().getZ()));
            }
        }

        if(label.equalsIgnoreCase("test")){
            if(sender instanceof Player){
                player = (Player) sender;
                EntityType entity = EntityType.PIG;
                player.getWorld().spawnEntity(player.getEyeLocation(), entity);
            }
        }

        if (label.equalsIgnoreCase("day")) {
            if (sender instanceof Player){
                player = (Player) sender;
                player.getWorld().setTime(1200);
                player.getWorld().setWeatherDuration(0);
            }
        }
        if(label.equalsIgnoreCase("xp")) {
            if (sender instanceof Player) {
                player = (Player) sender;

                player.giveExpLevels(500);
                player.sendMessage("Test");
            }
        }
        if (label.equalsIgnoreCase("bmessage")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Please specify a name and message!");
            } else {
                Player target = Bukkit.getPlayer(args[0]);

                if(target == null) {
                    sender.sendMessage(ChatColor.RED+args[0]+" is not online!");
                }
                StringBuilder x = new StringBuilder();

                for (int i=1; i<args.length; i++)
                    x.append(args[i]);

                String message = x.toString().trim();

                BossBar bar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', message), BarColor.YELLOW, BarStyle.SOLID);

                bar.addPlayer(target);

                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    bar.removePlayer(target);
                }, 200);
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
