require('dotenv').config();
const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const path = require('path');
const db = require('./db/database');
const testerRoutes = require('./routes/tester');
const adminRoutes = require('./routes/admin');
const authRoutes = require('./routes/auth');
const dashboardRoutes = require('./routes/dashboard');

const app = express();
const PORT = process.env.PORT || 6002;

app.use(cors());
app.use(express.json());
app.use(cookieParser());

// Routes
app.use('/api/tester', testerRoutes);
app.use('/api/tester/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/dashboard', dashboardRoutes);

// Serve uploads (immagini chat)
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Serve ARIA preset audio (8 voci uomo/donna)
app.use('/aria-preset', express.static('/opt/stoppai/asterisk/recordings/preset'));
// Serve ARIA custom audio (registrazioni personali tester)
app.use('/aria-custom', express.static('/opt/stoppai/asterisk/recordings/custom'));
// Serve ARIA recordings per player app (file WAV dei messaggi segreteria)
app.use('/aria-recordings', express.static('/opt/stoppai/asterisk/recordings'));

// Serve admin panel
app.use(express.static(path.join(__dirname, 'public')));

// Serve landing page
app.use(express.static(
  path.join(__dirname, '../landing')
));

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});

// DB test check
app.get('/api/db-test', (req, res) => {
  const count = db.prepare(
    'SELECT COUNT(*) as tot FROM testers'
  ).get();
  res.json({
    status: 'ok',
    testers: count.tot
  });
});

// Job: cleanup messaggi ARIA più vecchi di 24h (WAV + record DB)
function cleanupAriaMessages() {
  try {
    const fs = require('fs');
    const pathModule = require('path');
    const oldMsgs = db.prepare(
      "SELECT id, wav_filename FROM aria_messaggi WHERE timestamp <= datetime('now','-24 hours')"
    ).all();
    if (oldMsgs.length === 0) return;

    let deleted = 0;
    for (const m of oldMsgs) {
      if (m.wav_filename) {
        const safeName = pathModule.basename(m.wav_filename);
        const filePath = pathModule.join('/opt/stoppai/asterisk/recordings', safeName);
        try { if (fs.existsSync(filePath)) fs.unlinkSync(filePath); } catch (e) {
          console.error('[cleanup] errore unlink', filePath, e.message);
        }
      }
      db.prepare('DELETE FROM aria_messaggi WHERE id = ?').run(m.id);
      deleted++;
    }
    console.log(`[cleanup] cancellati ${deleted} messaggi ARIA > 24h`);
  } catch (e) {
    console.error('[cleanup] errore:', e.message);
  }
}
// Eseguilo subito all'avvio e poi ogni ora
setTimeout(cleanupAriaMessages, 5000);
setInterval(cleanupAriaMessages, 60 * 60 * 1000);

app.listen(PORT, () => {
  console.log(
    'StoppAI backend attivo su porta ' + PORT
  );
});
