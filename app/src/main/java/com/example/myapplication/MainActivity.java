package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONObject;
import org.json.JSONArray;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView balanceTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar loadingProgressBar;
    private ViewPager2 accountViewPager;
    private AccountAdapter accountAdapter;
    private static String ACCOUNT_ID = "";
    private LinearLayout dotsIndicator;
    private ImageView[] dots;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ACCOUNT_ID = String.valueOf(getIntent().getIntExtra("klientId", -1));

        balanceTextView = findViewById(R.id.balanceTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        accountViewPager = findViewById(R.id.accountViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);

        accountAdapter = new AccountAdapter();
        accountViewPager.setAdapter(accountAdapter);
        
        // Konfiguracja PageChangeCallback dla wskaźników kropkowych
        accountViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
        
        Button blikButton = findViewById(R.id.blikButton);
        blikButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BlikActivity.class);
            startActivity(intent);
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::fetchAccountBalance);
        swipeRefreshLayout.setColorSchemeResources(R.color.design_default_color_primary);

        fetchAccountBalance();

        configureEdgeToEdge();
    }

    private void fetchAccountBalance() {
        showLoading(true);

        BankApiClient apiClient = new BankApiClient("aaaabbbccc");
        apiClient.getAccountBalance(ACCOUNT_ID, new BankApiClient.BankApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    android.util.Log.d("MainActivity", "SALDO_RESPONSE: " + response);
                    try {
                        if (response.trim().startsWith("[")) {
                            JSONArray balancesArray = new JSONArray(response);
                            List<BigDecimal> balances = new ArrayList<>();
                            
                            for (int i = 0; i < balancesArray.length(); i++) {
                                BigDecimal balance = new BigDecimal(balancesArray.getString(i));
                                balances.add(balance);
                            }
                            
                            // Aktualizacja adaptera nowymi saldami
                            accountAdapter.setAccountBalances(balances);
                            
                            // Pokazywanie ViewPager tylko jeśli są jakieś konta
                            if (!balances.isEmpty()) {
                                accountViewPager.setVisibility(View.VISIBLE);
                                setupDots(balances.size());
                            } else {
                                balanceTextView.setText("Brak dostępnych kont");
                                balanceTextView.setVisibility(View.VISIBLE);
                                dotsIndicator.setVisibility(View.GONE);
                            }
                            
                        } else {
                            // ten kod nie powinien nigdy być wykonany ze względu na to, że backend zwraca tablicę, lecz jest to zabezpieczenie
                            BigDecimal balance = new BigDecimal(response);
                            List<BigDecimal> balances = new ArrayList<>();
                            balances.add(balance);
                            accountAdapter.setAccountBalances(balances);
                            accountViewPager.setVisibility(View.VISIBLE);
                            setupDots(1);
                        }
                    } catch (Exception e) {
                        handleError("Błąd podczas przetwarzania danych: " + e.getMessage());
                        android.util.Log.e("MainActivity", "SALDO_ERROR: Error occurred: " + e.getMessage());
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
        });
    }

    private void setupDots(int count) {
        dotsIndicator.removeAllViews();
        dots = new ImageView[count];
        
        // Ukryj wskaźniki kropkowe, jeśli jest tylko jedno konto
        if (count <= 1) {
            dotsIndicator.setVisibility(View.GONE);
            return;
        }
        
        dotsIndicator.setVisibility(View.VISIBLE);
        
        // Parametry dla kropek
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        
        // Tworzenie kropek
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(getDrawable(R.drawable.dot_indicator_inactive));
            dotsIndicator.addView(dots[i], params);
        }
        
        // Ustawienie pierwszej kropki jako aktywnej
        if (count > 0) {
            dots[0].setImageDrawable(getDrawable(R.drawable.dot_indicator_active));
        }
    }
    
    private void updateDots(int currentPosition) {
        if (dots == null) return;
        
        for (int i = 0; i < dots.length; i++) {
            if (dots[i] != null) {
                dots[i].setImageDrawable(getDrawable(
                    i == currentPosition ? 
                    R.drawable.dot_indicator_active : 
                    R.drawable.dot_indicator_inactive
                ));
            }
        }
    }

    private void handleError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean isLoading) {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        accountViewPager.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        dotsIndicator.setVisibility(isLoading ? View.GONE : (dots != null && dots.length > 1 ? View.VISIBLE : View.GONE));
    }

    private void configureEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);

            toolbar.setPadding(0, insets.top, 0, 0);

            TypedValue tv = new TypedValue();
            int actionBarHeight;
            if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            } else {
                actionBarHeight = (int) (56 * getResources().getDisplayMetrics().density);
            }

            ViewGroup.LayoutParams params = toolbar.getLayoutParams();
            params.height = actionBarHeight + insets.top;
            toolbar.setLayoutParams(params);

            // Dodaj padding do dolnego panelu aby zapobiec nakładaniu się na pasek nawigacji
            View bottomActionsLayout = findViewById(R.id.bottomActionsLayout);
            bottomActionsLayout.setPadding(
                bottomActionsLayout.getPaddingLeft(),
                bottomActionsLayout.getPaddingTop(),
                bottomActionsLayout.getPaddingRight(),
                insets.bottom + 12
            );

            v.setPadding(0, 0, 0, 0);

            return WindowInsetsCompat.CONSUMED;
        });
    }
}
