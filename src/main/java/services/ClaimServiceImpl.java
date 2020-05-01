package services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.spigotmc.Configuration;

import java.util.List;
import java.util.Map;

public class ClaimServiceImpl implements ClaimService {

    private final Fileservice fileservice;
    private final ClientService clientService;

    public ClaimServiceImpl(final Fileservice fileservice, final ClientService clientService) {
        this.fileservice = fileservice;
        this.clientService = clientService;
    }

    @Override
    public boolean createClaim(final Player player, final String claimName, final RegionManager regionManager, final RegionSelector regionSelector) {
        try {
            final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(player));
            final int totalClaimBlocksInUse = (Integer) playerConfig.get("player.totalClaimBlocksInUse");
            final int totalClaimBlocks = (Integer) playerConfig.get("player.totalClaimBlocks");

            final BlockVector3 p1 = regionSelector.getRegion().getMinimumPoint();
            final BlockVector3 p2 = regionSelector.getRegion().getMaximumPoint();
            ProtectedRegion parentRegion = null;
            final BlockVector2 pos1 = BlockVector2.at(p1.toBlockVector2().getX(),p1.toBlockVector2().getZ());
            final BlockVector2 pos2 = BlockVector2.at(p2.toBlockVector2().getX(),p2.toBlockVector2().getZ());
            final BlockVector2 pos3 = BlockVector2.at(p1.toBlockVector2().getX(),p2.toBlockVector2().getZ());
            final BlockVector2 pos4 = BlockVector2.at(p2.toBlockVector2().getX(),p1.toBlockVector2().getZ());
            final List<BlockVector2> points = Lists.newArrayList(pos1, pos2, pos3, pos4);
            for (final ProtectedRegion region : regionManager.getRegions().values()) {
                if (region.containsAny(points)) {
                    if(region.contains(points.get(0)) && region.contains(points.get(1))
                            && region.contains(points.get(2)) && region.contains(points.get(3))){
                        if (region.getOwners().contains(player.getName())) {
                            if(region.getParent() == null) {
                                parentRegion = region;
                                continue;
                            } else {
                                player.sendMessage(Configuration.PREFIX + "Claim is overlaping a child claim!");
                                return false;
                            }
                        } else {
                            player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim! (" + region.getId() + ")");
                            return false;
                        }
                    } else {
                        player.sendMessage(Configuration.PREFIX + "Claim overlaps with another claim! (" + region.getId().split("_" + player.getUniqueId() + "_")[1] + ")");
                        return false;
                    }
                }
            }
            if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) == (null)) {
                final ProtectedRegion region = new ProtectedCuboidRegion("claim_" + player.getUniqueId().toString() + "_" + claimName,
                        BlockVector3.at(p1.getBlockX(), 0, p1.getBlockZ()), BlockVector3.at(p2.getBlockX(), 255, p2.getBlockZ()));
                final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
                if(parentRegion != null) {
                    region.setParent(parentRegion);
                    region.setPriority(2);
                    region.setFlags(parentRegion.getFlags());
                } else {
                    if (overlapingClaims.size() != 0) {
                        player.sendMessage(Configuration.PREFIX + "Claim is overlaping with another claim!");
                        return false;
                    }
                    region.setPriority(1);
                }
                final int regionSize = region.volume() / 256;
                if((p2.getX() - p1.getX() >= 5 ) && (p2.getZ() - p1.getZ() >= 5)) {
                    if (totalClaimBlocksInUse + regionSize <= totalClaimBlocks) {
                        regionManager.addRegion(region);
                        final DefaultDomain owner = region.getOwners();
                        owner.addPlayer(player.getName());
                        region.setOwners(owner);
                        final Map<Flag<?>, Object> map = Maps.newHashMap();
                        map.put(Flags.PVP, StateFlag.State.DENY);
                        map.put(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY);
                        region.setFlags(map);
                        player.sendMessage(Configuration.PREFIX + "Claim " + region.getId().split("_" + player.getUniqueId() + "_")[1] + " created!");
                        final List<String> claims = (List<String>) playerConfig.getList("player.claims");
                        claims.add("claim_" + player.getUniqueId().toString() + "_" + claimName);
                        playerConfig.set("player.claims", claims);
                        playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + (region.volume() / 256));
                        fileservice.saveToFile(playerConfig, player);
                    } else
                        player.sendMessage(Configuration.PREFIX + "Not enough claimblocks! You need " + ((totalClaimBlocksInUse + regionSize) - totalClaimBlocks) + " blocks more!");
                } else
                    player.sendMessage(Configuration.PREFIX+"Claim not big enough! Claims must be atleast 6x6 wide.");
            } else
                player.sendMessage(Configuration.PREFIX + "Claim with that name already exist");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void removeClaim(final Player player, final String claimName, final RegionManager regionManager) {
        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
            final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(player));
            final int totalClaimBlocksInUse = (Integer) playerConfig.get("player.totalClaimBlocksInUse");
            playerConfig.set("player.name", player.getName());
            int childClaimsVolume = 0;
            for(final ProtectedRegion region : regionManager.getRegions().values())
                if(region.getParent() != null && region.getParent().getId().equalsIgnoreCase("claim_" + player.getUniqueId().toString() + "_" + claimName))
                    childClaimsVolume += region.volume()/256;
            final List<String> claims = (List<String>) playerConfig.getList("player.claims");
            final ProtectedRegion region = regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName);
            playerConfig.set("player.claims", claims);
            playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse - childClaimsVolume - (region.volume() / 256));
            fileservice.saveToFile(playerConfig, player);
            regionManager.removeRegion("claim_" + player.getUniqueId().toString() + "_" + claimName);
            clientService.resetClaimBorder(player, region);
            player.sendMessage(Configuration.PREFIX + "Claim " + claimName + " has been removed!");
        } else {
            player.sendMessage(Configuration.PREFIX + "No claim with that name found! " + "claim_" + player.getUniqueId().toString() + "_" + claimName);
        }
    }

    @Override
    public void listPlayerClaims(final Player player, final RegionManager regionManager) {
        final List<ProtectedRegion> playersClaim = Lists.newArrayList();
        final List <String> claims = Lists.newArrayList();
        player.sendMessage(Configuration.PREFIX + "Your claims:");
        for (final ProtectedRegion region : regionManager.getRegions().values()) {
            if (region.getId().contains("claim_" + player.getUniqueId().toString())) {
                playersClaim.add(region);
                claims.add(region.getId());
                player.sendMessage(Configuration.PREFIX + "Claim: " + region.getId().split(player.getUniqueId().toString() + "_")[1] +
                        ": x:" + region.getMinimumPoint().getX() + ", z:" + region.getMinimumPoint().getZ() + " (" + region.volume() / 256 + " blocks)");
            }
        }
        final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(player));
        playerConfig.set("player.claims", claims);
        fileservice.saveToFile(playerConfig, player);
    }

    @Override
    public void getClaimInfoById(final Player player, final String claimId, final RegionManager regionManager) {
        final ProtectedRegion region = regionManager.getRegions().get("claim_"+player.getUniqueId()+"_"+claimId);
        if (region != null && (region.getOwners().contains(player.getName()) || (region.getOwners().contains(player.getUniqueId())))) {
            player.sendMessage(Configuration.PREFIX + "Claim information:");
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
        } else
            player.sendMessage(Configuration.PREFIX+"Could not find a claim with that name!");
    }

    @Override
    public void getClaimInfoFromPlayerPosition(final Player player, final RegionManager regionManager) {
        final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
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

    @Override
    public void addMember(final Player player, final String claimName, final String member, final RegionManager regionManager) {
        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
            regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getMembers().addPlayer(member);
            player.sendMessage(Configuration.PREFIX+ "Added " + member + " to the claim!");
        } else {
            player.sendMessage(Configuration.PREFIX+ "No claim with that name exist!");
        }
    }

    @Override
    public void removeMember(final Player player, final String claimName, final String member, final RegionManager regionManager) {
        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
            regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getMembers().removePlayer(member);
            player.sendMessage(Configuration.PREFIX+ "Removed " + member + " to the claim!");
        } else {
            player.sendMessage(Configuration.PREFIX+ "No claim with that name exist!");
        }
    }

    @Override
    public boolean setFlag(final Player player, final String claimName, final String flagName, final String flagValue, final RegionManager regionManager) {
        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
            StateFlag.State stateFlag = StateFlag.State.DENY;
            boolean stringFlag = false;
            if (flagValue.equalsIgnoreCase("true") || flagValue.equalsIgnoreCase("allow")) {
                stateFlag = StateFlag.State.ALLOW;
            }
            final Map<Flag<?>, Object> mapFlags = Maps.newHashMap();
            mapFlags.putAll(regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getFlags());
            if(Configuration.DONATORFLAGS.contains(flagName.toLowerCase())) {
                if(player.hasPermission("gpflags.ownerfly")){
                    if(Configuration.getFlag(flagName) != null) {
                        mapFlags.put(Configuration.getFlag(flagName), stateFlag);
                    } else if(Configuration.getStringFlag(flagName) != null) {
                        stringFlag = true;
                        mapFlags.put(Configuration.getStringFlag(flagName), flagValue);
                    } else{
                        player.sendMessage(Configuration.PREFIX+"No such flag!");
                        return false;
                    }
                }else {
                    player.sendMessage(Configuration.PREFIX+"You do not have the fuycking permission! :) cunt sam will shank you with an acid knife m8.");
                    return false;
                }
            } else {
                if(Configuration.getFlag(flagName) !=(null)) {
                    mapFlags.put(Configuration.getFlag(flagName), stateFlag);
                }else if(Configuration.getStringFlag(flagName) !=(null)) {
                    stringFlag = true;
                    mapFlags.put(Configuration.getStringFlag(flagName), flagValue);
                }
                else {
                    player.sendMessage(Configuration.PREFIX+"No such flag!");
                    return false;
                }
            }
            regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).setFlags(mapFlags);
            if(stringFlag)
                player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " set to " + flagValue);
            else
                player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " set to " + stateFlag.name());
        }
        else
            player.sendMessage(Configuration.PREFIX+"No claim with the name " + claimName + " exists!");
        return true;
    }

    @Override
    public boolean removeFlag(final Player player, final String claimName, final String flagName, final RegionManager regionManager) {
        if (regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName) != null) {
            Flag flag = null;
            if(Configuration.DONATORFLAGS.contains(flagName.toLowerCase())) {
                if(player.hasPermission("gpflags.ownerfly")){
                    if(Configuration.getFlag(flagName) != null) {
                        flag = Configuration.getFlag(flagName);
                    }
                    if(Configuration.getStringFlag(flagName) != null) {
                        flag = Configuration.getStringFlag(flagName);
                    }
                }else {
                    player.sendMessage(Configuration.PREFIX+"You do not have permission!");
                    return false;
                }
            } else {
                if(Configuration.getFlag(flagName) != null) {
                    flag = Configuration.getFlag(flagName);
                } else if(Configuration.getStringFlag(flagName) != null) {
                    flag = Configuration.getStringFlag(flagName);
                } else {
                    player.sendMessage(Configuration.PREFIX+"No such flag!");
                    return false;
                }
            }
            final Map<Flag<?>, Object> claimFlags = regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).getFlags();
            claimFlags.remove(flag);
            regionManager.getRegion("claim_" + player.getUniqueId().toString() + "_" + claimName).setFlags(claimFlags);
            player.sendMessage(Configuration.PREFIX + "Flag " + flagName + " removed from: " + claimName);
        } else
            player.sendMessage(Configuration.PREFIX+"Could not find claim with id: " + claimName);
        return false;
    }

    @Override
    public boolean expandClaim(final Player player, final int amount, final RegionManager regionManager) {
        final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(fileservice.getPlayerFile(player));
        final int totalClaimBlocksInUse = (Integer) playerConfig.get("player.totalClaimBlocksInUse");
        final int totalClaimBlocks = (Integer) playerConfig.get("player.totalClaimBlocks");

        final ApplicableRegionSet regionList = regionManager.getApplicableRegions(BlockVector3.at(player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ()));
        if(!regionList.getRegions().isEmpty()) {
            for (final ProtectedRegion region : regionList) {
                if (region.getId().startsWith("claim_"+player.getUniqueId().toString())) {
                    final BlockVector3 p1 = region.getMinimumPoint();
                    final BlockVector3 p2 = region.getMaximumPoint();
                    ProtectedRegion newRegion = null;
                    if(player.getFacing() == BlockFace.NORTH) {
                        newRegion = new ProtectedCuboidRegion(region.getId(), p1.subtract(0,0,Integer.valueOf(amount)), p2);
                    } else if(player.getFacing() == BlockFace.SOUTH){
                        newRegion = new ProtectedCuboidRegion(region.getId(), p1, p2.add(0,0, Integer.valueOf(amount)));
                    } else if(player.getFacing() == BlockFace.WEST){
                        newRegion = new ProtectedCuboidRegion(region.getId(), p1.subtract(Integer.valueOf(amount),0,0), p2);
                    } else if(player.getFacing() == BlockFace.EAST) {
                        newRegion = new ProtectedCuboidRegion(region.getId(),p1, p2.add(Integer.valueOf(amount),0, 0));
                    } else {
                        player.sendMessage(Configuration.PREFIX+"Something went wrong! Please contact an administrator.");
                        return false;
                    }
                    if(newRegion.getIntersectingRegions(regionManager.getRegions().values()).size() > 0) {
                        for (final ProtectedRegion overlapingClaim : newRegion.getIntersectingRegions(regionManager.getRegions().values())) {
                            if(overlapingClaim.getParent() == null && overlapingClaim.getId() != newRegion.getId()) {
                                if(region.getParent() != null && region.getParent().getId() == overlapingClaim.getId()) {
                                    player.sendMessage(Configuration.PREFIX+"You can not expand a child claim!");
                                    return false;
                                } else {
                                    player.sendMessage(Configuration.PREFIX+"Expansion failed! Claim overlaping claim: " + overlapingClaim.getId().substring(43));
                                    return false;
                                }
                            }
                        }
                    }
                    final int newVolume = (newRegion.volume()/256) - (region.volume()/256);
                    if((totalClaimBlocksInUse + newVolume) <= totalClaimBlocks) {
                        try {
                            playerConfig.set("player.totalClaimBlocksInUse", totalClaimBlocksInUse + newVolume);
                            for (final ProtectedRegion claim : regionManager.getRegions().values()) {
                                if(claim.getParent() != null && claim.getParent().getId().equalsIgnoreCase(region.getId())){
                                    claim.clearParent();
                                        claim.setParent(newRegion);
                                }
                            }
                            clientService.resetClaimBorder(player, region);
                            newRegion.copyFrom(region);
                            regionManager.removeRegion(region.getId());
                            regionManager.addRegion(newRegion);
                            fileservice.saveToFile(playerConfig, player);
                            player.sendMessage(Configuration.PREFIX + "Claim expanded!");
                            return true;
                        } catch (final ProtectedRegion.CircularInheritanceException e) {
                            e.printStackTrace();
                        }
                    } else
                        player.sendMessage(Configuration.PREFIX + "You do not have enough claimblocks! You need " +
                                ((totalClaimBlocksInUse+newVolume)-totalClaimBlocks) + " more blocks.");
                }
            }
        }
        else {
            player.sendMessage(Configuration.PREFIX + "You are not standing in your claim.");
        }
        return false;
    }

    @Override
    public void renameClaim(final Player player, final String claimName, final String newClaimName, final RegionManager regionManager) {
        if(regionManager.getRegion("claim_"+player.getUniqueId().toString()+"_"+claimName) != null) {
            try {
                final ProtectedRegion region = regionManager.getRegion("claim_"+player.getUniqueId().toString()+"_"+claimName);
                final ProtectedRegion newRegion = new ProtectedCuboidRegion("claim_" + player.getUniqueId().toString() + "_" + newClaimName,
                        region.getMinimumPoint(), region.getMaximumPoint());
                for (final ProtectedRegion claim : regionManager.getRegions().values()) {
                    if(claim.getParent() != null && claim.getParent().getId().equalsIgnoreCase(region.getId())){
                        claim.clearParent();
                        claim.setParent(newRegion);
                    }
                }
                clientService.resetClaimBorder(player, region);
                newRegion.copyFrom(region);
                regionManager.addRegion(newRegion);
                regionManager.removeRegion(region.getId());
                player.sendMessage(Configuration.PREFIX + "Claim renamed!");
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        else
            player.sendMessage(Configuration.PREFIX + "No claim with that name exists.");
    }

    @Override
    public void getNearbyClaims(final Player player, final int radius, final RegionManager regionManager) {
        final ProtectedCuboidRegion region = new ProtectedCuboidRegion("tmpClaimName",
                BlockVector3.at(player.getLocation().getX()-radius, 0, player.getLocation().getZ()-radius),
                BlockVector3.at(player.getLocation().getX()+radius, 256, player.getLocation().getZ()+radius));
        final List<ProtectedRegion> overlapingClaims = region.getIntersectingRegions(regionManager.getRegions().values());
        if(!overlapingClaims.isEmpty()) {
            player.sendMessage(Configuration.PREFIX+"Found " + overlapingClaims.size() + " claims nearby:");
            overlapingClaims.forEach(claim -> {
                player.sendMessage(ChatColor.YELLOW+"Claim: " +  claim.getId().substring(43));
            });
        } else
            player.sendMessage(Configuration.PREFIX+"No claims nearby.");
    }
}
