const express = require('express');
const router = express.Router();
const path = require('path');
const db = require('../db/database');
const { Resend } = require('resend');

const resend = process.env.RESEND_API_KEY && process.env.RESEND_API_KEY !== 'inserisci_qui'
  ? new Resend(process.env.RESEND_API_KEY)
  : null;

// Middleware: verifica token admin
function authAdmin(req, res, next) {
  const token = req.cookies?.admin_token || req.headers['x-admin-token'];
  if (!token || token !== process.env.ADMIN_TOKEN_SECRET) {
    return res.status(401).json({ error: 'Non autorizzato' });
  }
  next();
}

// POST /api/admin/login — login con password O magic code
router.post('/login', (req, res) => {
  const { password, codice } = req.body;

  // Login classico con password (backward compatible)
  if (password && password === process.env.ADMIN_TOKEN_SECRET) {
    res.cookie('admin_token', process.env.ADMIN_TOKEN_SECRET, {
      httpOnly: true, maxAge: 24 * 60 * 60 * 1000
    });
    return res.json({ success: true });
  }

  // Login con magic code
  if (codice) {
    const now = new Date().toISOString();
    const adminEmail = (process.env.ADMIN_EMAIL || '').toLowerCase();
    const record = db.prepare(
      "SELECT * FROM auth_codes WHERE email = ? AND codice = ? AND usato = 0 AND scade_at > ? ORDER BY creato_at DESC LIMIT 1"
    ).get(adminEmail, codice.trim(), now);

    if (record) {
      db.prepare('UPDATE auth_codes SET usato = 1 WHERE id = ?').run(record.id);
      res.cookie('admin_token', process.env.ADMIN_TOKEN_SECRET, {
        httpOnly: true, maxAge: 24 * 60 * 60 * 1000
      });
      return res.json({ success: true });
    }
    return res.status(401).json({ error: 'Codice non valido o scaduto' });
  }

  return res.status(401).json({ error: 'Credenziali non valide' });
});

// POST /api/admin/request-code — richiedi magic code per admin
router.post('/request-code', async (req, res) => {
  const adminEmail = (process.env.ADMIN_EMAIL || '').toLowerCase();
  if (!adminEmail) return res.status(500).json({ error: 'ADMIN_EMAIL non configurata' });

  const { email } = req.body;
  if (!email || email.trim().toLowerCase() !== adminEmail) {
    return res.status(403).json({ error: 'Email non autorizzata' });
  }

  const codice = Math.floor(100000 + Math.random() * 900000).toString();
  const scade = new Date(Date.now() + 10 * 60 * 1000).toISOString();

  db.prepare("UPDATE auth_codes SET usato = 1 WHERE email = ? AND usato = 0").run(adminEmail);
  db.prepare('INSERT INTO auth_codes (email, codice, scade_at) VALUES (?, ?, ?)').run(adminEmail, codice, scade);

  if (resend) {
    try {
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: adminEmail,
        subject: `${codice} — Accesso Admin StoppAI`,
        html: `<div style="font-family:'Helvetica Neue',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0a0a0f;color:#f5f3ee;border-radius:12px;overflow:hidden">
          <div style="background:linear-gradient(135deg,#0a0a0f 0%,#1a1a2e 100%);padding:32px;text-align:center;border-bottom:2px solid #c8a96e">
            <h1 style="margin:0;font-size:24px;letter-spacing:2px">STOPP<span style="color:#c8a96e">AI</span> Admin</h1>
          </div>
          <div style="padding:40px 32px;text-align:center">
            <p style="font-size:15px;color:#ddd;margin-bottom:24px">Il tuo codice di accesso admin:</p>
            <div style="background:#12121a;border:2px solid #c8a96e;border-radius:12px;padding:24px;margin:0 auto;max-width:280px">
              <div style="font-size:36px;font-weight:bold;letter-spacing:12px;color:#c8a96e;font-family:monospace">${codice}</div>
            </div>
            <p style="font-size:13px;color:#666;margin-top:20px">Scade tra 10 minuti.</p>
          </div></div>`
      });
    } catch (err) {
      console.error('Errore invio email admin code:', err.message);
    }
  }

  res.json({ success: true, message: 'Codice inviato a ' + adminEmail });
});

// POST /api/admin/logout
router.post('/logout', (req, res) => {
  res.clearCookie('admin_token');
  res.json({ success: true });
});

// GET /api/admin/testers — lista completa tester
router.get('/testers', authAdmin, (req, res) => {
  const testers = db.prepare(`
    SELECT id, nome, cognome, telefono, email, stato, piano,
           modello_telefono, versione_app, data_iscrizione,
           data_accettazione, note
    FROM testers ORDER BY data_iscrizione DESC
  `).all();
  res.json(testers);
});

