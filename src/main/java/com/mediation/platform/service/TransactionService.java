package com.mediation.platform.service;

import com.mediation.platform.entity.Transaction;
import com.mediation.platform.entity.Don;
import com.mediation.platform.repository.TransactionRepository;
import com.mediation.platform.enums.StatutTransaction;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    public Optional<Transaction> findByReference(String reference) {
        return transactionRepository.findByReferenceExterne(reference);
    }

    public Transaction creerTransaction(Don don) {
        Transaction transaction = new Transaction();
        transaction.setDon(don);
        transaction.setMontant(don.getMontant());
        transaction.setReferenceExterne(genererReferenceUnique());
        transaction.setStatut(StatutTransaction.EN_ATTENTE);
        transaction.setModePayment("PayPal");

        return transactionRepository.save(transaction);
    }

    public Transaction update(Long id, Transaction transaction) {
        Transaction existingTransaction = findById(id);
        existingTransaction.setStatut(transaction.getStatut());
        existingTransaction.setReferenceExterne(transaction.getReferenceExterne());
        existingTransaction.setDetails(transaction.getDetails());
        existingTransaction.setMessageErreur(transaction.getMessageErreur());
        return transactionRepository.save(existingTransaction);
    }

    public List<Transaction> findByStatut(StatutTransaction statut) {
        return transactionRepository.findByStatut(statut);
    }

    public List<Transaction> findByModePayment(String modePayment) {
        return transactionRepository.findByModePayment(modePayment);
    }

    public List<Transaction> findByPeriod(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return transactionRepository.findByDateTransactionBetweenOrderByDateTransactionDesc(dateDebut, dateFin);
    }

    public List<Transaction> findRecentTransactions(LocalDateTime dateDebut) {
        return transactionRepository.findRecentTransactions(dateDebut);
    }

    public Double getTotalSuccessfulTransactions() {
        return transactionRepository.getTotalSuccessfulTransactions();
    }

    public Double getTotalFees() {
        return transactionRepository.getTotalFees();
    }

    public Double getSuccessRate() {
        return transactionRepository.getSuccessRate();
    }

    public Transaction marquerCommeReussie(String referenceTransaction, String referencePayPal) {
        Optional<Transaction> optTransaction = findByReference(referenceTransaction);
        if (optTransaction.isPresent()) {
            Transaction transaction = optTransaction.get();
            transaction.setStatut(StatutTransaction.REUSSIE);
            transaction.setDetails("Paiement PayPal réussi: " + referencePayPal);

            // Valider le don associé
            donService.validerDon(transaction.getDon().getIdDon());

            return transactionRepository.save(transaction);
        }
        throw new ResourceNotFoundException("Transaction non trouvée avec la référence: " + referenceTransaction);
    }

    public Transaction marquerCommeEchouee(String referenceTransaction) {
        Optional<Transaction> optTransaction = findByReference(referenceTransaction);
        if (optTransaction.isPresent()) {
            Transaction transaction = optTransaction.get();
            transaction.setStatut(StatutTransaction.ECHEC);
            transaction.setMessageErreur("Paiement échoué");

            // Rejeter le don associé
            donService.rejeterDon(transaction.getDon().getIdDon());

            return transactionRepository.save(transaction);
        }
        throw new ResourceNotFoundException("Transaction non trouvée avec la référence: " + referenceTransaction);
    }

    private String genererReferenceUnique() {
        String reference;
        do {
            reference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (transactionRepository.findByReferenceExterne(reference).isPresent());

        return reference;
    }
}
