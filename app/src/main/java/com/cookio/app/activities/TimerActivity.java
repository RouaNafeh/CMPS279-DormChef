package com.cookio.app.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cookio.app.R;
import com.cookio.app.models.CookingStep;
import com.cookio.app.models.CookingStepParser;
import com.cookio.app.models.RecipeContent;

import java.util.ArrayList;
import java.util.List;

public class TimerActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_NAME = "recipe_name";
    public static final String EXTRA_RECIPE_STEPS = "recipe_steps";
    public static final String EXTRA_RECIPE_TIMED_STEPS = "recipe_timed_steps";

    private TextView tvRecipeTitle;
    private TextView tvStepIndicator;
    private TextView tvStepDescription;
    private TextView tvTimer;
    private TextView tvTimerStatus;
    private Button btnNextStep;
    private Button btnStart;
    private Button btnPause;
    private Button btnReset;
    private ImageButton btnBack;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 0L;
    private long selectedDurationInMillis = 0L;
    private boolean timerRunning = false;

    private String recipeTitle = "Recipe";
    private List<CookingStep> steps = new ArrayList<>();
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        initViews();
        loadRecipeData();
        setupClickListeners();
        if (steps.isEmpty()) {
            Toast.makeText(this, R.string.cooking_mode_missing_steps, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadCurrentStep();
    }

    private void initViews() {
        tvRecipeTitle = findViewById(R.id.tvRecipeTitle);
        tvStepIndicator = findViewById(R.id.tvStepIndicator);
        tvStepDescription = findViewById(R.id.tvStepDescription);
        tvTimer = findViewById(R.id.tvTimer);
        tvTimerStatus = findViewById(R.id.tvTimerStatus);
        btnNextStep = findViewById(R.id.btnNextStep);
        btnStart = findViewById(R.id.btnStart);
        btnPause = findViewById(R.id.btnPause);
        btnReset = findViewById(R.id.btnReset);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadRecipeData() {
        recipeTitle = getIntent().getStringExtra(EXTRA_RECIPE_NAME);
        if (recipeTitle == null || recipeTitle.trim().isEmpty()) {
            recipeTitle = "Recipe";
        }
        tvRecipeTitle.setText(recipeTitle);

        ArrayList<String> timedSteps = getIntent().getStringArrayListExtra(EXTRA_RECIPE_TIMED_STEPS);
        if (timedSteps != null && !timedSteps.isEmpty()) {
            steps = CookingStepParser.parseList(timedSteps);
        }

        if (steps.isEmpty()) {
            String customSteps = getIntent().getStringExtra(EXTRA_RECIPE_STEPS);
            steps = CookingStepParser.parseDelimited(customSteps);
        }

        if (steps.isEmpty()) {
            steps = CookingStepParser.parseList(RecipeContent.getDetails(recipeTitle).getSteps());
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        btnStart.setOnClickListener(v -> startTimer());
        btnPause.setOnClickListener(v -> pauseTimer());
        btnReset.setOnClickListener(v -> resetTimer());
        btnNextStep.setOnClickListener(v -> nextStep());
    }

    private void loadCurrentStep() {
        cancelTimer();

        CookingStep step = steps.get(currentStep);
        selectedDurationInMillis = step.getMinutes() * 60000L;
        timeLeftInMillis = selectedDurationInMillis;
        timerRunning = false;

        tvStepIndicator.setText(getString(R.string.cooking_step_of_total, currentStep + 1, steps.size()));
        tvStepDescription.setText(step.getInstruction());

        updateTimerDisplay();
        updateTimerControls(step.hasTimer());
        btnNextStep.setText(currentStep == steps.size() - 1 ? "Finish" : "Next Step");
    }

    private void updateTimerControls(boolean hasTimer) {
        if (hasTimer) {
            tvTimerStatus.setText(getString(R.string.cooking_minutes_label, steps.get(currentStep).getMinutes()));
            btnStart.setEnabled(true);
            btnPause.setEnabled(false);
            btnReset.setEnabled(false);
            btnStart.setVisibility(View.VISIBLE);
            btnPause.setVisibility(View.VISIBLE);
            btnReset.setVisibility(View.VISIBLE);
        } else {
            tvTimerStatus.setText(R.string.cooking_no_timer);
            btnStart.setEnabled(false);
            btnPause.setEnabled(false);
            btnReset.setEnabled(false);
            btnStart.setVisibility(View.GONE);
            btnPause.setVisibility(View.GONE);
            btnReset.setVisibility(View.GONE);
        }
    }

    private void startTimer() {
        if (!steps.get(currentStep).hasTimer() || timerRunning) {
            return;
        }

        cancelTimer();
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                timeLeftInMillis = 0L;
                updateTimerDisplay();
                btnStart.setEnabled(true);
                btnPause.setEnabled(false);
                btnReset.setEnabled(true);
                tvTimerStatus.setText(R.string.cooking_timer_done);
                Toast.makeText(TimerActivity.this, R.string.cooking_timer_done, Toast.LENGTH_LONG).show();
            }
        }.start();

        timerRunning = true;
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnReset.setEnabled(true);
    }

    private void pauseTimer() {
        if (!timerRunning) {
            startTimer();
            return;
        }

        cancelTimer();
        timerRunning = false;
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnReset.setEnabled(true);
    }

    private void resetTimer() {
        cancelTimer();
        timerRunning = false;
        timeLeftInMillis = selectedDurationInMillis;
        updateTimerDisplay();
        btnStart.setEnabled(steps.get(currentStep).hasTimer());
        btnPause.setEnabled(false);
        btnReset.setEnabled(false);
        if (steps.get(currentStep).hasTimer()) {
            tvTimerStatus.setText(getString(R.string.cooking_minutes_label, steps.get(currentStep).getMinutes()));
        }
    }

    private void nextStep() {
        if (currentStep < steps.size() - 1) {
            currentStep++;
            loadCurrentStep();
        } else {
            Toast.makeText(this, R.string.cooking_recipe_complete, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}
