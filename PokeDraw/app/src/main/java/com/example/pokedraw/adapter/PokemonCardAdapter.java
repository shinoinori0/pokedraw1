package com.example.pokedraw.adapter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pokedraw.EvolutionChain;
import com.example.pokedraw.ParticleView;
import com.example.pokedraw.PokemonDetailActivity;
import com.example.pokedraw.R;
import com.example.pokedraw.RarityConfig;
import com.example.pokedraw.model.OwnedPokemon;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class PokemonCardAdapter extends RecyclerView.Adapter<PokemonCardAdapter.ViewHolder> {

    public interface OnEvolveListener {
        void onEvolve(int baseId, int adapterPosition);
    }

    private final List<OwnedPokemon> list;
    private final boolean silhouetteUnowned; // true = Pokedex mode
    private OnEvolveListener evolveListener;

    public PokemonCardAdapter(List<OwnedPokemon> list, boolean silhouetteUnowned) {
        this.list = list;
        this.silhouetteUnowned = silhouetteUnowned;
    }

    public void setOnEvolveListener(OnEvolveListener listener) {
        this.evolveListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pokemon_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        OwnedPokemon p     = list.get(position);
        boolean owned      = p.getCount() > 0;
        boolean silhouette = silhouetteUnowned && !owned;

        if (silhouette) {
            h.tvId.setText(String.format("#%03d", p.getId()));
            h.tvName.setText("");
            h.tvType.setText("");
            h.tvRarity.setText("");
            h.pbExp.setVisibility(View.INVISIBLE);
            h.tvTierBadge.setVisibility(View.GONE);
            h.btnEvolve.setVisibility(View.GONE);
            h.particleView.stopParticles();
            h.viewDarkOverlay.setVisibility(View.VISIBLE);
            applyCardStyle(h, EvolutionChain.TIER_NORMAL, false);
        } else {
            int baseId       = p.getId();
            int displayId    = p.getDisplayId() > 0 ? p.getDisplayId() : baseId;
            int tier         = p.getTier();
            int currentStage = p.getStage() == 0 ? 1 : p.getStage();
            int maxStages    = EvolutionChain.getMaxStages(baseId);
            boolean isCollection = !silhouetteUnowned;

            h.tvId.setText(String.format("#%03d", displayId));
            h.tvName.setText(capitalize(p.getName()));
            h.tvType.setText(p.getTypesString());
            h.tvRarity.setText(p.getRarity());
            h.tvRarity.setTextColor(rarityColor(h, p.getRarity()));

            // EXP bar
            if (isCollection) {
                int[] prog = EvolutionChain.getExpProgress(baseId, p.getExp());
                int pct = prog[1] > 0 ? (int) (prog[0] * 100f / prog[1]) : 100;
                h.pbExp.setProgress(pct);
                h.pbExp.setProgressTintList(ColorStateList.valueOf(expBarColor(h, tier)));
                h.pbExp.setVisibility(View.VISIBLE);
            } else {
                h.pbExp.setVisibility(View.INVISIBLE);
            }

            // Tier badge
            if (isCollection && tier > 0) {
                h.tvTierBadge.setText(tierText(tier));
                h.tvTierBadge.setTextColor(Color.BLACK);
                h.tvTierBadge.setBackgroundColor(tierTextColor(tier));
                h.tvTierBadge.setVisibility(View.VISIBLE);
            } else {
                h.tvTierBadge.setVisibility(View.GONE);
            }

            // Mobile-safe mode: disable per-card particles to reduce crashes on low-memory phones.
            h.particleView.stopParticles();

            // Evolve status — collection only, auto-evolve happens via EXP so just show status
            if (isCollection) {
                h.btnEvolve.setVisibility(View.VISIBLE);
                h.btnEvolve.setEnabled(false);
                boolean isMaxed = currentStage >= maxStages && tier >= EvolutionChain.TIER_GOLD;
                boolean spawnerMaxed = EvolutionChain.isSpawner(baseId) && tier >= EvolutionChain.TIER_GOLD;
                if (isMaxed || spawnerMaxed) {
                    h.btnEvolve.setText("MAXED");
                    h.btnEvolve.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#555555")));
                } else {
                    int[] prog = EvolutionChain.getExpProgress(baseId, p.getExp());
                    h.btnEvolve.setText(prog[0] + "/" + prog[1]);
                    h.btnEvolve.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A2A4A")));
                }
            } else {
                h.btnEvolve.setVisibility(View.GONE);
                h.particleView.stopParticles();
            }

            h.viewDarkOverlay.setVisibility(View.GONE);
            applyCardStyle(h, tier, isCollection);
        }

        // Sprite — always load normally, overlay handles the unowned visual
        h.ivPokemon.setImageResource(android.R.drawable.sym_def_app_icon);
        h.ivPokemon.clearColorFilter();
        h.ivPokemon.setAlpha(1f);

        // Only open detail on owned Pokedex cards
        if (silhouetteUnowned && owned) {
            h.itemView.setOnClickListener(v -> {
                int slotId = p.getId();
                int baseId = EvolutionChain.getBaseId(slotId);
                Intent intent = new Intent(v.getContext(), PokemonDetailActivity.class);
                intent.putExtra(PokemonDetailActivity.EXTRA_POKEMON_ID, baseId);
                intent.putExtra(PokemonDetailActivity.EXTRA_DISPLAY_ID, slotId);
                v.getContext().startActivity(intent);
            });
        } else {
            h.itemView.setOnClickListener(null);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder h) {
        super.onViewRecycled(h);
        h.particleView.stopParticles();
    }

    private void applyCardStyle(ViewHolder h, int tier, boolean isCollection) {
        int bgColor;
        float elevation;
        int borderColor;

        switch (tier) {
            case EvolutionChain.TIER_SHINY:
                bgColor     = h.itemView.getContext().getColor(R.color.tierCardShiny);
                elevation   = 6f;
                borderColor = isCollection ? Color.parseColor("#C0C0C0") : Color.parseColor("#40FFFFFF");
                break;
            case EvolutionChain.TIER_HOLO:
                bgColor     = h.itemView.getContext().getColor(R.color.tierCardHolo);
                elevation   = 10f;
                borderColor = isCollection ? Color.parseColor("#CF6FFF") : Color.parseColor("#40FFFFFF");
                break;
            case EvolutionChain.TIER_GOLD:
                bgColor     = h.itemView.getContext().getColor(R.color.tierCardGold);
                elevation   = 14f;
                borderColor = isCollection ? Color.parseColor("#FFB300") : Color.parseColor("#40FFFFFF");
                break;
            default:
                bgColor     = h.itemView.getContext().getColor(R.color.colorCard);
                elevation   = 2f;
                borderColor = Color.parseColor("#40FFFFFF");
                break;
        }

        float density   = h.itemView.getContext().getResources().getDisplayMetrics().density;
        int borderWidth = Math.round(1.5f * density); // always show border

        h.cardRoot.setCardBackgroundColor(bgColor);
        h.cardRoot.setCardElevation(elevation);
        h.cardRoot.setStrokeColor(borderColor);
        h.cardRoot.setStrokeWidth(borderWidth);
    }

    private int expBarColor(ViewHolder h, int tier) {
        switch (tier) {
            case EvolutionChain.TIER_SHINY: return h.itemView.getContext().getColor(R.color.tierBarShiny);
            case EvolutionChain.TIER_HOLO:  return h.itemView.getContext().getColor(R.color.tierBarHolo);
            case EvolutionChain.TIER_GOLD:  return h.itemView.getContext().getColor(R.color.tierBarGold);
            default:                        return h.itemView.getContext().getColor(R.color.colorAccent);
        }
    }

    private int tierTextColor(int tier) {
        switch (tier) {
            case EvolutionChain.TIER_SHINY: return Color.parseColor("#C0C0C0");
            case EvolutionChain.TIER_HOLO:  return Color.parseColor("#CF6FFF");
            case EvolutionChain.TIER_GOLD:  return Color.parseColor("#FFB300");
            default:                        return Color.parseColor("#AAAAAA");
        }
    }

    private String tierText(int tier) {
        switch (tier) {
            case EvolutionChain.TIER_SHINY: return "SHINY";
            case EvolutionChain.TIER_HOLO:  return "HOLO";
            case EvolutionChain.TIER_GOLD:  return "GOLD";
            default:                        return "";
        }
    }

    private int rarityColor(ViewHolder h, String rarity) {
        switch (rarity) {
            case RarityConfig.RARE:      return h.itemView.getContext().getColor(R.color.rarityRare);
            case RarityConfig.MYTHICAL:  return h.itemView.getContext().getColor(R.color.rarityMythical);
            case RarityConfig.LEGENDARY: return h.itemView.getContext().getColor(R.color.rarityLegendary);
            default:                     return h.itemView.getContext().getColor(R.color.rarityCommon);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        ParticleView particleView;
        View viewDarkOverlay;
        ImageView ivPokemon;
        TextView tvId, tvName, tvType, tvRarity, tvTierBadge;
        ProgressBar pbExp;
        Button btnEvolve;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot        = itemView.findViewById(R.id.cardRoot);
            particleView    = itemView.findViewById(R.id.particleView);
            viewDarkOverlay = itemView.findViewById(R.id.viewDarkOverlay);
            ivPokemon       = itemView.findViewById(R.id.ivPokemon);
            tvId            = itemView.findViewById(R.id.tvPokemonId);
            tvName          = itemView.findViewById(R.id.tvPokemonName);
            tvType          = itemView.findViewById(R.id.tvPokemonType);
            tvRarity        = itemView.findViewById(R.id.tvRarity);
            pbExp           = itemView.findViewById(R.id.pbExp);
            tvTierBadge     = itemView.findViewById(R.id.tvTierBadge);
            btnEvolve       = itemView.findViewById(R.id.btnEvolve);
        }
    }
}
