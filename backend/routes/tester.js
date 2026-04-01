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
            <p>Come ringraziamento, se selezionato riceverai <strong>1 anno di abbonamento Shield del valore di €119,88</strong> completamente gratuito.</p>
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

module.exports = router;
