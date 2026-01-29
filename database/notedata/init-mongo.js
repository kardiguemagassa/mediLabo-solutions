// MediLabo Solutions - Notes Service Database Initialization
// Author: Kardigué MAGASSA
// Date: January 09, 2026
// Version: 1.0
// Database: MongoDB


// UTILISATION
// Dans MongoDB Shell (mongosh):
// use medilabo_notes
// load('/path/to/init-mongo.js')
//
// Ou directement depuis le terminal:
// mongosh medilabo_notes < init-mongo.js

// Se connecter à la base de données
db = db.getSiblingDB('medilabo_notes');

print('════════════════════════════════════════════════════════════════');
print('Initialisation de la base medilabo_notes...');
print('════════════════════════════════════════════════════════════════');


// SUPPRIMER LA COLLECTION SI ELLE EXISTE (Développement uniquement)
// ATTENTION: Décommenter cette ligne UNIQUEMENT en développement !
// db.notes.drop();
// print('Collection notes supprimée (si existante)');

// CRÉER LA COLLECTION AVEC VALIDATION DE SCHÉMA
db.createCollection('notes', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['noteUuid', 'patientUuid', 'practitionerUuid', 'content', 'createdAt'],
      properties: {
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
        content: {
          bsonType: 'string',
          minLength: 1,
          description: 'Contenu de la note médicale - obligatoire'
        },
        createdBy: {
          bsonType: 'string',
          description: 'Nom du praticien (pour affichage)'
        },
        createdAt: {
          bsonType: 'date',
          description: 'Date de création - obligatoire'
        },
        updatedAt: {
          bsonType: 'date',
          description: 'Date de dernière modification'
        },
        active: {
          bsonType: 'bool',
          description: 'Note active (soft delete)'
        }
      }
    }
  },
  validationLevel: 'strict',
  validationAction: 'error'
});

print('✅ Collection "notes" créée avec validation de schéma');

// CRÉER LES INDEX POUR OPTIMISER LES PERFORMANCES

// Index unique sur noteUuid
db.notes.createIndex(
  { noteUuid: 1 },
  { 
    unique: true,
    name: 'idx_note_uuid_unique'
  }
);
print('✅ Index unique créé : idx_note_uuid_unique');

// Index composé pour recherche par patient (trié par date décroissante)
db.notes.createIndex(
  { patientUuid: 1, createdAt: -1 },
  { 
    name: 'idx_patient_created_at',
    background: true
  }
);
print('✅ Index composé créé : idx_patient_created_at');

// Index pour recherche par praticien
db.notes.createIndex(
  { practitionerUuid: 1 },
  { 
    name: 'idx_practitioner_uuid',
    background: true
  }
);
print('✅ Index créé : idx_practitioner_uuid');

// Index pour soft delete (filter notes actives)
db.notes.createIndex(
  { active: 1 },
  { 
    name: 'idx_active',
    background: true
  }
);
print('✅ Index créé : idx_active');

// Index composé pour recherche par patient ET actives
db.notes.createIndex(
  { patientUuid: 1, active: 1, createdAt: -1 },
  { 
    name: 'idx_patient_active_created',
    background: true
  }
);
print('✅ Index composé créé : idx_patient_active_created');


// VÉRIFICATION DES INDEX
print('\n════════════════════════════════════════════════════════════════');
print('Liste des index créés :');
print('════════════════════════════════════════════════════════════════');
db.notes.getIndexes().forEach(function(index) {
  print('  - ' + index.name);
});

// STATISTIQUES
print('\n════════════════════════════════════════════════════════════════');
print('Statistiques de la collection :');
print('════════════════════════════════════════════════════════════════');
const stats = db.notes.stats();
print('Nombre de documents : ' + stats.count);
print('Taille de la collection : ' + (stats.size / 1024).toFixed(2) + ' KB');
print('Nombre d\'index : ' + stats.nindexes);

print('\n════════════════════════════════════════════════════════════════');
print('✅ Initialisation terminée avec succès !');
print('════════════════════════════════════════════════════════════════');


// DONNÉES DE TEST (Optionnel - Décommenter pour insérer)

/*
print('\n════════════════════════════════════════════════════════════════');
print('Insertion de données de test...');
print('════════════════════════════════════════════════════════════════');

// Note de test 1
db.notes.insertOne({
  noteUuid: 'note-test-001',
  patientUuid: 'patient-uuid-here',
  practitionerUuid: 'practitioner-uuid-here',
  content: 'Patient présente des symptômes de diabète. Taux de glucose élevé. Recommandation: suivi régulier et ajustement du traitement.',
  createdBy: 'Dr. Jean Dupont',
  createdAt: new Date(),
  updatedAt: new Date(),
  active: true
});

// Note de test 2
db.notes.insertOne({
  noteUuid: 'note-test-002',
  patientUuid: 'patient-uuid-here',
  practitionerUuid: 'practitioner-uuid-here',
  content: 'Consultation de suivi. Patient montre une amélioration. Poids en baisse. Continue le traitement actuel.',
  createdBy: 'Dr. Marie Martin',
  createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), // Il y a 7 jours
  updatedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
  active: true
});

print('✅ Données de test insérées : ' + db.notes.countDocuments() + ' notes');
print('════════════════════════════════════════════════════════════════');
*/