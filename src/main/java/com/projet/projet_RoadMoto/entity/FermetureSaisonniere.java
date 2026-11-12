package com.projet.projet_RoadMoto.entity;

import jakarta.persistence.*;


@Entity
public class FermetureSaisonniere {
    @Id
    private Long poiId;
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "poi_id")
    private Poi poi;
    @Column(nullable = false)
    private boolean ouvertTouteAnnee;
    private Integer moisOuvertureDebut;
    private Integer moisOuvertureFin;


    // Égalité sur l'identifiant uniquement : pas de boucle entre associations bidirectionnelles,
    // pas de chargement lazy déclenché, et instanceof accepte les proxies Hibernate.
    // Une entité pas encore persistée n'est égale qu'à elle-même.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FermetureSaisonniere other)) return false;
        return poiId != null && poiId.equals(other.getPoiId());
    }

    // Constant pour rester stable quand l'id est attribué à la persistance (HashSet, etc.)
    @Override
    public int hashCode() {
        return FermetureSaisonniere.class.hashCode();
    }

    public Long getPoiId() {
        return poiId;
    }

    public void setPoiId(Long poiId) {
        this.poiId = poiId;
    }

    public Poi getPoi() {
        return poi;
    }

    public void setPoi(Poi poi) {
        this.poi = poi;
    }

    public boolean isOuvertTouteAnnee() {
        return ouvertTouteAnnee;
    }

    public void setOuvertTouteAnnee(boolean ouvertTouteAnnee) {
        this.ouvertTouteAnnee = ouvertTouteAnnee;
    }

    public Integer getMoisOuvertureDebut() {
        return moisOuvertureDebut;
    }

    public void setMoisOuvertureDebut(Integer moisOuvertureDebut) {
        this.moisOuvertureDebut = moisOuvertureDebut;
    }

    public Integer getMoisOuvertureFin() {
        return moisOuvertureFin;
    }

    public void setMoisOuvertureFin(Integer moisOuvertureFin) {
        this.moisOuvertureFin = moisOuvertureFin;
    }
}
