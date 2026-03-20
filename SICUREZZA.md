# 🛑 PROTOCOLLO DI VETO & DEPLOY (MANDATORIO)

1. **AMBIENTI**:
   - SVILUPPO: `localhost` (Docker)
   - TEST ONLINE: `http://46.225.14.90:5200`
   - PRODUZIONE: `http://46.225.14.90:4200`

2. **GATES**: Il trasferimento su Hetzner avviene solo dopo: "AMBROGIO, GATES OPEN [PORTA]".
   - **MOBILE CHECK**: Prima di ogni build Capacitor, verificare che non ci siano errori di puntamento file nel guscio.
   - **HETZNER GATES**: Conferma porte 5200 (Test) e 4200 (Prod) su IP 46.225.14.90.
3. **VETO**: Modifica file -> Verifica CEO -> Commit -> Push.
4. **AVVISO**: Mai tentare deploy sulle porte 4001/5001.
