import telnetlib
import time

def simulate_calls():
    try:
        tn = telnetlib.Telnet("localhost", 5554)
        # Auth if needed (usually in ~/.emulator_console_auth_token)
        # But for local dev it might be open
        
        numbers = ["+393331234567", "+393331234567", "+3906112233", "+39345999888", "unknown"]
        for n in numbers:
            print(f"Calling: {n}")
            tn.write(f"gsm call {n}\n".encode('ascii'))
            time.sleep(3)
            tn.write(f"gsm cancel {n}\n".encode('ascii'))
            time.sleep(2)
        tn.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    simulate_calls()
