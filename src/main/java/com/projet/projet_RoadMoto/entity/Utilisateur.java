package com.projet.projet_RoadMoto.entity;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, unique = true)
    private String pseudo;
    @Column(nullable = false, unique = true)
    private String email;
    private String motDePasseHash;
    private String motoModele;
    @Enumerated(EnumType.STRING)
    private Niveau niveau;
    @Enumerated(EnumType.STRING)
    private Role role;
    @CreationTimestamp
    private OffsetDateTime creeLe;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Utilisateur that = (Utilisateur) o;
        return id == that.id && Objects.equals(pseudo, that.pseudo) && Objects.equals(email, that.email) && Objects.equals(motDePasseHash, that.motDePasseHash) && Objects.equals(motoModele, that.motoModele) && niveau == that.niveau && role == that.role && Objects.equals(creeLe, that.creeLe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pseudo, email, motDePasseHash, motoModele, niveau, role, creeLe);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getMotoModele() {
        return motoModele;
    }

    public void setMotoModele(String motoModele) {
        this.motoModele = motoModele;
    }

    public Niveau getNiveau() {
        return niveau;
    }

    public void setNiveau(Niveau niveau) {
        this.niveau = niveau;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public OffsetDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(OffsetDateTime creeLe) {
        this.creeLe = creeLe;
    }
}