// GET /api/admin/stats — contatori rapidi
router.get('/stats', authAdmin, (req, res) => {
  const totale = db.prepare('SELECT COUNT(*) as n FROM testers').get().n;
  const inAttesa = db.prepare("SELECT COUNT(*) as n FROM testers WHERE stato = 'in_attesa'").get().n;
  const accettati = db.prepare("SELECT COUNT(*) as n FROM testers WHERE stato = 'accettato'").get().n;
  const shield = db.prepare("SELECT COUNT(*) as n FROM testers WHERE piano = 'shield'").get().n;
  const pro = db.prepare("SELECT COUNT(*) as n FROM testers WHERE piano = 'pro'").get().n;
  const free = db.prepare("SELECT COUNT(*) as n FROM testers WHERE piano = 'free'").get().n;
  res.json({ totale, inAttesa, accettati, piani: { shield, pro, free } });
});

// POST /api/admin/testers/:id/stato — accetta o rifiuta
router.post('/testers/:id/stato', authAdmin, async (req, res) => {
  const { stato } = req.body;
  if (!['accettato', 'rifiutato', 'in_attesa'].includes(stato)) {
    return res.status(400).json({ error: 'Stato non valido' });
  }
  const dataAcc = stato === 'accettato' ? new Date().toISOString() : null;
  db.prepare('UPDATE testers SET stato = ?, data_accettazione = ? WHERE id = ?')
    .run(stato, dataAcc, req.params.id);

  // Email di accettazione
  if (stato === 'accettato') {
    const tester = db.prepare('SELECT nome, email FROM testers WHERE id = ?').get(req.params.id);
    if (tester && resend) {
      try {
        await resend.emails.send({
          from: process.env.FROM_EMAIL,
          to: tester.email,
          subject: 'Benvenuto nel team StoppAI — Sei stato selezionato!',
          html: emailAccettazione(tester.nome)
        });
      } catch (err) {
        console.error('Errore invio email accettazione:', err.message);
      }
    }
  }

  res.json({ success: true });
});

function emailAccettazione(nome) {
  return `
  <div style="font-family:'Helvetica Neue',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0a0a0f;color:#f5f3ee;border-radius:12px;overflow:hidden">
    <div style="background:linear-gradient(135deg,#0a0a0f 0%,#1a1a2e 100%);padding:40px 32px;text-align:center;border-bottom:2px solid #c8a96e">
      <h1 style="margin:0;font-size:28px;letter-spacing:2px">STOPP<span style="color:#c8a96e">AI</span></h1>
      <p style="color:#888;margin-top:8px;font-size:13px">Il tuo bodyguard digitale</p>
    </div>
    <div style="padding:40px 32px">
      <h2 style="color:#c8a96e;margin-top:0;font-size:22px">Complimenti ${nome}!</h2>
      <p style="font-size:15px;line-height:1.7;color:#ddd">
        Sei stato selezionato per entrare nel <strong style="color:#c8a96e">programma tester di StoppAI</strong>.
        Fai parte di un gruppo esclusivo di persone che proveranno in anteprima
        il primo bodyguard digitale basato su intelligenza artificiale.
      </p>
      <div style="background:#12121a;border:1px solid #1e1e1e;border-radius:8px;padding:24px;margin:24px 0">
        <p style="margin:0 0 12px 0;font-size:14px;color:#c8a96e;font-weight:bold">Cosa succede adesso:</p>
        <p style="margin:0;font-size:14px;line-height:1.8;color:#ccc">
          1. Riceverai il link per scaricare l'app<br>
          2. Segui le istruzioni di configurazione<br>
          3. Inizia subito a testare tutte le funzionalita'
        </p>
      </div>
      <p style="font-size:15px;line-height:1.7;color:#ddd">
        Se completerai tutte le fasi dei test, avrai diritto a
        <strong style="color:#c8a96e">1 anno di abbonamento Shield</strong>
        del valore di &euro;59,88 — completamente gratuito.
      </p>
      <div style="text-align:center;margin:32px 0">
        <a href="#" style="display:inline-block;padding:14px 32px;background:#c8a96e;color:#0a0a0f;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;letter-spacing:1px">SCARICA L'APP</a>
      </div>
      <p style="font-size:12px;color:#666;text-align:center">
        Il link per il download sara' attivato a breve.<br>
        Ti invieremo un'email appena pronto.
      </p>
    </div>
    <div style="background:#08080d;padding:24px 32px;text-align:center;border-top:1px solid #1e1e1e">
      <p style="margin:0;font-size:12px;color:#555">
        StoppAI &mdash; Internet Full Service<br>
        <a href="mailto:info@internetfullservice.it" style="color:#c8a96e;text-decoration:none">info@internetfullservice.it</a>
      </p>
    </div>
  </div>`;
}

