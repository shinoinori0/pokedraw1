package com.example.pokedraw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class EvolutionChain {

    public static final int TIER_NORMAL = 0;
    public static final int TIER_SHINY  = 1;
    public static final int TIER_HOLO   = 2;
    public static final int TIER_GOLD   = 3;

    public static final int EXP_STAGE2 = 5;
    public static final int EXP_STAGE3 = 15;

    private static final Map<Integer, int[]>     FORWARD       = new HashMap<>();
    private static final Map<Integer, int[]>     BRANCH_STAGE2 = new HashMap<>();
    private static final Map<Integer, int[]>     BRANCH_STAGE3 = new HashMap<>();
    private static final Map<Integer, Integer>   BASE_OF       = new HashMap<>();
    private static final Map<Integer, Integer>   STAGE_OF      = new HashMap<>();
    private static final Map<Integer, int[]>     SPAWNERS      = new HashMap<>();

    /** All true legendary / mythical Pokémon IDs across all gens. */
    public static final Set<Integer> LEGENDARY_SET = new HashSet<>(Arrays.asList(
        // Gen 1
        144,145,146,150,151,
        // Gen 2
        243,244,245,249,250,251,
        // Gen 3
        377,378,379,380,381,382,383,384,385,386,
        // Gen 4
        480,481,482,483,484,485,486,487,488,489,490,491,492,493,
        // Gen 5
        494,638,639,640,641,642,643,644,645,646,647,648,649,
        // Gen 6
        716,717,718,719,720,721,
        // Gen 7
        785,786,787,788,789,790,791,792,793,794,795,796,797,798,799,800,801,802,803,804,805,806,807,
        // Gen 8
        888,889,890,893,894,895,896,897,898,905,
        // Gen 9
        1001,1002,1003,1004,1007,1008,1009,1010,1017,1018,1019,1020,1021,1022,1023,1024,1025
    ));

    private static final Random RNG = new Random();

    static { initChains(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void c2(int b, int e1) {
        FORWARD.put(b, new int[]{e1});
        r(b,b,1); r(e1,b,2);
    }
    private static void c3(int b, int e1, int e2) {
        FORWARD.put(b, new int[]{e1,e2});
        r(b,b,1); r(e1,b,2); r(e2,b,3);
    }
    private static void r(int id, int base, int stage) {
        BASE_OF.put(id, base);
        STAGE_OF.put(id, stage);
    }

    // ── Chain data ────────────────────────────────────────────────────────────

    private static void initChains() {

        // ── Gen 1 ─────────────────────────────────────────────────────────────
        c3(1,2,3); c3(4,5,6); c3(7,8,9);
        c3(10,11,12); c3(13,14,15); c3(16,17,18);
        c2(19,20); c2(21,22); c2(23,24); c2(27,28);
        c3(29,30,31); c3(32,33,34);
        c2(37,38);
        c3(41,42,169); // Zubat→Golbat→Crobat
        // Oddish→Gloom→{Vileplume/Bellossom}
        FORWARD.put(43,new int[]{44}); r(43,43,1); r(44,43,2); r(45,43,3); r(182,43,3);
        BRANCH_STAGE3.put(44,new int[]{45,182});
        c2(46,47); c2(48,49); c2(50,51); c2(52,53); c2(54,55); c2(56,57); c2(58,59);
        // Poliwag→Poliwhirl→{Poliwrath/Politoed}
        FORWARD.put(60,new int[]{61}); r(60,60,1); r(61,60,2); r(62,60,3); r(186,60,3);
        BRANCH_STAGE3.put(61,new int[]{62,186});
        c3(63,64,65); c3(66,67,68); c3(69,70,71);
        c2(72,73); c2(77,78);
        // Slowpoke→{Slowbro/Slowking}
        BRANCH_STAGE2.put(79,new int[]{80,199}); r(79,79,1); r(80,79,2); r(199,79,2);
        // Magnemite→Magneton→Magnezone
        c3(81,82,462);
        c2(84,85); c2(86,87); c2(88,89); c2(90,91);
        c3(92,93,94);
        c2(95,208); // Onix→Steelix
        c2(96,97); c2(98,99); c2(100,101); c2(102,103); c2(104,105);
        // Lickitung→Lickilicky
        c2(108,463);
        c2(109,110);
        // Rhyhorn→Rhydon→Rhyperior
        c3(111,112,464);
        // Chansey standalone (Happiny chain handled in Gen 4)
        c2(114,465); // Tangela→Tangrowth
        c3(116,117,230); // Horsea→Seadra→Kingdra
        c2(118,119); c2(120,121);
        // Scyther→{Scizor/Kleavor} — branch
        BRANCH_STAGE2.put(123,new int[]{212,900}); r(123,123,1); r(212,123,2); r(900,123,2);
        c2(129,130);
        // Eevee: spawner — all 8 eeveelutions
        SPAWNERS.put(133,new int[]{134,135,136,196,197,470,471,700});
        r(133,133,1);
        for (int id : new int[]{134,135,136,196,197,470,471,700}) r(id,id,1);
        // Porygon→Porygon2→Porygon-Z
        c3(137,233,474);
        c2(138,139); c2(140,141);
        c3(147,148,149);

        // ── Gen 2 ─────────────────────────────────────────────────────────────
        c3(152,153,154); c3(155,156,157); c3(158,159,160);
        c2(161,162); c2(163,164); c2(165,166); c2(167,168);
        c2(170,171);
        c3(172,25,26);  // Pichu→Pikachu→Raichu
        c3(173,35,36);  // Cleffa→Clefairy→Clefable
        c3(174,39,40);  // Igglybuff→Jigglypuff→Wigglytuff
        c2(175,176);    // Togepi→Togetic (→Togekiss in Gen 4, see below)
        c2(177,178);
        c3(179,180,181);
        c2(183,184);    // Marill→Azumarill (overridden by Azurill below)
        c3(187,188,189);
        c2(191,192); c2(194,195);
        c2(204,205); c2(209,210); c2(216,217); c2(218,219); c2(220,221);
        c2(223,224); c2(228,229); c2(231,232);
        // Tyrogue: spawner
        SPAWNERS.put(236,new int[]{106,107,237});
        r(236,236,1); for (int id : new int[]{106,107,237}) r(id,id,1);
        // Elekid→Electabuzz→Electivire
        c3(239,125,466);
        // Magby→Magmar→Magmortar
        c3(240,126,467);
        c3(246,247,248);
        // Smoochum→Jynx
        c2(238,124);
        // Mantine baby chain handled in Gen 4

        // ── Gen 3 ─────────────────────────────────────────────────────────────
        c3(252,253,254); c3(255,256,257); c3(258,259,260);
        c2(261,262); c2(263,264);
        // Wurmple branches
        BRANCH_STAGE2.put(265,new int[]{266,268});
        r(265,265,1); r(266,265,2); r(268,265,2); r(267,265,3); r(269,265,3);
        FORWARD.put(266,new int[]{267}); FORWARD.put(268,new int[]{269});
        c3(270,271,272); c3(273,274,275);
        c2(276,277); c2(278,279);
        // Ralts→Kirlia→{Gardevoir/Gallade}
        FORWARD.put(280,new int[]{281}); r(280,280,1); r(281,280,2); r(282,280,3); r(475,280,3);
        BRANCH_STAGE3.put(281,new int[]{282,475});
        c2(283,284); c2(285,286);
        c3(287,288,289);
        // Nincada→{Ninjask/Shedinja}
        BRANCH_STAGE2.put(290,new int[]{291,292}); r(290,290,1); r(291,290,2); r(292,290,2);
        c3(293,294,295); c2(296,297);
        // Azurill→Marill→Azumarill
        FORWARD.put(298,new int[]{183,184}); r(298,298,1); r(183,298,2); r(184,298,3);
        c2(300,301); c2(299,476); // Nosepass→Probopass
        c3(304,305,306); c2(307,308); c2(309,310);
        c2(316,317); c2(318,319); c2(320,321); c2(322,323); c2(325,326);
        c3(328,329,330); c2(331,332); c2(333,334); c2(339,340); c2(341,342);
        c2(343,344); c2(345,346); c2(347,348); c2(349,350);
        c2(353,354);
        // Duskull→Dusclops→Dusknoir
        c3(355,356,477);
        c3(363,364,365);
        // Snorunt→{Glalie/Froslass}
        BRANCH_STAGE2.put(361,new int[]{362,478}); r(361,361,1); r(362,361,2); r(478,361,2);
        // Clamperl→{Huntail/Gorebyss}
        BRANCH_STAGE2.put(366,new int[]{367,368}); r(366,366,1); r(367,366,2); r(368,366,2);
        c3(371,372,373); c3(374,375,376);
        // Wynaut→Wobbuffet
        c2(360,202);
        // Budew→Roselia→Roserade
        c3(406,315,407);
        // Chingling baby of Chimecho
        c2(433,358);
        // Bonsly baby of Sudowoodo
        c2(438,185);
        // Mime Jr. baby of Mr. Mime → Mr. Rime
        c3(439,122,866);
        // Happiny→Chansey→Blissey
        c3(440,113,242);
        // Mantyke→Mantine
        c2(458,226);
        // Swinub→Piloswine→Mamoswine
        c3(220,221,473);
        // Togepi→Togetic→Togekiss
        c3(175,176,468);

        // ── Gen 4 (387–493) ───────────────────────────────────────────────────
        c3(387,388,389); c3(390,391,392); c3(393,394,395);
        c3(396,397,398);
        c2(399,400); c2(401,402);
        c3(403,404,405);
        c2(408,409); c2(410,411);
        // Burmy→{Wormadam/Mothim}
        BRANCH_STAGE2.put(412,new int[]{413,414}); r(412,412,1); r(413,412,2); r(414,412,2);
        c2(415,416);
        // Pachirisu standalone
        r(417,417,1);
        c2(418,419); c2(420,421); c2(422,423);
        // Aipom→Ambipom
        c2(190,424);
        c2(425,426); c2(427,428);
        // Misdreavus→Mismagius
        c2(200,429);
        // Murkrow→Honchkrow
        c2(198,430);
        c2(431,432);
        c2(434,435); c2(436,437);
        c3(443,444,445);
        // Munchlax→Snorlax
        c2(446,143);
        c2(447,448); c2(449,450); c2(451,452); c2(453,454);
        // Carnivine standalone
        r(455,455,1);
        c2(456,457);
        c2(459,460); // Snover→Abomasnow
        // Weavile: Sneasel→Weavile (branch with Sneasler 903)
        BRANCH_STAGE2.put(215,new int[]{461,903}); r(215,215,1); r(461,215,2); r(903,215,2);
        // Gligar→Gliscor
        c2(207,472);
        // Rotom standalone
        r(479,479,1);

        // ── Gen 5 (494–649) ───────────────────────────────────────────────────
        c3(495,496,497); c3(498,499,500);
        c3(501,502,503);
        c2(504,505);
        c3(506,507,508);
        c2(509,510); c2(511,512); c2(513,514); c2(515,516); c2(517,518);
        c3(519,520,521);
        c2(522,523);
        c3(524,525,526);
        c2(527,528); c2(529,530);
        // Audino standalone
        r(531,531,1);
        c3(532,533,534); c3(535,536,537);
        // Throh, Sawk standalone
        r(538,538,1); r(539,539,1);
        c3(540,541,542); c3(543,544,545);
        c2(546,547); c2(548,549);
        // Basculin standalone
        r(550,550,1);
        c3(551,552,553); c2(554,555);
        // Maractus standalone
        r(556,556,1);
        c2(557,558); c2(559,560);
        // Sigilyph standalone
        r(561,561,1);
        c2(562,563); c2(564,565); c2(566,567); c2(568,569); c2(570,571); c2(572,573);
        c3(574,575,576); c3(577,578,579);
        c2(580,581); c3(582,583,584); c2(585,586);
        // Emolga standalone
        r(587,587,1);
        c2(588,589); c2(590,591); c2(592,593);
        // Alomomola standalone
        r(594,594,1);
        c2(595,596); c2(597,598);
        c3(599,600,601); c3(602,603,604);
        c2(605,606); c3(607,608,609); c3(610,611,612); c2(613,614);
        // Cryogonal standalone
        r(615,615,1);
        c2(616,617);
        // Stunfisk standalone
        r(618,618,1);
        c2(619,620);
        // Druddigon standalone
        r(621,621,1);
        c2(622,623); c2(624,625);
        // Bouffalant standalone
        r(626,626,1);
        c2(627,628); c2(629,630);
        // Heatmor, Durant standalone
        r(631,631,1); r(632,632,1);
        c3(633,634,635); c2(636,637);

        // ── Gen 6 (650–721) ───────────────────────────────────────────────────
        c3(650,651,652); c3(653,654,655); c3(656,657,658);
        c2(659,660); c3(661,662,663); c3(664,665,666);
        c2(667,668); c3(669,670,671); c2(672,673); c2(674,675);
        // Furfrou standalone
        r(676,676,1);
        c2(677,678); c3(679,680,681); c2(682,683); c2(684,685); c2(686,687); c2(688,689);
        c2(690,691); c2(692,693); c2(694,695); c2(696,697); c2(698,699);
        // Hawlucha, Dedenne, Carbink standalone
        r(701,701,1); r(702,702,1); r(703,703,1);
        c3(704,705,706);
        // Klefki standalone
        r(707,707,1);
        c2(708,709); c2(710,711); c2(712,713); c2(714,715);

        // ── Gen 7 (722–809) ───────────────────────────────────────────────────
        c3(722,723,724); c3(725,726,727); c3(728,729,730);
        c3(731,732,733); c2(734,735); c3(736,737,738); c2(739,740);
        // Oricorio standalone
        r(741,741,1);
        c2(742,743); c2(744,745);
        // Wishiwashi standalone
        r(746,746,1);
        c2(747,748); c2(749,750); c2(751,752); c2(753,754); c2(755,756); c2(757,758);
        c2(759,760); c3(761,762,763);
        // Comfey, Oranguru, Passimian standalone
        r(764,764,1); r(765,765,1); r(766,766,1);
        c2(767,768); c2(769,770);
        // Pyukumuku standalone
        r(771,771,1);
        // Type: Null→Silvally
        c2(772,773);
        // Minior, Komala, Turtonator, Togedemaru, Mimikyu, Bruxish, Drampa, Dhelmise standalone
        r(774,774,1); r(775,775,1); r(776,776,1); r(777,777,1);
        r(778,778,1); r(779,779,1); r(780,780,1); r(781,781,1);
        c3(782,783,784);
        // Cosmog→Cosmoem→{Solgaleo/Lunala} — but these are legendary
        FORWARD.put(789,new int[]{790}); r(789,789,1); r(790,789,2); r(791,789,3); r(792,789,3);
        BRANCH_STAGE3.put(790,new int[]{791,792});
        // Poipole→Naganadel
        c2(803,804);
        // Meltan→Melmetal
        c2(808,809);

        // ── Gen 8 (810–905) ───────────────────────────────────────────────────
        c3(810,811,812); c3(813,814,815); c3(816,817,818);
        c2(819,820); c3(821,822,823); c3(824,825,826); c2(827,828); c2(829,830);
        c2(831,832); c2(833,834); c2(835,836); c3(837,838,839);
        // Applin→{Flapple/Appletun}
        BRANCH_STAGE2.put(840,new int[]{841,842}); r(840,840,1); r(841,840,2); r(842,840,2);
        c2(843,844);
        // Cramorant standalone
        r(845,845,1);
        c2(846,847); c2(848,849); c2(850,851); c2(852,853); c2(854,855);
        c3(856,857,858); c3(859,860,861);
        // Obstagoon: Zigzagoon→Linoone→Obstagoon
        c3(263,264,862);
        // Cursola: Corsola→Cursola
        c2(222,864);
        // Sirfetch'd: Farfetch'd→Sirfetch'd
        c2(83,865);
        // Runerigus: treat as standalone (Galarian form)
        r(867,867,1);
        c2(868,869);
        // Falinks, Pincurchin standalone
        r(870,870,1); r(871,871,1);
        c2(872,873);
        // Stonjourner, Eiscue, Indeedee, Morpeko standalone
        r(874,874,1); r(875,875,1); r(876,876,1); r(877,877,1);
        c2(878,879);
        // Fossil standalones
        r(880,880,1); r(881,881,1); r(882,882,1); r(883,883,1);
        // Duraludon standalone
        r(884,884,1);
        c3(885,886,887);
        // Kubfu→Urshifu
        c2(891,892);
        // Wyrdeer: Stantler→Wyrdeer
        c2(234,899);
        // Ursaluna: Teddiursa→Ursaring→Ursaluna
        c3(216,217,901);
        // Basculegion: Basculin→Basculegion
        c2(550,902);
        // Overqwil: Qwilfish→Overqwil
        c2(211,904);

        // ── Gen 9 (906–1025) ──────────────────────────────────────────────────
        c3(906,907,908); c3(909,910,911); c3(912,913,914);
        c2(915,916); c2(917,918); c2(919,920); c3(921,922,923);
        c2(924,925); c2(926,927); c3(928,929,930);
        // Squawkabilly standalone
        r(931,931,1);
        c3(932,933,934);
        // Charcadet→{Armarouge/Ceruledge}
        BRANCH_STAGE2.put(935,new int[]{936,937}); r(935,935,1); r(936,935,2); r(937,935,2);
        c2(938,939); c2(940,941); c2(942,943); c2(944,945); c2(946,947); c2(948,949);
        // Klawf standalone
        r(950,950,1);
        c2(951,952); c2(953,954); c2(955,956); c3(957,958,959); c2(960,961);
        c2(962,963); c2(964,965); c3(966,967,968); c2(969,970); c2(971,972);
        // Cyclizar standalone
        r(973,973,1);
        c2(974,975); c2(976,977);
        // Greavard→Houndstone
        c2(978,979);
        c2(980,981); c2(982,983); c2(984,985); c2(986,987); c2(988,989);
        c3(990,991,992); c2(993,994); c2(995,996); c2(997,998); c2(999,1000);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static int getBaseId(int id) {
        Integer b = BASE_OF.get(id);
        return b != null ? b : id;
    }

    public static int getDrawStage(int id) {
        Integer s = STAGE_OF.get(id);
        return s != null ? s : 1;
    }

    public static int getMaxStages(int baseId) {
        if (SPAWNERS.containsKey(baseId)) return 1;
        if (BRANCH_STAGE2.containsKey(baseId)) {
            for (int opt : BRANCH_STAGE2.get(baseId))
                if (FORWARD.containsKey(opt)) return 3;
            return 2;
        }
        int[] chain = FORWARD.get(baseId);
        if (chain == null) return 1;
        if (chain.length >= 2) return 3;
        int e1 = chain[0];
        return (BRANCH_STAGE3.containsKey(e1) || FORWARD.containsKey(e1)) ? 3 : 2;
    }

    public static int computeStage(int baseId, int exp) {
        if (SPAWNERS.containsKey(baseId)) return 1;
        int max = getMaxStages(baseId);
        if (max >= 3 && exp >= EXP_STAGE3) return 3;
        if (max >= 2 && exp >= EXP_STAGE2) return 2;
        return 1;
    }

    public static int getPrestigeBase(int baseId) {
        int max = getMaxStages(baseId);
        if (max == 3) return EXP_STAGE3;
        if (max == 2) return EXP_STAGE2;
        return 0;
    }

    public static int computeTier(int baseId, int exp) {
        int p = exp - getPrestigeBase(baseId);
        if (p < 0)   return TIER_NORMAL;
        if (p >= 15) return TIER_GOLD;
        if (p >= 10) return TIER_HOLO;
        if (p >= 5)  return TIER_SHINY;
        return TIER_NORMAL;
    }

    public static int getNextDisplayId(int baseId, int currentDisplayId, int targetStage) {
        if (targetStage == 2) {
            if (BRANCH_STAGE2.containsKey(baseId)) {
                int[] opts = BRANCH_STAGE2.get(baseId);
                return opts[RNG.nextInt(opts.length)];
            }
            int[] chain = FORWARD.get(baseId);
            return (chain != null) ? chain[0] : currentDisplayId;
        }
        if (targetStage == 3) {
            if (BRANCH_STAGE3.containsKey(currentDisplayId)) {
                int[] opts = BRANCH_STAGE3.get(currentDisplayId);
                return opts[RNG.nextInt(opts.length)];
            }
            if (FORWARD.containsKey(currentDisplayId)) return FORWARD.get(currentDisplayId)[0];
            int[] base = FORWARD.get(baseId);
            if (base != null && base.length >= 2) return base[1];
        }
        return currentDisplayId;
    }

    public static boolean isSpawner(int baseId) { return SPAWNERS.containsKey(baseId); }

    public static int[] getSpawnTargets(int baseId) {
        int[] t = SPAWNERS.get(baseId);
        return t != null ? t : new int[0];
    }

    public static int[] getExpProgress(int baseId, int exp) {
        int stage       = computeStage(baseId, exp);
        int prestigeBase = getPrestigeBase(baseId);
        int tier        = computeTier(baseId, exp);
        if (exp < prestigeBase) {
            if (stage == 1) return new int[]{exp, EXP_STAGE2};
            return new int[]{exp - EXP_STAGE2, EXP_STAGE3 - EXP_STAGE2};
        }
        if (tier == TIER_GOLD) return new int[]{15, 15};
        int inPrestige = exp - prestigeBase;
        return new int[]{inPrestige - (tier * 5), 5};
    }

    public static String getProgressLabel(int baseId, int exp) {
        int tier = computeTier(baseId, exp);
        if (tier == TIER_GOLD) return "MAX";
        int[] p          = getExpProgress(baseId, exp);
        int prestigeBase = getPrestigeBase(baseId);
        if (exp < prestigeBase) {
            return p[0] + "/" + p[1] + "→S" + (computeStage(baseId, exp) + 1);
        }
        String[] next = {"Shiny","Holo","Gold"};
        return p[0] + "/" + p[1] + "→" + next[tier];
    }
}
