package com.mediation.platform.service;

import com.mediation.platform.entity.*;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutProjet;
import com.mediation.platform.enums.StatutUtilisateur;
import com.mediation.platform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class DataSeeder {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private DonRepository donRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Créer des données de test pour développement
     */
    public void createTestData() {
        // Vérifier si des données existent déjà
        if (utilisateurRepository.count() > 0) {
            System.out.println("Données déjà présentes, pas de création nécessaire");
            return;
        }

        System.out.println("🌱 Création des données de test...");

        // 1. Créer un administrateur
        Administrateur admin = new Administrateur();
        admin.setNom("Admin");
        admin.setPrenom("Super");
        admin.setEmail("admin@mediation.com");
        admin.setMotDePasse(passwordEncoder.encode("admin123"));
        admin.setStatut(StatutUtilisateur.ACTIF);
        admin.setNiveauAcces("SUPER_ADMIN");
        admin.setDepartement("IT");
        utilisateurRepository.save(admin);

        // 2. Créer des donateurs
        Donateur donateur1 = new Donateur();
        donateur1.setNom("Alami");
        donateur1.setPrenom("Mohammed");
        donateur1.setEmail("mohammed.alami@email.com");
        donateur1.setMotDePasse(passwordEncoder.encode("donateur123"));
        donateur1.setStatut(StatutUtilisateur.ACTIF);
        donateur1.setTelephone("0661234567");
        donateur1.setAdresse("Casablanca, Maroc");
        donateur1.setDateNaissance(LocalDate.of(1985, 5, 15));
        donateur1.setProfession("Ingénieur");
        utilisateurRepository.save(donateur1);

        Donateur donateur2 = new Donateur();
        donateur2.setNom("Benali");
        donateur2.setPrenom("Fatima");
        donateur2.setEmail("fatima.benali@email.com");
        donateur2.setMotDePasse(passwordEncoder.encode("donateur123"));
        donateur2.setStatut(StatutUtilisateur.ACTIF);
        donateur2.setTelephone("0662345678");
        donateur2.setAdresse("Rabat, Maroc");
        donateur2.setDateNaissance(LocalDate.of(1990, 8, 22));
        donateur2.setProfession("Médecin");
        utilisateurRepository.save(donateur2);

        // 3. Créer des associations
        Association association1 = new Association();
        association1.setNom("Tazi");
        association1.setPrenom("Ahmed");
        association1.setEmail("contact@solidarite-maroc.org");
        association1.setMotDePasse(passwordEncoder.encode("association123"));
        association1.setStatut(StatutUtilisateur.ACTIF);
        association1.setTelephone("0523456789");
        association1.setNomAssociation("Solidarité Maroc");
        association1.setAdresse("Fès, Maroc");
        association1.setSiteWeb("www.solidarite-maroc.org");
        association1.setDescription("Association dédiée à l'aide sociale et à l'éducation des enfants défavorisés");
        association1.setDomaineActivite("Éducation");
        association1.setDocumentsLegaux("Récépissé n°12345");
        association1.setStatutValidation(true);
        association1.setDateValidation(java.time.LocalDateTime.now().minusDays(30));
        utilisateurRepository.save(association1);

        Association association2 = new Association();
        association2.setNom("Kabbaj");
        association2.setPrenom("Aicha");
        association2.setEmail("contact@espoir-enfants.org");
        association2.setMotDePasse(passwordEncoder.encode("association123"));
        association2.setStatut(StatutUtilisateur.ACTIF);
        association2.setTelephone("0524567890");
        association2.setNomAssociation("Espoir pour les Enfants");
        association2.setAdresse("Marrakech, Maroc");
        association2.setSiteWeb("www.espoir-enfants.org");
        association2.setDescription("Association pour la protection et l'aide aux enfants en difficulté");
        association2.setDomaineActivite("Protection de l'enfance");
        association2.setDocumentsLegaux("Récépissé n°67890");
        association2.setStatutValidation(true);
        association2.setDateValidation(java.time.LocalDateTime.now().minusDays(15));
        utilisateurRepository.save(association2);

        // 4. Créer des projets
        Projet projet1 = new Projet();
        projet1.setTitre("Construction d'une école primaire");
        projet1.setDescription("Construction d'une école primaire dans un village rural pour 200 enfants");
        projet1.setObjectif("Offrir une éducation de qualité aux enfants des zones rurales");
        projet1.setMontantDemande(150000.0);
        projet1.setMontantCollecte(75000.0);
        projet1.setDateDebut(LocalDate.now().minusMonths(2));
        projet1.setDateFin(LocalDate.now().plusMonths(4));
        projet1.setStatut(StatutProjet.EN_COURS);
        projet1.setPriorite("HAUTE");
        projet1.setAssociation(association1);
        projetRepository.save(projet1);

        Projet projet2 = new Projet();
        projet2.setTitre("Centre d'accueil pour enfants");
        projet2.setDescription("Création d'un centre d'accueil pour enfants abandonnés");
        projet2.setObjectif("Fournir un toit et des soins aux enfants en détresse");
        projet2.setMontantDemande(200000.0);
        projet2.setMontantCollecte(120000.0);
        projet2.setDateDebut(LocalDate.now().minusMonths(1));
        projet2.setDateFin(LocalDate.now().plusMonths(6));
        projet2.setStatut(StatutProjet.EN_COURS);
        projet2.setPriorite("TRÈS HAUTE");
        projet2.setAssociation(association2);
        projetRepository.save(projet2);

        Projet projet3 = new Projet();
        projet3.setTitre("Bibliothèque mobile");
        projet3.setDescription("Création d'une bibliothèque mobile pour les zones reculées");
        projet3.setObjectif("Promouvoir la lecture et l'éducation");
        projet3.setMontantDemande(50000.0);
        projet3.setMontantCollecte(50000.0);
        projet3.setDateDebut(LocalDate.now().minusMonths(6));
        projet3.setDateFin(LocalDate.now().minusMonths(1));
        projet3.setStatut(StatutProjet.TERMINE);
        projet3.setPriorite("MOYENNE");
        projet3.setAssociation(association1);
        projetRepository.save(projet3);

        // 5. Créer des dons
        Don don1 = new Don();
        don1.setMontant(5000.0);
        don1.setMessage("Merci pour votre travail formidable !");
        don1.setAnonyme(false);
        don1.setStatut(com.mediation.platform.enums.StatutDon.VALIDE);
        don1.setDonateur(donateur1);
        don1.setProjet(projet1);
        don1.setDate(LocalDate.now().minusDays(10));
        donRepository.save(don1);

        Don don2 = new Don();
        don2.setMontant(10000.0);
        don2.setMessage("Continuez ainsi, c'est très important !");
        don2.setAnonyme(false);
        don2.setStatut(com.mediation.platform.enums.StatutDon.VALIDE);
        don2.setDonateur(donateur2);
        don2.setProjet(projet1);
        don2.setDate(LocalDate.now().minusDays(8));
        donRepository.save(don2);

        Don don3 = new Don();
        don3.setMontant(15000.0);
        don3.setMessage("");
        don3.setAnonyme(true);
        don3.setStatut(com.mediation.platform.enums.StatutDon.VALIDE);
        don3.setDonateur(donateur1);
        don3.setProjet(projet2);
        don3.setDate(LocalDate.now().minusDays(5));
        donRepository.save(don3);

        Don don4 = new Don();
        don4.setMontant(7500.0);
        don4.setMessage("Pour les enfants !");
        don4.setAnonyme(false);
        don4.setStatut(com.mediation.platform.enums.StatutDon.VALIDE);
        don4.setDonateur(donateur2);
        don4.setProjet(projet2);
        don4.setDate(LocalDate.now().minusDays(3));
        donRepository.save(don4);

        Don don5 = new Don();
        don5.setMontant(2000.0);
        don5.setMessage("Bravo pour cette initiative !");
        don5.setAnonyme(false);
        don5.setStatut(com.mediation.platform.enums.StatutDon.EN_ATTENTE);
        don5.setDonateur(donateur1);
        don5.setProjet(projet1);
        don5.setDate(LocalDate.now().minusDays(1));
        donRepository.save(don5);

        // 6. Créer une association en attente de validation
        Association associationEnAttente = new Association();
        associationEnAttente.setNom("Idrissi");
        associationEnAttente.setPrenom("Youssef");
        associationEnAttente.setEmail("contact@aide-solidaire.org");
        associationEnAttente.setMotDePasse(passwordEncoder.encode("association123"));
        associationEnAttente.setStatut(StatutUtilisateur.EN_ATTENTE);
        associationEnAttente.setTelephone("0525678901");
        associationEnAttente.setNomAssociation("Aide Solidaire");
        associationEnAttente.setAdresse("Meknes, Maroc");
        associationEnAttente.setSiteWeb("www.aide-solidaire.org");
        associationEnAttente.setDescription("Association d'aide aux personnes âgées");
        associationEnAttente.setDomaineActivite("Aide sociale");
        associationEnAttente.setDocumentsLegaux("Récépissé en cours");
        associationEnAttente.setStatutValidation(false);
        utilisateurRepository.save(associationEnAttente);

        System.out.println("✅ Données de test créées avec succès !");
        System.out.println("👤 Comptes créés :");
        System.out.println("  📋 Admin: admin@mediation.com / admin123");
        System.out.println("  💰 Donateur 1: mohammed.alami@email.com / donateur123");
        System.out.println("  💰 Donateur 2: fatima.benali@email.com / donateur123");
        System.out.println("  🏢 Association 1: contact@solidarite-maroc.org / association123");
        System.out.println("  🏢 Association 2: contact@espoir-enfants.org / association123");
        System.out.println("  ⏳ Association en attente: contact@aide-solidaire.org / association123");
        System.out.println("📊 Données créées :");
        System.out.println("  - 6 utilisateurs (1 admin, 2 donateurs, 3 associations)");
        System.out.println("  - 3 projets (2 en cours, 1 terminé)");
        System.out.println("  - 5 dons (4 validés, 1 en attente)");
    }

    /**
     * Supprimer toutes les données de test
     */
    public void clearTestData() {
        System.out.println("🗑️ Suppression des données de test...");

        donRepository.deleteAll();
        projetRepository.deleteAll();
        utilisateurRepository.deleteAll();

        System.out.println("✅ Données de test supprimées !");
    }

    /**
     * Vérifier si des données de test existent
     */
    public boolean testDataExists() {
        return utilisateurRepository.existsByEmail("admin@mediation.com");
    }

    /**
     * Obtenir des statistiques des données de test
     */
    public String getTestDataStats() {
        if (!testDataExists()) {
            return "❌ Aucune donnée de test trouvée";
        }

        StringBuilder stats = new StringBuilder();
        stats.append("📊 Statistiques des données de test :\n");
        stats.append("  👥 Utilisateurs : ").append(utilisateurRepository.count()).append("\n");
        stats.append("  🏢 Associations : ").append(utilisateurRepository.countByRole(RoleUtilisateur.ASSOCIATION)).append("\n");
        stats.append("  💰 Donateurs : ").append(utilisateurRepository.countByRole(RoleUtilisateur.DONATEUR)).append("\n");
        stats.append("  📋 Administrateurs : ").append(utilisateurRepository.countByRole(RoleUtilisateur.ADMINISTRATEUR)).append("\n");
        stats.append("  📁 Projets : ").append(projetRepository.count()).append("\n");
        stats.append("  💝 Dons : ").append(donRepository.count()).append("\n");

        return stats.toString();
    }
}
