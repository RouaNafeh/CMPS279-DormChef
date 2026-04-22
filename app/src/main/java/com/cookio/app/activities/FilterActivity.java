package com.cookio.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.cookio.app.R;
import com.cookio.app.databinding.ActivityFilterBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class FilterActivity extends AppCompatActivity {

    private ActivityFilterBinding binding;
    private FirebaseAuth auth;

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

    // Equipment — switches
    private SwitchCompat switchMicrowave, switchStove, switchAirFryer;

    // Equipment — free text input
    private EditText etEquipmentSearch;
    private ChipGroup chipGroupEquipment;
    private ArrayList<String> extraEquipment = new ArrayList<>();

    // Apply
    private Button btnApplyFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(FilterActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        setupBackButton();
        setupIngredientSearch();
        setupEquipmentSearch();
        setupTimeFilter();
        setupBudgetToggle();
        setupApplyButton();

        BottomNavigationView bottomNavigation = binding.bottomNavigation.bottomNavigation;
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_favorites) {
                startActivity(new Intent(FilterActivity.this, FavoritesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_saved) {
                startActivity(new Intent(FilterActivity.this, SavedPostsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(FilterActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_my_recipes) {
                startActivity(new Intent(FilterActivity.this, MyRecipesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void initViews() {
        etIngredientSearch   = binding.etIngredientSearch;
        chipGroupIngredients = binding.chipGroupIngredients;

        seekBarTime     = binding.seekBarTime;
        tvSliderValue   = binding.tvSliderValue;
        btnUnder15      = binding.btnUnder15;
        btnUnder30      = binding.btnUnder30;

        btnBudgetLow    = binding.btnBudgetLow;
        btnBudgetMedium = binding.btnBudgetMedium;
        btnBudgetHigh   = binding.btnBudgetHigh;

        switchMicrowave    = binding.switchMicrowave;
        switchStove        = binding.switchStove;
        switchAirFryer     = binding.switchAirFryer;

        etEquipmentSearch  = binding.etEquipmentSearch;
        chipGroupEquipment = binding.chipGroupEquipment;

        btnApplyFilters = binding.btnApplyFilters;
    }

    // ── Back button ───────────────────────────────────────────
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
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
        chip.setChipBackgroundColorResource(R.color.chip_text);
        chip.setTextColor(getColor(R.color.white));
        chip.setCloseIconTintResource(R.color.white);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupIngredients.removeView(chip);
            selectedIngredients.remove(name.toLowerCase());
        });
        chipGroupIngredients.addView(chip);
    }

    // ── Equipment free-text search ────────────────────────────
    private void setupEquipmentSearch() {
        etEquipmentSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                addEquipmentFromInput();
                return true;
            }
            return false;
        });
    }

    private void addEquipmentFromInput() {
        String input = etEquipmentSearch.getText().toString().trim();
        if (input.isEmpty()) return;
        if (extraEquipment.contains(input.toLowerCase())) {
            Toast.makeText(this, input + " already added!", Toast.LENGTH_SHORT).show();
            etEquipmentSearch.setText("");
            return;
        }
        extraEquipment.add(input.toLowerCase());
        addEquipmentChip(input);
        etEquipmentSearch.setText("");
    }

    private void addEquipmentChip(String name) {
        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCheckable(false);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.chip_text);
        chip.setTextColor(getColor(R.color.white));
        chip.setCloseIconTintResource(R.color.white);
        chip.setOnCloseIconClickListener(v -> {
            chipGroupEquipment.removeView(chip);
            extraEquipment.remove(name.toLowerCase());
        });
        chipGroupEquipment.addView(chip);
    }

    // ── Time filter ───────────────────────────────────────────
    private void setupTimeFilter() {
        resetTimeButtons();
        tvSliderValue.setText(selectedMaxTime + " min");

        seekBarTime.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
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
        selected.setBackgroundTintList(null);
        unselected.setBackgroundTintList(null);
        selected.setBackgroundResource(R.drawable.bg_time_button_selected);
        selected.setTextColor(getColor(R.color.white));
        unselected.setBackgroundResource(R.drawable.bg_time_button_default);
        unselected.setTextColor(getColor(R.color.textGrey));
    }

    private void resetTimeButtons() {
        btnUnder15.setBackgroundTintList(null);
        btnUnder30.setBackgroundTintList(null);
        btnUnder15.setBackgroundResource(R.drawable.bg_time_button_default);
        btnUnder15.setTextColor(getColor(R.color.textGrey));
        btnUnder30.setBackgroundResource(R.drawable.bg_time_button_default);
        btnUnder30.setTextColor(getColor(R.color.textGrey));
    }

    // ── Budget toggle ─────────────────────────────────────────
    private void setupBudgetToggle() {
        selectBudget(btnBudgetMedium, btnBudgetLow, btnBudgetHigh, "medium");
        btnBudgetLow.setOnClickListener(v    -> selectBudget(btnBudgetLow,    btnBudgetMedium, btnBudgetHigh,   "low"));
        btnBudgetMedium.setOnClickListener(v -> selectBudget(btnBudgetMedium, btnBudgetLow,    btnBudgetHigh,   "medium"));
        btnBudgetHigh.setOnClickListener(v   -> selectBudget(btnBudgetHigh,   btnBudgetLow,    btnBudgetMedium, "high"));
    }

    private void selectBudget(Button selected, Button other1, Button other2, String budget) {
        selectedBudget = budget;
        selected.setBackgroundTintList(null);
        other1.setBackgroundTintList(null);
        other2.setBackgroundTintList(null);
        selected.setBackgroundResource(R.drawable.bg_toggle_selected);
        selected.setTextColor(getColor(R.color.textGrey));
        other1.setBackgroundResource(R.drawable.bg_toggle_unselected);
        other1.setTextColor(getColor(R.color.textGrey));
        other2.setBackgroundResource(R.drawable.bg_toggle_unselected);
        other2.setTextColor(getColor(R.color.textGrey));
    }

    // ── Apply button ──────────────────────────────────────────
    private void setupApplyButton() {
        btnApplyFilters.setOnClickListener(v -> {

            // Collect equipment: switches + anything the user typed
            ArrayList<String> availableEquipment = new ArrayList<>();
            if (switchMicrowave.isChecked()) availableEquipment.add("microwave");
            if (switchStove.isChecked())     availableEquipment.add("stove");
            if (switchAirFryer.isChecked())  availableEquipment.add("air fryer");
            availableEquipment.addAll(extraEquipment); // add typed equipment chips

            Intent intent = new Intent(this, RecipeListActivity.class);
            intent.putStringArrayListExtra("ingredients", selectedIngredients);
            intent.putExtra("maxTime", selectedMaxTime);
            intent.putExtra("budget", selectedBudget);
            intent.putStringArrayListExtra("equipment", availableEquipment);
            intent.putExtra("includeUserRecipes", true);
            startActivity(intent);
        });
    }
}
