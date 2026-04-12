# CLAUDE.md — StoppAI Project Context v5.0

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
DEPLOY: Prima SCP + rebuild Docker per test, poi commit solo dopo approvazione Mario
DEPLOY: docker compose up -d --build (non restart! restart non aggiorna i file)
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
- Deploy: SCP file → `docker compose up -d --build` (MAI solo restart)
- Volumi Docker montati: solo `db/` e `uploads/` — tutto il resto richiede rebuild

---

## IL PRODOTTO

**StoppAI** — App Android anti-spam con segreteria AI vocale (ARIA).

### Concetto chiave:
**Le chiamate non vengono MAI bloccate — vengono solo silenziate. Nulla si perde.**

### Flusso chiamata:
```
Chiamata sconosciuta entra
-> CallScreeningService verifica: note? spam? whitelist? piano?
-> Se nota positiva (PRO+): SQUILLA + overlay verde "Numero conosciuto"
-> Se spam segnalato: SILENZIA + overlay rosso "Sospetto spam"
-> Se sconosciuto normale: SILENZIA (zero squilli, zero vibrazione)
-> Opensolution devia al trunk SIP 04211898065
-> Asterisk risponde con voce Isabella
-> Chiamante lascia messaggio vocale
-> WAV salvato in /opt/stoppai/asterisk/recordings/
-> whisper_worker.py trascrive (faster-whisper IT)
-> FCM notifica push all'app Android (per-tester via token DB)
-> App crea CallLogEntry e AriaMessaggio collegati per callLogId
-> Mini CRM mostra riga con icona microfono
-> Utente legge trascrizione nel BottomSheet ARIA
```

---

## ARCHITETTURA CORRENTE

### Android — v5.8.1 (Build 98) — IN SVILUPPO
- **Package:** `com.ifs.stoppai`
- **Device test:** Samsung Galaxy S22 (SM-S908N, Android 16 API 36)
- **Stack:** Kotlin, Room DB v10, CallScreeningService, Firebase FCM

**Componenti chiave:**
- `CallScreeningServiceImpl.kt` — intercetta, silenzia, controlla whitelist, overlay note/spam
- `CallerOverlayService.kt` — overlay sopra chiamata (verde=nota, rosso=spam, arancio=alert)
- `ScreeningLogic.kt` — logica decisionale (whitelist → contatti → SMS → esteri → ARIA)
- `PlanManager.kt` — gestione piani FREE/PRO/SHIELD, upgrade progressivo, flag admin
- `PricingSheet.kt` — BottomSheet con 3 card prezzi (da popup lucchetto)
- `UpgradeDialog.kt` — popup upgrade con tracking click + "Vedi i piani"
- `AriaFcmService.kt` — ricezione FCM, Magic Code auto-fill, salva AriaMessaggio
- `AriaConfigBottomSheet.kt` — configurazione messaggio ARIA (standard/preset/custom)
- `AriaTranscriptionSheet.kt` — player audio, rating trascrizione, spam/attendibile
- `BackendSyncService.kt` — sync device info + statistiche con backend
- `LoginFragment.kt` — Magic Link login con auto-fill da push FCM
- `ChatFragment.kt` — chat assistenza real-time con polling
- `InfoFragment.kt` — guida completa + tabella piani con upgrade
- `SettingsFragment.kt` — account, ARIA, white list, permessi, volume
- `HomeFragment.kt` — banner piano con countdown, protezione base/totale
- `CallLogAdapter.kt` — lista con frecce colorate, pallini, icone

**Room DB v10 — entita':**
- `CallLogEntry` — registro chiamate
- `AriaMessaggio` — messaggi segreteria (wavFilename, spamVoto, accuracy_rating)
- `WhitelistEntry` — white list numeri/prefissi (label + pattern)
- `AppSettings` — impostazioni app

**Menu bottom navigation (5 tab):**
Home | Invita | Impostazioni | Info | Aiuto (chat)

### Backend Hetzner — attivo (Docker)
- **IP:** 46.225.14.90
- **Container:** stoppai-backend (porta 6002)
- **Stack:** Node.js 20 + Express + SQLite + Resend + Multer
- **Auth:** Magic Link (codice 6 cifre via email + push FCM)

**Database SQLite — tabelle:**
- `testers` — anagrafica con piano, fcm_token, is_admin, piano_scadenza
- `messaggi_chat` — chat admin-tester con campo immagine
- `admin_notes` — note personali Mario per tester
- `admin_todos` — checklist per tester
- `auth_codes` — codici magic link con scadenza
- `tester_stats` — statistiche dettagliate per tester
- `piano_log` — storico cambi piano con timestamp
- `admin_tokens` — token admin
- `aria_messaggi` — messaggi segreteria con accuracy_rating e spam_score
- `aria_config` — configurazione messaggio ARIA per tester
- `spam_numbers` — database numeri spam crowd-sourced
- `upgrade_clicks` — tracking click sui lucchetti (statistiche conversione)
- `app_config` — configurazione app (playstore_link, versione, note rilascio)
- `tester_sessions` — sessioni dashboard web tester
- `test_items` / `test_items_done` / `test_items_comments` — TO-DO broadcast

**Pannello Super Admin CRM (admin.html):**
- Badge SUPER ADMIN nella sidebar
- Sidebar: Dashboard, Tester, Comunicazioni, Test TO-DO, Referral, Link pericolosi
- Dashboard: contatori, qualita' trascrizione ARIA, click sui lucchetti
- Scheda tester: info + ruolo admin + piano + scadenza + 4 tab (Chat, Stats, Note, To-Do)
- Chat: cancella/modifica singolo msg, cancella intera chat, allegato img
- Statistiche: grafici Chart.js (torta/barre/tabella), reset singolo/totale
- Toggle admin per tester (bypass limiti upgrade)
- Log cambi piano in sidebar con pulizia
- Broadcast: messaggio a tutti i tester accettati
- Login admin via Magic Code email

