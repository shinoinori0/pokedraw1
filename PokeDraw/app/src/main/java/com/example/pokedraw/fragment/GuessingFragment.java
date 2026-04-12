package com.example.pokedraw.fragment;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.pokedraw.GameManager;
import com.example.pokedraw.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GuessingFragment extends Fragment {

    // All Gen 1–3 names indexed by (pokémonId - 1)
    private static final String[] ALL_NAMES = {
        // Gen 1 (1–151)
        "bulbasaur","ivysaur","venusaur","charmander","charmeleon","charizard",
        "squirtle","wartortle","blastoise","caterpie","metapod","butterfree",
        "weedle","kakuna","beedrill","pidgey","pidgeotto","pidgeot","rattata",
        "raticate","spearow","fearow","ekans","arbok","pikachu","raichu",
        "sandshrew","sandslash","nidoran-f","nidorina","nidoqueen","nidoran-m",
        "nidorino","nidoking","clefairy","clefable","vulpix","ninetales",
        "jigglypuff","wigglytuff","zubat","golbat","oddish","gloom","vileplume",
        "paras","parasect","venonat","venomoth","diglett","dugtrio","meowth",
        "persian","psyduck","golduck","mankey","primeape","growlithe","arcanine",
        "poliwag","poliwhirl","poliwrath","abra","kadabra","alakazam","machop",
        "machoke","machamp","bellsprout","weepinbell","victreebel","tentacool",
        "tentacruel","geodude","graveler","golem","ponyta","rapidash","slowpoke",
        "slowbro","magnemite","magneton","farfetchd","doduo","dodrio","seel",
        "dewgong","grimer","muk","shellder","cloyster","gastly","haunter",
        "gengar","onix","drowzee","hypno","krabby","kingler","voltorb",
        "electrode","exeggcute","exeggutor","cubone","marowak","hitmonlee",
        "hitmonchan","lickitung","koffing","weezing","rhyhorn","rhydon",
        "chansey","tangela","kangaskhan","horsea","seadra","goldeen","seaking",
        "staryu","starmie","mr-mime","scyther","jynx","electabuzz","magmar",
        "pinsir","tauros","magikarp","gyarados","lapras","ditto","eevee",
        "vaporeon","jolteon","flareon","porygon","omanyte","omastar","kabuto",
        "kabutops","aerodactyl","snorlax","articuno","zapdos","moltres",
        "dratini","dragonair","dragonite","mewtwo","mew",
        // Gen 2 (152–251)
        "chikorita","bayleef","meganium","cyndaquil","quilava","typhlosion",
        "totodile","croconaw","feraligatr","sentret","furret","hoothoot",
        "noctowl","ledyba","ledian","spinarak","ariados","crobat",
        "chinchou","lanturn","pichu","cleffa","igglybuff","togepi",
        "togetic","natu","xatu","mareep","flaaffy","ampharos",
        "bellossom","marill","azumarill","sudowoodo","politoed","hoppip",
        "skiploom","jumpluff","aipom","sunkern","sunflora","yanma",
        "wooper","quagsire","espeon","umbreon","murkrow","slowking",
        "misdreavus","unown","wobbuffet","girafarig","pineco","forretress",
        "dunsparce","gligar","steelix","snubbull","granbull","qwilfish",
        "scizor","shuckle","heracross","sneasel","teddiursa","ursaring",
        "slugma","magcargo","swinub","piloswine","corsola","remoraid",
        "octillery","delibird","mantine","skarmory","houndour","houndoom",
        "kingdra","phanpy","donphan","porygon2","stantler","smeargle",
        "tyrogue","hitmontop","smoochum","elekid","magby","miltank",
        "blissey","raikou","entei","suicune","larvitar","pupitar",
        "tyranitar","lugia","ho-oh","celebi",
        // Gen 3 (252–386)
        "treecko","grovyle","sceptile","torchic","combusken","blaziken",
        "mudkip","marshtomp","swampert","poochyena","mightyena","zigzagoon",
        "linoone","wurmple","silcoon","beautifly","cascoon","dustox",
        "lotad","lombre","ludicolo","seedot","nuzleaf","shiftry",
        "taillow","swellow","wingull","pelipper","ralts","kirlia",
        "gardevoir","surskit","masquerain","shroomish","breloom","slakoth",
        "vigoroth","slaking","nincada","ninjask","shedinja","whismur",
        "loudred","exploud","makuhita","hariyama","azurill","nosepass",
        "skitty","delcatty","sableye","mawile","aron","lairon",
        "aggron","meditite","medicham","electrike","manectric","plusle",
        "minun","volbeat","illumise","roselia","gulpin","swalot",
        "carvanha","sharpedo","wailmer","wailord","numel","camerupt",
        "torkoal","spoink","grumpig","spinda","trapinch","vibrava",
        "flygon","cacnea","cacturne","swablu","altaria","zangoose",
        "seviper","lunatone","solrock","barboach","whiscash","corphish",
        "crawdaunt","baltoy","claydol","lileep","cradily","anorith",
        "armaldo","feebas","milotic","castform","kecleon","shuppet",
        "banette","duskull","dusclops","tropius","chimecho","absol",
        "wynaut","snorunt","glalie","spheal","sealeo","walrein",
        "clamperl","huntail","gorebyss","relicanth","luvdisc","bagon",
        "shelgon","salamence","beldum","metang","metagross",
        "regirock","regice","registeel","latias","latios",
        "kyogre","groudon","rayquaza","jirachi","deoxys"
    };

    private ImageView ivSilhouette;
    private ProgressBar progressBar;
    private TextView tvGuessesRemaining, tvResetTimer, tvGuessFeedback, tvAnswerReveal;
    private Button btnChoice1, btnChoice2, btnChoice3, btnNextGuess;

    private GameManager gameManager;
    private CountDownTimer countDownTimer;
    private final Random random = new Random();

    private int correctId;
    private String correctName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guess, container, false);

        ivSilhouette       = view.findViewById(R.id.ivSilhouette);
        progressBar        = view.findViewById(R.id.progressBarGuess);
        tvGuessesRemaining = view.findViewById(R.id.tvGuessesRemaining);
        tvResetTimer       = view.findViewById(R.id.tvGuessResetTimer);
        tvGuessFeedback    = view.findViewById(R.id.tvGuessFeedback);
        tvAnswerReveal     = view.findViewById(R.id.tvAnswerReveal);
        btnChoice1         = view.findViewById(R.id.btnChoice1);
        btnChoice2         = view.findViewById(R.id.btnChoice2);
        btnChoice3         = view.findViewById(R.id.btnChoice3);
        btnNextGuess       = view.findViewById(R.id.btnNextGuess);

        gameManager = GameManager.getInstance(requireContext());

        btnNextGuess.setOnClickListener(v -> loadNewPokemon());

        updateGuessDisplay();
        loadNewPokemon();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGuessDisplay();
        startTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void updateGuessDisplay() {
        int remaining = gameManager.getGuessesRemaining();
        tvGuessesRemaining.setText(getString(R.string.guesses_remaining, remaining));
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        long millisUntilMidnight = getMillisUntilMidnight();
        countDownTimer = new CountDownTimer(millisUntilMidnight, 1000) {
            @Override
            public void onTick(long ms) {
                if (!isAdded()) return;
                long h = ms / 3_600_000, m = (ms % 3_600_000) / 60_000, s = (ms % 60_000) / 1_000;
                tvResetTimer.setText(String.format("Resets in %02d:%02d:%02d", h, m, s));
            }
            @Override
            public void onFinish() {
                if (!isAdded()) return;
                updateGuessDisplay();
                startTimer();
                loadNewPokemon();
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

    private void loadNewPokemon() {
        // Reset UI state
        tvGuessFeedback.setVisibility(View.GONE);
        tvAnswerReveal.setVisibility(View.GONE);
        btnNextGuess.setVisibility(View.GONE);
        setChoicesEnabled(true);
        resetButtonColors();

        boolean outOfGuesses = !gameManager.canGuess();

        if (outOfGuesses) {
            // No guesses left — show disabled silhouette state
            ivSilhouette.setImageResource(android.R.drawable.ic_menu_help);
            ivSilhouette.clearColorFilter();
            tvGuessFeedback.setText(getString(R.string.no_guesses_left));
            tvGuessFeedback.setTextColor(Color.GRAY);
            tvGuessFeedback.setVisibility(View.VISIBLE);
            btnChoice1.setText("???");
            btnChoice2.setText("???");
            btnChoice3.setText("???");
            setChoicesEnabled(false);
            return;
        }

        // Pick a random Pokémon from Gen 1–3
        correctId   = random.nextInt(ALL_NAMES.length) + 1;
        correctName = ALL_NAMES[correctId - 1];

        // Build 3 choices: 1 correct + 2 unique wrong
        List<String> choices = new ArrayList<>();
        choices.add(correctName);
        while (choices.size() < 3) {
            String wrong = ALL_NAMES[random.nextInt(ALL_NAMES.length)];
            if (!choices.contains(wrong)) choices.add(wrong);
        }
        Collections.shuffle(choices);

        btnChoice1.setText(capitalize(choices.get(0)));
        btnChoice2.setText(capitalize(choices.get(1)));
        btnChoice3.setText(capitalize(choices.get(2)));

        btnChoice1.setOnClickListener(v -> handleGuess(choices.get(0)));
        btnChoice2.setOnClickListener(v -> handleGuess(choices.get(1)));
        btnChoice3.setOnClickListener(v -> handleGuess(choices.get(2)));

        // Load silhouette
        progressBar.setVisibility(View.VISIBLE);
        ivSilhouette.setImageDrawable(null);
        String url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + correctId + ".png";
        Glide.with(this)
                .load(url)
                .into(new com.bumptech.glide.request.target.SimpleTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        ivSilhouette.setImageDrawable(resource);
                        // Apply silhouette: greyscale + darken
                        ColorMatrix cm = new ColorMatrix();
                        cm.setSaturation(0);
                        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
                        ivSilhouette.setColorFilter(filter);
                        ivSilhouette.setAlpha(0.15f);
                    }
                    @Override
                    public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void handleGuess(String chosen) {
        gameManager.recordGuess();
        updateGuessDisplay();
        setChoicesEnabled(false);

        boolean correct = chosen.equalsIgnoreCase(correctName);

        // Highlight correct answer green, wrong answer red
        highlightButtons(chosen);

        if (correct) {
            gameManager.addBonusDraw();
            tvGuessFeedback.setText("✓ Correct! +1 Bonus Draw earned!");
            tvGuessFeedback.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvGuessFeedback.setText("✗ Wrong!");
            tvGuessFeedback.setTextColor(Color.parseColor("#F44336"));
        }
        tvGuessFeedback.setVisibility(View.VISIBLE);

        // Reveal the pokemon
        tvAnswerReveal.setText("It was " + capitalize(correctName) + "!");
        tvAnswerReveal.setVisibility(View.VISIBLE);

        // Reveal the image
        ivSilhouette.clearColorFilter();
        ivSilhouette.setAlpha(1f);

        // Show next button only if guesses remain
        if (gameManager.canGuess()) {
            btnNextGuess.setVisibility(View.VISIBLE);
        }
    }

    private void highlightButtons(String chosen) {
        for (Button btn : new Button[]{btnChoice1, btnChoice2, btnChoice3}) {
            String label = btn.getText().toString().toLowerCase();
            if (label.equalsIgnoreCase(correctName)) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#4CAF50")));
            } else if (label.equalsIgnoreCase(chosen)) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#F44336")));
            }
        }
    }

    private void resetButtonColors() {
        int cardColor = requireContext().getColor(R.color.colorCard);
        for (Button btn : new Button[]{btnChoice1, btnChoice2, btnChoice3}) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cardColor));
        }
    }

    private void setChoicesEnabled(boolean enabled) {
        btnChoice1.setEnabled(enabled);
        btnChoice2.setEnabled(enabled);
        btnChoice3.setEnabled(enabled);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
