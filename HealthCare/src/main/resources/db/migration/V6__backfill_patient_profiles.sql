ALTER TABLE patient
    MODIFY COLUMN nom VARCHAR(200) NULL,
    MODIFY COLUMN prenom VARCHAR(200) NULL;

INSERT INTO patient (id, nom, prenom, telephone, date_naissance)
SELECT u.id, NULL, NULL, NULL, NULL
FROM `user` u
LEFT JOIN patient p ON p.id = u.id
WHERE u.role = 'PATIENT'
  AND p.id IS NULL;
