# CLAUDE.md — StoppAI Project Context v4.0

> Questo file viene letto automaticamente da Claude Code (Alfred) ad ogni sessione.
> Contiene tutto il contesto del progetto StoppAI. Non modificare senza autorizzazione di Mario.

---

## IL TEAM

| Ruolo | Chi | Cosa fa |
|-------|-----|---------|
| **CEO / Product Owner** | Mario | Decide funzionalita', approva ogni modifica |
| **CTO** | Aldo (Claude chat) | Architettura, task design, supervisione tecnica |
| **Developer** | Alfred (Claude Code — sei tu) | Implementazione codice, esecuzione task diretti sul repo |

**Mario non esegue mai operazioni manuali su file o terminale.**
Se serve un'operazione, e' Alfred che la esegue dopo ordine esplicito di Mario.

---

## REGOLE PERMANENTI — LEGGILE PRIMA DI TUTTO

```
GIT: Zero operazioni Git senza ordine esplicito di Mario
     Commit, push, merge, creare/cancellare branch = VIETATO senza OK
FILE: Max 300 righe per file — un file = una responsabilita'
EXTRA: Niente codice, cartelle o file non richiesti
AUTONOMIA: Piano prima, poi aspetti OK di Mario. Sempre.
LINGUA: Rispondi sempre in italiano
BRANCH: Mostra sempre il branch attivo in ogni messaggio
BRANCH: Nuovo branch per ogni nuova fase di lavoro
DEPLOY: Prima SCP per test, poi commit solo dopo approvazione Mario
FINE TASK: Elenca file creati/modificati + fermati + aspetta
VERSIONE: Conferma sempre il numero di versione a fine task
DOPO 3 FALLIMENTI: Fai ricerca web prima del prossimo tentativo
```

**Flusso operativo:**
1. PROPONI — spiego cosa voglio fare
2. ATTENDI — aspetto la risposta di Mario
3. VALUTIAMO — discutiamo insieme
4. DECIDIAMO — Mario da' l'OK
5. ESEGUIAMO — solo allora parto

---

## PROTOCOLLO SICUREZZA

**Porte attive su Hetzner 46.225.14.90:**

| Porta | Servizio |
|-------|----------|
| 3000 | FCM Bridge (nospam + stoppai) |
| 6001 | Landing page (nginx proxy → 6002 per /api) |
| 6002 | Backend Node.js (Docker: stoppai-backend) |
| 8085 | Web player ARIA |

**Regole deploy:**
- Operazioni su Hetzner SOLO dopo comando esplicito di Mario
- VETO obbligatorio: Modifica → Test Mario → Commit → Push → Deploy
- MAI operazioni autonome su server
- Infrastruttura Docker: mai installare runtime sul server

---

## IL PRODOTTO

**StoppAI** — App Android anti-spam con segreteria AI vocale (ARIA).

### Flusso chiamata:
```
Chiamata sconosciuta entra
-> CallScreeningService blocca (zero squilli, zero vibrazione)
-> Opensolution devia al trunk SIP 04211898065
-> Asterisk risponde con voce Isabella
-> Chiamante lascia messaggio vocale
-> WAV salvato in /opt/stoppai/asterisk/recordings/
-> whisper_worker.py trascrive (faster-whisper IT)
-> FCM notifica push all'app Android
-> App crea CallLogEntry e AriaMessaggio collegati per callLogId
-> Mini CRM mostra riga con icona microfono
-> Mario legge trascrizione nel BottomSheet ARIA
```

---

## ARCHITETTURA CORRENTE

### Android — v5.5.0 (Build 83) — STABILE
- **Package:** `com.ifs.stoppai`
- **Device test:** Samsung Galaxy S22 (SM-S908N, Android 16 API 36)
- **Stack:** Kotlin, Room DB v8, CallScreeningService, Firebase FCM

**Componenti chiave:**
- `CallScreeningService` — intercetta, silenzia, blocca
- `AriaFcmService.kt` — ricezione FCM, Magic Code auto-fill, salva AriaMessaggio
- `BackendSyncService.kt` — sync device info + statistiche con backend
- `LoginFragment.kt` — Magic Link login con auto-fill da push FCM
- `ChatFragment.kt` — chat assistenza real-time con polling
- `InfoFragment.kt` — pagina informativa (ex HelpFragment)
- `CallLogAdapter.kt` — lista con frecce colorate, pallini, icone

**Menu bottom navigation (5 tab):**
Home | Invita | Impostazioni | Info | Aiuto (chat)

### Backend Hetzner — attivo (Docker)
- **IP:** 46.225.14.90
- **Container:** stoppai-backend (porta 6002)
- **Stack:** Node.js 20 + Express + SQLite + Resend + Multer
- **Auth:** Magic Link (codice 6 cifre via email + push FCM)

**Database SQLite — tabelle:**
- `testers` — anagrafica tester con piano (free/pro/shield)
- `messaggi_chat` — chat admin-tester con campo immagine
- `admin_notes` — note personali Mario per tester
- `admin_todos` — checklist per tester
- `auth_codes` — codici magic link con scadenza
- `tester_stats` — statistiche dettagliate per tester
- `piano_log` — storico cambi piano con timestamp
- `admin_tokens` — token admin

