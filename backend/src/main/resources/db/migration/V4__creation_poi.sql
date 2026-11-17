-- V4__creation_poi.sql

CREATE TABLE poi (
         id             BIGSERIAL PRIMARY KEY,
         nom            VARCHAR(255)            NOT NULL,
         type           VARCHAR(30)             NOT NULL,
         localisation   GEOGRAPHY(Point, 4326)  NOT NULL,
         altitude_metres INT,
         source         VARCHAR(20)             NOT NULL DEFAULT 'OSM',
         osm_id         BIGINT,

         CONSTRAINT uk_poi_osm_id UNIQUE (osm_id),

         CONSTRAINT ck_poi_altitude
             CHECK (altitude_metres IS NULL OR altitude_metres BETWEEN -500 AND 5000)
);

CREATE INDEX idx_poi_localisation ON poi USING GIST (localisation);
CREATE INDEX idx_poi_type ON poi (type);

CREATE TABLE fermeture_saisonniere (
       poi_id               BIGINT  PRIMARY KEY,
       ouvert_toute_annee   BOOLEAN NOT NULL DEFAULT false,
       mois_ouverture_debut INT,
       mois_ouverture_fin   INT,

       CONSTRAINT fk_fermeture_poi
           FOREIGN KEY (poi_id) REFERENCES poi (id) ON DELETE CASCADE,

       CONSTRAINT ck_fermeture_mois_valides
           CHECK (
               (mois_ouverture_debut IS NULL OR mois_ouverture_debut BETWEEN 1 AND 12)
                   AND (mois_ouverture_fin   IS NULL OR mois_ouverture_fin   BETWEEN 1 AND 12)
               ),

       CONSTRAINT ck_fermeture_coherence
           CHECK (
               (ouvert_toute_annee = true
                   AND mois_ouverture_debut IS NULL
                   AND mois_ouverture_fin IS NULL)
                   OR
               (ouvert_toute_annee = false
                   AND mois_ouverture_debut IS NOT NULL
                   AND mois_ouverture_fin IS NOT NULL)
               )
);