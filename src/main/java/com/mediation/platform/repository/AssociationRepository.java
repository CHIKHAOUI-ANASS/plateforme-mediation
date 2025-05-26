package com.mediation.platform.repository;

import com.mediation.platform.entity.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssociationRepository extends JpaRepository<Association, Long> {

    // Recherche par nom d'association
    List<Association> findByNomAssociationContainingIgnoreCase(String nom);

    // Associations validées
    List<Association> findByStatutValidationTrue();

    // Associations en attente de validation
    List<Association> findByStatutValidationFalse();

    // Recherche par domaine d'activité
    List<Association> findByDomaineActiviteContainingIgnoreCase(String domaine);

    // Recherche par ville/région
    List<Association> findByAdresseContainingIgnoreCase(String ville);

    // Associations récemment validées
    @Query("SELECT a FROM Association a WHERE a.statutValidation = true " +
            "AND a.dateValidation >= :dateDebut ORDER BY a.dateValidation DESC")
    List<Association> findRecentlyValidated(@Param("dateDebut") LocalDateTime dateDebut);

    // Associations avec des projets actifs
    @Query("SELECT DISTINCT a FROM Association a JOIN a.projets p WHERE p.statut = 'ACTIF'")
    List<Association> findWithActiveProjects();

    // Top associations par montant collecté
    @Query("SELECT a FROM Association a JOIN a.projets p JOIN p.dons d " +
            "WHERE d.statut = 'CONFIRME' GROUP BY a ORDER BY SUM(d.montant) DESC")
    List<Association> findTopAssociationsByDonations();
}