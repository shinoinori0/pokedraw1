package com.example.pokedraw;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.pokedraw.model.OwnedPokemon;

public class PokemonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_POKEMON_ID  = "pokemon_id";
    /** When set, forces the detail screen to show this exact sprite/stats (Pokedex use). */
    public static final String EXTRA_DISPLAY_ID  = "display_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int pokemonId  = getIntent().getIntExtra(EXTRA_POKEMON_ID, -1);
        if (pokemonId == -1) { finish(); return; }
        // If EXTRA_DISPLAY_ID is set (from Pokedex), always show that exact Pokémon
        int forcedDisplayId = getIntent().getIntExtra(EXTRA_DISPLAY_ID, -1);

        ImageView ivPokemon  = findViewById(R.id.ivDetailPokemon);
        TextView tvId        = findViewById(R.id.tvDetailId);
        TextView tvName      = findViewById(R.id.tvDetailName);
        TextView tvRarity    = findViewById(R.id.tvDetailRarity);
        LinearLayout llTypes = findViewById(R.id.llTypes);
        LinearLayout llStats = findViewById(R.id.llStats);
        TextView tvFlavor    = findViewById(R.id.tvFlavorText);

        GameManager.getInstance(this).getCollection(collection -> runOnUiThread(() -> {
            boolean owned = collection.containsKey(pokemonId);

            if (forcedDisplayId > 0) {
                // Opened from Pokedex — fetch everything from API for this exact slot id.
                tvId.setText(String.format("#%03d", forcedDisplayId));
                ivPokemon.setImageResource(android.R.drawable.sym_def_app_icon);
                if (!owned) {
                    ColorMatrix cm = new ColorMatrix();
                    cm.setSaturation(0);
                    ivPokemon.setColorFilter(new ColorMatrixColorFilter(cm));
                    ivPokemon.setAlpha(0.4f);
                }
                if (owned) {
                    OwnedPokemon p = collection.get(pokemonId);
                    tvName.setText(capitalize(p.getName()));
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(capitalize(p.getName()));
                    tvRarity.setText(p.getRarity());
                    tvRarity.setTextColor(rarityColor(p.getRarity()));
                    for (String type : p.getTypes()) llTypes.addView(makeTypeChip(type));
                } else {
                    tvName.setText("Pokemon #" + String.format("%03d", forcedDisplayId));
                    tvRarity.setText("Not yet obtained");
                    tvRarity.setTextColor(Color.GRAY);
                }
                llStats.setVisibility(View.GONE);
            } else if (owned) {
                OwnedPokemon p = collection.get(pokemonId);
                int displayId  = p.getDisplayId() > 0 ? p.getDisplayId() : pokemonId;

                if (getSupportActionBar() != null)
                    getSupportActionBar().setTitle(capitalize(p.getName()));

                tvId.setText(String.format("#%03d", displayId));
                tvName.setText(capitalize(p.getName()));
                tvRarity.setText(p.getRarity());
                tvRarity.setTextColor(rarityColor(p.getRarity()));
                for (String type : p.getTypes()) llTypes.addView(makeTypeChip(type));

                ivPokemon.setImageResource(android.R.drawable.sym_def_app_icon);
                llStats.setVisibility(View.GONE);
            } else {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("???");
                tvId.setText(String.format("#%03d", pokemonId));
                tvName.setText("???");
                tvRarity.setText("Not yet obtained");
                tvRarity.setTextColor(Color.GRAY);

                ivPokemon.setImageResource(android.R.drawable.sym_def_app_icon);
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ivPokemon.setColorFilter(new ColorMatrixColorFilter(cm));
                ivPokemon.setAlpha(0.4f);

                llStats.setVisibility(View.GONE);
            }

            tvFlavor.setVisibility(View.GONE);
        }));
    }

    private void bindStat(View row, String label, int value) {
        if (row == null) return;
        ((TextView) row.findViewById(R.id.tvStatName)).setText(label);
        ((TextView) row.findViewById(R.id.tvStatValue)).setText(String.valueOf(value));
        ProgressBar pb = row.findViewById(R.id.pbStat);
        pb.setProgress(value);
        pb.setProgressTintList(ColorStateList.valueOf(statColor(value)));
    }

    private int statColor(int value) {
        if (value >= 100) return Color.parseColor("#4CAF50"); // green
        if (value >= 60)  return Color.parseColor("#FFCC00"); // yellow
        return Color.parseColor("#FF5252");                    // red
    }


    private TextView makeTypeChip(String type) {
        TextView tv = new TextView(this);
        tv.setText(capitalize(type));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        tv.setPadding(16, 6, 16, 6);
        tv.setBackgroundColor(typeColor(type));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(4, 0, 4, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int rarityColor(String rarity) {
        switch (rarity) {
            case RarityConfig.RARE:      return getColor(R.color.rarityRare);
            case RarityConfig.MYTHICAL:  return getColor(R.color.rarityMythical);
            case RarityConfig.LEGENDARY: return getColor(R.color.rarityLegendary);
            default:                     return getColor(R.color.rarityCommon);
        }
    }

    private int typeColor(String type) {
        switch (type.toLowerCase()) {
            case "fire":     return getColor(R.color.typeFire);
            case "water":    return getColor(R.color.typeWater);
            case "grass":    return getColor(R.color.typeGrass);
            case "electric": return getColor(R.color.typeElectric);
            case "ice":      return getColor(R.color.typeIce);
            case "fighting": return getColor(R.color.typeFighting);
            case "poison":   return getColor(R.color.typePoison);
            case "ground":   return getColor(R.color.typeGround);
            case "flying":   return getColor(R.color.typeFlying);
            case "psychic":  return getColor(R.color.typePsychic);
            case "bug":      return getColor(R.color.typeBug);
            case "rock":     return getColor(R.color.typeRock);
            case "ghost":    return getColor(R.color.typeGhost);
            case "dragon":   return getColor(R.color.typeDragon);
            case "dark":     return getColor(R.color.typeDark);
            case "steel":    return getColor(R.color.typeSteel);
            case "fairy":    return getColor(R.color.typeFairy);
            default:         return getColor(R.color.typeNormal);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
