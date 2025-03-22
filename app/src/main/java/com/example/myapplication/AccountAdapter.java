package com.example.myapplication;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private List<BigDecimal> accountBalances;

    public AccountAdapter() {
        this.accountBalances = new ArrayList<>();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setAccountBalances(List<BigDecimal> balances) {
        this.accountBalances = balances;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.account_item, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        BigDecimal balance = accountBalances.get(position);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String formattedBalance = df.format(balance) + " PLN";

        holder.accountNumberLabel.setText("Konto " + (position + 1));
        holder.accountBalanceValue.setText(formattedBalance);
    }

    @Override
    public int getItemCount() {
        return accountBalances.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView accountNumberLabel;
        TextView accountBalanceValue;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountNumberLabel = itemView.findViewById(R.id.accountNumberLabel);
            accountBalanceValue = itemView.findViewById(R.id.accountBalanceValue);
        }
    }
}
