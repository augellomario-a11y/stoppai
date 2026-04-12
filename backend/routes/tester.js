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

  // Normalizza email (lowercase + trim) e valida formato Gmail
  const emailNorm = String(email).trim().toLowerCase();
  // regex: username gmail valido + dominio @gmail.com fisso
  if (!/^[a-z0-9._+\-]+@gmail\.com$/.test(emailNorm)) {
    return res.status(400).json({
      success: false,
      message: 'Sono accettati solo indirizzi @gmail.com. Il programma beta è distribuito tramite Google Play Internal Testing.'
    });
  }

  // Check if email exists (case-insensitive: confronto su versione normalizzata)
  const esistente = db.prepare('SELECT id FROM testers WHERE LOWER(email) = ?').get(emailNorm);
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
    `).run(nome, cognome, telefono, emailNorm, consenso ? 1 : 0);
  } catch (err) {
    return res.status(500).json({ success: false, message: 'Errore durante il salvataggio.' });
  }

  // Send Welcome Email
  try {
    if (resend) {
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: emailNorm,
        subject: 'Grazie per la tua candidatura — StoppAI',
        html: `
          <div style="font-family: sans-serif;max-width:560px;margin:0 auto;padding:2rem">
            <h2 style="color:#c8a96e">Ciao ${nome},</h2>
            <p>abbiamo ricevuto la tua candidatura per partecipare ai test di StoppAI.</p>
            <p>Stiamo valutando la tua richiesta e ti contatteremo a breve con tutti i dettagli.</p>
            <p>Se completerai tutte le fasi dei test, avrai diritto a <strong>1 anno di abbonamento Shield</strong> completamente gratuito.</p>
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
  }

  // Notifica admin: nuovo tester iscritto
  try {
    if (resend) {
      const adminEmail = process.env.ADMIN_EMAIL || 'info@internetfullservice.it';
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: adminEmail,
        subject: '🆕 Nuova iscrizione beta — ' + nome + ' ' + cognome,
        html: `
          <div style="font-family:'Helvetica Neue',Arial,sans-serif;max-width:560px;margin:0 auto;background:#0a0a0f;color:#f5f3ee;border-radius:12px;overflow:hidden">
            <div style="background:#12121a;padding:24px;border-bottom:2px solid #c8a96e;text-align:center">
              <h2 style="margin:0;color:#c8a96e">Nuova iscrizione beta test</h2>
            </div>
            <div style="padding:24px">
              <table style="width:100%;font-size:14px;color:#ddd">
                <tr><td style="padding:8px 0;color:#888">Nome</td><td style="padding:8px 0;font-weight:bold">${nome} ${cognome}</td></tr>
                <tr><td style="padding:8px 0;color:#888">Email</td><td style="padding:8px 0"><a href="mailto:${emailNorm}" style="color:#c8a96e">${emailNorm}</a></td></tr>
                <tr><td style="padding:8px 0;color:#888">Telefono</td><td style="padding:8px 0">${telefono}</td></tr>
              </table>
              <div style="margin-top:20px;text-align:center">
                <a href="https://stoppai.it/accedi.html" style="display:inline-block;padding:12px 28px;background:#c8a96e;color:#0a0a0f;text-decoration:none;border-radius:6px;font-weight:bold">Apri CRM Admin →</a>
              </div>
            </div>
          </div>
        `
      });
    }
  } catch (err) {
    console.error('Errore notifica admin:', err.message);
  }

  res.json({
    success: true,
    message: 'Candidatura inviata! Ti contatteremo presto.'
  });
});

