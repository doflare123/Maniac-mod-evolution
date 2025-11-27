package org.example.maniacrevolution.perk;

import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.perk.perks.common.*;
import org.example.maniacrevolution.perk.perks.maniac.*;
import org.example.maniacrevolution.perk.perks.survivor.*;

import java.util.*;
import java.util.stream.Collectors;

public class PerkRegistry {
    private static final Map<String, Perk> PERKS = new LinkedHashMap<>();

    public static void init() {
        // Общие перки (5 штук)
//        register(new BerserkerPerk());
//        register(new VampirePerk());
//        register(new SpeedsterPerk());
//        register(new FortressPerk());
//        register(new ScoutPerk());
        register(new BigmoneyPerk());
        register(new MegamindPerk());
        register(new GtoMedalPerk());
        register(new FearWavePerk());

        // Перки выживших (5 штук)
        register(new MimicPerk());
        register(new MedicPerk());
        register(new TrapperSurvivorPerk());
        register(new HackerPerk());
        register(new DecoyPerk());

        // Перки маньяка (5 штук)
        register(new PredatorPerk());
        register(new TerrorPerk());
        register(new TrapperManiacPerk());
        register(new ExecutionerPerk());
        register(new ShadowPerk());

        Maniacrev.LOGGER.info("Registered {} perks", PERKS.size());
    }

    public static void register(Perk perk) {
        if (PERKS.containsKey(perk.getId())) {
            throw new IllegalArgumentException("Perk with id " + perk.getId() + " already registered!");
        }
        PERKS.put(perk.getId(), perk);
    }

    public static Perk getPerk(String id) {
        return PERKS.get(id);
    }

    public static Collection<Perk> getAllPerks() {
        return Collections.unmodifiableCollection(PERKS.values());
    }

    public static List<Perk> getPerksForTeam(PerkTeam team) {
        return PERKS.values().stream()
                .filter(perk -> perk.isAvailableForTeam(team))
                .collect(Collectors.toList());
    }

    public static List<Perk> getPerksByType(PerkType type) {
        return PERKS.values().stream()
                .filter(perk -> perk.getType() == type)
                .collect(Collectors.toList());
    }

    public static List<Perk> getCommonPerks() {
        return PERKS.values().stream()
                .filter(perk -> perk.getTeam() == PerkTeam.ALL)
                .collect(Collectors.toList());
    }

    public static List<Perk> getSurvivorPerks() {
        return PERKS.values().stream()
                .filter(perk -> perk.getTeam() == PerkTeam.SURVIVOR)
                .collect(Collectors.toList());
    }

    public static List<Perk> getManiacPerks() {
        return PERKS.values().stream()
                .filter(perk -> perk.getTeam() == PerkTeam.MANIAC)
                .collect(Collectors.toList());
    }

    public static int getTotalPerkCount() {
        return PERKS.size();
    }
}