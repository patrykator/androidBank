package com.example.myapplication;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BankApiClient {
    private static final String TAG = "BankApiClient";
    private static final String API_BASE_URL = "http://10.0.2.2:8080/api/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String apiKey;

    public BankApiClient(String apiKey) {
        this.apiKey = apiKey;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void login(String email, String password, final BankApiCallback callback) {
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("email", email);
            requestData.put("haslo", password);

            RequestBody body = RequestBody.create(requestData.toString(), JSON);

            Request request = new Request.Builder()
                    .url(API_BASE_URL + "auth/login")
                    .post(body)
                    .build();

            executeRequest(request, callback);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void getAccountBalance(String accountNumber, final BankApiCallback callback) {
        Request request = new Request.Builder()
                .url(API_BASE_URL + "konto/" + accountNumber + "/saldo")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

        executeRequest(request, callback);
    }

    public void generateBlikCode(final BankApiCallback callback) {
        try {
            JSONObject requestData = new JSONObject();

            RequestBody body = RequestBody.create(requestData.toString(), JSON);

            Request request = new Request.Builder()
                    .url(API_BASE_URL + "platnosc/blik")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            Log.d(TAG, "Sending request to: " + API_BASE_URL + "platnosc/blik");
            executeRequest(request, callback);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    public void makeTransfer(String recipientAccount, String title, double amount,
                             String currency, final BankApiCallback callback) {
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("recipientAccount", recipientAccount);
            requestData.put("title", title);
            requestData.put("amount", amount);
            requestData.put("currency", currency);

            RequestBody body = RequestBody.create(requestData.toString(), JSON);

            Request request = new Request.Builder()
                    .url(API_BASE_URL + "transfers")
                    .header("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            executeRequest(request, callback);
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private void executeRequest(Request request, final BankApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    callback.onSuccess(responseData);
                } else {
                    assert response.body() != null;
                    callback.onError(response.code(), response.body().string());
                }
            }
        });
    }

    public interface BankApiCallback {
        void onSuccess(String response);
        void onError(int code, String message);
        void onFailure(Exception e);
    }
}