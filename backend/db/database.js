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

  CREATE TABLE IF NOT EXISTS batteria_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    livello INTEGER,
    in_carica INTEGER DEFAULT 0,
    temperatura REAL,
    timestamp TEXT DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS aria_messaggi (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER,
    caller_number TEXT NOT NULL,
    caller_name TEXT,
    wav_filename TEXT,
    trascrizione TEXT,
    durata_secondi INTEGER,
    dimensione_kb REAL,
    timestamp TEXT DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS aria_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL UNIQUE,
    tipo_messaggio TEXT NOT NULL DEFAULT 'base',
    preset_id TEXT,
    custom_wav_path TEXT,
    custom_sms_testo TEXT,
    updated_at TEXT DEFAULT (datetime('now'))
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

// Migrazione: aggiunge colonna custom_uploaded_at a aria_config
try {
  db.prepare("SELECT custom_uploaded_at FROM aria_config LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE aria_config ADD COLUMN custom_uploaded_at TEXT");
}

// Migrazione: aggiunge colonna accuracy_rating a aria_messaggi (valutazione trascrizione dal tester)
try {
  db.prepare("SELECT accuracy_rating FROM aria_messaggi LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE aria_messaggi ADD COLUMN accuracy_rating INTEGER");
}

// Migrazione: aggiunge colonna spam_score a aria_messaggi (0-100, analisi automatica testo)
try {
  db.prepare("SELECT spam_score FROM aria_messaggi LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE aria_messaggi ADD COLUMN spam_score INTEGER");
}

// Migrazione: aggiunge colonna fcm_token a testers (per push notification per-tester)
try {
  db.prepare("SELECT fcm_token FROM testers LIMIT 1").get();
} catch (e) {
  db.exec("ALTER TABLE testers ADD COLUMN fcm_token TEXT");
}

// Configurazione app (link Play Store, versione, note rilascio) — salvata da admin, usata in tutte le email
db.exec(`
  CREATE TABLE IF NOT EXISTS app_config (
    chiave TEXT PRIMARY KEY,
    valore TEXT,
    aggiornato_at TEXT DEFAULT (datetime('now'))
  );
`);
// Valori di default se non esistono
const defaults = {
  playstore_link: 'https://play.google.com/apps/internaltest/4701140799325601254',
  app_version: '5.8.0',
  release_notes: ''
};
for (const [k, v] of Object.entries(defaults)) {
  const existing = db.prepare('SELECT 1 FROM app_config WHERE chiave = ?').get(k);
  if (!existing) db.prepare('INSERT INTO app_config (chiave, valore) VALUES (?, ?)').run(k, v);
}

// Sessioni dashboard web tester (Step 4)
db.exec(`
  CREATE TABLE IF NOT EXISTS tester_sessions (
    token TEXT PRIMARY KEY,
    tester_id INTEGER NOT NULL,
    creato_at TEXT DEFAULT (datetime('now')),
    scade_at TEXT NOT NULL,
    FOREIGN KEY (tester_id) REFERENCES testers(id) ON DELETE CASCADE
  );
  CREATE INDEX IF NOT EXISTS idx_tester_sessions_tester ON tester_sessions(tester_id);
`);

// Checklist test per i tester (separata da admin_todos che è la TO-DO personale di Mario)
db.exec(`
  CREATE TABLE IF NOT EXISTS test_todos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    testo TEXT NOT NULL,
    completato INTEGER DEFAULT 0,
    timestamp TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (tester_id) REFERENCES testers(id) ON DELETE CASCADE
  );
  CREATE INDEX IF NOT EXISTS idx_test_todos_tester ON test_todos(tester_id);
`);

// Test TO-DO broadcast: lista unica che tutti i tester vedono.
// I numeri (id) sono stabili e non vengono riutilizzati anche dopo cancellazione (soft delete).
db.exec(`
  CREATE TABLE IF NOT EXISTS test_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    testo TEXT NOT NULL,
    cancellato INTEGER DEFAULT 0,
    creato_at TEXT DEFAULT (datetime('now')),
    aggiornato_at TEXT DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS test_items_done (
    tester_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    completato_at TEXT DEFAULT (datetime('now')),
    PRIMARY KEY (tester_id, item_id),
    FOREIGN KEY (tester_id) REFERENCES testers(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES test_items(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS test_items_comments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tester_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    testo TEXT NOT NULL,
    timestamp TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (tester_id) REFERENCES testers(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES test_items(id) ON DELETE CASCADE
  );
  CREATE INDEX IF NOT EXISTS idx_test_items_comments_item ON test_items_comments(item_id);
`);

module.exports = db;
