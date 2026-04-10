const express = require('express');
const router = express.Router();
const db = require('../db/database');

// Middleware: verifica cookie tester_session e carica req.tester
function authTester(req, res, next) {
  const token = req.cookies?.tester_session;
  if (!token) return res.status(401).json({ error: 'Non autenticato' });

  const now = new Date().toISOString();
  const session = db.prepare(
    "SELECT tester_id FROM tester_sessions WHERE token = ? AND scade_at > ?"
  ).get(token, now);

  if (!session) {
    res.clearCookie('tester_session');
    return res.status(401).json({ error: 'Sessione scaduta' });
  }

  const tester = db.prepare(
    'SELECT id, nome, cognome, email, telefono, stato, piano, data_iscrizione, data_accettazione FROM testers WHERE id = ?'
  ).get(session.tester_id);

  if (!tester) {
    res.clearCookie('tester_session');
    return res.status(401).json({ error: 'Tester non trovato' });
  }

  req.tester = tester;
  next();
}

// GET /api/dashboard/me — dati del tester loggato
router.get('/me', authTester, (req, res) => {
  res.json(req.tester);
});

// POST /api/dashboard/logout — elimina sessione e clear cookie
router.post('/logout', (req, res) => {
  const token = req.cookies?.tester_session;
  if (token) {
    db.prepare('DELETE FROM tester_sessions WHERE token = ?').run(token);
  }
  res.clearCookie('tester_session');
  res.json({ success: true });
});

// GET /api/dashboard/stats — statistiche personali dalla tabella tester_stats
router.get('/stats', authTester, (req, res) => {
  const s = db.prepare('SELECT * FROM tester_stats WHERE tester_id = ? ORDER BY ultimo_sync DESC LIMIT 1').get(req.tester.id);
  if (!s) {
    return res.json({
      chiamate_totali: 0, chiamate_oggi: 0,
      conosciuti_non_risposti: 0,
      sconosciuti_mobile_non_risposti: 0, sconosciuti_mobile_sms: 0,
      sconosciuti_mobile_segreteria: 0, sconosciuti_mobile_msg_lasciato: 0,
      sconosciuti_mobile_msg_non_lasciato: 0,
      sconosciuti_fissi_non_risposti: 0, sconosciuti_fissi_segreteria: 0,
      sconosciuti_fissi_msg_lasciato: 0, sconosciuti_fissi_msg_non_lasciato: 0,
      privati_non_risposti: 0, privati_segreteria: 0,
      privati_msg_lasciato: 0, privati_msg_non_lasciato: 0,
      versione_app: null, ultimo_sync: null
    });
  }
  res.json(s);
});

// GET /api/dashboard/test-items — lista broadcast (solo voci non cancellate) + flag fatto per il tester corrente
router.get('/test-items', authTester, (req, res) => {
  const rows = db.prepare(`
    SELECT
      i.id, i.testo, i.creato_at, i.aggiornato_at,
      CASE WHEN d.item_id IS NULL THEN 0 ELSE 1 END AS fatto,
      (SELECT COUNT(*) FROM test_items_comments WHERE item_id = i.id AND tester_id = ?) AS my_comments
    FROM test_items i
    LEFT JOIN test_items_done d ON d.item_id = i.id AND d.tester_id = ?
    WHERE i.cancellato = 0
    ORDER BY i.id ASC
  `).all(req.tester.id, req.tester.id);
  res.json(rows);
});

// POST /api/dashboard/test-items/:id/toggle — spunta/despunta la voce per il tester corrente
router.post('/test-items/:id/toggle', authTester, (req, res) => {
  const itemId = parseInt(req.params.id, 10);
  const item = db.prepare('SELECT id, cancellato FROM test_items WHERE id = ?').get(itemId);
  if (!item) return res.status(404).json({ error: 'Voce non trovata' });
  if (item.cancellato) return res.status(410).json({ error: 'Voce cancellata' });

  const existing = db.prepare('SELECT 1 FROM test_items_done WHERE tester_id = ? AND item_id = ?').get(req.tester.id, itemId);
  if (existing) {
    db.prepare('DELETE FROM test_items_done WHERE tester_id = ? AND item_id = ?').run(req.tester.id, itemId);
    return res.json({ success: true, fatto: 0 });
  }
  db.prepare('INSERT INTO test_items_done (tester_id, item_id) VALUES (?, ?)').run(req.tester.id, itemId);
  res.json({ success: true, fatto: 1 });
});