// POST /api/admin/testers/:id/piano — cambia piano + log
router.post('/testers/:id/piano', authAdmin, (req, res) => {
  const { piano } = req.body;
  if (!['free', 'pro', 'shield'].includes(piano)) {
    return res.status(400).json({ error: 'Piano non valido' });
  }
  const tester = db.prepare('SELECT piano FROM testers WHERE id = ?').get(req.params.id);
  const vecchio = tester?.piano || 'free';
  if (vecchio !== piano) {
    db.prepare('INSERT INTO piano_log (tester_id, piano_precedente, piano_nuovo) VALUES (?, ?, ?)')
      .run(req.params.id, vecchio, piano);
  }
  db.prepare('UPDATE testers SET piano = ? WHERE id = ?')
    .run(piano, req.params.id);
  res.json({ success: true });
});

// POST /api/admin/testers/:id/note — aggiorna note
router.post('/testers/:id/note', authAdmin, (req, res) => {
  const { note } = req.body;
  db.prepare('UPDATE testers SET note = ? WHERE id = ?')
    .run(note || '', req.params.id);
  res.json({ success: true });
});

// GET /api/admin/testers/:id — dettaglio singolo tester
router.get('/testers/:id', authAdmin, (req, res) => {
  const tester = db.prepare('SELECT * FROM testers WHERE id = ?').get(req.params.id);
  if (!tester) return res.status(404).json({ error: 'Tester non trovato' });
  res.json(tester);
});

// GET /api/admin/testers/:id/stats — statistiche dettagliate tester
router.get('/testers/:id/stats', authAdmin, (req, res) => {
  const stats = db.prepare('SELECT * FROM tester_stats WHERE tester_id = ?').get(req.params.id);
  res.json(stats || { message: 'Nessuna statistica sincronizzata' });
});

// DELETE /api/admin/testers/:id — elimina tester
router.delete('/testers/:id', authAdmin, (req, res) => {
  db.prepare('DELETE FROM testers WHERE id = ?').run(req.params.id);
  db.prepare('DELETE FROM messaggi_chat WHERE tester_id = ?').run(req.params.id);
  db.prepare('DELETE FROM admin_notes WHERE tester_id = ?').run(req.params.id);
  db.prepare('DELETE FROM admin_todos WHERE tester_id = ?').run(req.params.id);
  res.json({ success: true });
});

// --- CHAT ---
router.get('/testers/:id/messaggi', authAdmin, (req, res) => {
  const msgs = db.prepare(
    'SELECT * FROM messaggi_chat WHERE tester_id = ? ORDER BY timestamp ASC'
  ).all(req.params.id);
  res.json(msgs);
});

router.post('/testers/:id/messaggi', authAdmin, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  db.prepare(
    'INSERT INTO messaggi_chat (tester_id, mittente, testo) VALUES (?, ?, ?)'
  ).run(req.params.id, 'admin', testo.trim());
  res.json({ success: true });
});

// --- NOTE MARIO ---
router.get('/testers/:id/notes', authAdmin, (req, res) => {
  const notes = db.prepare(
    'SELECT * FROM admin_notes WHERE tester_id = ? ORDER BY timestamp DESC'
  ).all(req.params.id);
  res.json(notes);
});

router.post('/testers/:id/notes', authAdmin, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  db.prepare(
    'INSERT INTO admin_notes (tester_id, testo) VALUES (?, ?)'
  ).run(req.params.id, testo.trim());
  res.json({ success: true });
});

router.delete('/notes/:noteId', authAdmin, (req, res) => {
  db.prepare('DELETE FROM admin_notes WHERE id = ?').run(req.params.noteId);
  res.json({ success: true });
});

// --- TODO ---
router.get('/testers/:id/todos', authAdmin, (req, res) => {
  const todos = db.prepare(
    'SELECT * FROM admin_todos WHERE tester_id = ? ORDER BY timestamp ASC'
  ).all(req.params.id);
  res.json(todos);
});

router.post('/testers/:id/todos', authAdmin, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  db.prepare(
    'INSERT INTO admin_todos (tester_id, testo) VALUES (?, ?)'
  ).run(req.params.id, testo.trim());
  res.json({ success: true });
});

