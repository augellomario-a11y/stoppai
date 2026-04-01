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
`);

// Migrazione: aggiunge colonna piano se non esiste
try {
  db.prepare("SELECT piano FROM testers LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE testers ADD COLUMN piano TEXT DEFAULT 'free'");
}

module.exports = db;
