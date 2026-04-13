package com.example.pokedraw;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.pokedraw.api.PokemonListResponse;
import com.example.pokedraw.api.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PokemonDexCache {
    private static final String PREFS_NAME = "pokedraw_prefs";
    private static final String KEY_DEX_NAME_MAP = "dex_name_map";
    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<Integer, String>>() {}.getType();

    private static Map<Integer, String> cachedNames = new HashMap<>();
    private static boolean loaded;
    private static boolean syncing;

    private PokemonDexCache() {}

    public static synchronized void warmUpIfNeeded(Context context) {
        if (syncing) return;
        ensureLoadedFromDisk(context.getApplicationContext());
        if (!cachedNames.isEmpty()) return;
        syncing = true;
        RetrofitClient.getInstance().getApiService().getPokemonList()
                .enqueue(new retrofit2.Callback<PokemonListResponse>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<PokemonListResponse> call,
                                           @NonNull retrofit2.Response<PokemonListResponse> response) {
                        synchronized (PokemonDexCache.class) {
                            syncing = false;
                            if (!response.isSuccessful() || response.body() == null || response.body().getResults() == null) return;
                            HashMap<Integer, String> names = new HashMap<>();
                            for (PokemonListResponse.Result r : response.body().getResults()) {
                                int id = extractId(r.getUrl());
                                if (id > 0 && r.getName() != null) names.put(id, r.getName());
                            }
                            if (!names.isEmpty()) {
                                cachedNames = names;
                                saveToDisk(context.getApplicationContext(), names);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<PokemonListResponse> call, @NonNull Throwable t) {
                        synchronized (PokemonDexCache.class) {
                            syncing = false;
                        }
                    }
                });
    }

    public static synchronized String getName(Context context, int id) {
        ensureLoadedFromDisk(context.getApplicationContext());
        String name = cachedNames.get(id);
        if (name == null || name.isEmpty()) return fallbackName(id);
        return capitalize(name);
    }

    private static void ensureLoadedFromDisk(Context context) {
        if (loaded) return;
        loaded = true;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DEX_NAME_MAP, null);
        if (json == null || json.isEmpty()) return;
        Map<Integer, String> disk = gson.fromJson(json, mapType);
        if (disk != null) cachedNames = new HashMap<>(disk);
    }

    private static void saveToDisk(Context context, Map<Integer, String> names) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DEX_NAME_MAP, gson.toJson(names)).apply();
    }

    private static int extractId(String url) {
        if (url == null) return -1;
        String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        int slash = trimmed.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= trimmed.length()) return -1;
        try {
            return Integer.parseInt(trimmed.substring(slash + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
    }

    private static String fallbackName(int id) {
        return "Pokemon #" + String.format(Locale.US, "%03d", id);
    }
}
