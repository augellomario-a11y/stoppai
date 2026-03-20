# 📜 PROJECT CONSTITUTION: GEMINI.md
# PROJECT_NAME: StoppAI
# ROLE: CTO & PROJECT MANAGER (DOE FRAMEWORK)

# 🎯 INFRASTRUTTURA & PORTE
- **LOCAL DEV**: localhost via DOCKER.
- **PORT LOCK**: 4001/5001 OCCUPATE (Non usare).
- **PROJECT PORTE**: 
    - TEST ONLINE: Porta **5200**
    - PRODUZIONE: Porta **4200**
- **SERVER IP**: http://46.225.14.90/ (HETZNER)

# 📔 PROTOCOLLO ROADBOOK
Ogni task completato DEVE essere registrato in cima a ROADBOOK.md con Data, Ora e Agente.

# 📏 REGOLE TECNICHE
- LINGUA: Rispondi SEMPRE in lingua ITALIANA.
- CONSEGNA ATOMICA: Fornisci sempre il file intero.
- UTF-8 ALWAYS: Salvataggio in UTF-8 senza BOM.
- UI/UX: Layout 95% width, font grandi (text-lg).

## 📐 REGOLA FILE
- MAX 300 righe per file
- Un file = una responsabilità
- File grandi → dividere in moduli

## 🔖 REGOLA ID COMPONENTI
- Formato: ID_[SCHEDA]_[NUMERO]
- Esempio: ID_HOME_001, ID_HOME_002
- ID NON progressivi — se si cancella
  ID_HOME_002, ID_HOME_003 resta 003
- Tutti gli ID in docs/id-components.md

## 💬 REGOLA COMMENTI OBBLIGATORI
Ogni file .kt inizia con:
// FILE: nomefile.kt
// SCOPO: descrizione in una riga
// DIPENDENZE: file collegati
// ULTIMA MODIFICA: data
Ogni metodo ha commento sopra.
Ogni blocco logico ha commento inline.

## 🤖 AGENTE VOCALE AI
- Nome: ARIA (non Veronica)

# ⚙️ SPECIFICHE TECNICHE
- **TARGET FINALE**: Mobile APK via Capacitor.
- **REGOLA PERCORSI**: Utilizzare ESCLUSIVAMENTE percorsi relativi (`./`). Divieto assoluto di slash iniziali (`/`) per compatibilità Android Studio.
- **LAYOUT**: Ottimizzazione Mobile-First (95% width, touch-friendly).

# 💳 MODELLO PIANI
- **FREE**: €0 — blocco base, registro, statistiche.
- **PRO**: €2.99/mese — numero Telnyx, AI, trascrizioni, Mini CRM completo.
- **SHIELD**: €6.99/mese — tutto Pro + Security Shield, analisi link sospetti, semaforo rischio in tempo reale.

