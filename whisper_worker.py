import time
import os
import json
from faster_whisper import WhisperModel
import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging

RECORDINGS_DIR = "/opt/stoppai/asterisk/recordings"
TRANSCRIPTIONS_DIR = "/opt/stoppai/transcriptions"
CREDENTIALS_FILE = "/opt/stoppai/firebase-credentials.json"
FCM_TOKEN_FILE = "/opt/stoppai/fcm_token.txt"
MODEL_SIZE = "small"
LANGUAGE = "it"

os.makedirs(TRANSCRIPTIONS_DIR, exist_ok=True)

# Inizializza Firebase
cred = credentials.Certificate(CREDENTIALS_FILE)
firebase_admin.initialize_app(cred)

print("Caricamento modello Whisper...")
model = WhisperModel(
    MODEL_SIZE,
    device="cpu",
    compute_type="int8"
)
print("Modello pronto.")

def get_fcm_token():
    try:
        if os.path.exists(FCM_TOKEN_FILE):
            with open(FCM_TOKEN_FILE) as f:
                return f.read().strip()
    except:
        pass
    return None

def manda_notifica(numero, testo):
    try:
        token = get_fcm_token()
        if not token:
            print("Token FCM non trovato")
            return
        message = messaging.Message(
            notification=messaging.Notification(
                title=f"Nuovo messaggio da {numero}",
                body=testo[:100] + "..." if len(testo) > 100 else testo
            ),
            data={
                "numero": str(numero),
                "testo": str(testo),
                "tipo": "aria_messaggio"
            },
            android=messaging.AndroidConfig(
                priority="high"
            ),
            token=token
        )
        response = messaging.send(message)
        print(f"Notifica inviata: {response}")
    except Exception as e:
        print(f"Errore notifica: {str(e)}")

def trascrivi(wav_file):
    try:
        segments, info = model.transcribe(
            wav_file,
            language=LANGUAGE,
            beam_size=5
        )
        testo = " ".join([s.text for s in segments])
        return testo.strip()
    except Exception as e:
        return f"Errore: {str(e)}"

def salva_trascrizione(wav_file, testo):
    nome = os.path.basename(wav_file)
    json_file = os.path.join(
        TRANSCRIPTIONS_DIR,
        nome.replace(".wav", ".json")
    )
    parts = nome.replace(".wav","").split("_")
    numero = "Sconosciuto"
    for p in parts:
        if p and p != "msg" and not (p.isdigit() and int(p) > 1700000000):
            numero = p
            break
    dati = {
        "file": nome,
        "numero": numero,
        "testo": testo,
        "timestamp": int(time.time())
    }
    with open(json_file, "w") as f:
        json.dump(dati, f,
            ensure_ascii=False, indent=2)
    print(f"Trascritto: {nome}")
    print(f"Numero: {numero}")
    print(f"Testo: {testo}")
    manda_notifica(numero, testo)

def gia_processato(wav_file):
    nome = os.path.basename(wav_file)
    json_file = os.path.join(
        TRANSCRIPTIONS_DIR,
        nome.replace(".wav", ".json")
    )
    return os.path.exists(json_file)

print("In ascolto nuovi messaggi...")
while True:
    try:
        files = [
            f for f in os.listdir(RECORDINGS_DIR)
            if f.endswith(".wav")
            and f != "msg.wav"
        ]
        for nome in files:
            path = os.path.join(
                RECORDINGS_DIR, nome)
            if not gia_processato(path):
                size = os.path.getsize(path)
                if size > 44:
                    print(f"Trovato: {nome}")
                    testo = trascrivi(path)
                    salva_trascrizione(path, testo)
        time.sleep(10)
    except Exception as e:
        print(f"Errore: {str(e)}")
        time.sleep(10)
