const express = require('express');
const router = express.Router();
const db = require('../db/database');
const { Resend } = require('resend');

const resend = process.env.RESEND_API_KEY && process.env.RESEND_API_KEY !== 're_INSERISCI_CHIAVE_QUI' 
  ? new Resend(process.env.RESEND_API_KEY) 
  : null;

router.post('/iscriviti', async (req, res) => {
  const { nome, cognome, telefono, email, consenso } = req.body;

  if (!nome || !cognome || !telefono || !email || !consenso) {
    return res.status(400).json({
      success: false,
      message: 'Tutti i campi sono obbligatori.'
    });
  }

  // Check if email exists
  const esistente = db.prepare('SELECT id FROM testers WHERE email = ?').get(email);
  if (esistente) {
    return res.status(409).json({
      success: false,
      message: 'Email già registrata.'
    });
  }

  // Insert into DB
  try {
    db.prepare(`
      INSERT INTO testers (nome, cognome, telefono, email, consenso)
      VALUES (?, ?, ?, ?, ?)
    `).run(nome, cognome, telefono, email, consenso ? 1 : 0);
  } catch (err) {
    return res.status(500).json({ success: false, message: 'Errore durante il salvataggio.' });
  }

  // Send Welcome Email
  try {
    if (resend) {
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: email,
        subject: 'Grazie per la tua candidatura — StoppAI',
        html: `
          <div style="font-family: sans-serif;max-width:560px;margin:0 auto;padding:2rem">
            <h2 style="color:#c8a96e">Ciao ${nome},</h2>
            <p>abbiamo ricevuto la tua candidatura per partecipare ai test di StoppAI.</p>
            <p>Stiamo valutando la tua richiesta e ti contatteremo a breve con tutti i dettagli.</p>
            <p>Se completerai tutte le fasi dei test, avrai diritto a <strong>1 anno di abbonamento Shield del valore di €59,88</strong> completamente gratuito.</p>
            <br>
            <p>A presto,<br>Il team StoppAI<br>
            <a href="mailto:info@internetfullservice.it">info@internetfullservice.it</a></p>
          </div>
        `
      });
    } else {
      console.log('Resend non configurata. Salto invio email.');
    }
  } catch (err) {
    console.error('Errore invio email:', err.message);
    // Non blocchiamo la risposta se l'email fallisce (il tester è comunque nel DB)
  }

  res.json({
    success: true,
    message: 'Candidatura inviata! Ti contatteremo presto.'
  });
});

// GET /api/tester/piano/:email — l'app chiede che piano ha
router.get('/piano/:email', (req, res) => {
  const email = decodeURIComponent(req.params.email);
  const tester = db.prepare(
    "SELECT piano, stato FROM testers WHERE email = ?"
  ).get(email);

  if (!tester) {
    return res.json({ piano: 'free', stato: 'non_registrato' });
  }
  if (tester.stato !== 'accettato') {
    return res.json({ piano: 'free', stato: tester.stato });
  }
  res.json({ piano: tester.piano, stato: tester.stato });
});

// GET /api/tester/:id/messaggi — chat messaggi per l'app
router.get('/:id/messaggi', (req, res) => {
  const msgs = db.prepare(
    'SELECT * FROM messaggi_chat WHERE tester_id = ? ORDER BY timestamp ASC'
  ).all(req.params.id);
  res.json(msgs);
});

// POST /api/tester/:id/messaggi — tester invia messaggio
router.post('/:id/messaggi', (req, res) => {
  const { testo, mittente } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  db.prepare(
    'INSERT INTO messaggi_chat (tester_id, mittente, testo) VALUES (?, ?, ?)'
  ).run(req.params.id, mittente || 'tester', testo.trim());
  res.json({ success: true });
});

// POST /api/tester/:id/messaggi/img — tester invia immagine
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const uploadDir = path.join(__dirname, '..', 'uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `chat_${Date.now()}_${Math.random().toString(36).slice(2,8)}${ext}`);
  }
});
const uploadTester = multer({ storage, limits: { fileSize: 5 * 1024 * 1024 } });

router.post('/:id/messaggi/img', uploadTester.single('immagine'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Nessun file' });
  const imgPath = `/uploads/${req.file.filename}`;
  const testo = req.body.testo || '';
  db.prepare(
    'INSERT INTO messaggi_chat (tester_id, mittente, testo, immagine) VALUES (?, ?, ?, ?)'
  ).run(req.params.id, 'tester', testo, imgPath);
  res.json({ success: true, immagine: imgPath });
});

