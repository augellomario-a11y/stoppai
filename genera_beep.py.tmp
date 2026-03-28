import struct, wave, math

filename = "/opt/stoppai/asterisk/sounds/beep.wav"
freq, duration = 1000, 0.8
volume = 0.7
rate = 8000
samples = [int(volume * math.sin(2 * math.pi * freq * i / rate) * 32767) for i in range(int(rate * duration))]

with wave.open(filename, "w") as f:
    f.setnchannels(1)
    f.setsampwidth(2)
    f.setframerate(rate)
    f.writeframes(struct.pack("<" + "h" * len(samples), *samples))

print("Beep creato con successo!")
