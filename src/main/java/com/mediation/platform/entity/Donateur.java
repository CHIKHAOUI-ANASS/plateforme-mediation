package com.mediation.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutDon;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donateurs")
@PrimaryKeyJoinColumn(name = "id_utilisateur")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Donateur extends Utilisateur {

    @Size(max = 255, message = "L'adresse ne peut dépasser 255 caractères")
    @Column(length = 255)
    private String adresse;

    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    @Size(max = 100, message = "La profession ne peut dépasser 100 caractères")
    @Column(length = 100)
    private String profession;

    // Relations - CORRECTION: Ajout de @JsonIgnore pour éviter les boucles infinies
    @OneToMany(mappedBy = "donateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Don> dons = new ArrayList<>();

    // Constructeurs
    public Donateur() {
        super();
        this.setRole(RoleUtilisateur.DONATEUR);
    }

    public Donateur(String nom, String prenom, String email, String motDePasse) {
        super(nom, prenom, email, motDePasse, RoleUtilisateur.DONATEUR);
    }

    public Donateur(String nom, String prenom, String email, String motDePasse,
                    String adresse, LocalDate dateNaissance, String profession) {
        super(nom, prenom, email, motDePasse, RoleUtilisateur.DONATEUR);
        this.adresse = adresse;
        this.dateNaissance = dateNaissance;
        this.profession = profession;
    }

    // Getters et Setters
    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public List<Don> getDons() {
        return dons;
    }

    public void setDons(List<Don> dons) {
        this.dons = dons;
    }

    // AJOUT: Méthodes pour obtenir des informations dérivées sans exposer toute la liste
    public int getNombreDons() {
        return dons != null ? dons.size() : 0;
    }

    public int getNombreDonsValides() {
        if (dons == null) return 0;
        return (int) dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .count();
    }

    public int getNombreDonsEnAttente() {
        if (dons == null) return 0;
        return (int) dons.stream()
                .filter(don -> don.getStatut() == StatutDon.EN_ATTENTE)
                .count();
    }

    public int getNombreProjetsSoutenus() {
        if (dons == null) return 0;
        return (int) dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .map(Don::getProjet)
                .distinct()
                .count();
    }

    public int getNombreAssociationsSoutenues() {
        if (dons == null) return 0;
        return (int) dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .map(don -> don.getProjet().getAssociation())
                .distinct()
                .count();
    }

    // Méthodes métier
    public List<Don> consulterHistorique() {
        return new ArrayList<>(this.dons);
    }

    public Don effectuerDon(Double montant, Projet projet, String message) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant du don doit être positif");
        }
        if (projet == null) {
            throw new IllegalArgumentException("Le projet ne peut pas être null");
        }

        Don don = new Don(montant, this, projet, message);
        this.dons.add(don);
        return don;
    }

    public Double getMontantTotalDons() {
        if (dons == null) return 0.0;
        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .mapToDouble(Don::getMontant)
                .sum();
    }

    public Double getMontantTotalTousDons() {
        if (dons == null) return 0.0;
        return dons.stream()
                .mapToDouble(Don::getMontant)
                .sum();
    }

    public Double getMontantMoyenParDon() {
        List<Don> donsValides = getDonsValides();
        if (donsValides.isEmpty()) return 0.0;
        return getMontantTotalDons() / donsValides.size();
    }

    public List<Don> getDonsValides() {
        if (dons == null) return new ArrayList<>();
        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .toList();
    }

    public List<Don> getDonsEnAttente() {
        if (dons == null) return new ArrayList<>();
        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.EN_ATTENTE)
                .toList();
    }

    public List<Don> getDonsRecents() {
        if (dons == null) return new ArrayList<>();
        return dons.stream()
                .filter(Don::estRecent)
                .toList();
    }

    // AJOUT: Méthodes utilitaires pour l'âge
    public Integer getAge() {
        if (dateNaissance == null) return null;
        return LocalDate.now().getYear() - dateNaissance.getYear();
    }

    public String getTrancheAge() {
        Integer age = getAge();
        if (age == null) return "Non renseigné";

        if (age < 25) return "18-24 ans";
        else if (age < 35) return "25-34 ans";
        else if (age < 45) return "35-44 ans";
        else if (age < 55) return "45-54 ans";
        else if (age < 65) return "55-64 ans";
        else return "65 ans et plus";
    }

    // AJOUT: Méthodes pour les statistiques
    public Don getDonLePlusImportant() {
        if (dons == null || dons.isEmpty()) return null;
        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .max((d1, d2) -> Double.compare(d1.getMontant(), d2.getMontant()))
                .orElse(null);
    }

    public Don getDernierDon() {
        if (dons == null || dons.isEmpty()) return null;
        return dons.stream()
                .max((d1, d2) -> d1.getDate().compareTo(d2.getDate()))
                .orElse(null);
    }

    public LocalDate getDatePremierDon() {
        if (dons == null || dons.isEmpty()) return null;
        return dons.stream()
                .map(Don::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    public LocalDate getDateDernierDon() {
        if (dons == null || dons.isEmpty()) return null;
        return dons.stream()
                .map(Don::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    // AJOUT: Méthodes pour l'engagement
    public boolean estDonateurActif() {
        LocalDate dateRecente = LocalDate.now().minusMonths(3);
        return dons != null && dons.stream()
                .anyMatch(don -> don.getDate().isAfter(dateRecente) &&
                        don.getStatut() == StatutDon.VALIDE);
    }

    public boolean estDonateurRegulier() {
        return getNombreDonsValides() >= 3;
    }

    public boolean estGrossDonateur() {
        return getMontantTotalDons() >= 1000.0; // Seuil configurable
    }

    public String getNiveauDonateur() {
        double montantTotal = getMontantTotalDons();
        int nombreDons = getNombreDonsValides();

        if (montantTotal >= 5000 || nombreDons >= 20) {
            return "Platine";
        } else if (montantTotal >= 2000 || nombreDons >= 10) {
            return "Or";
        } else if (montantTotal >= 500 || nombreDons >= 5) {
            return "Argent";
        } else if (nombreDons >= 1) {
            return "Bronze";
        } else {
            return "Nouveau";
        }
    }

    public String getCouleurNiveau() {
        String niveau = getNiveauDonateur();
        switch (niveau) {
            case "Platine": return "purple";
            case "Or": return "yellow";
            case "Argent": return "gray";
            case "Bronze": return "orange";
            default: return "blue";
        }
    }

    // AJOUT: Méthodes pour les préférences de dons
    public String getDomainePreference() {
        if (dons == null || dons.isEmpty()) return null;

        return dons.stream()
                .filter(don -> don.getStatut() == StatutDon.VALIDE)
                .map(don -> don.getProjet().getDomaineActivite())
                .filter(domaine -> domaine != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        domaine -> domaine,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
    }

    public boolean peutEffectuerDon() {
        return this.estActif();
    }

    public boolean peutVoirHistorique() {
        return this.estActif();
    }

    // AJOUT: Génération de rapport personnalisé
    public String genererRapportPersonnel() {
        return String.format(
                "Rapport donateur %s:\n" +
                        "- Niveau: %s\n" +
                        "- Nombre de dons: %d\n" +
                        "- Montant total: %.2f DH\n" +
                        "- Projets soutenus: %d\n" +
                        "- Associations aidées: %d\n" +
                        "- Domaine préféré: %s\n" +
                        "- Dernier don: %s",
                getNomComplet(),
                getNiveauDonateur(),
                getNombreDonsValides(),
                getMontantTotalDons(),
                getNombreProjetsSoutenus(),
                getNombreAssociationsSoutenues(),
                getDomainePreference() != null ? getDomainePreference() : "Aucun",
                getDateDernierDon() != null ? getDateDernierDon().toString() : "Aucun"
        );
    }

    // toString
    @Override
    public String toString() {
        return "Donateur{" +
                "idUtilisateur=" + getIdUtilisateur() +
                ", nom='" + getNom() + '\'' +
                ", prenom='" + getPrenom() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", adresse='" + adresse + '\'' +
                ", dateNaissance=" + dateNaissance +
                ", profession='" + profession + '\'' +
                ", nombreDons=" + getNombreDons() +
                ", montantTotal=" + getMontantTotalDons() +
                ", niveau=" + getNiveauDonateur() +
                '}';
    }
}