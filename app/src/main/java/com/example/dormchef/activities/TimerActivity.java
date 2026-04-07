package com.example.dormchef.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormchef.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.res.ColorStateList;
import android.graphics.Color;

public class TimerActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvRecipeTitle;
    private TextView tvStepIndicator;
    private TextView tvStepDescription;
    private TextView tvTimer;
    private Button btnNextStep;
    private Button btnStart;
    private Button btnPause;
    private Button btnReset;

    // Timer variables
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 600000; // 10 minutes in milliseconds
    private boolean timerRunning = false;

    // Recipe data (using hardcoded data for now)
    private String recipeTitle = "Smashed Burger";
    private List<String> steps = new ArrayList<>();
    private int currentStep = 0;
    private int totalSteps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        initViews();
        loadSampleData(); // Using sample data since teammate hasn't built her part yet
        setupClickListeners();
        updateStepDisplay();
        updateTimerDisplay();
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
    }

    private void loadSampleData() {
        // Hardcoded steps for testing - your timer works 100% without anyone else!
        recipeTitle = "Smashed Burger";
        tvRecipeTitle.setText(recipeTitle);

        steps = Arrays.asList(
                "Step 1: Form loosely packed balls of cold 80/20 ground beef without overworking or seasoning the meat.",
                "Step 2: Heat a cast-iron skillet or griddle over high heat until it's smoking hot.",
                "Step 3: Place the meat balls in the skillet and press down firmly with a spatula to smash flat.",
                "Step 4: Season generously with salt and pepper, cook for 2-3 minutes until edges are crispy.",
                "Step 5: Flip, add cheese if desired, and cook for another 1-2 minutes. Serve on buns with toppings."
        );

        totalSteps = steps.size();
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> startTimer());
        btnPause.setOnClickListener(v -> pauseTimer());
        btnReset.setOnClickListener(v -> resetTimer());
        btnNextStep.setOnClickListener(v -> nextStep());
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
                // Update button states
                btnStart.setEnabled(true);
                btnPause.setEnabled(false);
                btnReset.setEnabled(true);
            }
        }.start();
        btnPause.setText("Pause");
        btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        timerRunning = true;

        // Update button states
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnReset.setEnabled(true);
    }

    private void pauseTimer() {
        if (timerRunning) {
            // Pause
            countDownTimer.cancel();
            timerRunning = false;
            btnPause.setText("Cont.");
            btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            // Resume
            startTimer();
            btnPause.setText("Pause");  // startTimer() already overwrites this
            btnPause.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D59493")));
        }
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        timeLeftInMillis = 600000; // Reset to 10 minutes
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

            // Reset timer for new step
            resetTimer();

            // Optional: Show a message when moving to next step
            Toast.makeText(this, "Moving to step " + (currentStep + 1), Toast.LENGTH_SHORT).show();
        } else {
            // Last step completed
            Toast.makeText(this, "🎉 Congratulations! Recipe completed! 🎉", Toast.LENGTH_LONG).show();
            finish(); // Close the timer activity
        }
    }

    private void updateStepDisplay() {
        tvStepIndicator.setText("Step " + (currentStep + 1) + " of " + totalSteps);
        tvStepDescription.setText(steps.get(currentStep));
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format("%02d:%02d:00", minutes, seconds);
        tvTimer.setText(timeFormatted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}