package services;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class BlockUpdateServiceImpl implements BlockUpdateService {

    @Override
    public void displayClaimBorder(final Player player, final ProtectedRegion region, final boolean childClaim) {
        final int minX = region.getMinimumPoint().getX();
        final int minZ = region.getMinimumPoint().getZ();
        final int maxX = region.getMaximumPoint().getX();
        final int maxZ = region.getMaximumPoint().getZ();
        final int midX = (maxX - minX)/2;
        final int midZ = (maxZ - minZ)/2;

        final BlockData blockdata = childClaim ? Material.IRON_BLOCK.createBlockData() : Material.DIAMOND_BLOCK.createBlockData();

        if(midX/5 >0) {
            int tmp = 0;
            final int amount = midX/5;
            for(int i = 0; i< amount; i++) {
                final int abc = minX + midX - tmp;
                final int abc2 = minX + midX + tmp;
                player.sendBlockChange(new Location(player.getWorld(), abc, player.getWorld().getHighestBlockYAt(abc, maxZ), minZ), Material.GLOWSTONE.createBlockData());
                player.sendBlockChange(new Location(player.getWorld(), abc, player.getWorld().getHighestBlockYAt(abc, maxZ), maxZ), Material.GLOWSTONE.createBlockData());
                player.sendBlockChange(new Location(player.getWorld(), abc2, player.getWorld().getHighestBlockYAt(abc2, maxZ), minZ), Material.GLOWSTONE.createBlockData());
                player.sendBlockChange(new Location(player.getWorld(), abc2, player.getWorld().getHighestBlockYAt(abc2, maxZ), maxZ), Material.GLOWSTONE.createBlockData());
                tmp+= 5;
            }
        }
        if(midZ/5 >0) {
            int tmp = 0;
            final int amount = midZ/5;
            for(int i = 0; i< amount; i++) {
                final int abc = minZ + midZ - tmp;
                final int abc2 = minZ + midZ + tmp;
                player.sendBlockChange(new Location(player.getWorld(), minX, player.getWorld().getHighestBlockYAt(maxX, abc), abc), Material.GLOWSTONE.createBlockData());
                player.sendBlockChange(new Location(player.getWorld(), maxX, player.getWorld().getHighestBlockYAt(maxX, abc), abc), Material.GLOWSTONE.createBlockData());
                player.sendBlockChange(new Location(player.getWorld(), minX, player.getWorld().getHighestBlockYAt(maxX, abc2), abc2), Material.GLOWSTONE.createBlockData());
                player.sendBlockChange(new Location(player.getWorld(), maxX, player.getWorld().getHighestBlockYAt(maxX, abc), abc2), Material.GLOWSTONE.createBlockData());
                tmp+= 5;
            }
        }

        //Four edges
        player.sendBlockChange(new Location(player.getWorld(), minX, player.getWorld().getHighestBlockYAt(minX, minZ), minZ), blockdata);
        player.sendBlockChange(new Location(player.getWorld(), minX, player.getWorld().getHighestBlockYAt(minX, maxZ), maxZ), blockdata);
        player.sendBlockChange(new Location(player.getWorld(), maxX, player.getWorld().getHighestBlockYAt(maxX, minZ), minZ), blockdata);
        player.sendBlockChange(new Location(player.getWorld(), maxX, player.getWorld().getHighestBlockYAt(maxX, maxZ), maxZ), blockdata);

        //Two glowstone next to the four edges
        player.sendBlockChange(new Location(player.getWorld(), minX, player.getWorld().getHighestBlockYAt(minX, minZ), minZ+1), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), minX+1, player.getWorld().getHighestBlockYAt(minX, minZ), minZ), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), minX, player.getWorld().getHighestBlockYAt(minX, maxZ), maxZ-1), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), minX+1, player.getWorld().getHighestBlockYAt(minX, maxZ), maxZ), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), maxX, player.getWorld().getHighestBlockYAt(maxX, minZ), minZ+1), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), maxX-1, player.getWorld().getHighestBlockYAt(maxX, minZ), minZ), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), maxX, player.getWorld().getHighestBlockYAt(maxX, maxZ), maxZ-1), Material.GLOWSTONE.createBlockData());
        player.sendBlockChange(new Location(player.getWorld(), maxX-1, player.getWorld().getHighestBlockYAt(maxX, maxZ), maxZ), Material.GLOWSTONE.createBlockData());
    }

    @Override
    public void resetClaimBorder(final Player player, final ProtectedRegion region) {
        final int minX = region.getMinimumPoint().getX();
        final int minZ = region.getMinimumPoint().getZ();
        final int maxX = region.getMaximumPoint().getX();
        final int maxZ = region.getMaximumPoint().getZ();
        final int midX = (maxX - minX)/2;
        final int midZ = (maxZ - minZ)/2;

        if(midX/5 >0) {
            int tmp = 0;
            final int amount = midX/5;
            for(int i = 0; i< amount; i++) {
                final int abc = minX + midX - tmp;
                final int abc2 = minX + midX + tmp;
                resetBlock(player, abc, abc, maxZ, minZ);
                resetBlock(player, abc, abc, maxZ, maxZ);
                resetBlock(player, abc2, abc2, maxZ, minZ);
                resetBlock(player, abc2, abc2, maxZ, maxZ);
                tmp+= 5;
            }
        }
        if(midZ/5 >0) {
            int tmp = 0;
            final int amount = midZ/5;
            for(int i = 0; i< amount; i++) {
                final int abc = minZ + midZ - tmp;
                final int abc2 = minZ + midZ + tmp;
                resetBlock(player, minX, maxX, abc, abc);
                resetBlock(player, maxX, maxX, abc, abc);
                resetBlock(player, minX, maxX, abc2, abc2);
                resetBlock(player, maxX, maxX, abc2, abc2);
                tmp+= 5;
            }
        }
        //Four middle
        resetBlock(player, minX+midX, minX+midX, maxZ, minZ);
        resetBlock(player, minX+midX, minX+midX, maxZ, maxZ);
        resetBlock(player, minX, minX, maxZ-midZ, maxZ-midZ);
        resetBlock(player, maxX, minX, maxZ-midZ, maxZ-midZ);
        //Four edges
        resetBlock(player, minX, minX, minZ, minZ);
        resetBlock(player, minX, minX, maxZ, maxZ);
        resetBlock(player, maxX, maxX, minZ, minZ);
        resetBlock(player, maxX, maxX, maxZ, maxZ);
        //Two glowstone next to the four edges
        resetBlock(player, minX, minX, minZ, minZ+1);
        resetBlock(player, minX+1, minX, minZ, minZ);
        resetBlock(player, minX, minX, maxZ, maxZ-1);
        resetBlock(player, minX+1, minX, maxZ, maxZ);
        resetBlock(player, maxX, maxX, minZ, minZ+1);
        resetBlock(player, maxX-1, maxX, minZ, minZ);
        resetBlock(player, maxX, maxX, maxZ, maxZ-1);
        resetBlock(player, maxX-1, maxX, maxZ, maxZ);
    }

    @Override
    public void resetBlock(final Player player, final int x, final int midX, final int midZ, final int z) {
        player.sendBlockChange(new Location(player.getWorld(), x, player.getWorld().getHighestBlockYAt(midX, midZ), z),
                player.getWorld().getBlockAt(x, player.getWorld().getHighestBlockYAt(midX, midZ), z).getType().createBlockData());
    }

}
