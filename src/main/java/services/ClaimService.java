package services;

import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.entity.Player;

public interface ClaimService {

    boolean createClaim(Player player, String claimName, RegionManager regionManager, RegionSelector regionSelector);

    void removeClaim(final Player player, final String claimName, final RegionManager regionManager);

    void listPlayerClaims(Player player, RegionManager regionManager);

    void getClaimInfoById(Player player, String claimId,  RegionManager regionManager);

    void getClaimInfoFromPlayerPosition(Player player, RegionManager regionManager);

    void addMember(Player player, String claimName, String member, RegionManager regionManager);

    void removeMember(Player player, String claimName, String member, RegionManager regionManager);

    boolean setFlag(Player player, String claimName, String flagName, String flagValue, RegionManager regionManager);

    boolean removeFlag(Player player, String claimName, String flagName, RegionManager regionManager);

    boolean expandClaim(Player player, int amount, RegionManager regionManager);

    void renameClaim(Player player, String claimName, String newClaimName, RegionManager regionManager);

    void getNearbyClaims(Player player, int radius, RegionManager regionManager);

}
