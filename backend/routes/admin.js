const express = require('express');
const router = express.Router();
const db = require('../db/database');

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
router.post('/testers/:id/stato', authAdmin, (req, res) => {
  const { stato } = req.body; // 'accettato' | 'rifiutato' | 'in_attesa'
  if (!['accettato', 'rifiutato', 'in_attesa'].includes(stato)) {
    return res.status(400).json({ error: 'Stato non valido' });
  }
  const dataAcc = stato === 'accettato' ? new Date().toISOString() : null;
  db.prepare('UPDATE testers SET stato = ?, data_accettazione = ? WHERE id = ?')
    .run(stato, dataAcc, req.params.id);
  res.json({ success: true });
});

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