// POST /api/tester/sync — l'app invia device info + statistiche
router.post('/sync', (req, res) => {
  const { tester_id, device, stats, batteria } = req.body;
  if (!tester_id) return res.status(400).json({ error: 'tester_id mancante' });

  const tester = db.prepare('SELECT id FROM testers WHERE id = ?').get(tester_id);
  if (!tester) return res.status(404).json({ error: 'Tester non trovato' });

  // Aggiorna info device nella tabella testers
  if (device) {
    db.prepare('UPDATE testers SET modello_telefono = ?, versione_app = ? WHERE id = ?')
      .run(device.modello || null, device.versione_app || null, tester.id);
  }

  // Upsert statistiche
  if (stats) {
    const existing = db.prepare('SELECT id FROM tester_stats WHERE tester_id = ?').get(tester.id);
    if (existing) {
      db.prepare(`UPDATE tester_stats SET
        modello_telefono = ?, versione_android = ?, versione_app = ?,
        chiamate_totali = ?, chiamate_oggi = ?,
        conosciuti_non_risposti = ?,
        sconosciuti_mobile_non_risposti = ?, sconosciuti_mobile_sms = ?,
        sconosciuti_mobile_segreteria = ?, sconosciuti_mobile_msg_lasciato = ?,
        sconosciuti_mobile_msg_non_lasciato = ?,
        sconosciuti_fissi_non_risposti = ?, sconosciuti_fissi_segreteria = ?,
        sconosciuti_fissi_msg_lasciato = ?, sconosciuti_fissi_msg_non_lasciato = ?,
        privati_non_risposti = ?, privati_segreteria = ?,
        privati_msg_lasciato = ?, privati_msg_non_lasciato = ?,
        ultimo_sync = datetime('now')
        WHERE tester_id = ?`).run(
        device?.modello || null, device?.versione_android || null, device?.versione_app || null,
        stats.chiamate_totali || 0, stats.chiamate_oggi || 0,
        stats.conosciuti_non_risposti || 0,
        stats.sconosciuti_mobile_non_risposti || 0, stats.sconosciuti_mobile_sms || 0,
        stats.sconosciuti_mobile_segreteria || 0, stats.sconosciuti_mobile_msg_lasciato || 0,
        stats.sconosciuti_mobile_msg_non_lasciato || 0,
        stats.sconosciuti_fissi_non_risposti || 0, stats.sconosciuti_fissi_segreteria || 0,
        stats.sconosciuti_fissi_msg_lasciato || 0, stats.sconosciuti_fissi_msg_non_lasciato || 0,
        stats.privati_non_risposti || 0, stats.privati_segreteria || 0,
        stats.privati_msg_lasciato || 0, stats.privati_msg_non_lasciato || 0,
        tester.id
      );
    } else {
      db.prepare(`INSERT INTO tester_stats (
        tester_id, modello_telefono, versione_android, versione_app,
        chiamate_totali, chiamate_oggi,
        conosciuti_non_risposti,
        sconosciuti_mobile_non_risposti, sconosciuti_mobile_sms,
        sconosciuti_mobile_segreteria, sconosciuti_mobile_msg_lasciato,
        sconosciuti_mobile_msg_non_lasciato,
        sconosciuti_fissi_non_risposti, sconosciuti_fissi_segreteria,
        sconosciuti_fissi_msg_lasciato, sconosciuti_fissi_msg_non_lasciato,
        privati_non_risposti, privati_segreteria,
        privati_msg_lasciato, privati_msg_non_lasciato
      ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`).run(
        tester.id, device?.modello || null, device?.versione_android || null, device?.versione_app || null,
        stats.chiamate_totali || 0, stats.chiamate_oggi || 0,
        stats.conosciuti_non_risposti || 0,
        stats.sconosciuti_mobile_non_risposti || 0, stats.sconosciuti_mobile_sms || 0,
        stats.sconosciuti_mobile_segreteria || 0, stats.sconosciuti_mobile_msg_lasciato || 0,
        stats.sconosciuti_mobile_msg_non_lasciato || 0,
        stats.sconosciuti_fissi_non_risposti || 0, stats.sconosciuti_fissi_segreteria || 0,
        stats.sconosciuti_fissi_msg_lasciato || 0, stats.sconosciuti_fissi_msg_non_lasciato || 0,
        stats.privati_non_risposti || 0, stats.privati_segreteria || 0,
        stats.privati_msg_lasciato || 0, stats.privati_msg_non_lasciato || 0
      );
    }
  }

  // Log batteria
  if (batteria && batteria.livello >= 0) {
    db.prepare(
      'INSERT INTO batteria_log (tester_id, livello, in_carica, temperatura) VALUES (?, ?, ?, ?)'
    ).run(tester.id, batteria.livello, batteria.in_carica ? 1 : 0, batteria.temperatura || null);
  }

  res.json({ success: true });
});

