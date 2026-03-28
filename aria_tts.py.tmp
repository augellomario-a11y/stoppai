import asyncio
import subprocess
import edge_tts

async def genera_voce():
    # Testo aggiornato con istruzioni di chiusura
    testo = (
        "Ciao, in questo momento non sono disponibile. "
        "Lasciate un messaggio dopo il segnale acustico e sarete ricontattati al più presto. "
        "Al termine del messaggio, chiudete la conversazione. Grazie."
    )
    communicate = edge_tts.Communicate(testo, voice="it-IT-IsabellaNeural")
    await communicate.save("/tmp/benvenuto.mp3")

if __name__ == "__main__":
    asyncio.run(genera_voce())
    subprocess.run(["ffmpeg", "-i", "/tmp/benvenuto.mp3", "-ar", "8000", "-ac", "1", "-acodec", "pcm_s16le", "/opt/stoppai/asterisk/sounds/benvenuto.wav", "-y"])
    print("Voce Isabella V5 pronta")
