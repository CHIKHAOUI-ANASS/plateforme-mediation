package com.mediation.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mediation.platform.enums.RoleUtilisateur;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "associations")
@PrimaryKeyJoinColumn(name = "id_utilisateur")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Association extends Utilisateur {

    @NotBlank(message = "Le nom de l'association est obligatoire")
    @Size(max = 200, message = "Le nom de l'association ne peut dépasser 200 caractères")
    @Column(nullable = false, length = 200)
    private String nomAssociation;

    @Size(max = 500, message = "L'adresse ne peut dépasser 500 caractères")
    @Column(length = 500)
    private String adresse;

    @Size(max = 255, message = "L'URL du site web ne peut dépasser 255 caractères")
    @Column(length = 255)
    private String siteWeb;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 100, message = "Le domaine d'activité ne peut dépasser 100 caractères")
    @Column(length = 100)
    private String domaineActivite;

    @Size(max = 500, message = "Les documents légaux ne peuvent dépasser 500 caractères")
    @Column(length = 500)
    private String documentsLegaux;

    @Column(nullable = false)
    private Boolean statutValidation = false;

    private LocalDateTime dateValidation;

    // Relations - CORRECTION: Ajout de @JsonIgnore pour éviter les boucles infinies
    @OneToMany(mappedBy = "association", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Projet> projets = new ArrayList<>();

    // Constructeurs
    public Association() {
        super();
        this.setRole(RoleUtilisateur.ASSOCIATION);
    }

    public Association(String nom, String prenom, String email, String motDePasse, String nomAssociation) {
        super(nom, prenom, email, motDePasse, RoleUtilisateur.ASSOCIATION);
        this.nomAssociation = nomAssociation;
    }

    public Association(String nom, String prenom, String email, String motDePasse,
                       String nomAssociation, String adresse, String description, String domaineActivite) {
        super(nom, prenom, email, motDePasse, RoleUtilisateur.ASSOCIATION);
        this.nomAssociation = nomAssociation;
        this.adresse = adresse;
        this.description = description;
        this.domaineActivite = domaineActivite;
    }

    // Getters et Setters
    public String getNomAssociation() {
        return nomAssociation;
    }

    public void setNomAssociation(String nomAssociation) {
        this.nomAssociation = nomAssociation;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomaineActivite() {
        return domaineActivite;
    }

    public void setDomaineActivite(String domaineActivite) {
        this.domaineActivite = domaineActivite;
    }

    public String getDocumentsLegaux() {
        return documentsLegaux;
    }

    public void setDocumentsLegaux(String documentsLegaux) {
        this.documentsLegaux = documentsLegaux;
    }

    public Boolean getStatutValidation() {
        return statutValidation;
    }

    public void setStatutValidation(Boolean statutValidation) {
        this.statutValidation = statutValidation;
        if (statutValidation && this.dateValidation == null) {
            this.dateValidation = LocalDateTime.now();
        }
    }

    public LocalDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(LocalDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }

    public List<Projet> getProjets() {
        return projets;
    }

    public void setProjets(List<Projet> projets) {
        this.projets = projets;
    }

    // AJOUT: Méthodes pour obtenir des informations dérivées sans exposer toute la liste
    public int getNombreProjets() {
        return projets != null ? projets.size() : 0;
    }

    public int getNombreProjetsActifs() {
        if (projets == null) return 0;
        return (int) projets.stream()
                .filter(p -> p.getStatut().name().equals("EN_COURS"))
                .count();
    }

    public int getNombreProjetsTermines() {
        if (projets == null) return 0;
        return (int) projets.stream()
                .filter(p -> p.getStatut().name().equals("TERMINE"))
                .count();
    }

    // Méthodes métier
    public Projet ajouterProjet(String titre, String description, Double montantDemande) {
        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du projet est obligatoire");
        }
        if (montantDemande <= 0) {
            throw new IllegalArgumentException("Le montant demandé doit être positif");
        }

        Projet projet = new Projet(titre, description, montantDemande, this);
        this.projets.add(projet);
        return projet;
    }

    public void modifierProjet(Projet projet, String nouveauTitre, String nouvelleDescription) {
        if (this.projets.contains(projet)) {
            if (nouveauTitre != null && !nouveauTitre.trim().isEmpty()) {
                projet.setTitre(nouveauTitre);
            }
            if (nouvelleDescription != null) {
                projet.setDescription(nouvelleDescription);
            }
        }
    }

    public List<Don> consulterDons() {
        List<Don> tousLesDons = new ArrayList<>();
        for (Projet projet : projets) {
            tousLesDons.addAll(projet.getDons());
        }
        return tousLesDons;
    }

    public String genererRapport() {
        Double montantTotal = consulterDons().stream()
                .mapToDouble(Don::getMontant)
                .sum();
        int nombreDons = consulterDons().size();
        int nombreProjets = projets.size();

        return String.format("Rapport Association %s:\n" +
                        "- Nombre de projets: %d\n" +
                        "- Nombre de dons reçus: %d\n" +
                        "- Montant total collecté: %.2f MAD",
                nomAssociation, nombreProjets, nombreDons, montantTotal);
    }

    public boolean estValidee() {
        return statutValidation != null && statutValidation;
    }

    public Double getMontantTotalCollecte() {
        return projets.stream()
                .mapToDouble(Projet::getMontantCollecte)
                .sum();
    }

    // OVERRIDE: Utiliser le nom de l'association pour l'affichage
    @Override
    public String getNomAffichage() {
        return this.nomAssociation != null ? this.nomAssociation : this.getNomComplet();
    }

    // AJOUT: Méthodes utilitaires pour l'API
    public boolean peutCreerProjet() {
        return this.estValidee() && this.estActif();
    }

    public boolean peutRecevoirDons() {
        return this.estValidee() && this.estActif();
    }

    // toString
    @Override
    public String toString() {
        return "Association{" +
                "idUtilisateur=" + getIdUtilisateur() +
                ", nomAssociation='" + nomAssociation + '\'' +
                ", email='" + getEmail() + '\'' +
                ", domaineActivite='" + domaineActivite + '\'' +
                ", statutValidation=" + statutValidation +
                ", nombreProjets=" + getNombreProjets() +
                '}';
    }
}