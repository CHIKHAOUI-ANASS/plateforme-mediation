package com.mediation.platform.enums;

public enum StatutUtilisateur {
    ACTIF("Actif"),
    INACTIF("Inactif"),
    EN_ATTENTE("En attente"),
    REFUSE("Refusé"),
    SUSPENDU("Suspendu");

    private final String libelle;

    StatutUtilisateur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}