**Pannello Admin CRM (admin.html):**
- Sidebar: Dashboard, Tester, Comunicazioni, Referral, Link pericolosi
- Scheda tester: info + 4 tab (Chat, Statistiche, Note, To-Do)
- Chat: cancella/modifica singolo msg, cancella intera chat, allegato img
- Statistiche: grafici Chart.js (torta/barre/tabella), reset singolo/totale
- Log cambi piano in sidebar con pulizia
- Broadcast: messaggio a tutti i tester accettati
- Login admin via Magic Code email

### Infrastruttura ARIA
- **Asterisk:** bare-metal (NON Docker)
- **Trunk SIP:** Opensolution, numero 04211898065
- **Trascrizione:** faster-whisper Python 3.11, modello "small", int8, IT
- **FCM Bridge:** Docker (nospam-cloud), dual Firebase (nospam + stoppai)
- **FCM token:** `/opt/stoppai/fcm_token.txt`

### Repository
- **URL:** `https://github.com/augellomario-a11y/stoppai`
- **Main:** stabile v5.4.4 (Build 82) con SA-117/118/119/120 mergiati
- **Branch attivo:** `feature/app-collegare-dash`

```
stoppai/
├── app/                  Android v5.5.0 (Build 83)
│   ├── core/             Services (FCM, Sync, Screening)
│   ├── db/               Room DB (CallLog, AriaMessaggio)
│   ├── ui/               Fragments (Home, Chat, Login, Info, Settings)
│   └── res/              Layout, drawable, menu
├── backend/
│   ├── server.js         Porta 6002, Express
│   ├── db/database.js    SQLite schema + migrazioni
│   ├── public/admin.html Pannello CRM admin
│   ├── routes/
│   │   ├── admin.js      API admin (CRUD tester, chat, stats, piano)
│   │   ├── tester.js     API tester (iscrizione, sync, chat)
│   │   └── auth.js       Magic Link (request + verify)
│   ├── uploads/          Immagini chat
│   ├── Dockerfile        Node 20 Alpine
│   └── docker-compose.yml
└── landing/              Landing page v1.1
    ├── index.html        Form iscrizione tester
    └── img/
```

---

## TASK COMPLETATI — Sessione 02/04/2026

| SA | Descrizione | Versione |
|----|-------------|----------|
| SA-117 | Backend Express + Docker + landing form collegato + deploy Hetzner | - |
| SA-118 | Fix messaggi ARIA (query 10 cifre) + frecce + eliminazione contatori | v5.4.3-v5.4.4 |
| SA-119 | Pannello admin + API piani + email accettazione + prezzi 4.99 | - |
| SA-120 | CRM admin: sidebar, scheda tester, chat, note, todo | - |
| SA-121 | App: menu 5 tab, ChatFragment, comunicazioni gruppo, polling real-time | v5.5.0 |
| SA-122 | Magic Link auto-login (email + push FCM auto-fill) | v5.5.0 |
| SA-123 | Sync statistiche app->backend, grafici Chart.js | v5.5.0 |
| SA-124 | Gestione chat admin (cancella/modifica), log piano, reset stats | - |

---

## PROSSIMO TASK

```
BRANCH: feature/app-collegare-dash

DA FARE:
1. Allegati immagini funzionanti (dashboard + app)
2. Sostituzione popup nativi browser con modali custom dark
3. Collegamento piano backend -> app (sblocco/blocco funzionalita')
```

**Roadmap successiva:**
- Gestione Referral
- Gestione Link/messaggi pericolosi
- iOS (fase futura)

---

## PIANI E PRICING

| Piano | Prezzo | Welcome ARIA | Chat |
|-------|--------|-------------|------|
| **FREE** | gratis | Un messaggio preset | No |
| **PRO** | 2,99/mese | Scelta tra preset | Si (limiti) |
| **SHIELD** | 4,99/mese | Registrazione personalizzata | Si (illimitata) |

**Tester beta:** 1 anno Shield gratuito se completano tutte le fasi (59,88)

**Email sistema:**
- Mittente: `info@internetfullservice.it`
- Admin Mario: `info@internetfullservice.it`
- Provider: Resend

---

## FILE IMPORTANTI

| File/Percorso | Contenuto |
|---------------|-----------|
| `/opt/stoppai/whisper_worker.py` | Trascrizione + notifica FCM |
| `/opt/stoppai/firebase-credentials.json` | Firebase progetto stoppai |
| `/opt/stoppai/fcm_token.txt` | Token FCM Android |
| `/opt/nospam-cloud/` | FCM Bridge Docker (dual Firebase) |
| `backend/.env` | Chiavi API (non in git) |
| `app/google-services.json` | Config Firebase Android (non in git) |
| `~/.ssh/id_ed25519` | Chiave SSH per Hetzner |

---

*CLAUDE.md — v4.0 — Alfred (Developer) — 02/04/2026*
*Android v5.5.0 (Build 83) | Branch: feature/app-collegare-dash*
