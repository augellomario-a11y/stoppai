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

// GET /api/admin/testers/export.csv — export email tester accettati per Play Console
router.get('/testers/export.csv', authAdmin, (req, res) => {
  const rows = db.prepare(`
    SELECT email FROM testers
    WHERE stato = 'accettato' AND email LIKE '%@gmail.com'
    ORDER BY data_accettazione ASC
  `).all();
  const lines = rows.map(r => r.email.trim().toLowerCase());
  const csv = lines.join('\n') + '\n';
  const filename = `stoppai-testers-accettati-${new Date().toISOString().slice(0,10)}.csv`;
  res.setHeader('Content-Type', 'text/csv; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
  res.send(csv);
});

// GET /api/admin/aria-accuracy — statistiche aggregate sui rating trascrizione ARIA
router.get('/aria-accuracy', authAdmin, (req, res) => {
  const row = db.prepare(`
    SELECT
      COUNT(*) AS rated,
      AVG(accuracy_rating) AS avg_rating,
      SUM(CASE WHEN accuracy_rating=100 THEN 1 ELSE 0 END) AS c100,
      SUM(CASE WHEN accuracy_rating=80  THEN 1 ELSE 0 END) AS c80,
      SUM(CASE WHEN accuracy_rating=60  THEN 1 ELSE 0 END) AS c60,
      SUM(CASE WHEN accuracy_rating=40  THEN 1 ELSE 0 END) AS c40,
      SUM(CASE WHEN accuracy_rating=20  THEN 1 ELSE 0 END) AS c20
    FROM aria_messaggi
    WHERE accuracy_rating IS NOT NULL
  `).get();
  const totMsgs = db.prepare('SELECT COUNT(*) AS n FROM aria_messaggi').get().n;
  res.json({
    total_messages: totMsgs,
    rated: row.rated || 0,
    avg_rating: row.avg_rating ? Math.round(row.avg_rating) : null,
    distribution: { 100: row.c100||0, 80: row.c80||0, 60: row.c60||0, 40: row.c40||0, 20: row.c20||0 }
  });
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

// POST /api/admin/testers/:id/stato — accetta, rifiuta o rimette in attesa
router.post('/testers/:id/stato', authAdmin, async (req, res) => {
  const { stato } = req.body;
  if (!['accettato', 'rifiutato', 'in_attesa'].includes(stato)) {
    return res.status(400).json({ error: 'Stato non valido' });
  }

  // Leggi stato corrente prima di aggiornare (per non inviare email se non è cambiato)
  const tester = db.prepare('SELECT nome, email, stato FROM testers WHERE id = ?').get(req.params.id);
  if (!tester) return res.status(404).json({ error: 'Tester non trovato' });

  const statoVecchio = tester.stato;
  const cambiato = statoVecchio !== stato;

  const dataAcc = stato === 'accettato' ? new Date().toISOString() : null;
  db.prepare('UPDATE testers SET stato = ?, data_accettazione = ? WHERE id = ?')
    .run(stato, dataAcc, req.params.id);

  // Invia email solo se lo stato è davvero cambiato
  if (cambiato && resend) {
    try {
      if (stato === 'accettato') {
        await resend.emails.send({
          from: process.env.FROM_EMAIL,
          to: tester.email,
          subject: 'Benvenuto nel team StoppAI — Sei stato selezionato!',
          html: emailAccettazione(tester.nome)
        });
      } else if (stato === 'rifiutato') {
        await resend.emails.send({
          from: process.env.FROM_EMAIL,
          to: tester.email,
          subject: 'StoppAI — Aggiornamento sulla tua candidatura',
          html: emailRifiutato(tester.nome)
        });
      } else if (stato === 'in_attesa') {
        await resend.emails.send({
          from: process.env.FROM_EMAIL,
          to: tester.email,
          subject: 'StoppAI — La tua candidatura è in revisione',
          html: emailInAttesa(tester.nome)
        });
      }
    } catch (err) {
      console.error('Errore invio email cambio stato:', err.message);
    }
  }

  res.json({ success: true, cambiato });
});

// --- TEMPLATE EMAIL ---
// Legge il link Play Store dal DB (app_config) — aggiornabile da admin senza deploy
function getPlayStoreLink() {
  try {
    const row = db.prepare("SELECT valore FROM app_config WHERE chiave = 'playstore_link'").get();
    return row?.valore || 'https://play.google.com/apps/internaltest/4701140799325601254';
  } catch (e) {
    return 'https://play.google.com/apps/internaltest/4701140799325601254';
  }
}

function emailLayout(titolo, corpo) {
  return `
  <div style="font-family:'Helvetica Neue',Arial,sans-serif;max-width:600px;margin:0 auto;background:#0a0a0f;color:#f5f3ee;border-radius:12px;overflow:hidden">
    <div style="background:linear-gradient(135deg,#0a0a0f 0%,#1a1a2e 100%);padding:40px 32px;text-align:center;border-bottom:2px solid #c8a96e">
      <h1 style="margin:0;font-size:28px;letter-spacing:2px">STOPP<span style="color:#c8a96e">AI</span></h1>
      <p style="color:#888;margin-top:8px;font-size:13px">Il tuo bodyguard digitale</p>
    </div>
    <div style="padding:40px 32px">
      <h2 style="color:#c8a96e;margin-top:0;font-size:22px">${titolo}</h2>
      ${corpo}
    </div>
    <div style="background:#08080d;padding:24px 32px;text-align:center;border-top:1px solid #1e1e1e">
      <p style="margin:0;font-size:12px;color:#555">
        StoppAI &mdash; Internet Full Service<br>
        <a href="mailto:info@internetfullservice.it" style="color:#c8a96e;text-decoration:none">info@internetfullservice.it</a>
      </p>
    </div>
  </div>`;
}

function emailAccettazione(nome) {
  return emailLayout(
    `Complimenti ${nome}!`,
    `
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Sei stato selezionato per entrare nel <strong style="color:#c8a96e">programma tester di StoppAI</strong>.
      Fai parte di un gruppo esclusivo di persone che proveranno in anteprima
      il primo bodyguard digitale italiano basato su intelligenza artificiale.
    </p>
    <div style="background:#12121a;border:1px solid #1e1e1e;border-radius:8px;padding:24px;margin:24px 0">
      <p style="margin:0 0 12px 0;font-size:14px;color:#c8a96e;font-weight:bold">Come installare l'app:</p>
      <p style="margin:0;font-size:14px;line-height:1.8;color:#ccc">
        1. Apri il link qui sotto dal telefono Android dove hai attiva questa email Gmail<br>
        2. Clicca "Diventa tester" e accetta di partecipare<br>
        3. Clicca "Scarica da Google Play" e installa StoppAI<br>
        4. Apri l'app e segui il setup guidato
      </p>
    </div>
    <div style="text-align:center;margin:32px 0">
      <a href="${getPlayStoreLink()}" style="display:inline-block;padding:16px 36px;background:#c8a96e;color:#0a0a0f;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;letter-spacing:1px">🤖 INSTALLA STOPPAI</a>
    </div>
    <p style="font-size:13px;line-height:1.6;color:#888;text-align:center;margin:24px 0">
      Se il bottone non funziona, copia e incolla questo link nel browser del tuo telefono:<br>
      <a href="${getPlayStoreLink()}" style="color:#c8a96e;word-break:break-all">${getPlayStoreLink()}</a>
    </p>
    <p style="font-size:15px;line-height:1.7;color:#ddd;border-top:1px solid #1e1e1e;padding-top:20px;margin-top:24px">
      🎁 <strong style="color:#c8a96e">Bonus tester:</strong> se completerai tutte le fasi dei test, avrai diritto a
      <strong>1 anno di abbonamento SHIELD gratuito</strong>.
    </p>
    <p style="font-size:13px;color:#888;margin-top:20px">
      Per qualsiasi dubbio puoi rispondere direttamente a questa email.
    </p>
    `
  );
}

function emailRifiutato(nome) {
  return emailLayout(
    `Ciao ${nome}`,
    `
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Ti scriviamo per aggiornarti sullo stato della tua candidatura al programma tester di StoppAI.
    </p>
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Per il momento <strong style="color:#c8a96e">non potremo includerti nel gruppo attuale dei beta tester</strong>.
      Ti ringraziamo sinceramente per l'interesse che hai dimostrato.
    </p>
    <div style="background:#12121a;border:1px solid #1e1e1e;border-radius:8px;padding:20px;margin:24px 0">
      <p style="margin:0;font-size:14px;line-height:1.7;color:#ccc">
        Quando la versione pubblica di StoppAI sarà disponibile su Google Play, te lo comunicheremo
        in anteprima con uno sconto dedicato su uno dei piani premium.
      </p>
    </div>
    <p style="font-size:13px;color:#888;margin-top:20px">
      Per qualsiasi domanda puoi rispondere a questa email.
    </p>
    `
  );
}

function emailInAttesa(nome) {
  return emailLayout(
    `Ciao ${nome}`,
    `
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      La tua candidatura al programma tester di StoppAI è attualmente <strong style="color:#c8a96e">in revisione</strong>.
    </p>
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Stiamo valutando attentamente ogni richiesta per costruire un gruppo di test equilibrato.
      Ti contatteremo al più presto con l'esito finale.
    </p>
    <div style="background:#12121a;border:1px solid #1e1e1e;border-radius:8px;padding:20px;margin:24px 0">
      <p style="margin:0;font-size:14px;line-height:1.7;color:#ccc">
        Nel frattempo assicurati che l'email Gmail che hai indicato sia <strong>attiva e loggata</strong>
        sul telefono Android che userai per i test. Questo è un requisito di Google Play per ricevere
        l'invito alla versione beta.
      </p>
    </div>
    <p style="font-size:13px;color:#888;margin-top:20px">
      Per qualsiasi domanda puoi rispondere a questa email.
    </p>
    `
  );
}

function emailCambioPiano(nome, pianoVecchio, pianoNuovo) {
  const labels = { free: 'FREE', pro: 'PRO', shield: 'SHIELD' };
  const descrizioni = {
    free: 'accesso base con protezione SMS e storico chiamate ricevute',
    pro: 'ARIA segreteria AI, trascrizioni automatiche, filtro numeri esteri e mini CRM',
    shield: 'tutte le funzionalità PRO + messaggio personalizzato, 8 voci preset, protezione totale e ascolto messaggi su web'
  };
  return emailLayout(
    `Il tuo piano è ora ${labels[pianoNuovo]}`,
    `
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Ciao ${nome},<br>
      il tuo piano StoppAI è stato aggiornato da
      <strong style="color:#999">${labels[pianoVecchio]}</strong>
      a
      <strong style="color:#c8a96e">${labels[pianoNuovo]}</strong>.
    </p>
    <div style="background:#12121a;border:1px solid #c8a96e;border-radius:8px;padding:24px;margin:24px 0">
      <p style="margin:0 0 8px 0;font-size:14px;color:#c8a96e;font-weight:bold">Cosa include il piano ${labels[pianoNuovo]}:</p>
      <p style="margin:0;font-size:14px;line-height:1.7;color:#ccc">${descrizioni[pianoNuovo]}.</p>
    </div>
    <p style="font-size:14px;line-height:1.7;color:#ddd">
      La modifica è attiva <strong>immediatamente</strong>. Apri l'app StoppAI per vedere le nuove funzionalità disponibili.
    </p>
    <p style="font-size:13px;color:#888;margin-top:20px">
      Se non hai richiesto tu questa modifica o hai dubbi, rispondi a questa email e ci mettiamo in contatto.
    </p>
    `
  );
}

// POST /api/admin/testers/:id/piano — cambia piano + log
router.post('/testers/:id/piano', authAdmin, async (req, res) => {
  const { piano } = req.body;
  if (!['free', 'pro', 'shield'].includes(piano)) {
    return res.status(400).json({ error: 'Piano non valido' });
  }
  const tester = db.prepare('SELECT nome, email, piano FROM testers WHERE id = ?').get(req.params.id);
  if (!tester) return res.status(404).json({ error: 'Tester non trovato' });

  const vecchio = tester.piano || 'free';
  const cambiato = vecchio !== piano;

  if (cambiato) {
    db.prepare('INSERT INTO piano_log (tester_id, piano_precedente, piano_nuovo) VALUES (?, ?, ?)')
      .run(req.params.id, vecchio, piano);
  }
  db.prepare('UPDATE testers SET piano = ? WHERE id = ?')
    .run(piano, req.params.id);

  // Invia email solo se il piano è davvero cambiato
  if (cambiato && resend) {
    try {
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: tester.email,
        subject: `StoppAI — Il tuo piano è ora ${piano.toUpperCase()}`,
        html: emailCambioPiano(tester.nome, vecchio, piano)
      });
    } catch (err) {
      console.error('Errore invio email cambio piano:', err.message);
    }
  }

  res.json({ success: true, cambiato });
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

// --- TEST TO-DO BROADCAST (lista globale visibile a tutti i tester accettati) ---

// GET /api/admin/test-items — lista voci con stats aggregati
router.get('/test-items', authAdmin, (req, res) => {
  const items = db.prepare(`
    SELECT
      i.id, i.testo, i.cancellato, i.creato_at, i.aggiornato_at,
      (SELECT COUNT(*) FROM test_items_done WHERE item_id = i.id) AS done_count,
      (SELECT COUNT(*) FROM test_items_comments WHERE item_id = i.id) AS comment_count
    FROM test_items i
    ORDER BY i.id ASC
  `).all();
  const totTesters = db.prepare("SELECT COUNT(*) AS n FROM testers WHERE stato = 'accettato'").get().n;
  res.json({ items, total_testers: totTesters });
});

// POST /api/admin/test-items — aggiunge una nuova voce broadcast
router.post('/test-items', authAdmin, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  const result = db.prepare('INSERT INTO test_items (testo) VALUES (?)').run(testo.trim());
  res.json({ success: true, id: result.lastInsertRowid });
});

// PUT /api/admin/test-items/:id — modifica il testo mantenendo lo stesso numero
router.put('/test-items/:id', authAdmin, (req, res) => {
  const { testo } = req.body;
  if (!testo?.trim()) return res.status(400).json({ error: 'Testo vuoto' });
  const now = new Date().toISOString();
  const r = db.prepare('UPDATE test_items SET testo = ?, aggiornato_at = ? WHERE id = ?').run(testo.trim(), now, req.params.id);
  if (r.changes === 0) return res.status(404).json({ error: 'Voce non trovata' });
  res.json({ success: true });
});

// DELETE /api/admin/test-items/reset-all — cancella TUTTO: voci, progressi, commenti (PRIMA di :id!)
router.delete('/test-items/reset-all', authAdmin, (req, res) => {
  db.prepare('DELETE FROM test_items_comments').run();
  db.prepare('DELETE FROM test_items_done').run();
  db.prepare('DELETE FROM test_items').run();
  res.json({ success: true });
});

// DELETE /api/admin/test-items/:id — soft delete (il numero non viene riutilizzato)
router.delete('/test-items/:id', authAdmin, (req, res) => {
  const r = db.prepare('UPDATE test_items SET cancellato = 1 WHERE id = ?').run(req.params.id);
  if (r.changes === 0) return res.status(404).json({ error: 'Voce non trovata' });
  res.json({ success: true });
});

// POST /api/admin/test-items/:id/restore — ripristina una voce cancellata
router.post('/test-items/:id/restore', authAdmin, (req, res) => {
  db.prepare('UPDATE test_items SET cancellato = 0 WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

// GET /api/admin/test-items/:id/progress — chi ha completato questa voce
router.get('/test-items/:id/progress', authAdmin, (req, res) => {
  const rows = db.prepare(`
    SELECT d.tester_id, d.completato_at, t.nome, t.cognome, t.email
    FROM test_items_done d
    JOIN testers t ON t.id = d.tester_id
    WHERE d.item_id = ?
    ORDER BY d.completato_at DESC
  `).all(req.params.id);
  res.json(rows);
});

// GET /api/admin/test-items/:id/comments — commenti per una voce
router.get('/test-items/:id/comments', authAdmin, (req, res) => {
  const rows = db.prepare(`
    SELECT c.id, c.testo, c.timestamp, c.tester_id, t.nome, t.cognome, t.email
    FROM test_items_comments c
    JOIN testers t ON t.id = c.tester_id
    WHERE c.item_id = ?
    ORDER BY c.timestamp ASC
  `).all(req.params.id);
  res.json(rows);
});

// DELETE /api/admin/test-items/comments/:commentId — admin può cancellare un commento
router.delete('/test-items/comments/:commentId', authAdmin, (req, res) => {
  db.prepare('DELETE FROM test_items_comments WHERE id = ?').run(req.params.commentId);
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

// --- BATTERIA ---

// GET /api/admin/testers/:id/batteria — storico batteria tester
router.get('/testers/:id/batteria', authAdmin, (req, res) => {
  const log = db.prepare(
    'SELECT * FROM batteria_log WHERE tester_id = ? ORDER BY timestamp DESC LIMIT 100'
  ).all(req.params.id);
  res.json(log);
});

// DELETE /api/admin/testers/:id/batteria — pulisci storico batteria
router.delete('/testers/:id/batteria', authAdmin, (req, res) => {
  db.prepare('DELETE FROM batteria_log WHERE tester_id = ?').run(req.params.id);
  res.json({ success: true });
});

// --- MESSAGGI ARIA ---

// GET /api/admin/testers/:id/aria — lista messaggi ARIA per tester
router.get('/testers/:id/aria', authAdmin, (req, res) => {
  const tester = db.prepare('SELECT telefono FROM testers WHERE id = ?').get(req.params.id);
  if (!tester) return res.status(404).json({ error: 'Tester non trovato' });

  // Cerca messaggi per tester_id, oppure tutti se tester_id è null (fase test singolo tester)
  const messaggi = db.prepare(`
    SELECT * FROM aria_messaggi
    WHERE tester_id = ? OR tester_id IS NULL
    ORDER BY timestamp DESC
  `).all(req.params.id);

  res.json(messaggi);
});

// DELETE /api/admin/aria/:msgId — elimina singolo messaggio ARIA
router.delete('/aria/:msgId', authAdmin, (req, res) => {
  db.prepare('DELETE FROM aria_messaggi WHERE id = ?').run(req.params.msgId);
  res.json({ success: true });
});

// GET /api/admin/aria-audio/:msgId — serve il file WAV (solo admin) con supporto Range
router.get('/aria-audio/:msgId', authAdmin, (req, res) => {
  const path = require('path');
  const fs = require('fs');
  const msg = db.prepare('SELECT wav_filename FROM aria_messaggi WHERE id = ?').get(parseInt(req.params.msgId, 10));
  if (!msg || !msg.wav_filename) return res.status(404).json({ error: 'File non trovato' });
  const safeName = path.basename(msg.wav_filename);
  const filePath = path.join('/opt/stoppai/asterisk/recordings', safeName);
  if (!fs.existsSync(filePath)) return res.status(404).json({ error: 'WAV non trovato' });
  res.sendFile(filePath, {
    headers: {
      'Content-Type': 'audio/wav',
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      'Accept-Ranges': 'bytes'
    }
  });
});

// DELETE /api/admin/testers/:id/aria — cancella tutti i messaggi ARIA del tester
router.delete('/testers/:id/aria', authAdmin, (req, res) => {
  db.prepare('DELETE FROM aria_messaggi WHERE tester_id = ? OR tester_id IS NULL').run(req.params.id);
  res.json({ success: true });
});

// GET /api/admin/aria — tutti i messaggi ARIA (per dashboard)
router.get('/aria', authAdmin, (req, res) => {
  const messaggi = db.prepare(
    'SELECT * FROM aria_messaggi ORDER BY timestamp DESC LIMIT 100'
  ).all();
  res.json(messaggi);
});

// GET /api/admin/app-config — leggi configurazione app (link, versione, note)
router.get('/app-config', authAdmin, (req, res) => {
  const rows = db.prepare('SELECT chiave, valore FROM app_config').all();
  const config = {};
  rows.forEach(r => config[r.chiave] = r.valore);
  res.json(config);
});

// POST /api/admin/app-config — salva configurazione app
router.post('/app-config', authAdmin, (req, res) => {
  const { playstore_link, app_version, release_notes } = req.body;
  const now = new Date().toISOString();
  if (playstore_link !== undefined) {
    db.prepare('INSERT OR REPLACE INTO app_config (chiave, valore, aggiornato_at) VALUES (?, ?, ?)').run('playstore_link', playstore_link.trim(), now);
  }
  if (app_version !== undefined) {
    db.prepare('INSERT OR REPLACE INTO app_config (chiave, valore, aggiornato_at) VALUES (?, ?, ?)').run('app_version', app_version.trim(), now);
  }
  if (release_notes !== undefined) {
    db.prepare('INSERT OR REPLACE INTO app_config (chiave, valore, aggiornato_at) VALUES (?, ?, ?)').run('release_notes', release_notes.trim(), now);
  }
  res.json({ success: true });
});

// POST /api/admin/send-update — invia email aggiornamento app a tester selezionati
router.post('/send-update', authAdmin, async (req, res) => {
  const { tester_ids, link, versione, note } = req.body;
  if (!link?.trim()) return res.status(400).json({ error: 'Link obbligatorio' });
  if (!tester_ids || !tester_ids.length) return res.status(400).json({ error: 'Seleziona almeno un tester' });

  // Recupera i tester selezionati
  const placeholders = tester_ids.map(() => '?').join(',');
  const testers = db.prepare(
    `SELECT id, nome, email FROM testers WHERE id IN (${placeholders}) AND stato = 'accettato'`
  ).all(...tester_ids);

  if (!testers.length) return res.status(404).json({ error: 'Nessun tester accettato trovato' });

  let sent = 0;
  let errors = 0;

  for (const t of testers) {
    if (!resend || !t.email) { errors++; continue; }
    try {
      await resend.emails.send({
        from: process.env.FROM_EMAIL,
        to: t.email,
        subject: `StoppAI — Aggiornamento app disponibile${versione ? ' (v' + versione + ')' : ''}`,
        html: emailAggiornamentoApp(t.nome, link, versione, note)
      });
      sent++;
    } catch (err) {
      console.error(`[UPDATE-EMAIL] Errore invio a ${t.email}:`, err.message);
      errors++;
    }
  }

  res.json({ success: true, sent, errors, total: testers.length });
});

function emailAggiornamentoApp(nome, link, versione, note) {
  return emailLayout(
    `Aggiornamento disponibile!`,
    `
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Ciao ${nome},<br>
      è disponibile una nuova versione di StoppAI${versione ? ' (<strong style="color:#c8a96e">v' + versione + '</strong>)' : ''}.
    </p>
    ${note ? `
    <div style="background:#12121a;border:1px solid #1e1e1e;border-radius:8px;padding:20px;margin:20px 0">
      <p style="margin:0 0 8px 0;font-size:14px;color:#c8a96e;font-weight:bold">Novità in questa versione:</p>
      <p style="margin:0;font-size:14px;line-height:1.8;color:#ccc">${note.replace(/\n/g, '<br>')}</p>
    </div>` : ''}
    <p style="font-size:15px;line-height:1.7;color:#ddd">
      Per aggiornare, apri il link qui sotto dal tuo telefono Android:
    </p>
    <div style="text-align:center;margin:28px 0">
      <a href="${link}" style="display:inline-block;padding:16px 36px;background:#c8a96e;color:#0a0a0f;text-decoration:none;border-radius:6px;font-weight:bold;font-size:15px;letter-spacing:1px">🔄 AGGIORNA STOPPAI</a>
    </div>
    <p style="font-size:13px;line-height:1.6;color:#888;text-align:center">
      Se il bottone non funziona, copia e incolla questo link nel browser del telefono:<br>
      <a href="${link}" style="color:#c8a96e;word-break:break-all">${link}</a>
    </p>
    <p style="font-size:13px;color:#888;margin-top:20px">
      L'aggiornamento potrebbe richiedere qualche minuto per essere disponibile su Google Play.<br>
      Per qualsiasi dubbio rispondi a questa email.
    </p>
    `
  );
}

module.exports = router;