// POST /api/tester/aria — whisper_worker salva messaggio ARIA dopo trascrizione
router.post('/aria', (req, res) => {
  const { caller_number, caller_name, wav_filename, trascrizione, durata_secondi, dimensione_kb } = req.body;
  if (!caller_number) return res.status(400).json({ error: 'caller_number obbligatorio' });

  // Cerca il tester associato (per ora match opzionale)
  const tester = db.prepare(
    "SELECT id FROM testers WHERE telefono LIKE ? LIMIT 1"
  ).get(`%${caller_number.slice(-10)}%`);

  // Cerca nome contatto gia' noto da invii precedenti dell'app
  let nome = caller_name || null;
  if (!nome) {
    const noto = db.prepare(
      "SELECT caller_name FROM aria_messaggi WHERE caller_number LIKE ? AND caller_name IS NOT NULL AND caller_name != '' ORDER BY id DESC LIMIT 1"
    ).get(`%${caller_number.slice(-10)}%`);
    if (noto) nome = noto.caller_name;
  }

  db.prepare(`
    INSERT INTO aria_messaggi (tester_id, caller_number, caller_name, wav_filename, trascrizione, durata_secondi, dimensione_kb)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(
    tester?.id || null,
    caller_number,
    nome,
    wav_filename || null,
    trascrizione || null,
    durata_secondi || null,
    dimensione_kb || null
  );

  res.json({ success: true });
});

// POST /api/tester/caller-name — l'app invia associazione numero-nome dalla rubrica
router.post('/caller-name', (req, res) => {
  const { caller_number, caller_name } = req.body;
  if (!caller_number || !caller_name) {
    return res.status(400).json({ error: 'caller_number e caller_name obbligatori' });
  }

  // Aggiorna i messaggi ARIA esistenti senza nome per quel numero
  db.prepare(
    "UPDATE aria_messaggi SET caller_name = ? WHERE caller_number LIKE ? AND (caller_name IS NULL OR caller_name = '')"
  ).run(caller_name, `%${caller_number.slice(-10)}%`);

  res.json({ success: true });
});

// ============================================
// ARIA CONFIG — scelta preset / custom per tester
// ============================================

const PRESET_VALIDI = ['uomo_1','uomo_2','uomo_3','uomo_4','donna_1','donna_2','donna_3','donna_4'];

// GET /api/tester/:id/aria-config — l'app legge la config corrente
router.get('/:id/aria-config', (req, res) => {
  const cfg = db.prepare('SELECT * FROM aria_config WHERE tester_id = ?').get(req.params.id);
  if (!cfg) {
    return res.json({
      tipo_messaggio: 'base',
      preset_id: null,
      custom_wav_path: null,
      custom_sms_testo: null
    });
  }
  res.json(cfg);
});

// POST /api/tester/:id/aria-config — salva scelta tipo messaggio (base/preset/custom)
router.post('/:id/aria-config', (req, res) => {
  const { tipo_messaggio, preset_id, custom_sms_testo } = req.body;
  if (!['base','preset','custom'].includes(tipo_messaggio)) {
    return res.status(400).json({ error: 'tipo_messaggio non valido' });
  }
  if (tipo_messaggio === 'preset' && !PRESET_VALIDI.includes(preset_id)) {
    return res.status(400).json({ error: 'preset_id non valido' });
  }

  const existing = db.prepare('SELECT id FROM aria_config WHERE tester_id = ?').get(req.params.id);
  if (existing) {
    db.prepare(`UPDATE aria_config SET tipo_messaggio = ?, preset_id = ?, custom_sms_testo = ?, updated_at = datetime('now') WHERE tester_id = ?`)
      .run(tipo_messaggio, preset_id || null, custom_sms_testo || null, req.params.id);
  } else {
    db.prepare(`INSERT INTO aria_config (tester_id, tipo_messaggio, preset_id, custom_sms_testo) VALUES (?, ?, ?, ?)`)
      .run(req.params.id, tipo_messaggio, preset_id || null, custom_sms_testo || null);
  }
  res.json({ success: true });
});

// POST /api/tester/:id/aria-config/custom-upload — upload WAV personalizzato
const multerAria = require('multer');
const pathAria = require('path');
const fsAria = require('fs');

const CUSTOM_DIR = '/opt/stoppai/asterisk/recordings/custom';
if (!fsAria.existsSync(CUSTOM_DIR)) {
  try { fsAria.mkdirSync(CUSTOM_DIR, { recursive: true }); } catch(e) {}
}

const customStorage = multerAria.diskStorage({
  destination: (req, file, cb) => cb(null, CUSTOM_DIR),
  filename: (req, file, cb) => {
    // Nome fisso per tester: sovrascrive automaticamente il vecchio
    cb(null, `custom_tester_${req.params.id}.wav`);
  }
});
const uploadCustom = multerAria({
  storage: customStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // max 5MB (circa 30s WAV)
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'audio/wav' || file.mimetype === 'audio/x-wav' || file.originalname.endsWith('.wav')) {
      cb(null, true);
    } else {
      cb(new Error('Solo file WAV accettati'));
    }
  }
});

router.post('/:id/aria-config/custom-upload', uploadCustom.single('wav'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Nessun file ricevuto' });

  const wavPath = `custom_tester_${req.params.id}.wav`;
  const fullPath = pathAria.join(CUSTOM_DIR, wavPath);

  // Converti il WAV uploadato a 8kHz mono 16bit (formato richiesto da Asterisk)
  const { execSync } = require('child_process');
  try {
    const tmpPath = fullPath + '.orig.wav';
    fsAria.renameSync(fullPath, tmpPath);
    execSync(`sox "${tmpPath}" -r 8000 -c 1 -b 16 "${fullPath}"`);
    fsAria.unlinkSync(tmpPath);
    try { execSync(`chown asterisk:asterisk "${fullPath}"`); } catch(e) {}
  } catch (e) {
    console.error('Errore conversione sox:', e.message);
    return res.status(500).json({ error: 'Errore conversione audio' });
  }

  // Aggiorna config: imposta tipo = 'custom', path e timestamp registrazione
  const existing = db.prepare('SELECT id FROM aria_config WHERE tester_id = ?').get(req.params.id);
  if (existing) {
    db.prepare(`UPDATE aria_config SET tipo_messaggio = 'custom', custom_wav_path = ?, custom_uploaded_at = datetime('now'), updated_at = datetime('now') WHERE tester_id = ?`)
      .run(wavPath, req.params.id);
  } else {
    db.prepare(`INSERT INTO aria_config (tester_id, tipo_messaggio, custom_wav_path, custom_uploaded_at) VALUES (?, 'custom', ?, datetime('now'))`)
      .run(req.params.id, wavPath);
  }
  res.json({ success: true, path: wavPath });
});

// ============================================
// ASTERISK ENDPOINT — dialplan chiama per sapere quale file usare
// ============================================

// GET /api/asterisk/message-for/:number
// Risponde con il path del file WAV da riprodurre per il tester chiamato
router.get('/asterisk-message/:number', (req, res) => {
  // Cerca tester per numero telefono
  const numero = req.params.number.replace(/[^0-9]/g, '');
  const tester = db.prepare(
    "SELECT id FROM testers WHERE telefono LIKE ? LIMIT 1"
  ).get(`%${numero.slice(-10)}%`);

  // Default: stringa vuota -> Asterisk fa fallback al messaggio standard 'benvenuto'
  let filePath = '';

  if (tester) {
    const cfg = db.prepare('SELECT * FROM aria_config WHERE tester_id = ?').get(tester.id);
    if (cfg) {
      if (cfg.tipo_messaggio === 'preset' && cfg.preset_id) {
        filePath = `/opt/stoppai/asterisk/recordings/preset/${cfg.preset_id}`;
      } else if (cfg.tipo_messaggio === 'custom' && cfg.custom_wav_path) {
        filePath = `/opt/stoppai/asterisk/recordings/custom/${cfg.custom_wav_path.replace('.wav','')}`;
      }
      // tipo_messaggio === 'base' -> filePath resta vuoto -> fallback dialplan
    }
  }

  // Asterisk Playback() vuole il path senza estensione
  res.type('text/plain').send(filePath);
});

module.exports = router;
