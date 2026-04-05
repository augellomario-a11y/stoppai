const Database = require('better-sqlite3');
const path = require('path');

const db = new Database(
  path.join(__dirname, 'stoppai.db')
);

db.pragma('journal_mode = WAL');

db.exec(`
  CREATE TABLE IF NOT EXISTS testers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    cognome TEXT NOT NULL,
    telefono TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    consenso INTEGER DEFAULT 1,
    stato TEXT DEFAULT 'in_attesa',
    piano TEXT DEFAULT 'free',
    modello_telefono TEXT,
    versione_app TEXT,
    data_iscrizione TEXT DEFAULT (datetime('now')),
    data_accettazione TEXT,
    note TEXT
  );

  CREATE TABLE IF NOT EXISTS admin_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token TEXT NOT NULL UNIQUE,
    usato INTEGER DEFAULT 0,
    creato_at TEXT DEFAULT (datetime('now')),
    scade_at TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS messaggi_chat (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    mittente TEXT NOT NULL,
    testo TEXT NOT NULL,
    timestamp TEXT DEFAULT (datetime('now')),
    letto INTEGER DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS admin_notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    testo TEXT NOT NULL,
    timestamp TEXT DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS admin_todos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    testo TEXT NOT NULL,
    completato INTEGER DEFAULT 0,
    timestamp TEXT DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS auth_codes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL,
    codice TEXT NOT NULL,
    creato_at TEXT DEFAULT (datetime('now')),
    scade_at TEXT NOT NULL,
    usato INTEGER DEFAULT 0
  );

  CREATE TABLE IF NOT EXISTS piano_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    piano_precedente TEXT NOT NULL,
    piano_nuovo TEXT NOT NULL,
    cambiato_da TEXT DEFAULT 'admin',
    timestamp TEXT DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS tester_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    modello_telefono TEXT,
    versione_android TEXT,
    versione_app TEXT,
    chiamate_totali INTEGER DEFAULT 0,
    chiamate_oggi INTEGER DEFAULT 0,
    conosciuti_non_risposti INTEGER DEFAULT 0,
    sconosciuti_mobile_non_risposti INTEGER DEFAULT 0,
    sconosciuti_mobile_sms INTEGER DEFAULT 0,
    sconosciuti_mobile_segreteria INTEGER DEFAULT 0,
    sconosciuti_mobile_msg_lasciato INTEGER DEFAULT 0,
    sconosciuti_mobile_msg_non_lasciato INTEGER DEFAULT 0,
    sconosciuti_fissi_non_risposti INTEGER DEFAULT 0,
    sconosciuti_fissi_segreteria INTEGER DEFAULT 0,
    sconosciuti_fissi_msg_lasciato INTEGER DEFAULT 0,
    sconosciuti_fissi_msg_non_lasciato INTEGER DEFAULT 0,
    privati_non_risposti INTEGER DEFAULT 0,
    privati_segreteria INTEGER DEFAULT 0,
    privati_msg_lasciato INTEGER DEFAULT 0,
    privati_msg_non_lasciato INTEGER DEFAULT 0,
    ultimo_sync TEXT DEFAULT (datetime('now'))
  );
`);

// Migrazione: aggiunge colonna immagine a messaggi_chat se non esiste
try {
  db.prepare("SELECT immagine FROM messaggi_chat LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE messaggi_chat ADD COLUMN immagine TEXT");
}

// Migrazione: aggiunge colonna piano se non esiste
try {
  db.prepare("SELECT piano FROM testers LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE testers ADD COLUMN piano TEXT DEFAULT 'free'");
}

module.exports = db;
