package com.mediation.platform.enums;

public enum StatutProjet {
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    ANNULE("Annulé"),
    SUSPENDU("Suspendu"),
    BROUILLON("Brouillon");

    private final String libelle;

    StatutProjet(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
