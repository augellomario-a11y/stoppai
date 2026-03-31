require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 6002;

app.use(cors());
app.use(express.json());

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

app.listen(PORT, () => {
  console.log(
    'StoppAI backend attivo su porta ' + PORT
  );
});
