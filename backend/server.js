require('dotenv').config();
const express = require('express');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const path = require('path');
const db = require('./db/database');
const testerRoutes = require('./routes/tester');
const adminRoutes = require('./routes/admin');
const authRoutes = require('./routes/auth');

const app = express();
const PORT = process.env.PORT || 6002;

app.use(cors());
app.use(express.json());
app.use(cookieParser());

// Routes
app.use('/api/tester', testerRoutes);
app.use('/api/tester/auth', authRoutes);
app.use('/api/admin', adminRoutes);

// Serve uploads (immagini chat)
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Serve ARIA preset audio (8 voci uomo/donna)
app.use('/aria-preset', express.static('/opt/stoppai/asterisk/recordings/preset'));
// Serve ARIA custom audio (registrazioni personali tester)
app.use('/aria-custom', express.static('/opt/stoppai/asterisk/recordings/custom'));

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

app.listen(PORT, () => {
  console.log(
    'StoppAI backend attivo su porta ' + PORT
  );
});
