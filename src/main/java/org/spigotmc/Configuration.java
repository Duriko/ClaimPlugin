package org.spigotmc;

import com.google.common.collect.Lists;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import org.bukkit.ChatColor;

import java.util.List;

public final class Configuration {

    public static final String PREFIX;
    public static final String ADMINPREFIX;
    public static final List<String> DONATORFLAGS;

    static  {
        PREFIX = ChatColor.WHITE+"["+ChatColor.YELLOW+"Claim"+ChatColor.WHITE+"] "+ChatColor.YELLOW;
        ADMINPREFIX = ChatColor.WHITE+"["+ChatColor.YELLOW+"ClaimAdmin"+ChatColor.WHITE+"] "+ChatColor.YELLOW;
        DONATORFLAGS = Lists.newArrayList("fly", "greeting-title", "farewell-title", "time-lock");
    }


    public static StringFlag getStringFlag(final String flag) {
        switch (flag.toLowerCase()) {
            case "greeting":
                return Flags.GREET_MESSAGE;
            case "farewell":
                return Flags.FAREWELL_MESSAGE;

                //Donator flags
            case "greeting-title":
                return Flags.GREET_TITLE;
            case "farewell-title":
                return Flags.FAREWELL_TITLE;
            case "time-lock":
                return Flags.TIME_LOCK;
        }
        return null;
    }

    public static StateFlag getFlag(final String flag) {
        switch (flag.toLowerCase()) {
            case "build":
                return Flags.BUILD;
            case "use":
                return Flags.USE;
            case "interact":
                return Flags.INTERACT;
            case "damage-animals":
                return Flags.DAMAGE_ANIMALS;
            case "pvp":
                return Flags.PVP;
            case "mob-spawning":
                return Flags.MOB_SPAWNING;
            case "mob-damage":
                return Flags.MOB_DAMAGE;
            case "creeper-explosion":
                return Flags.CREEPER_EXPLOSION;
            case "other-explosion":
                return Flags.OTHER_EXPLOSION;
            case "water-flow":
                return Flags.WATER_FLOW;
            case "lava-flow":
                return Flags.LAVA_FLOW;
            case "snow-melt":
                return Flags.SNOW_MELT;
            case "snow-fall":
                return Flags.SNOW_FALL;
            case "ice-form":
                return Flags.ICE_FORM;
            case "ice-melt":
                return Flags.ICE_MELT;
            case "frosted-ice-form":
                return Flags.FROSTED_ICE_FORM;
            case "frosted-ice-melt":
                return Flags.FROSTED_ICE_MELT;
            case "leaf-decay":
                return Flags.LEAF_DECAY;
            case "grass-spread":
                return Flags.GRASS_SPREAD;
            case "mycelium-spread":
                return Flags.MYCELIUM_SPREAD;
            case "vine-growth":
                return Flags.VINE_GROWTH;
            case "crop-growth":
                return Flags.CROP_GROWTH;
            case "entry":
                return Flags.ENTRY;
            case "enderpearl":
                return Flags.ENDERPEARL;
            case "chorus-fruit-teleport":
                return Flags.CHORUS_TELEPORT;
            case "vehicle-place":
                return Flags.PLACE_VEHICLE;
            case "vechicle-destroy":
                return Flags.DESTROY_VEHICLE;
            case "fall-damage":
                return Flags.FALL_DAMAGE;

                //Donator flag
            case "fly":
                return net.goldtreeservers.worldguardextraflags.flags.Flags.FLY;
        }
        return null;
    }

}
