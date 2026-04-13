package com.example.pokedraw.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pokedraw.GameManager;
import com.example.pokedraw.PokemonDexCache;
import com.example.pokedraw.R;
import com.example.pokedraw.RarityConfig;
import com.example.pokedraw.adapter.PokemonCardAdapter;
import com.example.pokedraw.model.OwnedPokemon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PokedexFragment extends Fragment {

    private static final int TOTAL    = RarityConfig.TOTAL_POKEMON;
    private static final int GEN1_END = 151;
    private static final int GEN2_END = 251;
    private static final int GEN3_END = 386;
    private static final int GEN4_END = 493;
    private static final int GEN5_END = 649;
    private static final int GEN6_END = 721;
    private static final int GEN7_END = 809;
    private static final int GEN8_END = 905;

    private int activeGen = 0;

    private RecyclerView rvPokedex;
    private ProgressBar progressBar;
    private EditText etSearch;
    private TextView tabAll,tabGen1,tabGen2,tabGen3,tabGen4,tabGen5,tabGen6,tabGen7,tabGen8,tabGen9;
    private PokemonCardAdapter adapter;

    private final List<OwnedPokemon> fullList    = new ArrayList<>();
    private final List<OwnedPokemon> displayList = new ArrayList<>();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pokedex, container, false);

        rvPokedex   = view.findViewById(R.id.rvPokedex);
        progressBar = view.findViewById(R.id.progressBarPokedex);
        etSearch    = view.findViewById(R.id.etSearch);
        tabAll  = view.findViewById(R.id.tabAll);
        tabGen1 = view.findViewById(R.id.tabGen1);
        tabGen2 = view.findViewById(R.id.tabGen2);
        tabGen3 = view.findViewById(R.id.tabGen3);
        tabGen4 = view.findViewById(R.id.tabGen4);
        tabGen5 = view.findViewById(R.id.tabGen5);
        tabGen6 = view.findViewById(R.id.tabGen6);
        tabGen7 = view.findViewById(R.id.tabGen7);
        tabGen8 = view.findViewById(R.id.tabGen8);
        tabGen9 = view.findViewById(R.id.tabGen9);
        Button btnClear = view.findViewById(R.id.btnClear);

        rvPokedex.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new PokemonCardAdapter(displayList, true);
        rvPokedex.setAdapter(adapter);

        buildPlaceholderList();
        progressBar.setVisibility(View.GONE);

        GameManager.getInstance(requireContext()).getCollection(collection -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> markOwned(collection));
        });

        tabAll.setOnClickListener(v  -> selectGen(0));
        tabGen1.setOnClickListener(v -> selectGen(1));
        tabGen2.setOnClickListener(v -> selectGen(2));
        tabGen3.setOnClickListener(v -> selectGen(3));
        tabGen4.setOnClickListener(v -> selectGen(4));
        tabGen5.setOnClickListener(v -> selectGen(5));
        tabGen6.setOnClickListener(v -> selectGen(6));
        tabGen7.setOnClickListener(v -> selectGen(7));
        tabGen8.setOnClickListener(v -> selectGen(8));
        tabGen9.setOnClickListener(v -> selectGen(9));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){ applyFilter(); }
            @Override public void afterTextChanged(Editable s){}
        });
        btnClear.setOnClickListener(v -> { etSearch.setText(""); applyFilter(); });

        return view;
    }

    private void buildPlaceholderList() {
        fullList.clear();
        for (int id = 1; id <= TOTAL; id++) {
            OwnedPokemon p = new OwnedPokemon();
            p.setId(id);
            p.setName(PokemonDexCache.getName(requireContext(), id));
            p.setTypes(new ArrayList<>());
            p.setRarity("???");
            p.setSpriteUrl("");
            p.setDisplayId(id);
            p.setCount(0);
            fullList.add(p);
        }
        applyFilter();
    }

    private void markOwned(Map<Integer, OwnedPokemon> collection) {
        for (int i = 0; i < fullList.size(); i++) {
            OwnedPokemon slot = fullList.get(i);
            int id = slot.getId();
            if (collection.containsKey(id)) {
                OwnedPokemon owned = collection.get(id);
                slot.setCount(1);
                slot.setDisplayId(owned.getDisplayId() > 0 ? owned.getDisplayId() : id);
                slot.setTier(owned.getTier());
                slot.setName(owned.getName() != null && !owned.getName().isEmpty()
                        ? owned.getName()
                        : "Pokemon #" + String.format("%03d", id));
                slot.setTypes(owned.getTypes() != null ? owned.getTypes() : new ArrayList<>());
                slot.setRarity(owned.getRarity() != null ? owned.getRarity() : RarityConfig.COMMON);
            } else {
                slot.setCount(0);
                slot.setName("???");
                slot.setTypes(new ArrayList<>());
                slot.setRarity("???");
            }
        }
        applyFilter();
    }

    private void selectGen(int gen) {
        activeGen = gen;
        updateTabStyles();
        applyFilter();
        rvPokedex.scrollToPosition(0);
    }

    private void updateTabStyles() {
        int accent    = requireContext().getColor(R.color.colorAccent);
        int bg        = requireContext().getColor(R.color.colorBackground);
        int card      = requireContext().getColor(R.color.colorCard);
        int secondary = requireContext().getColor(R.color.colorTextSecondary);
        TextView[] tabs = {tabAll,tabGen1,tabGen2,tabGen3,tabGen4,tabGen5,tabGen6,tabGen7,tabGen8,tabGen9};
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setBackgroundColor(activeGen == i ? accent : card);
            tabs[i].setTextColor(activeGen == i ? bg : secondary);
        }
    }

    private void applyFilter() {
        String q = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        displayList.clear();
        for (OwnedPokemon p : fullList) {
            int id = p.getId();
            if (activeGen == 1 && id > GEN1_END) continue;
            if (activeGen == 2 && (id <= GEN1_END || id > GEN2_END)) continue;
            if (activeGen == 3 && (id <= GEN2_END || id > GEN3_END)) continue;
            if (activeGen == 4 && (id <= GEN3_END || id > GEN4_END)) continue;
            if (activeGen == 5 && (id <= GEN4_END || id > GEN5_END)) continue;
            if (activeGen == 6 && (id <= GEN5_END || id > GEN6_END)) continue;
            if (activeGen == 7 && (id <= GEN6_END || id > GEN7_END)) continue;
            if (activeGen == 8 && (id <= GEN7_END || id > GEN8_END)) continue;
            if (activeGen == 9 && id <= GEN8_END) continue;
            if (!q.isEmpty()
                    && !p.getName().toLowerCase().contains(q)
                    && !String.format("%04d", id).contains(q)) continue;
            displayList.add(p);
        }
        adapter.notifyDataSetChanged();
    }
}
