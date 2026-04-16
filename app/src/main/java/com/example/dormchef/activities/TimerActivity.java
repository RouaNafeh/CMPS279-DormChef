package com.example.dormchef.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dormchef.R;
import com.example.dormchef.models.RecipeContent;

import java.util.ArrayList;
import java.util.List;

public class TimerActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_NAME = "recipe_name";
    public static final String EXTRA_RECIPE_STEPS = "recipe_steps";

    private TextView tvRecipeTitle;
    private TextView tvStepIndicator;
    private TextView tvStepDescription;
    private TextView tvTimer;
    private Button btnNextStep;
    private Button btnStart;
    private Button btnPause;
    private Button btnReset;
    private Button btnApplyCustomTime;
    private ImageButton btnBack;
    private EditText etTimerMinutes;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 300000;
    private long selectedDurationInMillis = 300000;
    private boolean timerRunning = false;

    private String recipeTitle = "Recipe";
    private List<String> steps = new ArrayList<>();
    private int currentStep = 0;
    private int totalSteps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        initViews();
        loadRecipeData();
        setupClickListeners();
        updateStepDisplay();
        updateTimerDisplay();
        btnPause.setEnabled(false);
        btnReset.setEnabled(false);
    }

    private void initViews() {
        tvRecipeTitle = findViewById(R.id.tvRecipeTitle);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        tvStepDescription = findViewById(R.id.tvStepDescription);
        tvTimer = findViewById(R.id.tvTimer);
        btnNextStep = findViewById(R.id.btnNextStep);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnReset = findViewById(R.id.btnReset);
        btnApplyCustomTime = findViewById(R.id.btnApplyCustomTime);
        btnBack = findViewById(R.id.btnBack);
        etTimerMinutes = findViewById(R.id.etTimerMinutes);
    }

    private void loadRecipeData() {
        recipeTitle = getIntent().getStringExtra(EXTRA_RECIPE_NAME);
        if (recipeTitle == null || recipeTitle.trim().isEmpty()) {
            recipeTitle = "Recipe";
        }

        tvRecipeTitle.setText(recipeTitle);
        String customSteps = getIntent().getStringExtra(EXTRA_RECIPE_STEPS);
        steps = splitMultiline(customSteps);
        if (steps.isEmpty()) {
            steps = new ArrayList<>(RecipeContent.getDetails(recipeTitle).getSteps());
        }
        totalSteps = steps.size();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnStart.setOnClickListener(v -> startTimer());
        btnPause.setOnClickListener(v -> pauseTimer());
        btnReset.setOnClickListener(v -> resetTimer());
        btnNextStep.setOnClickListener(v -> nextStep());
        btnApplyCustomTime.setOnClickListener(v -> applyCustomTime());
        etTimerMinutes.setText(String.valueOf(selectedDurationInMillis / 60000L));
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                Toast.makeText(TimerActivity.this, "Time's up! Check your cooking!", Toast.LENGTH_LONG).show();
                btnStart.setEnabled(true);
                btnPause.setEnabled(false);
                btnReset.setEnabled(true);
            }
        }.start();

        btnPause.setText("Pause");
        btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        timerRunning = true;
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnReset.setEnabled(true);
    }

    private void pauseTimer() {
        if (timerRunning) {
            countDownTimer.cancel();
            timerRunning = false;
            btnPause.setText("Cont.");
            btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            startTimer();
            btnPause.setText("Pause");
            btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D59493")));
        }
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timeLeftInMillis = selectedDurationInMillis;
        updateTimerDisplay();
        btnPause.setText("Pause");
        btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D59493")));
        timerRunning = false;
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnReset.setEnabled(false);
    }

    private void nextStep() {
        if (currentStep < totalSteps - 1) {
            currentStep++;
            updateStepDisplay();
            resetTimer();
            Toast.makeText(this, "Moving to step " + (currentStep + 1), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Recipe completed!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateStepDisplay() {
        tvStepIndicator.setText("Step " + (currentStep + 1) + " of " + totalSteps);
        tvStepDescription.setText(steps.get(currentStep));
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        tvTimer.setText(timeFormatted);
    }

    private void applyCustomTime() {
        if (timerRunning) {
            Toast.makeText(this, "Pause or reset the timer before changing it.", Toast.LENGTH_SHORT).show();
            return;
        }

        String input = etTimerMinutes.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter the number of minutes you need.", Toast.LENGTH_SHORT).show();
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid number of minutes.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minutes < 1) {
            Toast.makeText(this, "Timer must be at least 1 minute.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minutes > 60) {
            Toast.makeText(this, "Timer cannot be more than 60 minutes.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedDurationInMillis = minutes * 60000L;
        timeLeftInMillis = selectedDurationInMillis;
        updateTimerDisplay();
        Toast.makeText(this, "Timer set to " + minutes + " minute" + (minutes == 1 ? "" : "s") + ".", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private ArrayList<String> splitMultiline(String rawValue) {
        ArrayList<String> values = new ArrayList<>();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return values;
        }
        String[] lines = rawValue.split("\\r?\\n|\\|");
        for (String line : lines) {
            for (String part : line.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return values;
    }
}