// GET /api/dashboard/test-items/:id/comments — SOLO i commenti del tester corrente (mai vedere quelli degli altri)
router.get('/test-items/:id/comments', authTester, (req, res) => {
  const rows = db.prepare(`
    SELECT c.id, c.testo, c.timestamp
    FROM test_items_comments c
    WHERE c.item_id = ? AND c.tester_id = ?
    ORDER BY c.timestamp ASC
  `).all(req.params.id, req.tester.id);
  res.json(rows.map(r => ({ ...r, mine: true })));
});

// POST /api/dashboard/test-items/:id/comments — aggiunge commento
router.post('/test-items/:id/comments', authTester, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  const item = db.prepare('SELECT id, cancellato FROM test_items WHERE id = ?').get(req.params.id);
  if (!item) return res.status(404).json({ error: 'Voce non trovata' });
  if (item.cancellato) return res.status(410).json({ error: 'Voce cancellata' });
  const result = db.prepare(
    'INSERT INTO test_items_comments (tester_id, item_id, testo) VALUES (?, ?, ?)'
  ).run(req.tester.id, req.params.id, testo.trim());
  res.json({ success: true, id: result.lastInsertRowid });
});

// DELETE /api/dashboard/test-items/comments/:commentId — il tester può cancellare i propri commenti
router.delete('/test-items/comments/:commentId', authTester, (req, res) => {
  const c = db.prepare('SELECT tester_id FROM test_items_comments WHERE id = ?').get(req.params.commentId);
  if (!c) return res.status(404).json({ error: 'Commento non trovato' });
  if (c.tester_id !== req.tester.id) return res.status(403).json({ error: 'Non autorizzato' });
  db.prepare('DELETE FROM test_items_comments WHERE id = ?').run(req.params.commentId);
  res.json({ success: true });
});

// GET /api/dashboard/chat — chat con admin (usa colonna `mittente`: 'tester' o 'admin')
router.get('/chat', authTester, (req, res) => {
  const messaggi = db.prepare(
    "SELECT id, mittente, testo, immagine, timestamp FROM messaggi_chat WHERE tester_id = ? ORDER BY timestamp ASC LIMIT 200"
  ).all(req.tester.id);
  res.json(messaggi);
});

// POST /api/dashboard/chat — invia messaggio all'admin
router.post('/chat', authTester, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  const result = db.prepare(
    "INSERT INTO messaggi_chat (tester_id, mittente, testo) VALUES (?, 'tester', ?)"
  ).run(req.tester.id, testo.trim());
  res.json({ success: true, id: result.lastInsertRowid });
});

// GET /api/dashboard/aria-messaggi — messaggi ricevuti dalla segreteria ARIA (ultimi 24h)
// NOTA: finché c'è un solo tester reale in fase beta, i messaggi con tester_id=NULL
// (non ancora associati dal worker) vengono comunque mostrati — stessa logica di admin.js
router.get('/aria-messaggi', authTester, (req, res) => {
  const msgs = db.prepare(`
    SELECT id, caller_number, caller_name, wav_filename, trascrizione, durata_secondi, dimensione_kb, timestamp, accuracy_rating, spam_score
    FROM aria_messaggi
    WHERE tester_id = ?
      AND timestamp > datetime('now','-24 hours')
    ORDER BY timestamp DESC
    LIMIT 200
  `).all(req.tester.id);
  res.json(msgs);
});

