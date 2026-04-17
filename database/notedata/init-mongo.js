// ╔════════════════════════════════════════════════════════════════════════════╗
// ║     MediLabo Solutions - Notes Service Database medilabo_notes        ║
// ║                                                                            ║
// ║  Author: Kardigué MAGASSA                                                  ║
// ║  Date: February 09, 2026                                                   ║
// ║  Version: 2.0                                                              ║
// ║  Database: MongoDB                                                         ║
// ║                                                                            ║
// ║  NOUVEAUTÉS V2:                                                            ║
// ║    - Ajout du champ "files" (array) pour les fichiers attachés             ║
// ║    - Ajout du champ "comments" (array) pour les commentaires               ║
// ╚════════════════════════════════════════════════════════════════════════════╝


db.createCollection('notes', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['noteUuid', 'patientUuid', 'practitionerUuid', 'content', 'createdAt'],
      properties: {
        // CHAMPS EXISTANTS V1
        noteUuid: {
          bsonType: 'string',
          description: 'UUID unique de la note - obligatoire'
        },
        patientUuid: {
          bsonType: 'string',
          description: 'UUID du patient (référence Patient Service) - obligatoire'
        },
        practitionerUuid: {
          bsonType: 'string',
          description: 'UUID du praticien qui a créé la note - obligatoire'
        },
        practitionerName: {
          bsonType: 'string',
          description: 'Nom du praticien (pour affichage)'
        },
        content: {
          bsonType: 'string',
          minLength: 1,
          description: 'Contenu de la note médicale - obligatoire'
        },
        active: {
          bsonType: 'bool',
          description: 'Note active (soft delete)'
        },
        createdAt: {
          bsonType: 'date',
          description: 'Date de création - obligatoire'
        },
        updatedAt: {
          bsonType: 'date',
          description: 'Date de dernière modification'
        },

        // NOUVEAUX CHAMPS V2 - Fichiers attachés
        files: {
          bsonType: 'array',
          description: 'Liste des fichiers attachés à la note',
          items: {
            bsonType: 'object',
            required: ['fileUuid', 'originalName', 'uploadedAt'],
            properties: {
              fileUuid: {
                bsonType: 'string',
                description: 'UUID unique du fichier'
              },
              originalName: {
                bsonType: 'string',
                description: 'Nom original du fichier'
              },
              storedName: {
                bsonType: 'string',
                description: 'Nom de stockage (UUID.extension)'
              },
              extension: {
                bsonType: 'string',
                description: 'Extension du fichier (pdf, jpg, etc.)'
              },
              contentType: {
                bsonType: 'string',
                description: 'Type MIME du fichier'
              },
              size: {
                bsonType: 'long',
                description: 'Taille en bytes'
              },
              formattedSize: {
                bsonType: 'string',
                description: 'Taille formatée (ex: 1.5 MB)'
              },
              uri: {
                bsonType: 'string',
                description: 'Chemin relatif du fichier stocké'
              },
              uploadedByUuid: {
                bsonType: 'string',
                description: 'UUID de l\'utilisateur qui a uploadé'
              },
              uploadedByName: {
                bsonType: 'string',
                description: 'Nom de l\'utilisateur qui a uploadé'
              },
              uploadedByRole: {
                bsonType: 'string',
                description: 'Rôle de l\'utilisateur (DOCTOR, NURSE, etc.)'
              },
              uploadedAt: {
                bsonType: 'date',
                description: 'Date d\'upload'
              }
            }
          }
        },

        // NOUVEAUX CHAMPS (V2) - Commentaires
        comments: {
          bsonType: 'array',
          description: 'Liste des commentaires sur la note',
          items: {
            bsonType: 'object',
            required: ['commentUuid', 'content', 'authorUuid', 'createdAt'],
            properties: {
              commentUuid: {
                bsonType: 'string',
                description: 'UUID unique du commentaire'
              },
              content: {
                bsonType: 'string',
                minLength: 1,
                description: 'Contenu du commentaire'
              },
              authorUuid: {
                bsonType: 'string',
                description: 'UUID de l\'auteur'
              },
              authorName: {
                bsonType: 'string',
                description: 'Nom de l\'auteur'
              },
              authorRole: {
                bsonType: 'string',
                description: 'Rôle de l\'auteur (DOCTOR, NURSE, etc.)'
              },
              authorImageUrl: {
                bsonType: 'string',
                description: 'URL de l\'avatar de l\'auteur'
              },
              edited: {
                bsonType: 'bool',
                description: 'Indique si le commentaire a été modifié'
              },
              createdAt: {
                bsonType: 'date',
                description: 'Date de création'
              },
              updatedAt: {
                bsonType: 'date',
                description: 'Date de dernière modification'
              }
            }
          }
        }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});


// CRÉATION DES INDEX

// Index unique sur noteUuid
db.notes.createIndex(
  { noteUuid: 1 },
  { 
    unique: true,
    name: 'idx_note_uuid_unique'
  }
);

// Index composé pour recherche par patient (trié par date décroissante)
db.notes.createIndex(
  { patientUuid: 1, createdAt: -1 },
  { 
    name: 'idx_patient_created_at',
    background: true
  }
);

// Index pour recherche par praticien
db.notes.createIndex(
  { practitionerUuid: 1 },
  { 
    name: 'idx_practitioner_uuid',
    background: true
  }
);

// Index pour soft delete (filter notes actives)
db.notes.createIndex(
  { active: 1 },
  { 
    name: 'idx_active',
    background: true
  }
);

// Index composé pour recherche par patient ET actives
db.notes.createIndex(
  { patientUuid: 1, active: 1, createdAt: -1 },
  { 
    name: 'idx_patient_active_created',
    background: true
  }
);

// NOUVEL INDEX: Recherche de fichiers par fileUuid (pour téléchargement rapide)
db.notes.createIndex(
  { 'files.fileUuid': 1 },
  { 
    name: 'idx_files_uuid',
    background: true,
    sparse: true
  }
);

// NOUVEL INDEX: Recherche de commentaires par auteur
db.notes.createIndex(
  { 'comments.authorUuid': 1 },
  { 
    name: 'idx_comments_author',
    background: true,
    sparse: true
  }
);