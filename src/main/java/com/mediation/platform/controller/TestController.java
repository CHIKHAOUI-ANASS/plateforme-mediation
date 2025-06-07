package com.mediation.platform.controller;

import com.mediation.platform.dto.response.ApiResponse;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import com.mediation.platform.repository.UtilisateurRepository;
import com.mediation.platform.service.DataSeeder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Test", description = "Endpoints de test et de diagnostic")
public class TestController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private DataSeeder dataSeeder;

    /**
     * Test de base - vérifier que l'API fonctionne
     */
    @GetMapping("/ping")
    @Operation(summary = "Test de connectivité", description = "Vérifie que l'API répond correctement")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(ApiResponse.success("Pong! API fonctionne correctement ✅", "OK"));
    }

    /**
     * Test de la base de données
     */
    @GetMapping("/database")
    @Operation(summary = "Test base de données", description = "Vérifie la connexion à la base de données")
    public ResponseEntity<?> testDatabase() {
        try {
            long nombreUtilisateurs = utilisateurRepository.count();

            Map<String, Object> info = new HashMap<>();
            info.put("status", "✅ Connexion BD OK");
            info.put("nombreUtilisateurs", nombreUtilisateurs);
            info.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(ApiResponse.success("Base de données accessible", info));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "❌ Erreur BD");
            error.put("error", e.getMessage());

            return ResponseEntity.ok(ApiResponse.error("Erreur base de données", error));
        }
    }

    /**
     * Créer des données de test
     */
    @PostMapping("/seed-data")
    @Operation(summary = "Créer données de test", description = "Insère des données de test dans la base")
    public ResponseEntity<?> createTestData() {
        try {
            dataSeeder.createTestData();
            return ResponseEntity.ok(ApiResponse.success("Données de test créées avec succès! Consultez les logs pour les comptes créés.", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erreur création données test: " + e.getMessage()));
        }
    }

    /**
     * Supprimer les données de test
     */
    @DeleteMapping("/clear-data")
    @Operation(summary = "Supprimer données test", description = "Supprime toutes les données de test")
    public ResponseEntity<?> clearTestData() {
        try {
            dataSeeder.clearTestData();
            return ResponseEntity.ok(ApiResponse.success("Données de test supprimées avec succès", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erreur suppression données test: " + e.getMessage()));
        }
    }

    /**
     * Statistiques des données de test
     */
    @GetMapping("/data-stats")
    @Operation(summary = "Stats données test", description = "Affiche les statistiques des données de test")
    public ResponseEntity<?> getDataStats() {
        try {
            String stats = dataSeeder.getTestDataStats();
            return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées", stats));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Erreur récupération stats: " + e.getMessage()));
        }
    }

    /**
     * Lister tous les utilisateurs (pour debug)
     */
    @GetMapping("/users")
    @Operation(summary = "Lister utilisateurs", description = "Liste tous les utilisateurs (debug uniquement)")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
            return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", utilisateurs));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur récupération utilisateurs: " + e.getMessage()));
        }
    }

    /**
     * Informations sur l'application
     */
    @GetMapping("/info")
    @Operation(summary = "Informations système", description = "Retourne les informations sur l'application")
    public ResponseEntity<?> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "Plateforme de Médiation");
        info.put("version", "1.0.0");
        info.put("environment", "development");
        info.put("timestamp", java.time.LocalDateTime.now());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("springBootVersion", "3.0.6");

        return ResponseEntity.ok(ApiResponse.success("Informations système", info));
    }

    /**
     * Test des rôles et statuts (énumérations)
     */
    @GetMapping("/enums")
    @Operation(summary = "Test énumérations", description = "Liste les valeurs des énumérations")
    public ResponseEntity<?> getEnums() {
        Map<String, Object> enums = new HashMap<>();

        Map<String, String> roles = new HashMap<>();
        for (RoleUtilisateur role : RoleUtilisateur.values()) {
            roles.put(role.name(), role.getLibelle());
        }

        Map<String, String> statuts = new HashMap<>();
        for (StatutUtilisateur statut : StatutUtilisateur.values()) {
            statuts.put(statut.name(), statut.getLibelle());
        }

        enums.put("roles", roles);
        enums.put("statuts", statuts);

        return ResponseEntity.ok(ApiResponse.success("Énumérations disponibles", enums));
    }

    /**
     * Test de santé global
     */
    @GetMapping("/health")
    @Operation(summary = "État de santé", description = "Vérifie l'état général de l'application")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test BD
            long userCount = utilisateurRepository.count();
            health.put("database", "UP");
            health.put("userCount", userCount);
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("databaseError", e.getMessage());
        }

        // Test mémoire
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        health.put("memory", memory);

        // Test données de test
        health.put("testData", dataSeeder.testDataExists() ? "PRÉSENTES" : "ABSENTES");

        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success("État de santé", health));
    }

    /**
     * Test complet de l'API
     */
    @GetMapping("/full-test")
    @Operation(summary = "Test complet", description = "Lance une série de tests complets")
    public ResponseEntity<?> fullTest() {
        Map<String, Object> results = new HashMap<>();

        try {
            // Test 1: Base de données
            results.put("database", "✅ OK - " + utilisateurRepository.count() + " utilisateurs");

            // Test 2: Énumérations
            results.put("enums", "✅ OK - " + RoleUtilisateur.values().length + " rôles, " +
                    StatutUtilisateur.values().length + " statuts");

            // Test 3: Données de test
            if (dataSeeder.testDataExists()) {
                results.put("testData", "✅ OK - Données présentes");
            } else {
                results.put("testData", "⚠️ Aucune donnée de test");
            }

            // Test 4: Mémoire
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
            results.put("memory", "✅ OK - " + freeMemory + " MB libres");

            results.put("globalStatus", "✅ TOUS LES TESTS PASSÉS");

        } catch (Exception e) {
            results.put("globalStatus", "❌ ERREUR: " + e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("Résultats du test complet", results));
    }

    /**
     * Guide de démarrage rapide
     */
    @GetMapping("/quick-start")
    @Operation(summary = "Guide démarrage", description = "Guide pour démarrer rapidement avec l'API")
    public ResponseEntity<?> quickStart() {
        Map<String, Object> guide = new HashMap<>();

        guide.put("step1", "✅ L'API fonctionne ! Vous êtes sur /test/quick-start");
        guide.put("step2", "📊 Vérifiez la santé: GET /test/health");
        guide.put("step3", "🌱 Créez des données de test: POST /test/seed-data");
        guide.put("step4", "📖 Documentation: /swagger-ui.html");
        guide.put("step5", "🔐 Testez l'auth: POST /auth/login");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("auth", "/auth/* (login, register)");
        endpoints.put("donateurs", "/donateur/* (dashboard, profil)");
        endpoints.put("associations", "/associations/* (liste, détails)");
        endpoints.put("projets", "/projets/* (liste, recherche)");
        endpoints.put("dons", "/dons/* (créer, lister)");
        endpoints.put("admin", "/admin/* (gestion, stats)");
        endpoints.put("stats", "/statistiques/* (publiques, détaillées)");
        guide.put("endpoints_utiles", endpoints);

        Map<String, String> comptes = new HashMap<>();
        comptes.put("admin", "admin@mediation.com / admin123");
        comptes.put("donateur1", "mohammed.alami@email.com / donateur123");
        comptes.put("donateur2", "fatima.benali@email.com / donateur123");
        comptes.put("association1", "contact@solidarite-maroc.org / association123");
        comptes.put("association2", "contact@espoir-enfants.org / association123");
        guide.put("comptes_test", comptes);

        return ResponseEntity.ok(ApiResponse.success("Guide de démarrage rapide", guide));
    }
}