// GET /api/tester/piano/:email — l'app chiede che piano ha (con check scadenza)
router.get('/piano/:email', (req, res) => {
  const email = decodeURIComponent(req.params.email);
  const tester = db.prepare(
    "SELECT piano, stato, piano_scadenza, is_admin FROM testers WHERE email = ?"
  ).get(email);

  if (!tester) {
    return res.json({ piano: 'free', stato: 'non_registrato' });
  }
  if (tester.stato !== 'accettato') {
    return res.json({ piano: 'free', stato: tester.stato });
  }

  // Verifica scadenza piano
  let piano = tester.piano || 'free';
  if (tester.piano_scadenza && piano !== 'free') {
    const scadenza = new Date(tester.piano_scadenza);
    if (scadenza < new Date()) {
      // Piano scaduto → downgrade a free
      db.prepare('UPDATE testers SET piano = ?, piano_scadenza = NULL WHERE email = ?').run('free', email);
      db.prepare('INSERT INTO piano_log (tester_id, piano_precedente, piano_nuovo) VALUES ((SELECT id FROM testers WHERE email = ?), ?, ?)').run(email, piano, 'free');
      console.log('[PIANO] Scaduto per %s: %s → free', email, piano);
      piano = 'free';
    }
  }

  res.json({ piano, stato: tester.stato, scadenza: tester.piano_scadenza || null, is_admin: tester.is_admin === 1 });
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

// POST /api/tester/fcm-token — l'app invia il token FCM per le push notification
router.post('/fcm-token', (req, res) => {
  const { token, email, tester_id } = req.body;
  if (!token) return res.status(400).json({ error: 'token mancante' });

  let id = tester_id;
  if (!id && email) {
    const t = db.prepare("SELECT id FROM testers WHERE LOWER(email) = ?").get(email.toLowerCase().trim());
    if (t) id = t.id;
  }
  if (id) {
    db.prepare('UPDATE testers SET fcm_token = ? WHERE id = ?').run(token, id);
    console.log(`[FCM] Token aggiornato per tester_id=${id}`);
    res.json({ success: true });
  } else {
    // Fallback: salva nel file globale (compatibilità con vecchio whisper_worker)
    try {
      require('fs').writeFileSync('/opt/stoppai/fcm_token.txt', token);
      console.log('[FCM] Token salvato in file globale (no tester match)');
    } catch (e) {}
    res.json({ success: true, warning: 'tester non trovato, salvato globale' });
  }
});

/**
 * Invia push FCM a un tester specifico tramite FCM Bridge (porta 3000)
 */
function inviaPushAria(testerId, numero, testo) {
  const tester = db.prepare('SELECT fcm_token FROM testers WHERE id = ?').get(testerId);
  if (!tester?.fcm_token) {
    console.log(`[PUSH] Nessun token FCM per tester_id=${testerId}`);
    return;
  }
  const http = require('http');
  const data = JSON.stringify({
    token: tester.fcm_token,
    tipo: 'aria_messaggio',
    numero: String(numero),
    testo: String(testo || '').substring(0, 200),
    timestamp: String(Math.floor(Date.now() / 1000))
  });
  const options = {
    hostname: '172.17.0.1',
    port: 3000,
    path: '/api/push',
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) },
    timeout: 5000
  };
  const req = http.request(options, (r) => {
    let body = '';
    r.on('data', c => body += c);
    r.on('end', () => console.log(`[PUSH] FCM Bridge: status=${r.statusCode} body=${body} tester_id=${testerId}`));
  });
  req.on('error', (e) => console.error(`[PUSH] Errore: ${e.message}`));
  req.write(data);
  req.end();
}

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
  const { caller_number, caller_name, wav_filename, trascrizione, durata_secondi, dimensione_kb, spam_score, rdnis } = req.body;
  if (!caller_number) return res.status(400).json({ error: 'caller_number obbligatorio' });

  // Cerca il tester destinatario via rdnis (numero che ha deviato la chiamata ad ARIA)
  // rdnis è il numero del TESTER, caller_number è il numero del CHIAMANTE
  let tester = null;
  if (rdnis) {
    const rdnisNorm = rdnis.replace(/^\+?39/, '').slice(-10);
    tester = db.prepare(
      "SELECT id FROM testers WHERE telefono LIKE ? LIMIT 1"
    ).get(`%${rdnisNorm}%`);
    if (tester) {
      console.log(`[ARIA] rdnis ${rdnis} → tester_id ${tester.id}`);
    } else {
      console.log(`[ARIA] rdnis ${rdnis} → nessun tester trovato, provo con caller`);
    }
  }
  // Fallback: cerca per caller_number (vecchio metodo, meno affidabile)
  if (!tester) {
    tester = db.prepare(
      "SELECT id FROM testers WHERE telefono LIKE ? LIMIT 1"
    ).get(`%${caller_number.slice(-10)}%`);
  }

  // Cerca nome contatto gia' noto da invii precedenti dell'app
  let nome = caller_name || null;
  if (!nome) {
    const noto = db.prepare(
      "SELECT caller_name FROM aria_messaggi WHERE caller_number LIKE ? AND caller_name IS NOT NULL AND caller_name != '' ORDER BY id DESC LIMIT 1"
    ).get(`%${caller_number.slice(-10)}%`);
    if (noto) nome = noto.caller_name;
  }

  db.prepare(`
    INSERT INTO aria_messaggi (tester_id, caller_number, caller_name, wav_filename, trascrizione, durata_secondi, dimensione_kb, spam_score)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    tester?.id || null,
    caller_number,
    nome,
    wav_filename || null,
    trascrizione || null,
    durata_secondi || null,
    dimensione_kb || null,
    spam_score ?? null
  );

  // Invia push notification al tester se abbiamo il tester_id
  if (tester?.id) {
    try { inviaPushAria(tester.id, caller_number, trascrizione); } catch (e) {
      console.error('[PUSH] Errore invio push ARIA:', e.message);
    }
  }

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

// POST /api/tester/upgrade — upgrade piano beta (automatico, senza pagamento)
router.post('/upgrade', async (req, res) => {
  const { tester_id, email, piano } = req.body;
  if (!piano || !['pro', 'shield'].includes(piano)) {
    return res.status(400).json({ error: 'Piano non valido' });
  }

  let id = tester_id;
  if (!id && email) {
    const t = db.prepare("SELECT id FROM testers WHERE LOWER(email) = ?").get(email.toLowerCase().trim());
    if (t) id = t.id;
  }
  if (!id) return res.status(404).json({ error: 'Tester non trovato' });

  const tester = db.prepare('SELECT nome, cognome, email, piano FROM testers WHERE id = ?').get(id);
  if (!tester) return res.status(404).json({ error: 'Tester non trovato' });

  const pianoVecchio = tester.piano || 'free';
  db.prepare('UPDATE testers SET piano = ? WHERE id = ?').run(piano, id);
  db.prepare('INSERT INTO piano_log (tester_id, piano_precedente, piano_nuovo) VALUES (?, ?, ?)').run(id, pianoVecchio, piano);

  console.log('[UPGRADE] tester_id=%d %s %s: %s → %s', id, tester.nome, tester.cognome, pianoVecchio, piano);

  // Email notifica admin
  if (resend) {
    try {
      const adminEmail = process.env.ADMIN_EMAIL || 'info@internetfullservice.it';
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: adminEmail,
        subject: '⬆️ Upgrade ' + piano.toUpperCase() + ' — ' + tester.nome + ' ' + tester.cognome,
        html: '<div style="font-family:sans-serif;padding:20px"><h2 style="color:#c8a96e">Upgrade piano beta</h2><p><strong>' + tester.nome + ' ' + tester.cognome + '</strong> (' + tester.email + ')</p><p>Da <strong>' + pianoVecchio.toUpperCase() + '</strong> a <strong>' + piano.toUpperCase() + '</strong></p></div>'
      });
    } catch (err) {
      console.error('[UPGRADE] Errore email admin:', err.message);
    }
  }

  res.json({ success: true, piano });
});

// POST /api/tester/spam-report — l'app segnala un numero come spam
router.post('/spam-report', (req, res) => {
  const { caller_number } = req.body;
  if (!caller_number) return res.status(400).json({ error: 'caller_number obbligatorio' });

  const norm = caller_number.replace(/[^0-9+]/g, '').replace(/^\+?39/, '').slice(-10);
  if (norm.length < 5) return res.status(400).json({ error: 'Numero non valido' });

  // Cerca se esiste già nella tabella spam
  const existing = db.prepare(
    "SELECT id, segnalazioni_spam FROM spam_numbers WHERE numero LIKE ? LIMIT 1"
  ).get('%' + norm + '%');

  if (existing) {
    db.prepare("UPDATE spam_numbers SET segnalazioni_spam = segnalazioni_spam + 1, ultima_segnalazione = datetime('now') WHERE id = ?")
      .run(existing.id);
  } else {
    db.prepare(
      "INSERT INTO spam_numbers (numero, segnalazioni_spam, prima_segnalazione, ultima_segnalazione) VALUES (?, 1, datetime('now'), datetime('now'))"
    ).run(caller_number);
  }

  console.log('[SPAM] Segnalato: %s (norm: %s)', caller_number, norm);
  res.json({ success: true });
});

// POST /api/tester/aria-rating — salva valutazione trascrizione ARIA
router.post('/aria-rating', (req, res) => {
  const { msg_id, rating } = req.body;
  if (!msg_id || !rating) return res.status(400).json({ error: 'msg_id e rating obbligatori' });
  if (![100, 80, 60, 40, 20].includes(rating)) return res.status(400).json({ error: 'Rating non valido' });

  try {
    const result = db.prepare('UPDATE aria_messaggi SET accuracy_rating = ? WHERE id = ?').run(rating, msg_id);
    console.log('[ARIA] Rating %d%% per msg_id=%d (changes: %d)', rating, msg_id, result.changes);
    res.json({ success: true, changes: result.changes });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// POST /api/tester/upgrade-click — tracking click su feature bloccate (lucchetti)
router.post('/upgrade-click', (req, res) => {
  const { tester_id, feature } = req.body;
  if (!tester_id || !feature) return res.status(400).json({ error: 'tester_id e feature obbligatori' });

  try {
    db.prepare("INSERT INTO upgrade_clicks (tester_id, feature) VALUES (?, ?)").run(tester_id, feature);
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
