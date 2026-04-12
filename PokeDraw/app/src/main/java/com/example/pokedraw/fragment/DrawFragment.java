package com.example.pokedraw.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pokedraw.GameManager;
import com.example.pokedraw.R;
import com.example.pokedraw.RarityConfig;
import com.example.pokedraw.adapter.PokemonCardAdapter;
import com.example.pokedraw.api.PokeApiService;
import com.example.pokedraw.api.PokemonResponse;
import com.example.pokedraw.api.RetrofitClient;
import com.example.pokedraw.model.OwnedPokemon;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DrawFragment extends Fragment {

    private Button btnDraw, btnShowOdds, btnNextCard;
    private TextView tvDrawsRemaining, tvDrawResult, tvResetTimer;
    private TextView tvCardProgress, tvSingleId, tvSingleName, tvSingleType, tvSingleRarity;
    private ImageView ivSinglePokemon;
    private CardView cardSingle;
    private ProgressBar progressBar, progressBarSingle;
    private RecyclerView rvDrawResults;
    private LinearLayout layoutOdds, layoutSingleCard;
    private ScrollView scrollResults;
    private View spacerTop, spacerBottom;

    // Gen unlock UI
    private TextView tvGenDrawing, tvNextGenLabel, tvGenUnlockProgress, tvAllGensUnlocked;
    private LinearLayout layoutNextGenProgress;
    private ProgressBar pbGenUnlock;

    private GameManager gameManager;
    private CountDownTimer countDownTimer;
    private boolean oddsVisible = false;

    // Single-card mode state
    private List<OwnedPokemon> drawnListForReveal;
    private int currentCardIndex = 0;

    // Hold-to-repeat
    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private Runnable holdRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw, container, false);

        btnDraw           = view.findViewById(R.id.btnDraw);
        btnShowOdds       = view.findViewById(R.id.btnShowOdds);
        btnNextCard       = view.findViewById(R.id.btnNextCard);
        tvDrawsRemaining  = view.findViewById(R.id.tvDrawsRemaining);
        tvDrawResult      = view.findViewById(R.id.tvDrawResult);
        tvResetTimer      = view.findViewById(R.id.tvResetTimer);
        progressBar       = view.findViewById(R.id.progressBar);
        progressBarSingle = view.findViewById(R.id.progressBarSingle);
        rvDrawResults     = view.findViewById(R.id.rvDrawResults);
        layoutOdds        = view.findViewById(R.id.layoutOdds);
        layoutSingleCard  = view.findViewById(R.id.layoutSingleCard);
        scrollResults     = view.findViewById(R.id.scrollResults);
        spacerTop         = view.findViewById(R.id.spacerTop);
        spacerBottom      = view.findViewById(R.id.spacerBottom);
        tvCardProgress    = view.findViewById(R.id.tvCardProgress);
        tvSingleId        = view.findViewById(R.id.tvSingleId);
        tvSingleName      = view.findViewById(R.id.tvSingleName);
        tvSingleType      = view.findViewById(R.id.tvSingleType);
        tvSingleRarity    = view.findViewById(R.id.tvSingleRarity);
        ivSinglePokemon   = view.findViewById(R.id.ivSinglePokemon);
        cardSingle        = view.findViewById(R.id.cardSingle);

        gameManager = GameManager.getInstance(requireContext());
        rvDrawResults.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        android.widget.Switch switchAuto = view.findViewById(R.id.switchAutoDrawInline);
        switchAuto.setChecked(gameManager.isAutoDraw());
        switchAuto.setOnCheckedChangeListener((btn, checked) -> gameManager.setAutoDraw(checked));

        btnShowOdds.setOnClickListener(v -> showOdds(!oddsVisible));
        btnDraw.setOnClickListener(v -> performDraw());
        btnDraw.setOnTouchListener((v, event) -> setupHoldRepeat(event, 400, 150, this::performDraw));
        btnNextCard.setOnClickListener(v -> revealNextCard());
        btnNextCard.setOnTouchListener((v, event) -> setupHoldRepeat(event, 400, 250, this::revealNextCard));

        tvGenDrawing          = view.findViewById(R.id.tvGenDrawing);
        tvNextGenLabel        = view.findViewById(R.id.tvNextGenLabel);
        tvGenUnlockProgress   = view.findViewById(R.id.tvGenUnlockProgress);
        tvAllGensUnlocked     = view.findViewById(R.id.tvAllGensUnlocked);
        layoutNextGenProgress = view.findViewById(R.id.layoutNextGenProgress);
        pbGenUnlock           = view.findViewById(R.id.pbGenUnlock);

        updateDrawDisplay();
        updateGenProgressUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDrawDisplay();
        updateGenProgressUI();
        startTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) countDownTimer.cancel();
        holdHandler.removeCallbacks(holdRunnable != null ? holdRunnable : () -> {});
    }

    /**
     * Attaches hold-to-repeat behaviour to a button.
     * Returns false so the normal click event still fires on short tap.
     */
    private boolean setupHoldRepeat(MotionEvent event, long initialDelay, long repeatInterval, Runnable action) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                holdHandler.removeCallbacks(holdRunnable != null ? holdRunnable : () -> {});
                holdRunnable = new Runnable() {
                    @Override public void run() {
                        action.run();
                        holdHandler.postDelayed(this, repeatInterval);
                    }
                };
                holdHandler.postDelayed(holdRunnable, initialDelay);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                holdHandler.removeCallbacks(holdRunnable);
                break;
        }
        return false; // let normal onClick still fire
    }

    private void showOdds(boolean show) {
        oddsVisible = show;
        layoutOdds.setVisibility(show ? View.VISIBLE : View.GONE);
        btnShowOdds.setText(show ? "Hide Odds" : "Show Odds");
    }

    private void showResults(boolean show) {
        boolean oneAtATime = gameManager.isDrawModeOneAtATime();
        if (oneAtATime) {
            layoutSingleCard.setVisibility(show ? View.VISIBLE : View.GONE);
            scrollResults.setVisibility(View.GONE);
        } else {
            scrollResults.setVisibility(show ? View.VISIBLE : View.GONE);
            layoutSingleCard.setVisibility(View.GONE);
        }
        spacerTop.setVisibility(show ? View.GONE : View.VISIBLE);
        spacerBottom.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void updateDrawDisplay() {
        int remaining = gameManager.getDrawsRemaining();
        if (remaining > 0) {
            tvDrawsRemaining.setText(getString(R.string.draws_remaining, remaining));
            btnDraw.setEnabled(true);
            btnDraw.setAlpha(1f);
        } else {
            tvDrawsRemaining.setText(R.string.no_draws_left);
            btnDraw.setEnabled(false);
            btnDraw.setAlpha(0.45f);
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(getMillisUntilMidnight(), 1000) {
            @Override
            public void onTick(long ms) {
                if (!isAdded()) return;
                long h = ms / 3_600_000, m = (ms % 3_600_000) / 60_000, s = (ms % 60_000) / 1_000;
                tvResetTimer.setText(String.format("Resets in %02d:%02d:%02d", h, m, s));
            }
            @Override
            public void onFinish() {
                if (!isAdded()) return;
                updateDrawDisplay();
                startTimer();
            }
        }.start();
    }

    private long getMillisUntilMidnight() {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        return midnight.getTimeInMillis() - System.currentTimeMillis();
    }

    private void performDraw() {
        if (!gameManager.canDraw()) {
            Toast.makeText(getContext(), getString(R.string.no_draws_left), Toast.LENGTH_SHORT).show();
            return;
        }

        btnDraw.setEnabled(false);
        btnDraw.setAlpha(0.45f);
        showOdds(false);
        showResults(true);

        boolean oneAtATime = gameManager.isDrawModeOneAtATime();

        if (oneAtATime) {
            progressBarSingle.setVisibility(View.VISIBLE);
            cardSingle.setVisibility(View.GONE);
            btnNextCard.setVisibility(View.GONE);
            tvCardProgress.setText("");
        } else {
            progressBar.setVisibility(View.VISIBLE);
            tvDrawResult.setVisibility(View.GONE);
            rvDrawResults.setVisibility(View.GONE);
        }

        List<int[]> rolls = gameManager.rollTen();
        gameManager.recordDraw();
        updateDrawDisplay();

        List<OwnedPokemon> drawnList = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pending = new AtomicInteger(rolls.size());
        PokeApiService api = RetrofitClient.getInstance().getApiService();

        for (int[] roll : rolls) {
            int pokemonId = roll[0];
            String rarity = GameManager.ordinalToRarity(roll[1]);

            api.getPokemon(pokemonId).enqueue(new retrofit2.Callback<PokemonResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<PokemonResponse> call,
                                       @NonNull retrofit2.Response<PokemonResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PokemonResponse data = response.body();
                        List<String> types = new ArrayList<>();
                        for (PokemonResponse.TypeSlot slot : data.getTypes())
                            types.add(slot.getType().getName());

                        String sprite = null;
                        if (data.getSprites().getOther() != null &&
                                data.getSprites().getOther().getOfficialArtwork() != null)
                            sprite = data.getSprites().getOther().getOfficialArtwork().getFrontDefault();
                        if (sprite == null) sprite = data.getSprites().getFrontDefault();

                        OwnedPokemon owned = new OwnedPokemon(data.getId(), data.getName(), types, rarity, sprite);
                        gameManager.addToCollection(owned);
                        drawnList.add(owned);
                    }
                    checkDone(pending, drawnList);
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<PokemonResponse> call, @NonNull Throwable t) {
                    checkDone(pending, drawnList);
                }
            });
        }
    }

    private void checkDone(AtomicInteger pending, List<OwnedPokemon> drawnList) {
        if (pending.decrementAndGet() != 0) return;
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            drawnList.sort((a, b) -> rarityOrder(b.getRarity()) - rarityOrder(a.getRarity()));

            if (gameManager.isDrawModeOneAtATime()) {
                progressBarSingle.setVisibility(View.GONE);
                drawnListForReveal = new ArrayList<>(drawnList);
                currentCardIndex = 0;
                showCardAt(0);
            } else {
                progressBar.setVisibility(View.GONE);
                tvDrawResult.setText("You drew " + drawnList.size() + " Pokémon!");
                tvDrawResult.setVisibility(View.VISIBLE);
                rvDrawResults.setVisibility(View.VISIBLE);
                // Use silhouetteUnowned=true for Pokedex-style clean cards (no evolve/bar)
                rvDrawResults.setAdapter(new PokemonCardAdapter(drawnList, true));
                // Auto-draw: schedule next draw if enabled
                if (gameManager.isAutoDraw() && gameManager.canDraw())
                    holdHandler.postDelayed(this::performDraw, 600);
            }
            updateGenProgressUI();
        });
    }

    private void showCardAt(int index) {
        if (drawnListForReveal == null || index >= drawnListForReveal.size()) return;
        OwnedPokemon p = drawnListForReveal.get(index);
        int total = drawnListForReveal.size();

        tvCardProgress.setText((index + 1) + " / " + total);
        cardSingle.setVisibility(View.VISIBLE);

        tvSingleId.setText(String.format("#%03d", p.getId()));
        tvSingleName.setText(capitalize(p.getName()));
        tvSingleType.setText(p.getTypesString());
        tvSingleRarity.setText(p.getRarity());
        tvSingleRarity.setTextColor(rarityColor(p.getRarity()));

        Glide.with(this).load(p.getSpriteUrl()).into(ivSinglePokemon);

        btnNextCard.setVisibility(View.VISIBLE);
        btnNextCard.setText(index < total - 1 ? "Next" : "Done");

        // Auto-advance cards if auto-draw is on
        if (gameManager.isAutoDraw())
            holdHandler.postDelayed(this::revealNextCard, 500);
    }

    private void revealNextCard() {
        holdHandler.removeCallbacks(holdRunnable != null ? holdRunnable : () -> {});
        currentCardIndex++;
        if (drawnListForReveal != null && currentCardIndex < drawnListForReveal.size()) {
            showCardAt(currentCardIndex);
        } else {
            showResults(false);
            drawnListForReveal = null;
            // Auto-draw: trigger next draw if still has draws
            if (gameManager.isAutoDraw() && gameManager.canDraw())
                holdHandler.postDelayed(this::performDraw, 300);
        }
    }

    private void updateGenProgressUI() {
        if (!isAdded()) return;
        int unlockedGen = gameManager.getUnlockedGenCount();
        tvGenDrawing.setText(unlockedGen == 1 ? "Gen 1" : "Gen 1–" + unlockedGen);
        if (unlockedGen >= 9) {
            layoutNextGenProgress.setVisibility(View.GONE);
            tvAllGensUnlocked.setVisibility(View.VISIBLE);
        } else {
            layoutNextGenProgress.setVisibility(View.VISIBLE);
            tvAllGensUnlocked.setVisibility(View.GONE);
            int shiny   = gameManager.countShinyInGen(unlockedGen);
            int nextGen = unlockedGen + 1;
            tvNextGenLabel.setText("Unlock Gen " + nextGen + ":");
            pbGenUnlock.setMax(GameManager.SHINY_UNLOCK_THRESHOLD);
            pbGenUnlock.setProgress(shiny);
            tvGenUnlockProgress.setText(shiny + "/" + GameManager.SHINY_UNLOCK_THRESHOLD + " ✨ Shiny in Gen " + unlockedGen);
        }
    }

    private int rarityOrder(String r) {
        switch (r) {
            case RarityConfig.LEGENDARY: return 3;
            case RarityConfig.MYTHICAL:  return 2;
            case RarityConfig.RARE:      return 1;
            default:                     return 0;
        }
    }

    private int rarityColor(String rarity) {
        switch (rarity) {
            case RarityConfig.RARE:      return requireContext().getColor(R.color.rarityRare);
            case RarityConfig.MYTHICAL:  return requireContext().getColor(R.color.rarityMythical);
            case RarityConfig.LEGENDARY: return requireContext().getColor(R.color.rarityLegendary);
            default:                     return requireContext().getColor(R.color.rarityCommon);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
