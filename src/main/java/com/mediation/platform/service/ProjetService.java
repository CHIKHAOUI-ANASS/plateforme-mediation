package com.mediation.platform.service;

import com.mediation.platform.entity.Projet;
import com.mediation.platform.entity.Association;
import com.mediation.platform.repository.ProjetRepository;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProjetService {

    @Autowired
    private ProjetRepository projetRepository;

    public List<Projet> findAll() {
        return projetRepository.findAll();
    }

    public Projet findById(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet non trouvé avec l'ID: " + id));
    }

    public Projet save(Projet projet) {
        validateProjet(projet);
        // Utiliser EN_COURS selon votre énumération
        projet.setStatut(StatutProjet.EN_COURS);
        projet.setMontantCollecte(0.0);
        return projetRepository.save(projet);
    }

    public Projet update(Long id, Projet projet) {
        Projet existingProjet = findById(id);

        existingProjet.setTitre(projet.getTitre());
        existingProjet.setDescription(projet.getDescription());
        existingProjet.setObjectif(projet.getObjectif());
        existingProjet.setMontantDemande(projet.getMontantDemande());
        existingProjet.setDateFin(projet.getDateFin());
        existingProjet.setPriorite(projet.getPriorite());

        return projetRepository.save(existingProjet);
    }

    public void deleteById(Long id) {
        Projet projet = findById(id);
        projet.setStatut(StatutProjet.ANNULE);
        projetRepository.save(projet);
    }

    public List<Projet> findByAssociation(Association association) {
        return projetRepository.findByAssociation(association);
    }

    public List<Projet> findByStatut(StatutProjet statut) {
        return projetRepository.findByStatut(statut);
    }

    public List<Projet> findByTitre(String titre) {
        return projetRepository.findByTitreContainingIgnoreCase(titre);
    }

    public List<Projet> findByPriorite(String priorite) {
        return projetRepository.findByPrioriteOrderByDateCreationDesc(priorite);
    }

    public List<Projet> findActiveProjects() {
        return projetRepository.findByStatutOrderByDateCreationDesc(StatutProjet.EN_COURS);
    }

    public List<Projet> findNearGoal(double pourcentage) {
        return projetRepository.findNearGoal(pourcentage);
    }

    public List<Projet> findOverdueProjects() {
        return projetRepository.findOverdueProjects(LocalDate.now());
    }

    public List<Projet> findRecentProjects(LocalDateTime dateDebut) {
        return projetRepository.findRecentProjects(dateDebut);
    }

    public List<Projet> findTopProjects() {
        return projetRepository.findTopProjectsByAmount();
    }

    public List<Projet> searchByKeyword(String keyword) {
        return projetRepository.searchByKeyword(keyword);
    }

    public Projet mettreAJourMontantCollecte(Long projetId, Double nouveauMontant) {
        Projet projet = findById(projetId);
        projet.setMontantCollecte(nouveauMontant);

        // Vérifier si l'objectif est atteint
        if (nouveauMontant >= projet.getMontantDemande()) {
            projet.setStatut(StatutProjet.TERMINE);
        }

        return projetRepository.save(projet);
    }

    public void marquerCommeTermine(Long projetId) {
        Projet projet = findById(projetId);
        projet.setStatut(StatutProjet.TERMINE);
        projetRepository.save(projet);
    }

    private void validateProjet(Projet projet) {
        if (projet.getTitre() == null || projet.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du projet est obligatoire");
        }

        if (projet.getMontantDemande() == null || projet.getMontantDemande() <= 0) {
            throw new IllegalArgumentException("Le montant demandé doit être supérieur à 0");
        }

        if (projet.getDateFin() != null && projet.getDateFin().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date de fin doit être dans le futur");
        }

        if (projet.getAssociation() == null) {
            throw new IllegalArgumentException("L'association est obligatoire pour un projet");
        }
    }
}