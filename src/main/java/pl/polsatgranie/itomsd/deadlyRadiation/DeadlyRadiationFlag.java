package pl.polsatgranie.itomsd.deadlyRadiation;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

public class DeadlyRadiationFlag {
    public static StateFlag DEADLY_RADIATION = new StateFlag("deadly-radiation", false);

    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            registry.register(DEADLY_RADIATION);
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("deadly-radiation");
            if (existing instanceof StateFlag) {
                DEADLY_RADIATION = (StateFlag) existing;
            } else {
                throw e;
            }
        }
    }
}
