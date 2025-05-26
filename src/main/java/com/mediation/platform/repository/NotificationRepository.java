package com.mediation.platform.repository;

import com.mediation.platform.entity.Notification;
import com.mediation.platform.entity.Utilisateur;
import com.mediation.platform.enums.TypeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Notifications d'un utilisateur
    List<Notification> findByUtilisateurOrderByDateEnvoiDesc(Utilisateur utilisateur);

    // Notifications non lues
    List<Notification> findByUtilisateurAndLuFalseOrderByDateEnvoiDesc(Utilisateur utilisateur);

    // Notifications par type
    List<Notification> findByType(TypeNotification type);

    // Notifications urgentes non lues
    List<Notification> findByUrgentTrueAndLuFalseOrderByDateEnvoiDesc();

    // Compter notifications non lues par utilisateur
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.utilisateur = :utilisateur AND n.lu = false")
    long countUnreadByUser(@Param("utilisateur") Utilisateur utilisateur);

    // Notifications récentes
    @Query("SELECT n FROM Notification n WHERE n.dateEnvoi >= :dateDebut " +
            "ORDER BY n.dateEnvoi DESC")
    List<Notification> findRecentNotifications(@Param("dateDebut") LocalDateTime dateDebut);

    // Marquer comme lues
    @Query("UPDATE Notification n SET n.lu = true WHERE n.utilisateur = :utilisateur")
    void markAllAsReadForUser(@Param("utilisateur") Utilisateur utilisateur);
}