### Infrastruttura ARIA
- **Asterisk:** bare-metal (NON Docker)
- **Trunk SIP:** Opensolution, numero 04211898065
- **Trascrizione:** faster-whisper Python 3.11, modello "small", int8, IT
- **FCM:** push diretto via Firebase Admin SDK in whisper_worker.py
- **FCM token:** salvato in DB per-tester (colonna fcm_token su testers)

### Repository
- **URL:** `https://github.com/augellomario-a11y/stoppai`
- **Main:** stabile v5.4.4 (Build 82) con SA-117/118/119/120 mergiati
- **Branch attivo:** `fix-app-v2`

```
stoppai/
├── app/                  Android v5.8.1 (Build 98)
│   ├── core/             Services + PlanManager + UpgradeDialog + PricingSheet
│   ├── db/               Room DB v10 (CallLog, AriaMessaggio, WhitelistEntry)
│   ├── ui/               Fragments (Home, Chat, Login, Info, Settings, BottomSheets)
│   └── res/              Layout, drawable, menu
├── backend/
│   ├── server.js         Porta 6002, Express, cleanup WAV cron
│   ├── db/database.js    SQLite schema + migrazioni
│   ├── public/admin.html Pannello Super Admin CRM
│   ├── routes/
│   │   ├── admin.js      API admin (CRUD, chat, stats, piano, admin toggle, upgrade-clicks)
│   │   ├── tester.js     API tester (iscrizione, sync, chat, upgrade, aria-rating, spam-report)
│   │   └── auth.js       Magic Link (request + verify)
│   ├── uploads/          Immagini chat
│   ├── Dockerfile        Node 20 Alpine
│   └── docker-compose.yml (volumi: db/, uploads/, recordings)
└── landing/              Landing page v1.1
    ├── index.html        Form iscrizione tester
    └── img/
```

---

## PIANI E PRICING

| Piano | Prezzo | Welcome ARIA | Chat | White list |
|-------|--------|-------------|------|------------|
| **FREE** | gratis | Messaggio standard | No | No |
| **PRO** | 2,99/mese (18/anno) | Scelta tra 8 preset | Si (limiti) | No |
| **SHIELD** | 4,99/mese (29/anno) | Registrazione personalizzata | Si (illimitata) | Si |

**Upgrade progressivo (beta):**
- FREE → PRO: dopo 5 giorni dall'installazione
- PRO → SHIELD: dopo 5 giorni dall'attivazione PRO
- Admin: nessun limite temporale (flag is_admin)

**Tester beta:** 1 anno Shield gratuito se completano tutte le fasi (59,88)

**Email sistema:**
- Mittente: `info@internetfullservice.it`
- Admin Mario: `info@internetfullservice.it`
- Provider: Resend

---

## TASK COMPLETATI

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
| SA-125 | White list, upgrade progressivo, BottomSheet prezzi, flag admin, tracking click | v5.8.1 |
| SA-126 | Overlay chiamata (note verde, spam rosso), screening per piano, FCM token fix, TO-DO admin | v5.8.1 |

---

## PROSSIMI TASK

| # | Task | Priorita' |
|---|------|-----------|
| ~~13~~ | ~~Fix FCM token nel DB~~ | FATTO |
| 7 | Restyling pagina Invita | Alta |
| 10 | Contatori mensili backend (30 SMS, 3 ARIA free, 10 player PRO) | Alta |
| 14 | SEO restante (sitemap, robots, favicon, schema.org, Umami) | Media |
| 15 | Sezione Referral | Media |
| 16 | FCM push per eventi admin (nuova voce broadcast, cambio piano) | Media |
| 17 | Grafico progresso team Test TO-DO admin | Bassa |
| 18 | Deviazioni ARIA automatiche in base al piano | Media |
| 22 | Restyling pagina Invita/Referral completo | Alta |
| 22a | — Regolamento affiliazione (studiare concorrenza) | Alta |
| 22b | — Prezzi aggiornati (mensili + annuali nuovi) | Alta |
| 22c | — Open Graph per social (immagini WhatsApp, IG, TikTok, FB, email) | Alta |
| 22d | — Bottone "Condividi" nativo al posto di "Copia" | Alta |
| 22e | — Simulatore guadagno per influencer | Alta |
| 22f | — Percentuali referral da definire | Alta |
| 22g | — Rimuovere Stripe Connect, aggiornare con Creem | Alta |
| 23 | Pagina ringraziamenti influencer nell'app | Media |
| 21 | Integrazione pagamenti Creem (ULTIMA COSA) | Finale |

**Parcheggiati:**
- Gestione utenti post-beta
- iOS (fattibile ma parcheggiato)
- Shield Ultra (admin web personale per cliente)
- SLS Security LinkSystem

---

## FILE IMPORTANTI

| File/Percorso | Contenuto |
|---------------|-----------|
| `/opt/stoppai/whisper_worker.py` | Trascrizione + push FCM diretto |
| `/opt/stoppai/firebase-credentials.json` | Firebase progetto stoppai |
| `/opt/nospam-cloud/` | FCM Bridge Docker (dual Firebase) |
| `backend/.env` | Chiavi API (non in git) |
| `app/google-services.json` | Config Firebase Android (non in git) |
| `~/.ssh/id_ed25519` | Chiave SSH per Hetzner |

---

*CLAUDE.md — v5.0 — Alfred (Developer) — 13/04/2026*
*Android v5.8.1 (Build 98) | Branch: fix-app-v2*
