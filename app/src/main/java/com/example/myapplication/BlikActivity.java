package com.example.myapplication;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

public class BlikActivity extends AppCompatActivity {

    private TextView blikCodeTextView;
    private TextView timerTextView;
    private Button regenerateButton;
    private ProgressBar circleProgress;
    private CountDownTimer countDownTimer;
    private static final long BLIK_VALIDITY_MILLIS = 2 * 60 * 1000;

    private String idKarty = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blik);
        idKarty = getIntent().getStringExtra("cardId");

        // Initialize views
        blikCodeTextView = findViewById(R.id.blikCodeTextView);
        timerTextView = findViewById(R.id.timerTextView);
        regenerateButton = findViewById(R.id.regenerateButton);
        circleProgress = findViewById(R.id.circleProgress);

        // Set up initial state
        regenerateButton.setVisibility(View.GONE);
        circleProgress.setMax(100);

        // Generate BLIK code
        generateBlikCode();

        // Set up regenerate button click listener
        regenerateButton.setOnClickListener(v -> generateBlikCode());

        // Set up window insets listener for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.blikToolbar);

            // Apply top insets to toolbar
            toolbar.setPadding(0, insets.top, 0, 0);

            TypedValue tv = new TypedValue();
            int actionBarHeight;
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            } else {
                actionBarHeight = (int) (56 * getResources().getDisplayMetrics().density);
            }

            // Adjust toolbar height to include status bar
            ViewGroup.LayoutParams params = toolbar.getLayoutParams();
            params.height = actionBarHeight + insets.top;
            toolbar.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void generateBlikCode() {
        showLoading(true);

        BankApiClient apiClient = new BankApiClient("aaaabbbccc");

        android.util.Log.d("MainActivity", "SALDO_RESPONSE3: " + idKarty);

        apiClient.generateBlikCode(new BankApiClient.BankApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        showLoading(false);
                        JSONObject responseObj = new JSONObject(response);

                        String code = responseObj.getString("blikCode");
                        long validity = responseObj.getLong("validityInMillis");

                        blikCodeTextView.setText(code);

                        // Ukryj przycisk regeneracji
                        regenerateButton.setVisibility(View.GONE);

                        // Anuluj istniejący timer
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }

                        // Uruchom animację i timer
                        startCircleAnimation(validity);
                        startCountdownTimer(validity);

                    } catch (Exception e) {
                        handleError("Błąd podczas przetwarzania odpowiedzi: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    handleError("Błąd serwera: " + code + " - " + message);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    handleError("Błąd połączenia: " + e.getMessage());
                });
            }
        }, idKarty);
    }

    // Pomocnicza metoda do obsługi błędów
    private void handleError(String errorMessage) {
        // Wyświetl dialog z błędem lub Toast
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        android.util.Log.e("BlikActivity", "BLIK_ERROR: Error occurred: " + errorMessage);

        // Pokaż przycisk regenerowania kodu
        regenerateButton.setVisibility(View.VISIBLE);
    }

    // Pomocnicza metoda do pokazywania/ukrywania wskaźnika ładowania
    private void showLoading(boolean isLoading) {
        // Kod obsługujący wskaźnik ładowania
    }

    private void startCircleAnimation(long validity) {
        // Reset progress
        circleProgress.setProgress(100);

        // Użyj ValueAnimator zamiast ObjectAnimator dla lepszej kontroli
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setDuration(BLIK_VALIDITY_MILLIS);
        animator.setInterpolator(new LinearInterpolator());

        // Aktualizuj wartość progresji w trakcie animacji
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            circleProgress.setProgress((int) (value * 100));
        });

        animator.start();
    }

    private void startCountdownTimer(long validity) {
        // Zwiększ częstotliwość aktualizacji dla płynniejszego efektu
        countDownTimer = new CountDownTimer(BLIK_VALIDITY_MILLIS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                long millis = (millisUntilFinished % 1000) / 100;

                // Wyświetlaj tylko minuty i sekundy, ale obliczaj płynnie
                timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                timerTextView.setText("00:00");
                regenerateButton.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}