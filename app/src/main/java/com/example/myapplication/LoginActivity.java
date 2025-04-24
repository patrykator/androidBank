package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private ProgressBar loginProgressBar;

    private TextView restorePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.usernameLayout);
        emailEditText = findViewById(R.id.usernameEditText);
        passwordLayout = findViewById(R.id.passwordLayout);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        restorePasswordButton = findViewById(R.id.forgotPasswordTextView);

        loginButton.setOnClickListener(v -> attemptLogin());
        restorePasswordButton.setOnClickListener(v -> displayFunctionalityNotAvailable());

        configureEdgeToEdge();
    }

    private void displayFunctionalityNotAvailable() {
        Toast.makeText(this, "Functionality not available", Toast.LENGTH_SHORT).show();
    }

    private void attemptLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;


        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            focusView = passwordEditText;
            cancel = true;
        } else if (password.length() < 4) {
            passwordLayout.setError("Password is too short");
            focusView = passwordEditText;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            focusView = emailEditText;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showProgress(false);

                login(email, password);
            }, 1500);
        }
    }

    private void login(String email, String password) {
        BankApiClient apiClient = new BankApiClient("aaaabbbccc");

        apiClient.login(email, password, new BankApiClient.BankApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.optString("status", "success");

                        if ("error".equals(status)) {
                            String message = jsonResponse.optString("message", "Unknown error");
                            // Note: These checks depend on the exact Polish error messages from the server.
                            // If the server messages change, these checks might need adjustment.
                            if (message.contains("nie istnieje")) { // "does not exist"
                                emailLayout.setError(message); // Keep original server message for now
                            } else if (message.contains("hasło")) { // "password"
                                passwordLayout.setError(message); // Keep original server message for now
                            } else {
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Integer klientId = jsonResponse.getInt("klientId");
                            loginSuccessful(klientId);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(LoginActivity.this, "Error processing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                    showProgress(false);
                    if (code == 401) {
                        // Assuming 401 specifically means invalid password based on previous Polish text
                        passwordLayout.setError("Invalid password");
                    } else {
                        Toast.makeText(LoginActivity.this, "Server error: " + code + " - " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loginSuccessful(Integer klientId) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        android.util.Log.d("MainActivity", "SALDO_RESPONSE: " + klientId);
        intent.putExtra("klientId", klientId);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        loginProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        emailEditText.setEnabled(!show);
        passwordEditText.setEnabled(!show);
    }

    private void configureEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.loginToolbar);
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

            v.setPadding(0, 0, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
