package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Statut;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;

@Entity
public class Sortie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titre;
    private String description;
    private OffsetDateTime dateDepart;
    private String nomLieuDepart;
    @Column(columnDefinition = "geography(Point,4326)")
    private Point lieuDepart;
    private Integer distanceKm;
    private Integer dureeEstimeeMin;
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    private int placesMax;
    @Enumerated(EnumType.STRING)
    private Statut statut;
    @Version
    private int version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Utilisateur organisateur;
    @CreationTimestamp
    private OffsetDateTime creeLe;

    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sortie other)) return false;
        return id != null && id.equals(other.getId());
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return Sortie.class.hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(OffsetDateTime dateDepart) {
        this.dateDepart = dateDepart;
    }

    public String getNomLieuDepart() {
        return nomLieuDepart;
    }

    public void setNomLieuDepart(String nomLieuDepart) {
        this.nomLieuDepart = nomLieuDepart;
    }

    public Point getLieuDepart() {
        return lieuDepart;
    }

    public void setLieuDepart(Point lieuDepart) {
        this.lieuDepart = lieuDepart;
    }

    public Integer getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Integer distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDureeEstimeeMin() {
        return dureeEstimeeMin;
    }

    public void setDureeEstimeeMin(Integer dureeEstimeeMin) {
        this.dureeEstimeeMin = dureeEstimeeMin;
    }

    public Niveau getNiveau() {
        return niveau;
    }

    public void setNiveau(Niveau niveau) {
        this.niveau = niveau;
    }

    public int getPlacesMax() {
        return placesMax;
    }

    public void setPlacesMax(int placesMax) {
        this.placesMax = placesMax;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Utilisateur getOrganisateur() {
        return organisateur;
    }

    public void setOrganisateur(Utilisateur organisateur) {
        this.organisateur = organisateur;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(OffsetDateTime creeLe) {
        this.creeLe = creeLe;
    }
}
