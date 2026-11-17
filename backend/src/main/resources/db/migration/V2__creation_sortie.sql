-- V2__creation_sortie.sql

CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE sortie (
                        id                 BIGSERIAL PRIMARY KEY,
                        titre              VARCHAR(120)            NOT NULL,
                        description        TEXT,
                        date_depart        TIMESTAMPTZ             NOT NULL,
                        nom_lieu_depart    VARCHAR(255)            NOT NULL,
                        lieu_depart        GEOGRAPHY(Point, 4326)  NOT NULL,
                        distance_km        INT,
                        duree_estimee_min  INT,
                        niveau             VARCHAR(20)             NOT NULL,
                        places_max         INT                     NOT NULL,
                        statut             VARCHAR(20)             NOT NULL DEFAULT 'BROUILLON',
                        version            INT                     NOT NULL DEFAULT 0,
                        organisateur_id    BIGINT                  NOT NULL,
                        cree_le            TIMESTAMPTZ             NOT NULL DEFAULT now(),

                        CONSTRAINT fk_sortie_organisateur
                            FOREIGN KEY (organisateur_id) REFERENCES utilisateur (id),

                        CONSTRAINT ck_sortie_places_max
                            CHECK (places_max >= 2),

                        CONSTRAINT ck_sortie_duree_positive
                            CHECK (duree_estimee_min IS NULL OR duree_estimee_min > 0)
);

CREATE INDEX idx_sortie_lieu_depart ON sortie USING GIST (lieu_depart);
CREATE INDEX idx_sortie_organisateur ON sortie (organisateur_id);
CREATE INDEX idx_sortie_date_statut ON sortie (date_depart, statut);