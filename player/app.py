from flask import Flask, send_file, jsonify
import os, glob, re
from datetime import datetime

app = Flask(__name__)

RECORDINGS_DIR = "/opt/stoppai/asterisk/recordings"

def parse_filename(filename):
    try:
        # Estraiamo il timestamp (10 cifre epoch)
        match = re.search(r'(\d{10})', filename)
        if match:
            epoch = int(match.group(1))
            dt = datetime.fromtimestamp(epoch)
            return dt.strftime("%d/%m/%Y %H:%M:%S")
    except:
        pass
    return "Data sconosciuta"

def get_caller(filename):
    # Logica per estrarre il chiamante se presente nel file
    parts = filename.split('_')
    for p in parts:
        if p.startswith('+') or p.replace('-','').isdigit():
            # Pulizia e formattazione numero
            return p.replace('-','')
    return "Numero sconosciuto"

@app.route('/')
def index():
    try:
        os.makedirs(RECORDINGS_DIR, exist_ok=True)
        # Troviamo tutti i file .wav ordinati per data (più recenti in alto)
        files = sorted(glob.glob(f"{RECORDINGS_DIR}/*.wav"), reverse=True)

        items = ""
        for f in files:
            nome = os.path.basename(f)
            data = parse_filename(nome)
            numero = get_caller(nome)
            size = os.path.getsize(f)
            size_kb = round(size/1024, 1)

            items += f"""
            <div style="margin:12px 0; padding:16px; background:#f9f9f9; border-radius:10px; border-left:4px solid #1a5c2e; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                <div style="display:flex; justify-content: space-between; margin-bottom:8px;">
                    <span style="font-weight:bold; color:#1a5c2e; font-size:15px;">
                        <i style="color:#888;">Chi:</i> {numero}
                    </span>
                    <span style="color:#888; font-size:12px; font-weight:normal;">
                        {data}
                    </span>
                </div>
                <div style="color:#888; font-size:11px; margin-bottom:8px; border-bottom:1px solid #eee; padding-bottom:4px;">
                    ID Argomenti: {nome} | Peso: {size_kb} KB
                </div>
                <audio controls src="/play/{nome}" style="width:100%; margin-top:8px;"></audio>
            </div>"""

        conteggio = len(files)
        contenuto = items if items else """
            <div style="text-align:center; color:#888; padding:40px 0; border:2px dashed #eee; border-radius:10px;">
                <p style="font-size:18px; margin-bottom:8px;">📭 Nessun messaggio</p>
                <p style="font-size:14px;">Chiama il <b>04211898065</b> per testare ARIA.</p>
            </div>"""

        return f"""
        <html>
        <head>
            <title>ARIA — Monitor Messaggi</title>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body {{ font-family: -apple-system, system-ui, sans-serif; max-width:600px; margin:0 auto; padding:20px 40px; background:#fff; color:#333; }}
                h1 {{ color:#1a5c2e; font-size:28px; margin-bottom:8px; letter-spacing:-0.5px; }}
                .stats {{ color:#666; font-size:14px; margin-bottom:24px; display:flex; justify-content:space-between; align-items:center; border-bottom:2px solid #f0f0f0; padding-bottom:12px; }}
                .refresh {{ background:#1a5c2e; color:white; border:none; padding:8px 16px; border-radius:8px; cursor:pointer; font-weight:600; font-size:12px; transition:0.2s; }}
                .refresh:hover {{ background:#144623; transform:scale(1.02); }}
                audio {{ height:40px; }}
            </style>
        </head>
        <body>
            <h1>ARIA Messaggi</h1>
            <div class="stats">
                <span>🛡️ {conteggio} Messaggi Registrati</span>
                <button class="refresh" onclick="location.reload()">AGGIORNA</button>
            </div>
            {contenuto}
            <div style="text-align:center; margin-top:40px; font-size:11px; color:#ccc;">
                Infrastruttura StoppAI ARIA-Core v5.2.0 (Hetzner Cloud)
            </div>
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
