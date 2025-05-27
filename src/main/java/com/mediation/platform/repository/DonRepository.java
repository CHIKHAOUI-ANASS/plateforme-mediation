package com.mediation.platform.repository;

import com.mediation.platform.entity.Don;
import com.mediation.platform.entity.Donateur;
import com.mediation.platform.entity.Projet;
import com.mediation.platform.enums.StatutDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DonRepository extends JpaRepository<Don, Long> {

    // Dons d'un donateur
    List<Don> findByDonateurOrderByDateDesc(Donateur donateur);

    // Dons pour un projet
    List<Don> findByProjetOrderByDateDesc(Projet projet);

    // Dons par statut
    List<Don> findByStatut(StatutDon statut);

    // Dons confirmés
    List<Don> findByStatutOrderByDateDesc(StatutDon statut);

    // Dons anonymes
    List<Don> findByAnonymeTrue();

    // Dons avec message
    @Query("SELECT d FROM Don d WHERE d.message IS NOT NULL AND d.message <> ''")
    List<Don> findDonsWithMessage();

    // Dons par période
    List<Don> findByDateBetweenOrderByDateDesc(LocalDate dateDebut, LocalDate dateFin);

    // Montant total des dons confirmés
    @Query("SELECT SUM(d.montant) FROM Don d WHERE d.statut = 'CONFIRME'")
    Double getTotalConfirmedDonations();

    // Montant total pour un projet
    @Query("SELECT SUM(d.montant) FROM Don d WHERE d.projet = :projet AND d.statut = 'CONFIRME'")
    Double getTotalForProject(@Param("projet") Projet projet);

    // Nombre de donateurs uniques
    @Query("SELECT COUNT(DISTINCT d.donateur) FROM Don d WHERE d.statut = 'CONFIRME'")
    long getUniqueDonorsCount();

    // Dons récents (derniers 30 jours)
    @Query("SELECT d FROM Don d WHERE d.date >= :dateDebut ORDER BY d.date DESC")
    List<Don> findRecentDonations(@Param("dateDebut") LocalDate dateDebut);

    // Gros dons (montant supérieur à un seuil)
    @Query("SELECT d FROM Don d WHERE d.montant >= :montantMin AND d.statut = 'CONFIRME' " +
            "ORDER BY d.montant DESC")
    List<Don> findLargeDonations(@Param("montantMin") Double montantMin);
}

