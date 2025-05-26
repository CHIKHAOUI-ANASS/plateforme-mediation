package com.mediation.platform.service;

import com.mediation.platform.entity.Transaction;
import com.mediation.platform.entity.Don;
import com.mediation.platform.repository.TransactionRepository;
import com.mediation.platform.enums.StatutTransaction;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DonService donService;

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Transaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée avec l'ID: " + id));
    }

    public Transaction findByReference(String reference) {
        return transactionRepository.findByReferenceTransaction(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée avec la référence: " + reference));
    }

    public Transaction creerTransaction(Don don) {
        Transaction transaction = new Transaction();
        transaction.setDon(don);
        transaction.setMontant(don.getMontant());
        transaction.setReferenceTransaction(genererReferenceUnique());
        transaction.setStatut(StatutTransaction.EN_ATTENTE);
        transaction.setDateTransaction(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public Transaction update(Long id, Transaction transaction) {
        Transaction existingTransaction = findById(id);
        existingTransaction.setStatut(transaction.getStatut());
        existingTransaction.setReferencePayPal(transaction.getReferencePayPal());
        return transactionRepository.save(existingTransaction);
    }

    public List<Transaction> findByDon(Long donId) {
        return transactionRepository.findByDonId(donId);
    }

    public List<Transaction> findByStatut(StatutTransaction statut) {
        return transactionRepository.findByStatut(statut);
    }

    public Transaction marquerCommeReussie(String referenceTransaction, String referencePayPal) {
        Transaction transaction = findByReference(referenceTransaction);
        transaction.setStatut(StatutTransaction.REUSSIE);
        transaction.setReferencePayPal(referencePayPal);

        // Valider le don associé
        donService.validerDon(transaction.getDon().getId());

        return transactionRepository.save(transaction);
    }

    public Transaction marquerCommeEchouee(String referenceTransaction) {
        Transaction transaction = findByReference(referenceTransaction);
        transaction.setStatut(StatutTransaction.ECHEC);

        // Rejeter le don associé
        donService.rejeterDon(transaction.getDon().getId());

        return transactionRepository.save(transaction);
    }

    // Méthodes simplifiées sans requêtes complexes pour commencer
    public BigDecimal getTotalTransactionsReussies() {
        List<Transaction> transactions = transactionRepository.findByStatut(StatutTransaction.REUSSIE);
        return transactions.stream()
                .map(Transaction::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long countTransactionsEchec() {
        return (long) transactionRepository.findByStatut(StatutTransaction.ECHEC).size();
    }

    private String genererReferenceUnique() {
        String reference;
        do {
            reference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (transactionRepository.existsByReferenceTransaction(reference));

        return reference;
    }
}