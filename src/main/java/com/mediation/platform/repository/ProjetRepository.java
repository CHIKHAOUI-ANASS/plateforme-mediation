package com.mediation.platform.repository;

import com.mediation.platform.entity.Association;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.enums.StatutProjet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {

    // Recherche par titre
    List<Projet> findByTitreContainingIgnoreCase(String titre);

    // Projets par statut
    List<Projet> findByStatut(StatutProjet statut);

    // Projets d'une association
    List<Projet> findByAssociation(Association association);

    // Projets actifs
    List<Projet> findByStatutOrderByDateCreationDesc(StatutProjet statut);

    // Projets par priorité
    List<Projet> findByPrioriteOrderByDateCreationDesc(String priorite);

    // Projets proches de l'objectif
    @Query("SELECT p FROM Projet p WHERE p.statut = 'ACTIF' " +
            "AND (p.montantCollecte / p.montantDemande) >= :pourcentage")
    List<Projet> findNearGoal(@Param("pourcentage") double pourcentage);

    // Projets en retard
    @Query("SELECT p FROM Projet p WHERE p.dateFin < :dateActuelle " +
            "AND p.statut = 'ACTIF'")
    List<Projet> findOverdueProjects(@Param("dateActuelle") LocalDate dateActuelle);

    // Projets récents
    @Query("SELECT p FROM Projet p WHERE p.dateCreation >= :dateDebut " +
            "ORDER BY p.dateCreation DESC")
    List<Projet> findRecentProjects(@Param("dateDebut") java.time.LocalDateTime dateDebut);

    // Top projets par montant collecté
    @Query("SELECT p FROM Projet p ORDER BY p.montantCollecte DESC")
    List<Projet> findTopProjectsByAmount();

    // Recherche par mots-clés dans titre ou description
    @Query("SELECT p FROM Projet p WHERE p.titre LIKE %:keyword% " +
            "OR p.description LIKE %:keyword% OR p.objectif LIKE %:keyword%")
    List<Projet> searchByKeyword(@Param("keyword") String keyword);
}