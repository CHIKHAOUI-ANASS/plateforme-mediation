package com.mediation.platform.service;

import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private TransactionService transactionService;

    @Value("${paypal.client.id:default-client-id}")
    private String paypalClientId;

    @Value("${paypal.client.secret:default-client-secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode:sandbox}")
    private String paypalMode;

    public String creerPaiementPayPal(Don don) {
        try {
            // Créer une transaction en base
            Transaction transaction = transactionService.creerTransaction(don);

            // Pour l'instant, on retourne juste la référence
            // L'intégration PayPal complète sera ajoutée plus tard
            return transaction.getReferenceExterne();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du paiement PayPal", e);
        }
    }

    public boolean validerPaiementPayPal(String referenceTransaction, String paypalPaymentId) {
        try {
            // Simulation de validation - à remplacer par l'API PayPal réelle
            transactionService.marquerCommeReussie(referenceTransaction, paypalPaymentId);
            return true;

        } catch (Exception e) {
            transactionService.marquerCommeEchouee(referenceTransaction);
            return false;
        }
    }

    public void annulerPaiement(String referenceTransaction) {
        transactionService.marquerCommeEchouee(referenceTransaction);
    }

    public Map<String, Object> getPayPalConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("clientId", paypalClientId);
        config.put("mode", paypalMode);
        return config;
    }

    // Calculer les frais de transaction
    public Double calculerFrais(Double montant) {
        // Frais PayPal standard: 2.9% + 0.30 DH
        return (montant * 0.029) + 0.30;
    }

    // Obtenir le montant net après frais
    public Double getMontantNet(Double montantBrut) {
        return montantBrut - calculerFrais(montantBrut);
    }

    // Vérifier si le paiement est supporté
    public boolean isPaiementSupporte(String modePaiement) {
        return "PayPal".equalsIgnoreCase(modePaiement) ||
                "CB".equalsIgnoreCase(modePaiement);
    }

    // Générer URL de paiement PayPal (simulation)
    public String genererUrlPaiement(String referenceTransaction) {
        if ("sandbox".equals(paypalMode)) {
            return "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=" + referenceTransaction;
        } else {
            return "https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=" + referenceTransaction;
        }
    }
}