package com.example.pokedraw.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PokemonResponse {

    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("sprites")
    private Sprites sprites;
    @SerializedName("types")
    private List<TypeSlot> types;
    @SerializedName("stats")
    private List<StatSlot> stats;

    public int getId() { return id; }
    public String getName() { return name; }
    public Sprites getSprites() { return sprites; }
    public List<TypeSlot> getTypes() { return types; }
    public List<StatSlot> getStats() { return stats; }

    /** Returns the base value for a named stat, or 0 if not found. */
    public int getStat(String statName) {
        if (stats == null) return 0;
        for (StatSlot s : stats)
            if (s.getStat() != null && statName.equals(s.getStat().getName()))
                return s.getBaseStat();
        return 0;
    }

    public static class Sprites {
        @SerializedName("other")
        private OtherSprites other;
        @SerializedName("front_default")
        private String frontDefault;

        public OtherSprites getOther() { return other; }
        public String getFrontDefault() { return frontDefault; }
    }

    public static class OtherSprites {
        @SerializedName("official-artwork")
        private OfficialArtwork officialArtwork;

        public OfficialArtwork getOfficialArtwork() { return officialArtwork; }
    }

    public static class OfficialArtwork {
        @SerializedName("front_default")
        private String frontDefault;

        public String getFrontDefault() { return frontDefault; }
    }

    public static class TypeSlot {
        @SerializedName("type")
        private TypeInfo type;

        public TypeInfo getType() { return type; }
    }

    public static class TypeInfo {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }

    public static class StatSlot {
        @SerializedName("base_stat")
        private int baseStat;
        @SerializedName("stat")
        private StatInfo stat;

        public int getBaseStat() { return baseStat; }
        public StatInfo getStat() { return stat; }
    }

    public static class StatInfo {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }
}
