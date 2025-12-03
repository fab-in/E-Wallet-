package com.example.transaction_service.Service;

import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Client.UserServiceClient;
import com.example.transaction_service.Client.WalletServiceClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class StatementCsvBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] buildStatementCsv(UserServiceClient.UserDTO user, 
                                     List<Transaction> transactions, 
                                     List<WalletServiceClient.WalletDTO> userWallets) {
        StringBuilder csv = new StringBuilder();

        csv.append("Account Holder,").append(escapeCsvField(user != null ? user.getName() : "")).append("\n");
        csv.append("Account Details,").append(escapeCsvField(buildAccountDetails(userWallets))).append("\n\n");

        // CSV Header
        csv.append("Transaction ID,Date,Amount,Status,Remarks,Sender Wallet ID,Receiver Wallet ID\n");

        // Sort transactions by date descending
        List<Transaction> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort(Comparator.comparing(Transaction::getTransactionDate));
        for (int i = sortedTransactions.size() - 1; i >= 0; i--) {
            Transaction transaction = sortedTransactions.get(i);
            String statusValue = normalizeStatus(transaction.getStatus());
            boolean isSuccessful = "SUCCESS".equals(statusValue);

            csv.append(valueOrEmpty(transaction != null ? transaction.getId() : null)).append(",")
                    .append(formatDate(transaction != null ? transaction.getTransactionDate() : null)).append(",")
                    .append(isSuccessful ? formatAmount(transaction.getAmount()) : "")
                    .append(",")
                    .append(statusValue).append(",")
                    .append(escapeCsvField(transaction != null ? transaction.getRemarks() : "")).append(",")
                    .append(valueOrEmpty(transaction != null ? transaction.getSenderWalletId() : null)).append(",")
                    .append(valueOrEmpty(transaction != null ? transaction.getReceiverWalletId() : null))
                    .append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_FORMATTER.format(dateTime);
    }

    private String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private String buildAccountDetails(List<WalletServiceClient.WalletDTO> wallets) {
        if (wallets == null || wallets.isEmpty()) {
            return "";
        }
        return wallets.stream()
                .map(wallet -> {
                    if (wallet == null) {
                        return "";
                    }
                    StringBuilder builder = new StringBuilder();
                    if (wallet.getWalletName() != null && !wallet.getWalletName().isBlank()) {
                        builder.append(wallet.getWalletName()).append(" ");
                    }
                    if (wallet.getAccountNumber() != null && !wallet.getAccountNumber().isBlank()) {
                        builder.append("(").append(wallet.getAccountNumber()).append(")");
                    }
                    return builder.toString().trim();
                })
                .filter(entry -> !entry.isEmpty())
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private String valueOrEmpty(UUID value) {
        return value != null ? value.toString() : "";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String upper = status.trim().toUpperCase(Locale.US);
        if (upper.contains("SUCCESS")) {
            return "SUCCESS";
        }
        if (upper.contains("FAIL")) {
            return "FAILED";
        }
        return upper;
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
