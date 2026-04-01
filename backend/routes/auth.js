const express = require('express');
const router = express.Router();
const db = require('../db/database');
const { Resend } = require('resend');

const resend = process.env.RESEND_API_KEY && process.env.RESEND_API_KEY !== 'inserisci_qui'
  ? new Resend(process.env.RESEND_API_KEY)
  : null;

function genera6cifre() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// POST /api/tester/auth/request — richiedi codice magic link
router.post('/request', async (req, res) => {
  const { email, fcm_token } = req.body;
  if (!email?.trim()) return res.status(400).json({ error: 'Email obbligatoria' });

  const emailNorm = email.trim().toLowerCase();

  // Verifica che il tester esista
  const tester = db.prepare('SELECT id, nome, stato FROM testers WHERE LOWER(email) = ?').get(emailNorm);
  if (!tester) {
    return res.status(404).json({ error: 'Email non registrata. Iscriviti prima dalla landing page.' });
  }

  // Genera codice 6 cifre
  const codice = genera6cifre();
  const scade = new Date(Date.now() + 10 * 60 * 1000).toISOString(); // 10 minuti

  // Invalida codici precedenti per questa email
  db.prepare("UPDATE auth_codes SET usato = 1 WHERE email = ? AND usato = 0").run(emailNorm);

  // Salva nuovo codice
  db.prepare('INSERT INTO auth_codes (email, codice, scade_at) VALUES (?, ?, ?)')
    .run(emailNorm, codice, scade);

  // 1. Manda email con codice
  if (resend) {
    try {
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: emailNorm,
        subject: `${codice} — Il tuo codice StoppAI`,
        html: emailCodice(tester.nome || 'Tester', codice)
      });
    } catch (err) {
      console.error('Errore invio email codice:', err.message);
    }
  }

  // 2. Manda notifica push FCM con il codice (se abbiamo il token)
  if (fcm_token) {
    try {
      // Salva/aggiorna token FCM per questo tester
      db.prepare('UPDATE testers SET modello_telefono = COALESCE(modello_telefono, ?) WHERE id = ?')
        .run('fcm_attivo', tester.id);

      // Usa il file fcm_token.txt sul server ARIA per inviare
      // Ma qui usiamo direttamente il token passato dall'app
      await inviaFcmCodice(fcm_token, codice);
    } catch (err) {
      console.error('Errore invio FCM codice:', err.message);
    }
  }

  res.json({ success: true, message: 'Codice inviato alla tua email' });
});

// POST /api/tester/auth/verify — verifica codice
router.post('/verify', (req, res) => {
  const { email, codice } = req.body;
  if (!email?.trim() || !codice?.trim()) {
    return res.status(400).json({ error: 'Email e codice obbligatori' });
  }

  const emailNorm = email.trim().toLowerCase();
  const now = new Date().toISOString();

  const record = db.prepare(
    "SELECT * FROM auth_codes WHERE email = ? AND codice = ? AND usato = 0 AND scade_at > ? ORDER BY creato_at DESC LIMIT 1"
  ).get(emailNorm, codice.trim(), now);

  if (!record) {
    return res.status(401).json({ error: 'Codice non valido o scaduto' });
  }

  // Segna come usato
  db.prepare('UPDATE auth_codes SET usato = 1 WHERE id = ?').run(record.id);

  // Recupera dati tester
  const tester = db.prepare('SELECT id, nome, cognome, email, stato, piano FROM testers WHERE LOWER(email) = ?').get(emailNorm);

  res.json({
    success: true,
    tester_id: tester.id,
    nome: tester.nome,
    cognome: tester.cognome,
    email: tester.email,
    stato: tester.stato,
    piano: tester.piano
  });
});

// Invio push FCM con codice tramite bridge locale (porta 3000)
async function inviaFcmCodice(fcmToken, codice) {
  try {
    const http = require('http');
    const data = JSON.stringify({ token: fcmToken, tipo: 'magic_code', codice: codice });
    const options = {
      hostname: '127.0.0.1',
      port: 3000,
      path: '/api/push',
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': data.length }
    };
    return new Promise((resolve) => {
      const req = http.request(options, (res) => { resolve(res.statusCode); });
      req.on('error', () => resolve(null));
      req.write(data);
      req.end();
    });
  } catch (e) {
    console.error('FCM push codice fallito:', e.message);
  }
}

function emailCodice(nome, codice) {
  return `
  <div style="font-family:'Helvetica Neue',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0a0a0f;color:#f5f3ee;border-radius:12px;overflow:hidden">
    <div style="background:linear-gradient(135deg,#0a0a0f 0%,#1a1a2e 100%);padding:32px;text-align:center;border-bottom:2px solid #c8a96e">
      <h1 style="margin:0;font-size:24px;letter-spacing:2px">STOPP<span style="color:#c8a96e">AI</span></h1>
    </div>
    <div style="padding:40px 32px;text-align:center">
      <p style="font-size:15px;color:#ddd;margin-bottom:24px">Ciao ${nome}, ecco il tuo codice di accesso:</p>
      <div style="background:#12121a;border:2px solid #c8a96e;border-radius:12px;padding:24px;margin:0 auto;max-width:280px">
        <div style="font-size:36px;font-weight:bold;letter-spacing:12px;color:#c8a96e;font-family:monospace">${codice}</div>
      </div>
      <p style="font-size:13px;color:#666;margin-top:20px">Il codice scade tra 10 minuti.<br>Se non hai richiesto tu questo codice, ignora questa email.</p>
    </div>
    <div style="background:#08080d;padding:20px 32px;text-align:center;border-top:1px solid #1e1e1e">
      <p style="margin:0;font-size:11px;color:#444">StoppAI — Internet Full Service</p>
    </div>
  </div>`;
}

module.exports = router;