// POST /api/dashboard/aria-messaggi/:id/rating — il tester valuta la fedeltà della trascrizione
router.post('/aria-messaggi/:id/rating', authTester, (req, res) => {
  const { rating } = req.body;
  const valori = [20, 40, 60, 80, 100];
  if (!valori.includes(rating)) return res.status(400).json({ error: 'Rating non valido (20/40/60/80/100)' });
  const msg = db.prepare('SELECT id, tester_id FROM aria_messaggi WHERE id = ?').get(parseInt(req.params.id, 10));
  if (!msg) return res.status(404).json({ error: 'Messaggio non trovato' });
  if (msg.tester_id !== null && msg.tester_id !== req.tester.id) {
    return res.status(403).json({ error: 'Non autorizzato' });
  }
  db.prepare('UPDATE aria_messaggi SET accuracy_rating = ? WHERE id = ?').run(rating, msg.id);
  res.json({ success: true, rating });
});

// GET /api/dashboard/aria-download/:id — scarica il WAV come allegato
router.get('/aria-download/:id', authTester, (req, res) => {
  const path = require('path');
  const fs = require('fs');
  const msg = db.prepare('SELECT tester_id, wav_filename, caller_number, timestamp FROM aria_messaggi WHERE id = ?').get(parseInt(req.params.id, 10));
  if (!msg) return res.status(404).json({ error: 'Messaggio non trovato' });
  if (msg.tester_id !== null && msg.tester_id !== req.tester.id) {
    return res.status(403).json({ error: 'Non autorizzato' });
  }
  if (!msg.wav_filename) return res.status(404).json({ error: 'File audio non disponibile' });
  const safeName = path.basename(msg.wav_filename);
  const filePath = path.join('/opt/stoppai/asterisk/recordings', safeName);
  if (!fs.existsSync(filePath)) return res.status(404).json({ error: 'WAV non trovato' });
  const date = (msg.timestamp || '').replace(/[: ]/g, '-').slice(0, 16);
  const downloadName = `stoppai-${msg.caller_number || 'sconosciuto'}-${date}.wav`;
  res.download(filePath, downloadName, { headers: { 'Content-Type': 'audio/wav' } });
});

// DELETE /api/dashboard/aria-messaggi/:id — il tester cancella un messaggio ARIA
router.delete('/aria-messaggi/:id', authTester, (req, res) => {
  const msg = db.prepare('SELECT id, tester_id FROM aria_messaggi WHERE id = ?').get(parseInt(req.params.id, 10));
  if (!msg) return res.status(404).json({ error: 'Messaggio non trovato' });
  if (msg.tester_id !== req.tester.id) {
    return res.status(403).json({ error: 'Non autorizzato' });
  }
  db.prepare('DELETE FROM aria_messaggi WHERE id = ?').run(msg.id);
  res.json({ success: true });
});

// GET /api/dashboard/aria-audio/:id — serve il file WAV di un messaggio ARIA (autenticato, con supporto Range)
router.get('/aria-audio/:id', authTester, (req, res) => {
  const path = require('path');
  const fs = require('fs');
  const msg = db.prepare('SELECT tester_id, wav_filename FROM aria_messaggi WHERE id = ?').get(parseInt(req.params.id, 10));
  if (!msg) return res.status(404).json({ error: 'Messaggio non trovato' });
  if (msg.tester_id !== null && msg.tester_id !== req.tester.id) {
    return res.status(403).json({ error: 'Non autorizzato' });
  }
  if (!msg.wav_filename) return res.status(404).json({ error: 'File audio non disponibile' });
  const safeName = path.basename(msg.wav_filename);
  const filePath = path.join('/opt/stoppai/asterisk/recordings', safeName);
  if (!fs.existsSync(filePath)) return res.status(404).json({ error: 'File audio non trovato sul server' });

  res.sendFile(filePath, {
    headers: {
      'Content-Type': 'audio/wav',
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      'Accept-Ranges': 'bytes'
    }
  });
});

module.exports = router;
