package com.mediation.platform.entity;



import com.mediation.platform.enums.RoleUtilisateur;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donateurs")
@PrimaryKeyJoinColumn(name = "id_utilisateur")
public class Donateur extends Utilisateur {

    @Size(max = 255, message = "L'adresse ne peut dépasser 255 caractères")
    @Column(length = 255)
    private String adresse;

    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;

    @Size(max = 100, message = "La profession ne peut dépasser 100 caractères")
    @Column(length = 100)
    private String profession;

    // Relations
    @OneToMany(mappedBy = "donateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
        return dons.stream()
                .mapToDouble(Don::getMontant)
                .sum();
    }

    public int getNombreDons() {
        return dons.size();
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
                ", nombreDons=" + dons.size() +
                '}';
    }
}
