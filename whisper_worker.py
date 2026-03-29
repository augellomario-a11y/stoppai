import time
import os
import json
from faster_whisper import WhisperModel

RECORDINGS_DIR = "/opt/stoppai/asterisk/recordings"
TRANSCRIPTIONS_DIR = "/opt/stoppai/transcriptions"
MODEL_SIZE = "small"
LANGUAGE = "it"

os.makedirs(TRANSCRIPTIONS_DIR, exist_ok=True)

print("Caricamento modello Whisper...")
model = WhisperModel(
    MODEL_SIZE,
    device="cpu",
    compute_type="int8"
)
print("Modello pronto.")

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
        return f"Errore trascrizione: {str(e)}"

def salva_trascrizione(wav_file, testo):
    nome = os.path.basename(wav_file)
    json_file = os.path.join(
        TRANSCRIPTIONS_DIR,
        nome.replace(".wav", ".json")
    )
    dati = {
        "file": nome,
        "testo": testo,
        "timestamp": int(time.time())
    }
    with open(json_file, "w") as f:
        json.dump(dati, f,
            ensure_ascii=False, indent=2)
    print(f"Trascritto: {nome}")
    print(f"Testo: {testo}")

def già_processato(wav_file):
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
            if not già_processato(path):
                size = os.path.getsize(path)
                if size > 44:
                    print(f"Trovato: {nome}")
                    testo = trascrivi(path)
                    salva_trascrizione(path, testo)
        time.sleep(10)
    except Exception as e:
        print(f"Errore: {str(e)}")
        time.sleep(10)
