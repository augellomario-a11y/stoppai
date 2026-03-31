require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const db = require('./db/database');
const testerRoutes = require('./routes/tester');

const app = express();
const PORT = process.env.PORT || 6002;

app.use(cors());
app.use(express.json());

// Routes
app.use('/api/tester', testerRoutes);

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
