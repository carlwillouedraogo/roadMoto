package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.TypeEtape;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;


@Entity
public class Etape {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itineraire_id", nullable = false)
    private Itineraire itineraire;
    @Column(nullable = false)
    private int ordre;
    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point position;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id")
    private Poi poi;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeEtape typeEtape;
    @Column(name = "distance_depuis_precedente_m")
    private Integer distanceDepuisPrecedenteM;
    @Column(name = "duree_depuis_precedente_s")
    private Integer dureeDepuisPrecedenteS;
    private Integer dureeArretMin;

    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Etape other)) return false;
        return id != null && id.equals(other.getId());
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return Etape.class.hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Itineraire getItineraire() {
        return itineraire;
    }

    public void setItineraire(Itineraire itineraire) {
        this.itineraire = itineraire;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public Poi getPoi() {
        return poi;
    }

    public void setPoi(Poi poi) {
        this.poi = poi;
    }

    public TypeEtape getTypeEtape() {
        return typeEtape;
    }

    public void setTypeEtape(TypeEtape typeEtape) {
        this.typeEtape = typeEtape;
    }

    public Integer getDistanceDepuisPrecedenteM() {
        return distanceDepuisPrecedenteM;
    }

    public void setDistanceDepuisPrecedenteM(Integer distanceDepuisPrecedenteM) {
        this.distanceDepuisPrecedenteM = distanceDepuisPrecedenteM;
    }

    public Integer getDureeDepuisPrecedenteS() {
        return dureeDepuisPrecedenteS;
    }

    public void setDureeDepuisPrecedenteS(Integer dureeDepuisPrecedenteS) {
        this.dureeDepuisPrecedenteS = dureeDepuisPrecedenteS;
    }

    public Integer getDureeArretMin() {
        return dureeArretMin;
    }

    public void setDureeArretMin(Integer dureeArretMin) {
        this.dureeArretMin = dureeArretMin;
    }
}
