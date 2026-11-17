-- V3__creation_participation.sql

CREATE TABLE participation (
   id              BIGSERIAL PRIMARY KEY,
   sortie_id       BIGINT       NOT NULL,
   utilisateur_id  BIGINT       NOT NULL,
   statut          VARCHAR(20)  NOT NULL DEFAULT 'INSCRIT',
   inscrit_le      TIMESTAMPTZ  NOT NULL DEFAULT now(),

   CONSTRAINT fk_participation_sortie
       FOREIGN KEY (sortie_id) REFERENCES sortie (id) ON DELETE CASCADE,

   CONSTRAINT fk_participation_utilisateur
       FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id),

   CONSTRAINT uk_participation_sortie_utilisateur
       UNIQUE (sortie_id, utilisateur_id)
);

CREATE INDEX idx_participation_utilisateur ON participation (utilisateur_id);