router.post('/todos/:todoId/toggle', authAdmin, (req, res) => {
  const todo = db.prepare('SELECT completato FROM admin_todos WHERE id = ?').get(req.params.todoId);
  if (!todo) return res.status(404).json({ error: 'Todo non trovato' });
  db.prepare('UPDATE admin_todos SET completato = ? WHERE id = ?')
    .run(todo.completato ? 0 : 1, req.params.todoId);
  res.json({ success: true });
});

router.delete('/todos/:todoId', authAdmin, (req, res) => {
  db.prepare('DELETE FROM admin_todos WHERE id = ?').run(req.params.todoId);
  res.json({ success: true });
});

// POST /api/admin/broadcast — messaggio a tutti i tester
router.post('/broadcast', authAdmin, (req, res) => {
  const { oggetto, testo } = req.body;
  if (!oggetto?.trim() || !testo?.trim()) {
    return res.status(400).json({ error: 'Oggetto e testo obbligatori' });
  }

  const testers = db.prepare("SELECT id FROM testers WHERE stato = 'accettato'").all();

  testers.forEach(t => {
    db.prepare(
      'INSERT INTO messaggi_chat (tester_id, mittente, testo) VALUES (?, ?, ?)'
    ).run(t.id, 'admin', `[${oggetto}] ${testo}`);
  });

  res.json({ success: true, count: testers.length });
});

// --- GESTIONE MESSAGGI CHAT (admin only) ---

// DELETE /api/admin/messaggi/:msgId — cancella singolo messaggio
router.delete('/messaggi/:msgId', authAdmin, (req, res) => {
  db.prepare('DELETE FROM messaggi_chat WHERE id = ?').run(req.params.msgId);
  res.json({ success: true });
});

// PUT /api/admin/messaggi/:msgId — modifica singolo messaggio
router.put('/messaggi/:msgId', authAdmin, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  db.prepare('UPDATE messaggi_chat SET testo = ? WHERE id = ?')
    .run(testo.trim(), req.params.msgId);
  res.json({ success: true });
});

// DELETE /api/admin/testers/:id/chat — cancella intera chat
router.delete('/testers/:id/chat', authAdmin, (req, res) => {
  db.prepare('DELETE FROM messaggi_chat WHERE tester_id = ?').run(req.params.id);
  res.json({ success: true });
});

// --- LOG CAMBI PIANO ---
router.get('/testers/:id/piano-log', authAdmin, (req, res) => {
  const log = db.prepare(
    'SELECT * FROM piano_log WHERE tester_id = ? ORDER BY timestamp DESC'
  ).all(req.params.id);
  res.json(log);
});

router.delete('/testers/:id/piano-log', authAdmin, (req, res) => {
  db.prepare('DELETE FROM piano_log WHERE tester_id = ?').run(req.params.id);
  res.json({ success: true });
});

// POST /api/admin/testers/:id/stats/reset — azzera singole o tutte le stats
router.post('/testers/:id/stats/reset', authAdmin, (req, res) => {
  const { campo } = req.body; // 'all' oppure nome campo specifico
  if (campo === 'all') {
    db.prepare('DELETE FROM tester_stats WHERE tester_id = ?').run(req.params.id);
  } else if (campo) {
    // Azzera solo quel campo
    const campiValidi = ['chiamate_totali','chiamate_oggi','conosciuti_non_risposti',
      'sconosciuti_mobile_non_risposti','sconosciuti_mobile_sms','sconosciuti_mobile_segreteria',
      'sconosciuti_mobile_msg_lasciato','sconosciuti_mobile_msg_non_lasciato',
      'sconosciuti_fissi_non_risposti','sconosciuti_fissi_segreteria',
      'sconosciuti_fissi_msg_lasciato','sconosciuti_fissi_msg_non_lasciato',
      'privati_non_risposti','privati_segreteria','privati_msg_lasciato','privati_msg_non_lasciato'];
    if (campiValidi.includes(campo)) {
      db.prepare(`UPDATE tester_stats SET ${campo} = 0 WHERE tester_id = ?`).run(req.params.id);
    }
  }
  res.json({ success: true });
});

// --- UPLOAD IMMAGINI CHAT ---
const multer = require('multer');
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
const upload = multer({ storage, limits: { fileSize: 5 * 1024 * 1024 } });

// POST /api/admin/testers/:id/messaggi/img — admin invia immagine
router.post('/testers/:id/messaggi/img', authAdmin, upload.single('immagine'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Nessun file' });
  const imgPath = `/uploads/${req.file.filename}`;
  const testo = req.body.testo || '';
  db.prepare(
    'INSERT INTO messaggi_chat (tester_id, mittente, testo, immagine) VALUES (?, ?, ?, ?)'
  ).run(req.params.id, 'admin', testo, imgPath);
  res.json({ success: true, immagine: imgPath });
});

module.exports = router;
