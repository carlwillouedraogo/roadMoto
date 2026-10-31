package com.projet.projet_RoadMoto.entity;

import jakarta.persistence.*;

import java.util.Objects;

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


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FermetureSaisonniere that = (FermetureSaisonniere) o;
        return ouvertTouteAnnee == that.ouvertTouteAnnee && Objects.equals(poiId, that.poiId) && Objects.equals(poi, that.poi) && Objects.equals(moisOuvertureDebut, that.moisOuvertureDebut) && Objects.equals(moisOuvertureFin, that.moisOuvertureFin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poiId, poi, ouvertTouteAnnee, moisOuvertureDebut, moisOuvertureFin);
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
