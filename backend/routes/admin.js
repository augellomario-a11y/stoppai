const express = require('express');
const router = express.Router();
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

// POST /api/admin/login — genera cookie di sessione
router.post('/login', (req, res) => {
  const { password } = req.body;
  if (password !== process.env.ADMIN_TOKEN_SECRET) {
    return res.status(401).json({ error: 'Password errata' });
  }
  res.cookie('admin_token', process.env.ADMIN_TOKEN_SECRET, {
    httpOnly: true,
    maxAge: 24 * 60 * 60 * 1000 // 24 ore
  });
  res.json({ success: true });
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
        Come ringraziamento per il tuo contributo, hai diritto a
        <strong style="color:#c8a96e">1 anno di abbonamento Shield</strong>
        del valore di &euro;119,88 — completamente gratuito.
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

// POST /api/admin/testers/:id/piano — cambia piano
router.post('/testers/:id/piano', authAdmin, (req, res) => {
  const { piano } = req.body; // 'free' | 'pro' | 'shield'
  if (!['free', 'pro', 'shield'].includes(piano)) {
    return res.status(400).json({ error: 'Piano non valido' });
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

// DELETE /api/admin/testers/:id — elimina tester
router.delete('/testers/:id', authAdmin, (req, res) => {
  db.prepare('DELETE FROM testers WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

module.exports = router;
