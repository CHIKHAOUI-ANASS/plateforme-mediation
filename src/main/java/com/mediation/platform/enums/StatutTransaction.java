package com.mediation.platform.enums;

public enum StatutTransaction {
    EN_ATTENTE("En attente"),
    REUSSIE("Réussie"),
    ECHEC("Échec"),
    ANNULE("Annulé"),
    REMBOURSE("Remboursé"),
    EXPIRE("Expiré");

    private final String libelle;

    StatutTransaction(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
