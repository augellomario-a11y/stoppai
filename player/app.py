from flask import Flask, send_file, redirect
import os, glob
from datetime import datetime

app = Flask(__name__)
RECORDINGS_DIR = "/opt/stoppai/asterisk/recordings"

def parse_filename(filename):
    try:
        parts = filename.replace('.wav','').split('_')
        # Il timestamp EPOCH è l'ultima parte del nome file
        if len(parts) >= 3:
            epoch = int(parts[-1])
            dt = datetime.fromtimestamp(epoch)
            return dt.strftime("%d/%m/%Y %H:%M:%S")
    except:
        pass
    return "Data sconosciuta"

def get_caller(filename):
    try:
        parts = filename.replace('.wav','').split('_')
        # Il numero chiamante è la parte centrale msg_NUMERO_timestamp.wav
        if len(parts) >= 3:
            return parts[1]
    except:
        pass
    return "Numero sconosciuto"

@app.route('/')
def index():
    try:
        os.makedirs(RECORDINGS_DIR, exist_ok=True)
        files = sorted(glob.glob(f"{RECORDINGS_DIR}/*.wav"), reverse=True)
        items = ""
        for f in files:
            nome = os.path.basename(f)
            data = parse_filename(nome)
            numero = get_caller(nome)
            size = round(os.path.getsize(f)/1024, 1)
            items += f"""
            <div style="margin:12px 0;padding:16px;
            background:#f9f9f9;border-radius:10px;
            border-left:4px solid #1a5c2e;">
              <div style="display:flex;justify-content:space-between;margin-bottom:8px;">
                <span style="font-weight:bold;color:#1a5c2e;">Chi: {numero}</span>
                <span style="color:#888;font-size:13px;">{data}</span>
              </div>
              <div style="color:#555;font-size:12px;margin-bottom:8px;">
                {nome} | Peso: {size} KB
              </div>
              <audio controls src="/play/{nome}"
                style="width:100%;margin-top:4px;"></audio>
              <div style="text-align:right;margin-top:8px;">
                <a href="/delete/{nome}"
                  onclick="return confirm('Eliminare questo messaggio?')"
                  style="color:#cc0000;font-size:12px;
                  text-decoration:none;">Elimina</a>
              </div>
            </div>"""
        contenuto = items if items else """
            <div style="text-align:center;color:#888;padding:40px 0;">
            Nessun messaggio ancora.<br>
            Chiama il 04211898065 per testare.
            </div>"""
        return f"""<html>
        <head><title>ARIA Messaggi</title>
        <meta charset="utf-8">
        <meta name="viewport"
          content="width=device-width,initial-scale=1">
        <style>
          body{{font-family:-apple-system,sans-serif;
            max-width:640px;margin:0 auto;padding:20px;}}
          h1{{color:#1a5c2e;margin-bottom:4px;}}
          .stats{{color:#888;font-size:14px;margin-bottom:20px;
            padding-bottom:16px;border-bottom:1px solid #eee;}}
          .refresh{{float:right;background:#1a5c2e;color:white;
            border:none;padding:6px 14px;border-radius:6px;
            cursor:pointer;font-size:13px;}}
        </style></head>
        <body>
          <h1>ARIA Messaggi</h1>
          <div class="stats">
            {len(files)} Messaggi Registrati
            <button class="refresh"
              onclick="location.reload()">Aggiorna</button>
          </div>
          {contenuto}
        </body></html>"""
    except Exception as e:
        return f"Errore: {str(e)}", 500

@app.route('/play/<filename>')
def play(filename):
    try:
        path = os.path.join(RECORDINGS_DIR,
          os.path.basename(filename))
        if os.path.exists(path):
            return send_file(path, mimetype='audio/wav')
        return "File non trovato", 404
    except Exception as e:
        return f"Errore: {str(e)}", 500

@app.route('/delete/<filename>')
def delete(filename):
    try:
        path = os.path.join(RECORDINGS_DIR,
          os.path.basename(filename))
        if os.path.exists(path):
            os.remove(path)
        return redirect('/')
    except Exception as e:
        return f"Errore: {str(e)}", 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8085, debug=False)
