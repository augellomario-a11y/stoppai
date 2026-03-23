import socket
import time

def simulate_calls():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect(("localhost", 5554))
        # No auth for local test usually
        
        numbers = ["+393331234567", "+3906112233", "unknown"]
        for n in numbers:
            print(f"Calling: {n}")
            s.send(f"gsm call {n}\r\n".encode())
            time.sleep(3)
            s.send(f"gsm cancel {n}\r\n".encode())
            time.sleep(2)
        s.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    simulate_calls()
