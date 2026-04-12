package com.example.pokedraw.model;

import java.util.List;

public class OwnedPokemon {
    private int id;
    private String name;
    private List<String> types;
    private String rarity;
    private String spriteUrl;
    private int count;
    private int exp;
    private int displayId;   // current sprite (evolves visually)
    private int stage;       // 1, 2, or 3
    private int tier;        // 0=Normal, 1=Shiny, 2=Holo, 3=Gold

    public OwnedPokemon() {}

    public OwnedPokemon(int id, String name, List<String> types, String rarity, String spriteUrl) {
        this.id        = id;
        this.name      = name;
        this.types     = types;
        this.rarity    = rarity;
        this.spriteUrl = spriteUrl;
        this.count     = 1;
        this.exp       = 0;
        this.displayId = id;
        this.stage     = 1;
        this.tier      = 0;
    }

    public int getId()          { return id; }
    public String getName()     { return name; }
    public List<String> getTypes() { return types; }
    public String getRarity()   { return rarity; }
    public String getSpriteUrl(){ return spriteUrl; }
    public int getCount()       { return count; }
    public int getExp()         { return exp; }
    public int getDisplayId()   { return displayId; }
    public int getStage()       { return stage; }
    public int getTier()        { return tier; }

    public void setId(int id)                   { this.id = id; }
    public void setName(String name)            { this.name = name; }
    public void setTypes(List<String> types)    { this.types = types; }
    public void setRarity(String rarity)        { this.rarity = rarity; }
    public void setSpriteUrl(String spriteUrl)  { this.spriteUrl = spriteUrl; }
    public void setCount(int count)             { this.count = count; }
    public void setExp(int exp)                 { this.exp = exp; }
    public void setDisplayId(int displayId)     { this.displayId = displayId; }
    public void setStage(int stage)             { this.stage = stage; }
    public void setTier(int tier)               { this.tier = tier; }

    public String getTypesString() {
        if (types == null || types.isEmpty()) return "";
        return String.join(" / ", types);
    }
}
