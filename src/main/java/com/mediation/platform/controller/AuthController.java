package com.mediation.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    /**
     * Test endpoint pour vérifier que le contrôleur fonctionne
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ AuthController fonctionne correctement!");
    }

    /**
     * Endpoint de login temporaire
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        Map<String, Object> response = new HashMap<>();

        String email = loginData.get("email");
        String password = loginData.get("motDePasse");

        // Validation simple temporaire
        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email requis");
            return ResponseEntity.badRequest().body(response);
        }

        if (password == null || password.isEmpty()) {
            response.put("success", false);
            response.put("message", "Mot de passe requis");
            return ResponseEntity.badRequest().body(response);
        }

        // Simulation d'un login réussi
        response.put("success", true);
        response.put("message", "Connexion réussie");
        response.put("token", "temp-token-" + System.currentTimeMillis());
        response.put("user", Map.of(
                "email", email,
                "role", "DONATEUR",
                "nom", "Utilisateur Test"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint d'inscription donateur temporaire
     */
    @PostMapping("/register/donateur")
    public ResponseEntity<Map<String, Object>> registerDonateur(@RequestBody Map<String, String> registerData) {
        Map<String, Object> response = new HashMap<>();

        String email = registerData.get("email");
        String nom = registerData.get("nom");
        String prenom = registerData.get("prenom");

        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email requis");
            return ResponseEntity.badRequest().body(response);
        }

        // Simulation d'une inscription réussie
        response.put("success", true);
        response.put("message", "Compte donateur créé avec succès");
        response.put("user", Map.of(
                "email", email,
                "nom", nom != null ? nom : "",
                "prenom", prenom != null ? prenom : "",
                "role", "DONATEUR"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint d'inscription association temporaire
     */
    @PostMapping("/register/association")
    public ResponseEntity<Map<String, Object>> registerAssociation(@RequestBody Map<String, String> registerData) {
        Map<String, Object> response = new HashMap<>();

        String email = registerData.get("email");
        String nomAssociation = registerData.get("nomAssociation");

        if (email == null || email.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email requis");
            return ResponseEntity.badRequest().body(response);
        }

        if (nomAssociation == null || nomAssociation.isEmpty()) {
            response.put("success", false);
            response.put("message", "Nom de l'association requis");
            return ResponseEntity.badRequest().body(response);
        }

        // Simulation d'une inscription réussie
        response.put("success", true);
        response.put("message", "Demande d'association créée avec succès");
        response.put("data", "Demande en cours de traitement");

        return ResponseEntity.ok(response);
    }

    /**
     * Vérifier si un email existe
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        // Simulation - toujours dire que l'email est disponible pour l'instant
        response.put("success", true);
        response.put("message", "Email disponible");
        response.put("data", false); // false = email n'existe pas encore

        return ResponseEntity.ok(response);
    }
}