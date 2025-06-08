// 1. UtilisateurRepository.java
package com.mediation.platform.repository;

import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.RoleUtilisateur;
import com.mediation.platform.enums.StatutUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // Recherche par email (pour l'authentification)
    Optional<Utilisateur> findByEmail(String email);

    // Vérifier si un email existe
    boolean existsByEmail(String email);

    // Recherche par rôle
    List<Utilisateur> findByRole(RoleUtilisateur role);

    // Recherche par statut
    List<Utilisateur> findByStatut(StatutUtilisateur statut);

    // Recherche par nom et prénom
    List<Utilisateur> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
            String nom, String prenom);

    // Compter par rôle
    @Query("SELECT COUNT(u) FROM Utilisateur u WHERE u.role = :role")
    long countByRole(@Param("role") RoleUtilisateur role);

    // Utilisateurs actifs
    @Query("SELECT u FROM Utilisateur u WHERE u.statut = 'ACTIF'")
    List<Utilisateur> findActiveUsers();
}