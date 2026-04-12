package com.example.pokedraw.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PokemonSpeciesResponse {

    @SerializedName("flavor_text_entries")
    private List<FlavorTextEntry> flavorTextEntries;

    public List<FlavorTextEntry> getFlavorTextEntries() { return flavorTextEntries; }

    /** Returns the first English flavor text, cleaned of escape characters. */
    public String getEnglishFlavorText() {
        if (flavorTextEntries == null) return "";
        for (FlavorTextEntry entry : flavorTextEntries) {
            if (entry.getLanguage() != null && "en".equals(entry.getLanguage().getName())) {
                return entry.getFlavorText()
                        .replace("\f", " ")
                        .replace("\n", " ")
                        .replace("\u00ad", "")
                        .trim();
            }
        }
        return "";
    }

    public static class FlavorTextEntry {
        @SerializedName("flavor_text")
        private String flavorText;
        @SerializedName("language")
        private NamedResource language;

        public String getFlavorText() { return flavorText; }
        public NamedResource getLanguage() { return language; }
    }

    public static class NamedResource {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
    }
}
