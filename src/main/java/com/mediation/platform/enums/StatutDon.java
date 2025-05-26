package com.mediation.platform.enums;

public enum StatutDon {
    EN_ATTENTE("En attente"),
    VALIDE("Validé"),
    REFUSE("Refusé"),
    ANNULE("Annulé"),
    REMBOURSE("Remboursé");

    private final String libelle;

    StatutDon(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
