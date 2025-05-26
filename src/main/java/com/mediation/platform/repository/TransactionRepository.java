package com.mediation.platform.repository;

import com.mediation.platform.entity.Transaction;
import com.mediation.platform.enums.StatutTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Transaction par statut
    List<Transaction> findByStatut(StatutTransaction statut);

    // Transaction par référence externe (PayPal)
    Optional<Transaction> findByReferenceExterne(String referenceExterne);

    // Transactions par mode de paiement
    List<Transaction> findByModePayment(String modePayment);

    // Transactions échouées
    List<Transaction> findByStatutOrderByDateTransactionDesc(StatutTransaction statut);

    // Transactions par période
    List<Transaction> findByDateTransactionBetweenOrderByDateTransactionDesc(
            LocalDateTime dateDebut, LocalDateTime dateFin);

    // Montant total des transactions réussies
    @Query("SELECT SUM(t.montant) FROM Transaction t WHERE t.statut = 'REUSSIE'")
    Double getTotalSuccessfulTransactions();

    // Frais totaux collectés
    @Query("SELECT SUM(t.frais) FROM Transaction t WHERE t.statut = 'REUSSIE'")
    Double getTotalFees();

    // Taux de réussite des transactions
    @Query("SELECT " +
            "(SELECT COUNT(t1) FROM Transaction t1 WHERE t1.statut = 'REUSSIE') * 100.0 / " +
            "(SELECT COUNT(t2) FROM Transaction t2) as taux")
    Double getSuccessRate();

    // Transactions récentes
    @Query("SELECT t FROM Transaction t WHERE t.dateTransaction >= :dateDebut " +
            "ORDER BY t.dateTransaction DESC")
    List<Transaction> findRecentTransactions(@Param("dateDebut") LocalDateTime dateDebut);
}

