from flask import Flask, send_file
import os, glob

app = Flask(__name__)

RECORDINGS_DIR = "/opt/stoppai/asterisk/recordings"

@app.route('/')
def index():
    try:
        os.makedirs(RECORDINGS_DIR, exist_ok=True)
        files = sorted(glob.glob(f"{RECORDINGS_DIR}/*.wav"), reverse=True)
        items = ""
        for f in files:
            nome = os.path.basename(f)
            items += f"""
            <div style="margin:12px 0; padding:12px; background:#f5f5f5; border-radius:8px;">
              <b>{nome}</b><br>
              <audio controls src="/play/{nome}" style="margin-top:8px; width:100%"></audio>
            </div>"""
        conteggio = len(files)
        contenuto = items if items else "<p>Nessun messaggio ancora. Chiama il 04211898065 per testare.</p>"
        return f"""
        <html>
        <head>
          <title>ARIA Messaggi</title>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width">
          <style>
            body {{font-family:sans-serif; max-width:600px; margin:40px auto; padding:0 20px; background:#fff;}}
            h1 {{color:#1a5c2e;}}
            p {{color:#555;}}
          </style>
        </head>
        <body>
          <h1>ARIA Messaggi</h1>
          <p>{conteggio} messaggi registrati</p>
          {contenuto}
        </body>
        </html>"""
    except Exception as e:
        return f"Errore: {str(e)}", 500

@app.route('/play/<filename>')
def play(filename):
    try:
        path = os.path.join(RECORDINGS_DIR, os.path.basename(filename))
        if os.path.exists(path):
            return send_file(path, mimetype='audio/wav')
        return "File non trovato", 404
    except Exception as e:
        return f"Errore: {str(e)}", 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8085, debug=False)
