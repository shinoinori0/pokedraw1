package com.example.pokedraw;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.pokedraw.api.PokeApiService;
import com.example.pokedraw.api.PokemonResponse;
import com.example.pokedraw.api.RetrofitClient;
import com.example.pokedraw.model.OwnedPokemon;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class GameManager {

    private static final String PREFS_NAME        = "pokedraw_prefs";
    private static final String KEY_DRAW_DATE     = "draw_date";
    private static final String KEY_DRAW_COUNT    = "draw_count";
    private static final String KEY_BONUS_DRAWS   = "bonus_draws";
    private static final String KEY_GUESS_COUNT   = "guess_count";
    private static final String KEY_COLLECTION    = "local_collection";
    public  static final String KEY_DRAW_MODE_ONE  = "draw_mode_one_at_a_time";
    public  static final String KEY_BGM_MUTED      = "bgm_muted";
    public  static final String KEY_AUTO_DRAW      = "auto_draw";
    public  static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_REDEEMED_DRAWS = "redeemed_draws"; // never day-reset
    private static final int    MAX_DRAWS          = 3;
    private static final int    MAX_GUESSES        = 5;

    public interface CollectionCallback {
        void onLoaded(Map<Integer, OwnedPokemon> collection);
    }

    /** Fired when a Pokémon evolves to a new stage or a card tier upgrades. */
    public interface EvolutionListener {
        /** @param from display label before evolution, @param to display label after */
        void onEvolved(String from, String to);
    }

    private static GameManager instance;
    private final SharedPreferences prefs;
    private final Gson gson   = new Gson();
    private final Random rand = new Random();

    private EvolutionListener evolutionListener;

    private final Map<Integer, OwnedPokemon> localCollection = new HashMap<>();
    private boolean collectionLoaded = false;
    private final List<CollectionCallback> pendingCallbacks = new ArrayList<>();
    private DatabaseReference collectionRef;

    private GameManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadLocalCache();
        attachListener();
    }

    public void setEvolutionListener(EvolutionListener listener) { evolutionListener = listener; }

    public static GameManager getInstance(Context context) {
        if (instance == null) instance = new GameManager(context);
        return instance;
    }

    // ── Local cache ────────────────────────────────────────────────────────────

    private void loadLocalCache() {
        String json = prefs.getString(KEY_COLLECTION, null);
        if (json != null) {
            Type type = new TypeToken<List<OwnedPokemon>>() {}.getType();
            List<OwnedPokemon> list = gson.fromJson(json, type);
            if (list != null)
                for (OwnedPokemon p : list) localCollection.put(p.getId(), p);
        }
        collectionLoaded = true;
        for (CollectionCallback cb : pendingCallbacks)
            cb.onLoaded(new HashMap<>(localCollection));
        pendingCallbacks.clear();
    }

    private void saveLocalCache() {
        List<OwnedPokemon> list = new ArrayList<>(localCollection.values());
        prefs.edit().putString(KEY_COLLECTION, gson.toJson(list)).apply();
    }

    // ── Firebase ───────────────────────────────────────────────────────────────

    private void attachListener() {
        String uid = uid();
        if (uid == null) return;
        collectionRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("collection");
        collectionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                localCollection.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    OwnedPokemon p = child.getValue(OwnedPokemon.class);
                    if (p != null) localCollection.put(p.getId(), p);
                }
                saveLocalCache();
                for (CollectionCallback cb : pendingCallbacks)
                    cb.onLoaded(new HashMap<>(localCollection));
                pendingCallbacks.clear();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void onUserLoggedIn() {
        collectionLoaded = false;
        localCollection.clear();
        pendingCallbacks.clear();
        loadLocalCache();
        attachListener();
    }

    // ── Collection access ──────────────────────────────────────────────────────

    public boolean isCollectionLoaded() { return collectionLoaded; }

    public void getCollection(CollectionCallback callback) {
        if (collectionLoaded) callback.onLoaded(new HashMap<>(localCollection));
        else pendingCallbacks.add(callback);
    }

    /**
     * Called when a Pokémon is drawn from the gacha.
     * Awards EXP to the base-form entry (creating it if new).
     * Handles evolution stage/display updates and spawner logic (Eevee/Tyrogue).
     */
    public void addToCollection(OwnedPokemon drawn) {
        int baseId = EvolutionChain.getBaseId(drawn.getId());
        int expGain = EvolutionChain.getDrawStage(drawn.getId());

        OwnedPokemon entry = localCollection.get(baseId);
        if (entry == null) {
            // First time — if we drew a higher stage, show that form immediately, not the base
            int drawnStage = EvolutionChain.getDrawStage(drawn.getId());
            String sprite  = drawn.getId() == baseId
                    ? drawn.getSpriteUrl()
                    : "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + drawn.getId() + ".png";
            entry = new OwnedPokemon(baseId, drawn.getName(), drawn.getTypes(), drawn.getRarity(), sprite);
            entry.setDisplayId(drawn.getId());
            entry.setStage(drawnStage);
            localCollection.put(baseId, entry);
        } else {
            entry.setCount(entry.getCount() + 1);
        }

        addExpToEntry(entry, expGain);
        saveLocalCache();
        syncToFirebase(entry);
    }

    /**
     * Adds EXP to an entry. Tier upgrades apply automatically (visual overlays only).
     * Stage/displayId auto-advance when EXP crosses a threshold.
     * Mutates the entry in-place; caller is responsible for save+sync.
     */
    private void addExpToEntry(OwnedPokemon entry, int expGain) {
        int baseId       = entry.getId();
        int oldExp       = entry.getExp();
        int newExp       = oldExp + expGain;
        int oldStage     = entry.getStage() == 0 ? 1 : entry.getStage();
        int oldTier      = entry.getTier();
        entry.setExp(newExp);
        entry.setTier(EvolutionChain.computeTier(baseId, newExp));
        int newTier      = entry.getTier();
        if (newTier > oldTier && evolutionListener != null) {
            String name = capitalize(entry.getName());
            String from = oldTier > 0 ? "Your " + name + " " + tierName(oldTier) : "Your " + name;
            String to   = name + " " + tierName(newTier);
            evolutionListener.onEvolved(from, to);
        }

        // ── Spawner logic (Eevee/Tyrogue) ─────────────────────────────────────
        if (EvolutionChain.isSpawner(baseId)) {
            int[] targets    = EvolutionChain.getSpawnTargets(baseId);
            int threshold    = EvolutionChain.EXP_STAGE2;
            int crossedNow   = newExp / threshold;
            int crossedOld   = oldExp / threshold;
            int newCrossings = crossedNow - crossedOld;
            for (int i = 0; i < newCrossings; i++) {
                List<Integer> unobtained = new ArrayList<>();
                for (int t : targets)
                    if (!localCollection.containsKey(t)) unobtained.add(t);
                if (!unobtained.isEmpty())
                    spawnEntry(unobtained.get(rand.nextInt(unobtained.size())), entry);
            }
            return;
        }

        // ── Auto-evolve when EXP crosses a stage threshold ──────────────────────────
        int newStage = EvolutionChain.computeStage(baseId, newExp);
        if (newStage > oldStage) {
            int displayId = entry.getDisplayId() == 0 ? baseId : entry.getDisplayId();
            for (int s = oldStage + 1; s <= newStage; s++)
                displayId = EvolutionChain.getNextDisplayId(baseId, displayId, s);
            entry.setDisplayId(displayId);
            entry.setStage(newStage);
            final String oldName = capitalize(entry.getName());
            final int finalDisplayId = displayId;
            RetrofitClient.getInstance().getApiService().getPokemon(finalDisplayId)
                    .enqueue(new retrofit2.Callback<com.example.pokedraw.api.PokemonResponse>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<com.example.pokedraw.api.PokemonResponse> call,
                                               @NonNull retrofit2.Response<com.example.pokedraw.api.PokemonResponse> r) {
                            if (r.isSuccessful() && r.body() != null) {
                                entry.setName(r.body().getName());
                                saveLocalCache();
                                syncToFirebase(entry);
                                if (evolutionListener != null)
                                    evolutionListener.onEvolved(oldName, capitalize(r.body().getName()));
                            }
                        }
                        @Override
                        public void onFailure(@NonNull retrofit2.Call<com.example.pokedraw.api.PokemonResponse> call,
                                              @NonNull Throwable t) {}
                    });
        }
    }

    /**
     * Manually evolve a Pokémon — called when the player presses the Evolve button.
     * Returns true if evolution happened, false if not ready or already at max.
     */
    public boolean evolve(int baseId) {
        OwnedPokemon entry = localCollection.get(baseId);
        if (entry == null) return false;
        int currentStage = entry.getStage() == 0 ? 1 : entry.getStage();
        int targetStage  = EvolutionChain.computeStage(baseId, entry.getExp());
        if (targetStage <= currentStage) return false;
        int displayId = entry.getDisplayId() == 0 ? baseId : entry.getDisplayId();
        for (int s = currentStage + 1; s <= targetStage; s++)
            displayId = EvolutionChain.getNextDisplayId(baseId, displayId, s);
        entry.setDisplayId(displayId);
        entry.setStage(targetStage);
        saveLocalCache();
        syncToFirebase(entry);
        // Fetch the evolved form's name from API and update asynchronously
        final String oldName = capitalize(entry.getName());
        final int finalDisplayId = displayId;
        PokeApiService api = RetrofitClient.getInstance().getApiService();
        api.getPokemon(finalDisplayId).enqueue(new retrofit2.Callback<PokemonResponse>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull retrofit2.Call<PokemonResponse> call,
                                   @androidx.annotation.NonNull retrofit2.Response<PokemonResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    entry.setName(response.body().getName());
                    saveLocalCache();
                    syncToFirebase(entry);
                    if (evolutionListener != null)
                        evolutionListener.onEvolved(oldName, capitalize(response.body().getName()));
                }
            }
            @Override
            public void onFailure(@androidx.annotation.NonNull retrofit2.Call<PokemonResponse> call,
                                  @androidx.annotation.NonNull Throwable t) {}
        });
        return true;
    }

    /** Creates a brand-new independent entry for a spawned Pokémon (eeveelution / hitmon). */
    private void spawnEntry(int spawnId, OwnedPokemon spawner) {
        // Inherit rarity from spawner; sprite URL uses standard PokeAPI pattern
        String spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + spawnId + ".png";
        OwnedPokemon spawn = new OwnedPokemon(spawnId, "", spawner.getTypes(), spawner.getRarity(), spriteUrl);
        spawn.setDisplayId(spawnId);
        spawn.setStage(1);
        spawn.setTier(0);
        spawn.setExp(0);
        spawn.setCount(1);
        localCollection.put(spawnId, spawn);
        syncToFirebase(spawn);
    }

    private void syncToFirebase(OwnedPokemon entry) {
        if (collectionRef != null)
            collectionRef.child(String.valueOf(entry.getId())).setValue(entry);
    }

    // ── Clear all data ─────────────────────────────────────────────────────────

    public void clearAllData() {
        boolean oneAtATime = prefs.getBoolean(KEY_DRAW_MODE_ONE, true);
        boolean bgmMuted   = prefs.getBoolean(KEY_BGM_MUTED, false);
        prefs.edit().clear()
                .putBoolean(KEY_DRAW_MODE_ONE, oneAtATime)
                .putBoolean(KEY_BGM_MUTED, bgmMuted)
                .apply();
        localCollection.clear();
        collectionLoaded = true;
        if (collectionRef != null) collectionRef.removeValue(null);
    }

    // ── Draw tracking ──────────────────────────────────────────────────────────

    public int getDrawsRemaining() {
        resetIfNewDay();
        int used     = prefs.getInt(KEY_DRAW_COUNT, 0);
        int bonus    = prefs.getInt(KEY_BONUS_DRAWS, 0);
        int redeemed = prefs.getInt(KEY_REDEEMED_DRAWS, 0);
        return Math.max(0, MAX_DRAWS + bonus - used) + redeemed;
    }

    public boolean canDraw()  { return getDrawsRemaining() > 0; }

    public void recordDraw() {
        resetIfNewDay();
        // Drain redeemed pool first before consuming daily draws
        int redeemed = prefs.getInt(KEY_REDEEMED_DRAWS, 0);
        if (redeemed > 0) {
            prefs.edit().putInt(KEY_REDEEMED_DRAWS, redeemed - 1).apply();
        } else {
            prefs.edit().putInt(KEY_DRAW_COUNT, prefs.getInt(KEY_DRAW_COUNT, 0) + 1).apply();
        }
    }

    public void addBonusDraw() {
        resetIfNewDay();
        prefs.edit().putInt(KEY_BONUS_DRAWS, prefs.getInt(KEY_BONUS_DRAWS, 0) + 1).apply();
    }

    /** Redeem a code. Returns true if valid, false if unrecognised. No use limit. */
    public boolean redeemCode(String code) {
        switch (code.trim().toLowerCase()) {
            case "pokedraw666":
            case "pokedraw777":
            case "pokedraw999":
                int current = prefs.getInt(KEY_REDEEMED_DRAWS, 0);
                prefs.edit().putInt(KEY_REDEEMED_DRAWS, current + 500).apply();
                return true;
            default:
                return false;
        }
    }

    // ── Guess tracking ─────────────────────────────────────────────────────────

    public int getGuessesRemaining() {
        resetIfNewDay();
        return MAX_GUESSES - prefs.getInt(KEY_GUESS_COUNT, 0);
    }

    public boolean canGuess() { return getGuessesRemaining() > 0; }

    public void recordGuess() {
        resetIfNewDay();
        prefs.edit().putInt(KEY_GUESS_COUNT, prefs.getInt(KEY_GUESS_COUNT, 0) + 1).apply();
    }

    // ── Draw mode preference ───────────────────────────────────────────────────

    public boolean isDrawModeOneAtATime() { return prefs.getBoolean(KEY_DRAW_MODE_ONE, true); }

    public void setDrawModeOneAtATime(boolean v) { prefs.edit().putBoolean(KEY_DRAW_MODE_ONE, v).apply(); }

    // ── BGM mute preference ────────────────────────────────────────────────────

    public boolean isBgmMuted() { return prefs.getBoolean(KEY_BGM_MUTED, false); }

    public void setBgmMuted(boolean muted) {
        prefs.edit().putBoolean(KEY_BGM_MUTED, muted).apply();
        MusicManager.getInstance().setMuted(muted);
    }

    // ── Auto draw preference ───────────────────────────────────────────────────

    public boolean isAutoDraw() { return prefs.getBoolean(KEY_AUTO_DRAW, false); }

    public void setAutoDraw(boolean v) { prefs.edit().putBoolean(KEY_AUTO_DRAW, v).apply(); }

    public boolean isOnboardingDone() { return prefs.getBoolean(KEY_ONBOARDING_DONE, false); }

    public void setOnboardingDone() { prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply(); }

    // ── Day reset ─────────────────────────────────────────────────────────────

    private void resetIfNewDay() {
        String today = today();
        if (!today.equals(prefs.getString(KEY_DRAW_DATE, ""))) {
            prefs.edit()
                    .putString(KEY_DRAW_DATE, today)
                    .putInt(KEY_DRAW_COUNT, 0)
                    .putInt(KEY_BONUS_DRAWS, 0)
                    .putInt(KEY_GUESS_COUNT, 0)
                    .apply();
        }
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // ── Gen unlock system ──────────────────────────────────────────────────────

    private static final int[] GEN_ENDS = {151, 251, 386, 493, 649, 721, 809, 905, 1025};
    public  static final int SHINY_UNLOCK_THRESHOLD = 10;

    /** Returns the highest gen number (1–9) currently unlocked for drawing. */
    public int getUnlockedGenCount() {
        int unlocked = 1;
        for (int gen = 1; gen < 9; gen++) {
            if (countShinyInGen(gen) >= SHINY_UNLOCK_THRESHOLD) unlocked = gen + 1;
            else break;
        }
        return unlocked;
    }

    /** Counts Pokémon in the given gen (by base ID range) that have tier >= Shiny. */
    public int countShinyInGen(int gen) {
        int start = gen == 1 ? 1 : GEN_ENDS[gen - 2] + 1;
        int end   = GEN_ENDS[gen - 1];
        int count = 0;
        for (OwnedPokemon p : localCollection.values())
            if (p.getId() >= start && p.getId() <= end && p.getTier() >= EvolutionChain.TIER_SHINY) count++;
        return count;
    }

    // ── Gacha ─────────────────────────────────────────────────────────────────

    public List<int[]> rollTen() {
        int maxGen = getUnlockedGenCount();
        int[] filtCommon    = filterByGen(RarityConfig.COMMON_IDS, maxGen);
        int[] filtRare      = filterByGen(RarityConfig.RARE_IDS, maxGen);
        int[] filtMythical  = filterByGen(RarityConfig.MYTHICAL_IDS, maxGen);
        int[] filtLegendary = filterByGen(RarityConfig.LEGENDARY_IDS, maxGen);
        if (filtRare.length      == 0) filtRare      = filtCommon;
        if (filtMythical.length  == 0) filtMythical  = filtCommon;
        if (filtLegendary.length == 0) filtLegendary = filtCommon;
        List<int[]> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int roll = rand.nextInt(1000);
            String rarity;
            int[] pool;
            if (roll < RarityConfig.COMMON_THRESHOLD) {
                rarity = RarityConfig.COMMON;    pool = filtCommon;
            } else if (roll < RarityConfig.RARE_THRESHOLD) {
                rarity = RarityConfig.RARE;      pool = filtRare;
            } else if (roll < RarityConfig.MYTHICAL_THRESHOLD) {
                rarity = RarityConfig.MYTHICAL;  pool = filtMythical;
            } else {
                rarity = RarityConfig.LEGENDARY; pool = filtLegendary;
            }
            results.add(new int[]{pool[rand.nextInt(pool.length)], rarityOrdinal(rarity)});
        }
        return results;
    }

    private int[] filterByGen(int[] pool, int maxGen) {
        int end = GEN_ENDS[maxGen - 1];
        List<Integer> filtered = new ArrayList<>();
        for (int id : pool)
            if (EvolutionChain.getBaseId(id) <= end) filtered.add(id);
        int[] arr = new int[filtered.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = filtered.get(i);
        return arr;
    }

    private int rarityOrdinal(String r) {
        switch (r) {
            case RarityConfig.RARE:      return 1;
            case RarityConfig.MYTHICAL:  return 2;
            case RarityConfig.LEGENDARY: return 3;
            default:                     return 0;
        }
    }

    public static String ordinalToRarity(int o) {
        switch (o) {
            case 1: return RarityConfig.RARE;
            case 2: return RarityConfig.MYTHICAL;
            case 3: return RarityConfig.LEGENDARY;
            default: return RarityConfig.COMMON;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String tierName(int tier) {
        switch (tier) {
            case EvolutionChain.TIER_SHINY: return "Shiny";
            case EvolutionChain.TIER_HOLO:  return "Holo";
            case EvolutionChain.TIER_GOLD:  return "Gold";
            default:                        return "";
        }
    }

    private String uid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return null;
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
