package com.mediation.platform.service;

import com.mediation.platform.entity.Donateur;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.DonateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DonateurService {

    @Autowired
    private DonateurRepository donateurRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Trouver un donateur par ID
     */
    public Donateur findById(Long id) {
        return donateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donateur non trouvé avec l'ID: " + id));
    }

    /**
     * Trouver tous les donateurs
     */
    public List<Donateur> findAll() {
        return donateurRepository.findAll();
    }

    /**
     * Sauvegarder un donateur
     */
    public Donateur save(Donateur donateur) {
        return donateurRepository.save(donateur);
    }

    /**
     * Mettre à jour un donateur
     */
    public Donateur update(Long id, Donateur donateurData) {
        Donateur donateur = findById(id);

        // Mettre à jour les champs modifiables
        if (donateurData.getNom() != null) {
            donateur.setNom(donateurData.getNom());
        }
        if (donateurData.getPrenom() != null) {
            donateur.setPrenom(donateurData.getPrenom());
        }
        if (donateurData.getTelephone() != null) {
            donateur.setTelephone(donateurData.getTelephone());
        }
        if (donateurData.getAdresse() != null) {
            donateur.setAdresse(donateurData.getAdresse());
        }
        if (donateurData.getProfession() != null) {
            donateur.setProfession(donateurData.getProfession());
        }
        if (donateurData.getDateNaissance() != null) {
            donateur.setDateNaissance(donateurData.getDateNaissance());
        }

        return donateurRepository.save(donateur);
    }

    /**
     * Supprimer un donateur
     */
    public void deleteById(Long id) {
        Donateur donateur = findById(id);
        donateurRepository.delete(donateur);
    }

    /**
     * Rechercher donateurs par profession
     */
    public List<Donateur> findByProfession(String profession) {
        return donateurRepository.findByProfessionContainingIgnoreCase(profession);
    }

    /**
     * Rechercher donateurs par tranche d'âge
     */
    public List<Donateur> findByAgeBetween(int ageMin, int ageMax) {
        return donateurRepository.findByAgeBetween(ageMin, ageMax);
    }

    /**
     * Donateurs qui ont fait des dons confirmés
     */
    public List<Donateur> findDonatorsWithConfirmedDonations() {
        return donateurRepository.findDonatorsWithConfirmedDonations();
    }

    /**
     * Top donateurs par montant total
     */
    public List<Donateur> findTopDonators() {
        return donateurRepository.findTopDonators();
    }

    /**
     * Rechercher donateurs par ville
     */
    public List<Donateur> findByVille(String ville) {
        return donateurRepository.findByAdresseContainingIgnoreCase(ville);
    }

    /**
     * Statistiques générales des donateurs
     */
    public DonateurStats getGeneralStats() {
        List<Donateur> allDonateurs = findAll();

        DonateurStats stats = new DonateurStats();
        stats.setTotalDonateurs(allDonateurs.size());
        stats.setDonateursActifs((int) allDonateurs.stream().filter(Donateur::estDonateurActif).count());
        stats.setDonateursReguliers((int) allDonateurs.stream().filter(Donateur::estDonateurRegulier).count());
        stats.setGrosDonateurs((int) allDonateurs.stream().filter(Donateur::estGrossDonateur).count());

        double montantTotalTousDons = allDonateurs.stream()
                .mapToDouble(Donateur::getMontantTotalDons)
                .sum();
        stats.setMontantTotalDons(montantTotalTousDons);

        if (!allDonateurs.isEmpty()) {
            stats.setMontantMoyenParDonateur(montantTotalTousDons / allDonateurs.size());
        }

        return stats;
    }

    /**
     * Vérifier si un donateur existe
     */
    public boolean existsById(Long id) {
        return donateurRepository.existsById(id);
    }

    /**
     * Compter le nombre total de donateurs
     */
    public long count() {
        return donateurRepository.count();
    }

    /**
     * Activer/désactiver un donateur
     */
    public Donateur toggleStatus(Long id) {
        Donateur donateur = findById(id);

        if (donateur.estActif()) {
            donateur.setStatut(com.mediation.platform.enums.StatutUtilisateur.INACTIF);
        } else {
            donateur.setStatut(com.mediation.platform.enums.StatutUtilisateur.ACTIF);
        }

        return donateurRepository.save(donateur);
    }

    /**
     * Classe interne pour les statistiques
     */
    public static class DonateurStats {
        private int totalDonateurs;
        private int donateursActifs;
        private int donateursReguliers;
        private int grosDonateurs;
        private double montantTotalDons;
        private double montantMoyenParDonateur;

        // Getters et setters
        public int getTotalDonateurs() { return totalDonateurs; }
        public void setTotalDonateurs(int totalDonateurs) { this.totalDonateurs = totalDonateurs; }

        public int getDonateursActifs() { return donateursActifs; }
        public void setDonateursActifs(int donateursActifs) { this.donateursActifs = donateursActifs; }

        public int getDonateursReguliers() { return donateursReguliers; }
        public void setDonateursReguliers(int donateursReguliers) { this.donateursReguliers = donateursReguliers; }

        public int getGrosDonateurs() { return grosDonateurs; }
        public void setGrosDonateurs(int grosDonateurs) { this.grosDonateurs = grosDonateurs; }

        public double getMontantTotalDons() { return montantTotalDons; }
        public void setMontantTotalDons(double montantTotalDons) { this.montantTotalDons = montantTotalDons; }

        public double getMontantMoyenParDonateur() { return montantMoyenParDonateur; }
        public void setMontantMoyenParDonateur(double montantMoyenParDonateur) { this.montantMoyenParDonateur = montantMoyenParDonateur; }
    }
}