package com.example.dormchef.activities;

import com.example.dormchef.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.dormchef.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class FilterActivity extends AppCompatActivity {

    // Ingredients
    private EditText etIngredientSearch;
    private ChipGroup chipGroupIngredients;
    private ArrayList<String> selectedIngredients = new ArrayList<>();

    // Time
    private SeekBar seekBarTime;
    private TextView tvSliderValue;
    private Button btnUnder15, btnUnder30;
    private int selectedMaxTime = 60;

    // Budget
    private Button btnBudgetLow, btnBudgetMedium, btnBudgetHigh;
    private String selectedBudget = "medium";

    // Equipment
    private SwitchCompat switchMicrowave, switchStove, switchAirFryer;

    // Apply
    private Button btnApplyFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        initViews();
        setupBackButton();
        setupIngredientSearch();
        setupTimeFilter();
        setupBudgetToggle();
        setupApplyButton();
    }

    private void initViews() {
        etIngredientSearch   = findViewById(R.id.etIngredientSearch);
        chipGroupIngredients = findViewById(R.id.chipGroupIngredients);

        seekBarTime    = findViewById(R.id.seekBarTime);
        tvSliderValue  = findViewById(R.id.tvSliderValue);
        btnUnder15     = findViewById(R.id.btnUnder15);
        btnUnder30     = findViewById(R.id.btnUnder30);

        btnBudgetLow    = findViewById(R.id.btnBudgetLow);
        btnBudgetMedium = findViewById(R.id.btnBudgetMedium);
        btnBudgetHigh   = findViewById(R.id.btnBudgetHigh);

        switchMicrowave = findViewById(R.id.switchMicrowave);
        switchStove     = findViewById(R.id.switchStove);
        switchAirFryer  = findViewById(R.id.switchAirFryer);

        btnApplyFilters = findViewById(R.id.btnApplyFilters);
    }

    // ── Back button ───────────────────────────────────────────
    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    // ── Ingredient search ─────────────────────────────────────
    private void setupIngredientSearch() {
        etIngredientSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                addIngredientFromInput();
                return true;
            }
            return false;
        });
    }

    private void addIngredientFromInput() {
        String input = etIngredientSearch.getText().toString().trim();
        if (input.isEmpty()) return;

        if (selectedIngredients.contains(input.toLowerCase())) {
            Toast.makeText(this, input + " already added!", Toast.LENGTH_SHORT).show();
            etIngredientSearch.setText("");
            return;
        }

        selectedIngredients.add(input.toLowerCase());
        addIngredientChip(input);
        etIngredientSearch.setText("");
    }

    private void addIngredientChip(String name) {
        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCheckable(false);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.primary);
        chip.setTextColor(getColor(R.color.white));
        chip.setCloseIconTintResource(R.color.white);

        chip.setOnCloseIconClickListener(v -> {
            chipGroupIngredients.removeView(chip);
            selectedIngredients.remove(name.toLowerCase());
        });

        chipGroupIngredients.addView(chip);
    }

    // ── Time filter ───────────────────────────────────────────
    private void setupTimeFilter() {
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedMaxTime = Math.max(progress, 5);
                tvSliderValue.setText(selectedMaxTime + " min");
                if (fromUser) resetTimeButtons();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnUnder15.setOnClickListener(v -> {
            seekBarTime.setProgress(15);
            selectedMaxTime = 15;
            tvSliderValue.setText("15 min");
            setTimeButtonSelected(btnUnder15, btnUnder30);
        });

        btnUnder30.setOnClickListener(v -> {
            seekBarTime.setProgress(30);
            selectedMaxTime = 30;
            tvSliderValue.setText("30 min");
            setTimeButtonSelected(btnUnder30, btnUnder15);
        });
    }

    private void setTimeButtonSelected(Button selected, Button unselected) {
        selected.setBackgroundResource(R.drawable.bg_time_button_selected);
        selected.setTextColor(getColor(R.color.primary));
        unselected.setBackgroundResource(R.drawable.bg_time_button_default);
        unselected.setTextColor(getColor(R.color.textGrey));
    }

    private void resetTimeButtons() {
        btnUnder15.setBackgroundResource(R.drawable.bg_time_button_default);
        btnUnder15.setTextColor(getColor(R.color.textGrey));
        btnUnder30.setBackgroundResource(R.drawable.bg_time_button_default);
        btnUnder30.setTextColor(getColor(R.color.textGrey));
    }

    // ── Budget toggle ─────────────────────────────────────────
    private void setupBudgetToggle() {
        selectBudget(btnBudgetMedium, btnBudgetLow, btnBudgetHigh, "medium");

        btnBudgetLow.setOnClickListener(v ->
                selectBudget(btnBudgetLow, btnBudgetMedium, btnBudgetHigh, "low"));
        btnBudgetMedium.setOnClickListener(v ->
                selectBudget(btnBudgetMedium, btnBudgetLow, btnBudgetHigh, "medium"));
        btnBudgetHigh.setOnClickListener(v ->
                selectBudget(btnBudgetHigh, btnBudgetLow, btnBudgetMedium, "high"));
    }

    private void selectBudget(Button selected, Button other1, Button other2, String budget) {
        selectedBudget = budget;
        selected.setBackgroundResource(R.drawable.bg_toggle_selected);
        selected.setTextColor(getColor(R.color.textDark));
        other1.setBackgroundResource(R.drawable.bg_toggle_unselected);
        other1.setTextColor(getColor(R.color.textGrey));
        other2.setBackgroundResource(R.drawable.bg_toggle_unselected);
        other2.setTextColor(getColor(R.color.textGrey));
    }

    // ── Apply button ──────────────────────────────────────────
    private void setupApplyButton() {
        btnApplyFilters.setOnClickListener(v -> {

            if (selectedIngredients.isEmpty()) {
                Toast.makeText(this,
                        "Please add at least one ingredient!", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> equipment = new ArrayList<>();
            if (switchMicrowave.isChecked()) equipment.add("microwave");
            if (switchStove.isChecked())     equipment.add("stove");
            if (switchAirFryer.isChecked())  equipment.add("air fryer");

            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putStringArrayListExtra("ingredients", selectedIngredients);
            intent.putExtra("maxTime", selectedMaxTime);
            intent.putExtra("budget", selectedBudget);
            intent.putStringArrayListExtra("equipment", equipment);
            startActivity(intent);
        });
    }
}