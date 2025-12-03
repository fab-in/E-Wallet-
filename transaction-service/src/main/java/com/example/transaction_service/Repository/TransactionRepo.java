package com.example.transaction_service.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.transaction_service.Model.Transaction;
import java.util.List;
import java.util.UUID;

public interface TransactionRepo extends JpaRepository<Transaction, UUID> {

       @Query("SELECT t FROM Transaction t " +
                     "WHERE t.senderWalletId IN :walletIds OR t.receiverWalletId IN :walletIds " +
                     "ORDER BY t.transactionDate DESC")
       Page<Transaction> findByWalletIds(@Param("walletIds") List<UUID> walletIds, Pageable pageable);

       Page<Transaction> findAll(Pageable pageable);

       @Query("SELECT t FROM Transaction t " +
                     "WHERE (t.senderWalletId IN :walletIds OR t.receiverWalletId IN :walletIds) " +
                     "AND LOWER(COALESCE(t.remarks, '')) LIKE LOWER(CONCAT('%', :remark, '%')) " +
                     "ORDER BY t.transactionDate DESC")
       Page<Transaction> findByWalletIdsAndRemarkContaining(@Param("walletIds") List<UUID> walletIds,
                     @Param("remark") String remark,
                     Pageable pageable);

       @Query("SELECT t FROM Transaction t WHERE LOWER(COALESCE(t.remarks, '')) LIKE LOWER(CONCAT('%', :remark, '%')) ORDER BY t.transactionDate DESC")
       Page<Transaction> findAllByRemarkContaining(@Param("remark") String remark, Pageable pageable);

       @Query("SELECT t FROM Transaction t " +
                     "WHERE (t.senderWalletId IN :walletIds OR t.receiverWalletId IN :walletIds) " +
                     "AND LOWER(t.status) = 'failed' " +
                     "ORDER BY t.transactionDate DESC")
       Page<Transaction> findFailedTransactionsByWalletIds(@Param("walletIds") List<UUID> walletIds, Pageable pageable);

       @Query("SELECT t FROM Transaction t WHERE LOWER(t.status) = 'failed' ORDER BY t.transactionDate DESC")
       Page<Transaction> findAllFailedTransactions(Pageable pageable);

       @Query("SELECT t FROM Transaction t " +
                     "WHERE t.senderWalletId IN :walletIds OR t.receiverWalletId IN :walletIds " +
                     "ORDER BY t.transactionDate DESC")
       List<Transaction> findAllByWalletIds(@Param("walletIds") List<UUID> walletIds);
}
