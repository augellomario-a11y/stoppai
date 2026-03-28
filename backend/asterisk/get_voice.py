from gtts import gTTS
import os
import subprocess

testo = 'Ciao, sono l assistant personale di ARIA. In questo momento Mario non può rispondere. Lascia un messaggio dopo il segnale acustico e sarai ricontattato. Grazie.'
tts = gTTS(text=testo, lang='it')
tts.save('/tmp/voice.mp3')
os.system('ffmpeg -i /tmp/voice.mp3 -ar 8000 -ac 1 -acodec pcm_s16le /opt/stoppai/asterisk/sounds/benvenuto.wav -y')
