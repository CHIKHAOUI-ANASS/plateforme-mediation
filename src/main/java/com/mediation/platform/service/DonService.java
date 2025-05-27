package com.mediation.platform.service;

import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.repository.DonRepository;
import com.mediation.platform.enums.StatutDon;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DonService {

    @Autowired
    private DonRepository donRepository;

    @Autowired
    private ProjetService projetService;

    public List<Don> findAll() {
        return donRepository.findAll();
    }

    public Don findById(Long id) {
        return donRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Don non trouvé avec l'ID: " + id));
    }

    public Don save(Don don) {
        validateDon(don);
        don.setStatut(StatutDon.EN_ATTENTE);
        return donRepository.save(don);
    }

    public Don update(Long id, Don don) {
        Don existingDon = findById(id);
        existingDon.setMontant(don.getMontant());
        existingDon.setMessage(don.getMessage());
        existingDon.setAnonyme(don.getAnonyme());
        return donRepository.save(existingDon);
    }

    public void deleteById(Long id) {
        Don don = findById(id);
        don.setStatut(StatutDon.ANNULE);
        donRepository.save(don);
    }

    public List<Don> findByDonateur(Donateur donateur) {
        return donRepository.findByDonateurOrderByDateDesc(donateur);
    }

    public List<Don> findByProjet(Projet projet) {
        return donRepository.findByProjetOrderByDateDesc(projet);
    }

    public List<Don> findByStatut(StatutDon statut) {
        return donRepository.findByStatut(statut);
    }

    public List<Don> findValidatedDons() {
        return donRepository.findByStatutOrderByDateDesc(StatutDon.VALIDE);
    }

    public List<Don> findAnonymousDons() {
        return donRepository.findByAnonymeTrue();
    }

    public List<Don> findDonsWithMessage() {
        return donRepository.findDonsWithMessage();
    }

    public List<Don> findByPeriod(LocalDate dateDebut, LocalDate dateFin) {
        return donRepository.findByDateBetweenOrderByDateDesc(dateDebut, dateFin);
    }

    public List<Don> findRecentDonations(LocalDate dateDebut) {
        return donRepository.findRecentDonations(dateDebut);
    }

    public List<Don> findLargeDonations(Double montantMin) {
        return donRepository.findLargeDonations(montantMin);
    }

    public Double getTotalConfirmedDonations() {
        return donRepository.getTotalConfirmedDonations();
    }

    public Double getTotalForProject(Projet projet) {
        return donRepository.getTotalForProject(projet);
    }

    public long getUniqueDonorsCount() {
        return donRepository.getUniqueDonorsCount();
    }

    public Don validerDon(Long donId) {
        Don don = findById(donId);
        don.setStatut(StatutDon.VALIDE);

        // Mettre à jour le montant collecté du projet
        Projet projet = don.getProjet();
        Double nouveauMontant = getTotalForProject(projet);
        projetService.mettreAJourMontantCollecte(projet.getIdProjet(), nouveauMontant);

        return donRepository.save(don);
    }

    public Don rejeterDon(Long donId) {
        Don don = findById(donId);
        don.setStatut(StatutDon.REFUSE);
        return donRepository.save(don);
    }

    private void validateDon(Don don) {
        if (don.getMontant() == null || don.getMontant() <= 0) {
            throw new IllegalArgumentException("Le montant du don doit être supérieur à 0");
        }

        if (don.getDonateur() == null) {
            throw new IllegalArgumentException("Le donateur est obligatoire");
        }

        if (don.getProjet() == null) {
            throw new IllegalArgumentException("Le projet est obligatoire");
        }

        // Vérifier que le projet est actif (utiliser EN_COURS)
        Projet projet = projetService.findById(don.getProjet().getIdProjet());
        if (projet.getStatut() != StatutProjet.EN_COURS) {
            throw new IllegalArgumentException("Le projet n'est plus actif pour recevoir des dons");
        }
    }
}
