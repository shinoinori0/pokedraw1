package com.example.pokedraw;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Queue;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;

import com.example.pokedraw.fragment.CollectionFragment;
import com.example.pokedraw.fragment.DrawFragment;
import com.example.pokedraw.fragment.GuessingFragment;
import com.example.pokedraw.fragment.PokedexFragment;

public class MainActivity extends AppCompatActivity {

    private TextView tabDraw, tabGuess, tabPokedex, tabCollection;
    private final Queue<String[]> evoQueue = new ArrayDeque<>();
    private boolean evoDialogShowing = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GameManager.getInstance(this).setEvolutionListener((from, to) ->
            mainHandler.post(() -> enqueueEvolution(from, to)));

        tabDraw       = findViewById(R.id.tabDraw);
        tabGuess      = findViewById(R.id.tabGuess);
        tabPokedex    = findViewById(R.id.tabPokedex);
        tabCollection = findViewById(R.id.tabCollection);

        tabDraw.setOnClickListener(v -> selectTab(0));
        tabGuess.setOnClickListener(v -> selectTab(1));
        tabPokedex.setOnClickListener(v -> selectTab(2));
        tabCollection.setOnClickListener(v -> selectTab(3));

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        if (savedInstanceState == null) {
            selectTab(0);
            // Show onboarding only on very first launch
            if (!GameManager.getInstance(this).isOnboardingDone()) {
                GameManager.getInstance(this).setOnboardingDone();
                showOnboardingDialog();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            MusicManager.getInstance().release();
            GameManager.getInstance(this).setEvolutionListener(null);
        }
    }

    private void enqueueEvolution(String from, String to) {
        evoQueue.add(new String[]{from, to});
        if (!evoDialogShowing) showNextEvo();
    }

    private void showNextEvo() {
        String[] next = evoQueue.poll();
        if (next == null) { evoDialogShowing = false; return; }
        evoDialogShowing = true;
        new AlertDialog.Builder(this)
            .setTitle("✨ Evolution!")
            .setMessage(next[0] + " has evolved into " + next[1] + "!")
            .setPositiveButton("OK", (d, w) -> showNextEvo())
            .setCancelable(false)
            .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MusicManager.getInstance().start(this);
        // Restore mute state across restarts
        boolean muted = GameManager.getInstance(this).isBgmMuted();
        MusicManager.getInstance().setMuted(muted);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.getInstance().resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.getInstance().pause();
    }

    private void showSettingsDialog() {
        GameManager gm = GameManager.getInstance(this);
        boolean oneAtATime = gm.isDrawModeOneAtATime();
        boolean bgmMuted   = gm.isBgmMuted();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);

        TextView dotOneAtATime  = dialogView.findViewById(R.id.dotOneAtATime);
        TextView dotAllAtOnce   = dialogView.findViewById(R.id.dotAllAtOnce);
        Switch   switchBgm      = dialogView.findViewById(R.id.switchBgm);
        TextView tvBgmStatus    = dialogView.findViewById(R.id.tvBgmStatus);
        Switch   switchAutoDraw = dialogView.findViewById(R.id.switchAutoDraw);

        // Set initial state
        dotOneAtATime.setVisibility(oneAtATime  ? View.VISIBLE : View.GONE);
        dotAllAtOnce.setVisibility(!oneAtATime  ? View.VISIBLE : View.GONE);
        switchBgm.setChecked(!bgmMuted);
        tvBgmStatus.setText(bgmMuted ? "Off" : "On");
        switchAutoDraw.setChecked(gm.isAutoDraw());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        dialogView.findViewById(R.id.rowOneAtATime).setOnClickListener(v -> {
            gm.setDrawModeOneAtATime(true);
            dotOneAtATime.setVisibility(View.VISIBLE);
            dotAllAtOnce.setVisibility(View.GONE);
        });

        dialogView.findViewById(R.id.rowAllAtOnce).setOnClickListener(v -> {
            gm.setDrawModeOneAtATime(false);
            dotOneAtATime.setVisibility(View.GONE);
            dotAllAtOnce.setVisibility(View.VISIBLE);
        });

        dialogView.findViewById(R.id.rowBgm).setOnClickListener(v -> {
            boolean nowMuted = !gm.isBgmMuted();
            gm.setBgmMuted(nowMuted);
            switchBgm.setChecked(!nowMuted);
            tvBgmStatus.setText(nowMuted ? "Off" : "On");
        });

        dialogView.findViewById(R.id.rowAutoDraw).setOnClickListener(v -> {
            boolean now = !gm.isAutoDraw();
            gm.setAutoDraw(now);
            switchAutoDraw.setChecked(now);
        });

        dialogView.findViewById(R.id.rowLogout).setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            GameManager.getInstance(this).onUserLoggedIn(); // reset local state
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialogView.findViewById(R.id.rowHowToPlay).setOnClickListener(v -> {
            dialog.dismiss();
            showOnboardingDialog();
        });

        dialogView.findViewById(R.id.rowClearData).setOnClickListener(v -> {
            dialog.dismiss();
            confirmClearData();
        });

        EditText etCode        = dialogView.findViewById(R.id.etRedeemCode);
        TextView tvRedeemResult = dialogView.findViewById(R.id.tvRedeemResult);
        dialogView.findViewById(R.id.btnRedeem).setOnClickListener(v -> {
            String code = etCode.getText().toString();
            if (gm.redeemCode(code)) {
                tvRedeemResult.setText("✓ +500 draws added!");
                tvRedeemResult.setTextColor(0xFF4CAF50);
                etCode.setText("");
                // Refresh balance on DrawFragment if it's currently active
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current instanceof DrawFragment) ((DrawFragment) current).updateDrawDisplay();
            } else {
                tvRedeemResult.setText("✗ Invalid code");
                tvRedeemResult.setTextColor(0xFFFF5252);
            }
            tvRedeemResult.setVisibility(View.VISIBLE);
        });

        dialog.show();
    }

    private void showOnboardingDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_onboarding, null);
        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton("Got it!", null)
                .create()
                .show();
    }

    private void confirmClearData() {
        new AlertDialog.Builder(this)
            .setTitle("Clear all data?")
            .setMessage("This will reset your collection, draws, and guesses. This cannot be undone.")
            .setPositiveButton("Clear", (d, w) -> {
                GameManager.getInstance(this).clearAllData();
                selectTab(0);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void selectTab(int index) {
        tabDraw.setTextColor(getColor(R.color.colorTextSecondary));
        tabGuess.setTextColor(getColor(R.color.colorTextSecondary));
        tabPokedex.setTextColor(getColor(R.color.colorTextSecondary));
        tabCollection.setTextColor(getColor(R.color.colorTextSecondary));

        Fragment fragment;
        switch (index) {
            case 1:
                tabGuess.setTextColor(getColor(R.color.colorAccent));
                fragment = new GuessingFragment();
                break;
            case 2:
                tabPokedex.setTextColor(getColor(R.color.colorAccent));
                fragment = new PokedexFragment();
                break;
            case 3:
                tabCollection.setTextColor(getColor(R.color.colorAccent));
                fragment = new CollectionFragment();
                break;
            default:
                tabDraw.setTextColor(getColor(R.color.colorAccent));
                fragment = new DrawFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
