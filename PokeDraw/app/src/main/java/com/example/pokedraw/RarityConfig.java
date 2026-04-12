package com.example.pokedraw;

import java.util.ArrayList;
import java.util.List;

public class RarityConfig {

    public static final String COMMON    = "Common";
    public static final String RARE      = "Rare";
    public static final String MYTHICAL  = "Epic";
    public static final String LEGENDARY = "Legendary";

    public static final int TOTAL_POKEMON = 1025;

    // Cumulative thresholds out of 1000
    public static final int COMMON_THRESHOLD   = 745; // 74.5%
    public static final int RARE_THRESHOLD     = 945; // 20%
    public static final int MYTHICAL_THRESHOLD = 995; // 5% Epic
    // 995–999 = Legendary (0.5%)

    /**
     * Pools are built dynamically from EvolutionChain data:
     *   Common    = stage 1 of a multi-stage chain (not legendary)
     *   Rare      = stage 2 of a 3-stage chain (not legendary)
     *   Epic      = final/only form (stage 2 of 2-stage, stage 3, or standalone) (not legendary)
     *   Legendary = explicitly legendary IDs
     */
    public static final int[] LEGENDARY_IDS;
    public static final int[] COMMON_IDS;
    public static final int[] RARE_IDS;
    public static final int[] MYTHICAL_IDS; // Epic

    static {
        List<Integer> legendary = new ArrayList<>();
        List<Integer> common    = new ArrayList<>();
        List<Integer> rare      = new ArrayList<>();
        List<Integer> epic      = new ArrayList<>();

        for (int id = 1; id <= TOTAL_POKEMON; id++) {
            if (EvolutionChain.LEGENDARY_SET.contains(id)) {
                legendary.add(id);
                continue;
            }
            int baseId   = EvolutionChain.getBaseId(id);
            int stage    = EvolutionChain.getDrawStage(id);
            int maxStages = EvolutionChain.getMaxStages(baseId);

            if (maxStages == 1) {
                // Standalone (no evolution) → Epic
                epic.add(id);
            } else if (stage == 1) {
                // Base form → Common
                common.add(id);
            } else if (stage == 2 && maxStages == 3) {
                // Middle evolution of 3-stage chain → Rare
                rare.add(id);
            } else {
                // Final evolution (stage 2 of 2-stage, or stage 3) → Epic
                epic.add(id);
            }
        }

        LEGENDARY_IDS = toArray(legendary);
        COMMON_IDS    = toArray(common);
        RARE_IDS      = toArray(rare);
        MYTHICAL_IDS  = toArray(epic);
    }

    private static int[] toArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
