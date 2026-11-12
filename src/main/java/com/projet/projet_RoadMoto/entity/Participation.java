package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.StatutParticipation;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Set;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_participation_sortie_utilisateur",
        columnNames = {"sortie_id", "utilisateur_id"}
))
public class Participation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sortie_id", nullable = false)
    private Sortie sortie;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutParticipation statut;
    @CreationTimestamp
    private OffsetDateTime inscritLe;

    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participation other)) return false;
        return id != null && id.equals(other.getId());
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return Participation.class.hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sortie getSortie() {
        return sortie;
    }

    public void setSortie(Sortie sortie) {
        this.sortie = sortie;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public StatutParticipation getStatut() {
        return statut;
    }

    public void setStatut(StatutParticipation statut) {
        this.statut = statut;
    }

    public OffsetDateTime getInscritLe() {
        return inscritLe;
    }

    public void setInscritLe(OffsetDateTime inscritLe) {
        this.inscritLe = inscritLe;
    }
}
