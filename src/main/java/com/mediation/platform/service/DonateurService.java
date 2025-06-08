package com.mediation.platform.service;

import com.mediation.platform.entity.Donateur;
import com.mediation.platform.exception.ResourceNotFoundException;
import com.mediation.platform.repository.DonateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DonateurService {

    @Autowired
    private DonateurRepository donateurRepository;

    @Autowired
    private NotificationService notificationService;

    public Donateur findById(Long id) {
        return donateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donateur non trouvé avec l'ID: " + id));
    }

    public List<Donateur> findAll() {
        return donateurRepository.findAll();
    }

    public Donateur save(Donateur donateur) {
        return donateurRepository.save(donateur);
    }

    public Donateur update(Long id, Donateur donateurData) {
        Donateur donateur = findById(id);

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

    public void deleteById(Long id) {
        Donateur donateur = findById(id);
        donateurRepository.delete(donateur);
    }

    public List<Donateur> findByProfession(String profession) {
        return donateurRepository.findByProfessionContainingIgnoreCase(profession);
    }

    public List<Donateur> findByAgeBetween(int ageMin, int ageMax) {
        return donateurRepository.findByAgeBetween(ageMin, ageMax);
    }

    public List<Donateur> findDonatorsWithConfirmedDonations() {
        return donateurRepository.findDonatorsWithConfirmedDonations();
    }

    public List<Donateur> findTopDonators() {
        return donateurRepository.findTopDonators();
    }

    public List<Donateur> findByVille(String ville) {
        return donateurRepository.findByAdresseContainingIgnoreCase(ville);
    }

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

    public boolean existsById(Long id) {
        return donateurRepository.existsById(id);
    }

    public long count() {
        return donateurRepository.count();
    }

    public Donateur toggleStatus(Long id) {
        Donateur donateur = findById(id);

        if (donateur.estActif()) {
            donateur.setStatut(com.mediation.platform.enums.StatutUtilisateur.INACTIF);
        } else {
            donateur.setStatut(com.mediation.platform.enums.StatutUtilisateur.ACTIF);
        }

        return donateurRepository.save(donateur);
    }

    public static class DonateurStats {
        private int totalDonateurs;
        private int donateursActifs;
        private int donateursReguliers;
        private int grosDonateurs;
        private double montantTotalDons;
        private double montantMoyenParDonateur;

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