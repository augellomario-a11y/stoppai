# 📔 ROADBOOK: StoppAI
---
### 📅 2026-03-27 17:15 | Agente: Antigravity
- **TASK**: [TASK-SA-071-ASTERISK] — ENGINE VOIP ARIA
- **STATUS**: ✅ COMPLETATO
- **VERSIONE**: v5.2.0-Voicemail-Aware (Server Hetzner Active)
- **AZIONI**:
  - **DOCKER**: Installato Asterisk 20+ via Docker in `network_mode: host` su Hetzner.
  - **TRUNK**: Configurato peering PJSIP con Opensolution (Registered).
  - **VOICE**: Generato benvenuto ARIA in italiano (gTTS 8kHz Mono).
  - **DIALPLAN**: Answer -> Playback -> Record (con timestamp `${EPOCH}`).

### 📅 2026-03-27 16:45 | Agente: Antigravity
- **TASK**: [TASK-SA-070-HETZNER-AUDIT] — AUDIT INFRASTRUTTURA
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **SYSTEM**: Verificato Ubuntu 24.04 LTS, 8GB RAM, 150GB Disco.
  - **PORTS**: Aperte 5060/5061 (SIP) e 10000:20000 (RTP) su UFW.
  - **SOFTWARE**: Verificata presenza Docker, Nginx e Python 3.12.

### 📅 2026-03-25 12:20 | Agente: Antigravity
- **TASK**: [TASK-SA-068-SEGRETERIA-AVVISO] — VOICEMAIL AWARENESS
- **STATUS**: ✅ COMPLETATO
- **VERSIONE**: v5.2.0-Voicemail-Aware (Build 67)
- **AZIONI**:
  - **USER GUARD**: Inserito dialog d'avviso allo spegnimento della protezione base se la segreteria è ancora attiva.
  - **SYNC**: Salvataggio stato `segreteria_attiva` nelle prefs dopo ogni azione USSD.
  - **DASHBOARD**: Stato segreteria ON/OFF visibile nel menu collassato in Home.

### 📅 2026-03-24 18:30 | Agente: Antigravity
- **TASK**: [TASK-SA-067] — SETTINGS POWER-UP
- **STATUS**: ✅ COMPLETATO
- **VERSIONE**: v5.1.1-Reorg-Voicemail (Build 66)
- **AZIONI**:
  - **UI**: Riorganizzata sezione segreteria (Aria First), rimosso tasto verde ridondante.
  - **SYNC**: Implementata ProgressBar reale per il caricamento rubrica con broadcast receiver.
  - **RESET**: Aggiunto tasto "Ripristina valori default" nelle impostazioni.
---
### 📅 2026-03-23 21:15 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-056-RIPRISTINO-VOLUME] — RIGENERAZIONE AUDIO
- **STATUS**: ✅ COMPLETATO
- **VERSIONE**: 3.8.5-AudioAudited (Build Code 42)
- **AZIONI**:
  - **LOGICA**: In `CallScreeningServiceImpl.kt`, il ripristino in `IDLE` è ora incondizionato.
  - **DOPPIO CONTROLLO**: Aggiunta chiamata `alzaVolume()` anche all'arrivo di chiamate dai **contatti** o se la **protezione è spenta**. Questo "forza" l'unmute del telefono anche se il ripristino post-chiamata precedente fosse fallito.
  - **VOLUME**: Confermata lettura del `volumePreferito` per il ripristino (Default 10).
---
### 📅 2026-03-23 16:45 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-055-RINGTONE] — SUONERIA STOPPAI
- **STATUS**: ✅ COMPLETATO
- **VERSIONE**: 3.8.4-AudioAudited (Build Code 41)
- **AZIONI**:
  - **LOGICA**: Aggiunto salvataggio suoneria originale e impostazione suoneria StoppAI nello switch Protezione Base di `HomeFragment`.
  - **PERMESSI**: Implementato controllo runtime per `WRITE_SETTINGS` con dialog di reindirizzamento alle impostazioni di sistema.
  - **RIPRISTINO**: Garantito il ritorno alla suoneria originale quando la protezione viene disattivata.
---
### 📅 2026-03-23 14:10 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-054-FIX-DEFINITIVO] — LOGICA ATOMICA AUDIO
- **STATUS**: ✅ COMPLETATO
- **VERSIONE**: 3.8.2-AudioAudited (Build Code 39)
- **AZIONI**:
  - **ISOLAMENTO**: Rimosse manipolazioni a `STREAM_MUSIC`, `STREAM_NOTIFICATION`, `STREAM_SYSTEM`.
  - **RING PRECISE**: Il volume `STREAM_RING` viene toccato SOLO se la chiamata è spam (0) e ripristinato SOLO a fine chiamata (IDLE).
  - **UI**: Rimosso ogni trigger audio dai toggle della Dashboard.
  - **FIX**: Corretto errore di compilazione su `nuovoVol` in `HomeFragment`.
---
### 📅 2026-03-23 13:50 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-054-FIX2] — RIMOZIONE RIGHE ERRATE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **AUDIT FINALE**: Eliminate tutte le chiamate a `setStreamVolume` in `HomeFragment`, `ProtezioneBottomSheet`, `MainActivity` e `SettingsFragment`.
  - **LOGICA ATOMICA**: La gestione audio ora risiede al 100% nel `CallScreeningServiceImpl`.
  - **REAZIONALITÀ**: Silenzio (0) applicato solo all'arrivo della chiamata da bloccare; ripristino (`volumePreferito`) applicato solo al ritorno in stato IDLE.
---
### 📅 2026-03-23 11:45 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-054-FIX] — RIMOZIONE SILENZIO AVVIO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **DASHBOARD**: Rimosso il reset volume in `StoppAiApp`. Ora l'app non tocca i volumi all'apertura.
  - **LOGICA**: Il volume va a zero gestito esclusivamente dal `CallScreeningServiceImpl` durante il filtraggio spam.
  - **TEST**: Verificata persistenza volume all'avvio su Samsung S22 ed Emulator.
---
### 📅 2026-03-23 11:20 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-054] — VOLUME-FIX & RINGTONE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **AUDIO**: Rimosse interferenze con `STREAM_MUSIC/NOTIFICATION/SYSTEM`. Ora l'app gestisce esclusivamente `STREAM_RING`.
  - **INIT**: Implementato Reset Volumi al 75% al primo avvio per sicurezza utente.
  - **UX**: Aggiunto Popup informativo "StoppAI è attivo" post-reset volumi.
  - **BRAND**: Integrata suoneria `stoppai_ring.mp3` in `res/raw` e impostata come predefinita di sistema.
  - **MANIFEST**: Aggiunto permesso `WRITE_SETTINGS` per il controllo suoneria.
---
### 📅 2026-03-23 02:00 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-COMMIT-01] — COMMIT + PUSH
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **GIT**: Eseguito `git add .` e `git commit` ("feat: aggiunto logo, migliorata UI...").
  - **GITHUB**: Eseguito `git push` sul branch `feature/20260322-crm-registro`.
  - **STATO**: Tutto il lavoro della sessione sincronizzato correttamente (v3.8).
---
### 📅 2026-03-23 02:05 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-051] — REGISTRO-FIX
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **LOGIC**: Implementato lookup rubrica (`ContactsContract`) in `saveCallLog`.
  - **DB**: Migrazione v3->v4 per aggiunta campo `displayName` in `CallLogEntry`.
  - **UX**: Il registro ora mostra il nome del contatto se presente in rubrica.
  - **FIX**: Numeri nascosti ora vengono correttamente loggati come "Numero nascosto".
  - **FIX**: Il salvataggio su DB avviene prima del comando `respondToCall` per i blocchi.
---
### 📅 2026-03-23 01:50 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-050-LOGO-ULTRA] — LOGO HQ
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **ASSET**: Sostituito `logo_stoppai.png` con la versione High-Resolution (55KB) fornita dal CEO.
  - **TEST**: Verificata nitidezza su Samsung S22 e Emulator.
---
### 📅 2026-03-23 01:45 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-050-LOGO-FIX] — LOGO TROPPO PICCOLO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **UI**: Ingrandito logo PNG in `fragment_home.xml` da 72dp a **120dp**.
  - **UI**: Impostato `maxWidth="320dp"` e `adjustViewBounds="true"` per stabilità layout.
  - **TEST**: Validata resa visiva su Samsung S22 e Emulator.
---
### 📅 2026-03-23 01:40 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-050-LOGO-PNG] — INSERIMENTO LOGO PNG
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **ASSET**: Salvato `logo_stoppai.png` in 5 cartelle drawable (densità HDPI fino a XXXHDPI).
  - **DASHBOARD**: Sostituito header con `ImageView` del logo PNG ufficiale (H: 72dp).
  - **PAYOFF**: "Il tuo bodyguard digitale" centrato sotto il logo PNG.
  - **TEST**: Verificato rendering nitido su Samsung S22 e Emulator.
---
### 📅 2026-03-23 01:10 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-050-LOGO] — HEADER LOGO FINALE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **DASHBOARD**: Ridisegnato header Home con logo centrato "Stopp" (56sp Black) + Scudo (90dp).
  - **LOGOTIPO**: Creato `ic_shield_logo.xml` ultra-premium: gradiente smeraldo, lettere **A** e **I** integrate, linea centrale luce e ombreggiature 3D.
  - **PAYOFF**: "Il tuo bodyguard digitale · v3.8" centrato sotto il brand-mark.
  - **TEST**: Layout validato su Samsung S22 e emulatore.
---
### 📅 2026-03-23 00:35 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-050-FIX] — HEADER SHIELD RESIZE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **UI**: Ingrandito logo scudo in `fragment_home.xml` da 48dp a **72dp**.
  - **UI**: Aggiunto `scaleType="fitCenter"` alla ImageView del logo.
  - **TEST**: Verificato l'impatto visivo e installato su tutti i dispositivi.
---
### 📅 2026-03-22 23:55 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-050] — ICON-HEADER
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **LOGOTIPO**: Creato logo scudo ufficiale `ic_shield_logo.xml` (viola #7C4DFF + S bianca).
  - **DASHBOARD**: Ridisegnato header con logo in evidenza (48dp), titolo/versione e payoff grigio (#9E9E9E).
  - **NAV**: Inserita nuova icona ingranaggio `ic_settings_gear.xml` per le impostazioni.
  - **LAUNCHER**: Impostata l'icona scudo come icona ufficiale dell'app nel Manifest.
  - **VERSIONE**: Bump a v3.8 (Build 38). Installato con successo.
---
### 📅 2026-03-22 23:30 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-049] — UI-POLISH
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **DASHBOARD**: Riprogettato header (Titolo/Versione), payoff centrato, 3 box statistiche (Totale/Oggi/Referral) e separatori visivi.
  - **SETTINGS**: Aggiunta sezione "🔊 Audio" con SeekBar, bottoni +/- e feedback testuale "X/15".
  - **LOGICA**: Implementato recupero statistiche reali da DB Room (`getTotalCalls`/`getCallsToday`).
  - **NAV**: Bottom Navigation con sfondo neutro, icone corrette e label visibili.
  - **VERSIONE**: Bump a v3.7 (Build 37). Installato su S22 e Emulator.
---
### 📅 2026-03-22 23:00 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-048] — SPEAKER-ICONS
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **UI**: Sostituite emoji 🔊 con `ImageView` vettoriali (`ic_speaker_active.xml` / `ic_speaker_inactive.xml`).
  - **LOGICA**: Aggiornato `HomeFragment.kt` per gestire il cambio risorsa immagine asincrono.
  - **VERSIONE**: Bump a v3.6 (Build 36).

---
### 📅 2026-03-22 22:30 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-047] — UI-FIXES
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **HOME**: Titoli centrati, rimosso tasto svuota, aggiunto registro chiamate live con `RecyclerView`.
  - **SETTINGS**: Spostato tasto "Svuota registro" con dialog di conferma.
  - **NAV**: Icone aggiornate (ingranaggio e alert) e tab "Da implementare".
  - **FIX**: Colore altoparlanti reso indipendente per switch (Red/Gray).
  - **VERSIONE**: Bump a v3.5 (Build 35).

---
### 📅 2026-03-22 10:30 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-044] — SPEAKER-STATUS
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **LOGICA**: Aggiunta lettura volume `AudioManager` in `onResume/onStart`.
  - **UI**: Aggiunti indicatori icona+testo (ID_HOME_010/011) per volume reale.
  - **VERSIONE**: Bump a v3.1 (Build 31).

---
### 📅 2026-03-22 10:15 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-043] — SETTINGS-DB
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **DB**: Creata `AppSettingsEntity` e migration v1->v2.
  - **LOGICA**: Migrata gestione volume originale e deviation_number su DB Room.
  - **UI**: Aggiunto tasto Reset (ID_SETT_001) in SettingsFragment.

---
### 📅 2026-03-22 09:30 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-042] — NUOVO-BRANCH
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **GIT**: Creato e pushato branch `feature/20260322-silenzio-footer-ui`.

---
### 📅 2026-03-22 09:15 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-041] — AGGIORNA-DIARIO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**:
  - **DIARIO**: Sostituito `docs/storia-progetto.md` con il nuovo diario v3.0 (28 capitoli).
  - **GIT**: Commit e push su `main`.

---
### 📅 2026-03-22 01:20 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-037] v2.9 — MILESTONE: Protezione Totale Fixed & Menu Cleaned
- **STATUS**: ✅ COMPLETATO (Pushato su feature-branch)
- **AZIONI**:
  - **MENU [SA-037]**: Rimosse opzioni "Fino a stasera/domani" dal BottomSheet (lasciate solo durate fisse e personalizzate).
  - **LOGICA**: Confermata stabilità logica preferiti e timer personalizzato.
  - **GIT**: Commit e push su `feature/gestione-timing-protezione`.

---
### 📅 2026-03-22 00:50 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-034] v2.9 — MILESTONE: Timing Protezione + Crash Fix
- **STATUS**: ✅ COMPLETATO (Milestone Raggiunta e Pushato su `feature/gestione-timing-protezione`)
- **AZIONI**:
  - **TIMING PROTEZIONE [SA-028]**: Implementato BottomSheet per scelta durata Protezione Totale (30m, 1h, 2h, Stasera, Domani, Personalizzato).
  - **COUNTDOWN [SA-029]**: Aggiunto timer dinamico in HomeFragment aggiornato ogni secondo con auto-disattivazione e ripristino volume.
  - **PREFERITI [SA-028]**: Nuova cache preferiti in `ContactCacheManager`. Protezione Totale ora permette di escludere i preferiti (squillano anche in modalità totale).
  - **CRASH FIX [SA-031/034]**: Risolto crash `IllegalStateException` su Samsung Android 14. Rimosso ogni riferimento a `setSkipNotification` quando la chiamata è permessa (`disallowCall(false)`).
  - **VERSIONE**: 2.9 (Build 29).

---
### 📅 2026-03-21 23:04 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-SA-025] v2.4 — Call End Volume + RECAP sessione serale v2.1→v2.4
- **STATUS**: ✅ COMPLETATO (Milestone + Commit Pushato `8be0ff4`)
- **AZIONI SESSIONE SERALE**:
  - **v2.1 [SA-021] Notification Fix**: Modificato `setSkipNotification(true)` per sconosciuti nel tentativo di silenziare il ring di sistema Android. Risolta icona Home con vector drawable dedicato `ic_home.xml`.
  - **v2.2 [SA-022] Volume Definitivo**: Aggiunto fallback `run { if (saved > 0) saved else 7 }` in `CallScreeningServiceImpl` e guardia anti-zero in `StoppAiApp.onCreate()`. Fix `vol_originale=0` nelle SharedPreferences tramite ADB.
  - **v2.3 [SA-024] Rollback v1.7**: Ripristinato `setSkipNotification(false)` e lettura volume con default 5 come nella v1.7 funzionante. Diagnosi confermata: la modifica a `true` non era la causa del problema.
  - **v2.4 [SA-025] Call End Volume**: Rimosso timer fisso 35 secondi. Implementato `TelephonyCallback` (SDK≥31) con fallback `PhoneStateListener` per rilevare `CALL_STATE_IDLE`. Metodo `silenceRing()` riporta volume a 0 solo se `protezione_base` è attiva. Volume ora gestito da evento reale fine chiamata.
  - **NAVIGAZIONE UI [SA-018/019]**: Creati Fragment (Home, Settings, Calls, Help) con BottomNavigationView a 4 icone. MainActivity ridotta a container + nav. Logica switch spostata in HomeFragment.
  - **GIT**: Commit pushato su origin/main.

---
### 📅 2026-03-21 17:29 | Agente: ARIA (Antigravity)
- **TASK**: [TASK-SA-017] v1.7 Total Shield — RECAP GIORNATA DA v1.3 A v1.7
- **STATUS**: ✅ COMPLETATO (Milestone Conclusa e Commit Pushato)
- **AZIONI ODIERNE**: Riassunto di fine giornata sulle logiche asincrone di Screening che hanno finalmente domato l'intera architettura base:
  - **v1.3/v1.4 (Pure Silence / Stable Launch)**: Sostituito il blocco forzato della chiamata (`setDisallowCall`) con la logica del *Volume Invertito* (Suoneria master 0 per ignorati / Restoration suoneria a 5 per Amici rilevati). Risolto anche il blocco fatale in avvio scatenato dall'accesso rubrica (inserito doppio check nativo `checkSelfPermission`).
  - **v1.5 (Sync Cache)**: Estinta la *race-condition* cronica della lettura contatti iniziale disattivando la Coroutine in background. Usato esclusivamente `loadContactsSync()` in `onResume()`, completando la cache (es. 3146 records) PRIMA di rendere disponibile l'app. Aggiunto anche il semaforo visivo verde/rosso `ID_PERM_006`.
  - **v1.6 (Switch Volume)**: Congelato il volume utente preferito (`vol_originale`) tramite shared preferences dirette estratte all'avvio. La mutazione a master-zero ora viaggia solo premendo `ON` allo Switch "Protezione Base". Rimettendolo su `OFF` vieni riportato magicamente ai pieni squilli.
  - **v1.7 (Total Shield)**: Introdotta e formalmente innescata l'Opzione Totale `ID_HOME_007`. Essa disabilita lo switch Base e setta `.setSkipNotification(true)` *hardcoded* dal Service su qualsiasi numero chiami, garantendo silenzio irreale su amici, parenti e call center senza sbalzi di decibel. Prevede l'auto-scadenza garantita oltre i +60 minuti.
  - **GIT**: Tutti i moduli confermati in produzione, push sul repository eseguito (`"Total Shield — Protezione Totale funzionante..."`).

---
### 📅 2026-03-21 17:16 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-016 (Appendice) — AGGIORNAMENTO DIARIO PROGETTO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Aggiunta formalmente al Repository Git la nuova documentazione testuale `docs/storia-progetto.md` introdotta da Mario, riguardante il "Diario completo di progetto v2.0 - da NO_SPAM a StoppAI". Nessun task logico alterato.

---
### 📅 2026-03-21 16:55 | Agente: ARIA (Antigravity)
- **TASK**: MILESTONE CORE SCREENING completata!
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Giornata di ingegnerizzazione per il core silenzioso, con repository ufficialmente pushato. Raggiunta **v1.6 — Switch Volume**.
  - **Crash Zero**: Risolto in modo chirurgico e a doppio-livello un crash letale in accensione scovolato dall'accesso avventato a `READ_CONTACTS`. L'app non si schianta più se l'utente ritarda nel concederli grazie all'`checkSelfPermission()` atomico in testa ai provider, e un callback in `onResume()`.
  - **Sincronizzazione della Cache**: Neutralizzata la fastidiosa asincronia originaria del Thread `IO` di Kotlin che causava `0 contatti scaricati` nel primo fatale istante d'accensione. Utilizzando `loadContactsSync()` abbiamo un pool contatti pre-farcito all'attivazione dell'Activity.
  - **Protezione Ringer Unificata**: L'azione invasiva di "abbassare" ed "alzare" la suoneria è migrata allo Switch `ID_HOME_001` che protegge costantemente la cornetta al livello di `STREAM_RING = 0` finché sei in Protezione Base, pur ricordando in background il tuo `vol_originale` (es. 5) da ricaricare la notte.
  - **Identificazione Distinta**: Lo screening di `CallScreeningServiceImpl.kt` incrocia precisamente i prefissi rubrica e permette che lo "sconosciuto" suoni a Volume Zero (non disturbando minimamente l'utente), ed il volto amico suoni normalmente a Volume 5 e poi ristacca per i futuri squilli.
  - **Commit Git Eseguito**: `git push origin main` andato a buon fine.

---
### 📅 2026-03-21 16:45 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-016 — SWITCH-VOLUME
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: La gestione del volume è stata decentralizzata alla UI (livello Switch Base).
  1. All'avvio in `onCreate()` l'app aggancia in millisecondi il volume di suoneria del device e, se superiore a 0, lo congela in salvataggio SharedPreferences come `vol_originale` (altrimenti spara un 5 default).
  2. Spostamento Switch ad ON: rileva il livello attuale, lo cristallizza in RAM se maggiore di zero e azzera il Master Ringer, loggando `STOPPAI_VOL: Protezione ON - volume -> 0`.
  3. Spostamento Switch ad OFF: disinnesca e riflasha in hardware il `vol_originale` salvato.
  4. La logica invertita è spezzata: chi chiama da ignoto entra a Master 0 naturale. I contatti continuano con la restoration dinamica su Service. Compilata la **v1.6 — Switch Volume**.

---
### 📅 2026-03-21 02:45 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-015B — SYNC-CACHE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Eliminata l'asincronia che rendeva cieca la prima iterazione dello Screening sui contatti.
  1. `ContactCacheManager`: Introdotto `loadContactsSync()` che estrae e satura le struct `tempSet` localmente bloccando il flow fino al caricamento integrale di tutta la base. Aggiunto modulo d'uscita per `getSize()`.
  2. `StoppAiApp` & `MainActivity`: Il layer di caricamento migra integralmente alle invoke in versione *Sync* durante il boot e sull'onResume.
  3. `activity_main.xml` e UI: Integrato il marker visivo `ID_PERM_006`. Se il pool ha grandezza utile (>0) l'App rassicura con "🟢 Rubrica caricata (N)", altrimenti sventola semaforo rosso di transizione.
  4. Tagliata e compresso il Release `1.5 - Sync Cache`.

---
### 📅 2026-03-21 02:40 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-014 — STABLE-LAUNCH (Crash fix)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Eradicato il blocco fatale in avvio scatenato dalla negazione esplicita del `READ_CONTACTS` nativo Android/Samsung.
  1. File `ContactCacheManager.kt` protetto a doppio strato: `startSync` e `loadContacts` invocano un check atomico sui permessi usando `ContextCompat.checkSelfPermission` prima ancora di accendere l'IO o interagire coi Content Provider, silurandosi da soli se il check fallisce.
  2. Implementato in `StoppAiApp.kt` un `try-catch` robusto che previene qualsiasi Exception scagliata dal Context Application alla richiesta di permessi su boot e lo logga elegantemente senza abortire l'esecuzione dell'Activity principale.
  3. Cablata `MainActivity.kt`: quando il lifecycle dell'Activity si rianima (`onResume`), valuta asincronamente se il permesso è stato appena accordato dall'utente; se sì, innesta il trigger e avvia il Sync dei contatti alla perfezione.
  4. App compilata e pushata via ADB sul Samsung S22, signature *"1.4 — Stable Launch"*.

---
### 📅 2026-03-21 02:18 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-014 — LOGICA-FINALE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Demolizione e sostituzione radicale dello strato API `CallScreeningService`. Da adesso Android e StoppAI gestiscono tutto il carrier pass-through senza il benché minimo blocco hardware/software.
  1. `CallScreeningServiceImpl.kt`: riscritto interamente `onScreenCall` rimpiazzando lo statement originario. Costruita una via preferenziale spoglia in cui OGNI chiamata in entrata ottiene uno score `setDisallowCall(false)` incondizionato e restituito istantaneamente nel main thread per massima compliance coi driver Samsung. Eliminato ogni `setRejectCall(true)`.
  2. Implementazione Post-Risposta: un `SingleThreadExecutor` valuta a cascata se il numero sia in rubrica usando l'HashSet asincrono in `CacheManager`.
  3. Bypass Muting: Se riscontra un Amico, sguinzaglia un `Handler(Looper.getMainLooper())` per spezzare il Silenzio di `StoppAiApp` risvegliando momentaneamente il volume di `STREAM_RING`, abbassandolo nuovamente dopo 35 sec.
  4. Se rileva numero Sconosciuto/Spam, il background thread salva passivamente il logging a DB disinteressandosi del volume, mantenendolo blindato sullo Zero assoluto del Boot (`StoppAiApp`). La telefonata resta muta.
  5. Sfornata V1.4 - "Pure Silence". Compilata su ADB per S22.

---
### 📅 2026-03-21 02:11 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-013 — VOLUME-LOGIC
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Modello Volume Inverso ricostruito con isolamento thread per scongiurare fatal crashes.
  1. Reinserito l'assorbimento primario del volume in `StoppAiApp.kt` (Boot) ma protetto integralmente in un layer Try-Catch anti-crush, incatenato logicamente alla `protezione_base` attiva. Il ring va a 0 solo se confermato.
  2. Implementato in `CallScreeningServiceImpl.kt` il rimbalzo volume per chiamate Whitelistate/Rubrica. Rigorosamente su `Looper.getMainLooper().post` per garantire l'accesso al framework `AudioManager` nativo di Android. Il volume risale temporaneamente e viene poi riazzerato 35 s dopo tramite `postDelayed`. 
  3. Numeri sconosciuti subiscono uno hard-block spietato, non sfiorando mai `AudioManager` ed ereditando lo Zero assoluto di avvio.
  4. Intercettato il volume toggle in `MainActivity.kt` su accensione/spegnimento e persino in `onDestroy()` per rilasciare l'hardware alla chiusura dell'App.
  5. Progetto taggato a *"1.3 — Volume Logic"*, compilato. Deployato su Samsung S22 fisico.

---
### 📅 2026-03-21 02:03 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-012 — RITORNO-V04
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Purga e rollback drastico dell'esperimento sul volume-guard per ripristinare il bloccaggio perfetto hard-coded (v0.4 architecture).
  1. Spazzata via ogni invocazione nativa o indiretta dello stack e Context dell'`AudioManager`. Cancellata del tutto la logica del muting selettivo per affidarsi ai puri comandi Call Screening IPC di base.
  2. In `CallScreeningServiceImpl.kt`, impostata `CallResponse.Builder` a tagliare ogni connessione con numero sconosciuto usando l'accoppiamento spietato `.setDisallowCall(true)` e `.setRejectCall(true)`.
  3. Il set di contatti validi transita vergine tramite un response di puro `.setDisallowCall(false)`. Nessun handler, nessun thread posticipato se non i task Database puri.
  4. Cancellati reazioni di Switch e controlli residui dal file `MainActivity.kt`.
  5. Etichetta `v1.2 — Back to Basics` cucita sul Gradle e sul UI footer. Deploy eseguito.

---
### 📅 2026-03-21 02:00 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-011 — FIX-CRASH-VOLUME
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Ottimizzazione asincrona e migrazione `AudioManager` al MainThread nativo per scongiurare crash di servizio legati ai proxy context e lock di sistema.
  1. Estirpata completamente la logica cruda e "troppo precoce" all'interno di `StoppAiApp.kt` al bootstrap. Questo decongestiona la start-chain e protegge da Fatal loop Android.
  2. Spostata integralmente la governance `audio.setStreamVolume` per la Rubrica/Whitelists dentro due incapsulamenti garantiti: `Handler(Looper.getMainLooper()).post {}`. Ora l'OS assicura che il muting/unmuting sia sottomesso in perfetta sincronia dal main-thread hardware, bloccando gli alert su eventuali worker thread secondari.
  3. L'handling di numeri sconosciuti non invoca minimamente le librerie di audio-injection e lascia passare lo stato corrente (riducendo drasticamente il carico cpu per gli SPAM calls).
  4. Costruita versione 1.1 — Stable, spinta su R3CT905WBDF via ADB senza crash.

---
### 📅 2026-03-21 01:49 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-010 — INVERSE-VOLUME
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Logica silenziatore radicale Inverse Shield assecondata per Samsung S22.
  1. Spostata prevaricazione di volume in avvio App nel costruttore `StoppAiApp.kt`. Salvia dell'indice volume originale in `SharedPreferences` e azzeramento coercitivo hardcoded di `STREAM_RING` sin dal bootstrap applicativo.
  2. Implementazione della Positive-Rebound Logic per la Rubrica: in `CallScreeningServiceImpl.kt` soltanto le chiamate dei contatti noti `!shouldBlock` triggerano dinamicamente un recupero del volume temporaneo, il quale viene innescato una frazione di secondo prima dello screen pass-through, e riportato a `0` esatto dopo un safe delay di 35 sec.
  3. L'ecosistema per i numeri sconosciuti non interroga più `AudioManager` a zero poichè protetto in partenza. Aggiunta direttiva `setSkipNotification(true)` per coprire alert visivi.
  4. Cablate le reazioni Toggle in `MainActivity.kt`: de-switchare la protezione Base o Totale ristabilisce in tempo reale il reale volume originario (mentre attivarle rinforza lo Zero-Volume). 
  5. APK Deployato con successo via ADB stream con firma *"1.0 — Inverse Shield"*.

---
### 📅 2026-03-21 01:43 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-010 — VOLUME-TIMING
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Shift temporale nell'overriding volume per impedire lo start hardware della suoneria.
  1. Ricollocata l'estrazione e il set 0 di `STREAM_RING` prelevandolo da `AudioManager` come **istruzione prima assoluta** allo scoccare di `onScreenCall()`, ancor prima di interrogare SharedPreferences o Cache Ram. Questo chiude ad eventuali latenze iniziali di intercettazione.
  2. Implementato `Executors.newSingleThreadExecutor()` per ripristinare il livello acustico pre-esistente dopo il delay prefissato di 20 secondi per i blocchi sconosciuti.
  3. Aggiunta reinizializzazione condizionale immediata per le chiamate legittime (consentendo un normale squillo dei contatti whitelisted).
  4. Footer aggiornato a *StoppAI v1.0 — Volume Guard* e bump su *build.gradle* installato con zero operazioni Git nel network. Installazione eseguita.

---
### 📅 2026-03-21 01:40 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-009 — FIX-SILENCE-SAMSUNG
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Override totale e profondo su driver audio e policy DND (Do Not Disturb) per mutare le architetture Android customizzate (es. One UI).
  1. Integrato e autorizzato `ACCESS_NOTIFICATION_POLICY` nel Manifest Android per ottenere il permesso di abbassare lo stato di suoneria.
  2. Modificato `CallScreeningServiceImpl.kt` inserendo lo stratagemma combo "Deep Silence". Anziché agire superficialmente con lo status in CallResponse o RingerMode, ho manipolato l'esatto byte-level di `AudioManager.STREAM_RING` (volume zero forzato a tempo) assieme a un check su `isNotificationPolicyAccessGranted` volto a innestare una vera eccezione `INTERRUPTION_FILTER_NONE` su `NotificationManager`. 
  3. L'override acustico è isolato e annullato istantaneamente tramite un `Handler.postDelayed()` dopo 20 secondi, riconsegnando il controllo del dispositivo di output a fine dirottamento chiamate (verso la segreteria).
  4. Footer "StoppAI v0.9 — Deep Silence" e build.gradle aggiornati senza intaccare macro-logiche su MainActivity. Compilazione lanciata e deployata su hardware Samsung S22.

---
### 📅 2026-03-21 01:34 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-008 — SILENT-FIX
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Operazione terminale sul silencing fisico del ring device.
  1. Integrato e applicato un cast brutale ad `AudioManager` in `CallScreeningServiceImpl.kt` imponendo il `RINGER_MODE_SILENT` un istante prima di sparare via IPC la response CallScreening molla, e ripristinato a `RINGER_MODE_NORMAL` subito dopo in blocco try-catch di sicurezza. Questo forza l'assoluto mutismo in override sul dispositivo prima dei tick di squillo.
  2. Aggiornato l'URI intent del Dial ad attivare le impostazioni della segreteria con un TTL accorciato da 30 a 15 secondi (codice di attivazione MMI **`**61*0421633844*11*15%23`**).
  3. Avanzata la pipeline a v0.8. Compilato e installato. Nessun DB e XML layout toccati.

---
### 📅 2026-03-21 01:27 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-007 — FIX-DEVIAZIONE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Passaggio al modello Deviazione Operatore Assoluta.
  1. Modificato l'invio codice MMI in `MainActivity.kt` utiizzando l'API `Uri.fromParts` per sfuggire all'encoding bug del cancelleto `%23`. Adesso il Dialer Android assorbe esattamente la stringa `##61#`.
  2. Aggiornato `CallScreeningServiceImpl.kt` al pattern "Silent Forwarding" parziale: `setSilenceCall(true)` e `setSkipNotification(true)` ma senza bloccare lo stream (`setDisallowCall(false)`). Questo assicura zero squilli in UI, ma la chiamata avanza nel network per 30 secondi, lasciando il tempo alla segreteria/router dell'operatore di completare la deviazione da backend.
  3. Incremental v0.7 nel Gradle e forzato programmaticamente il rendering footer `ID_HOME_099`. Compilato e Deploy su S22.

---
### 📅 2026-03-21 01:21 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-006 — UX-REGISTRO-SEGRETERIA (Parte B)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Aggiunti pulsanti interattivi per dialer Segreteria GSM.
  1. Incrementata versione su `build.gradle` (v0.6 — Segreteria).
  2. Aggiunti pulsanti `ID_HOME_005` ("Attiva segreteria") e `ID_HOME_006` ("Disattiva segreteria") in `activity_main.xml`.
  3. Collegati event listener in `MainActivity.kt` per lanciare il Dialer (`ACTION_DIAL`) con i codici europei standard `**61*...#` e `##61#`.
  4. Footer aggiornato a v0.6. Buildato e installato su S22. Nessun'altra configurazione toccata.

---
### 📅 2026-03-21 01:10 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-006B — FIX-BLOCCO-CRITICO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Rollback strategico del blocco hard per contrastare l'inefficacia parziale di USSD Forwarding locale.
  1. Commentata/Rimossa interazione con `UssdManager.kt` dalla pipeline di `CallScreeningServiceImpl.kt`.
  2. Ripristinato il blocco hard asincrono zero-latenza per i numeri sconosciuti: `setDisallowCall(true)`, `setRejectCall(true)`. Eliminato il silence pattern (`setSilenceCall`).
  3. Aggiunta configurazione esplicita `setDisallowCall(false)` per i numeri noti in whitelist/rubrica.
  4. Ricompilato e pushato nuovo aggiornamento APK nel dispositivo fisico S22 (zero git).

---
### 📅 2026-03-21 01:05 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-006 — UX-REGISTRO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Riperfezionata la User Experience del Registro e aggiunto pulsante Wipe.
  1. Incrementata versione su `build.gradle` (v0.5 — UX Fix).
  2. Modificato `item_call_log.xml` innestando un nuovo `TextView` per la formattazione di Data e Ora (`ID_LOG_002`) formattato via `SimpleDateFormat`.
  3. Aggiunte diciture testuali leggibili con Emoji ("⏳ Da gestire", "✅ Whitelist" etc) nel log di ciascuna chiamata (`CallLogAdapter.kt`).
  4. Inserito pulsante distruttivo `ID_HOME_006` ("🗑 Svuota registro") con interazione basata su AlertDialog di sicurezza che va ad invocare la funzione nativa di piallaggio su `clearAllTables()`. Aggiornato anche il footer su v0.5 in `activity_main.xml`.
  5. Buildato e installato su S22. Zero interferenze su CallScreening o Ussd.

---
### 📅 2026-03-21 00:55 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-005 — FIX-SCREENING-CHECK
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Bugfix rilevamento permessi su Samsung One UI.
  1. Modificata in modo isolato la funzione `aggiornaPermessi()` in `MainActivity.kt`.
  2. Sostituito il check tramite `Settings.Secure` con le API native basate su `RoleManager`.
  3. `ID_PERM_003` (App verifica chiamate) viene ora correttamente ricalcolato col semaforo in base a `isRoleHeld(ROLE_CALL_SCREENING)`.
  4. Ricompilato e pushato nuovo aggiornamento APK nel S22 (zero git).

---
### 📅 2026-03-21 00:46 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-004 — VERSIONING-PERMESSI
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creazione modulo visivo per permessi e Versioning.
  1. Incrementato `build.gradle` a v0.4 ("Dynamic Shield").
  2. Layout `activity_main.xml` esteso: creata Card di Configurazione con 5 text view interattive (Contacts, Phone State, Screening Default, Phone Call, Notifications) e Footer finale ID_HOME_099 per versione fissa in schermo.
  3. Modificata `MainActivity.kt` estraendo logica su `onResume` per ricalcolo icone in real-time. Linkati tutti gli `Intent` appositi ai Settings di sistema Android se un permesso rosso (`🔴`) viene tappato dall'utente.
  4. Eseguito deploy con adb per validazione versionName ed estrazione test visuale UI su emulatore.

---
### 📅 2026-03-21 00:26 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-003 — DEVIAZIONE-DINAMICA
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Completata l'infrastruttura di Inoltro Chiamata (Silent Forwarding).
  1. Integrato e autorizzato `CALL_PHONE` in `AndroidManifest` (Manifest e Request).
  2. Costruito il layer di comunicazione `UssdManager` per invocare i codici di deviazione WindTre (`**21*0421633844#` e `##21#`).
  3. Modificato `CallScreeningServiceImpl`: disattivato il blocco hard (`setDisallowCall(false)`), attivato il Mute UI (`setSilenceCall` e `setSkipNotification`) ed integrata l'invocazione ad `UssdManager.activateForward()`.
  4. Realizzato `PhoneStateListener`/`TelephonyCallback` persistente agganciato ad `applicationContext` per attendere `CALL_STATE_IDLE` (chiamata conclusa) e sganciare la deviazione (`##21#`).
  5. Eseguito avvio su emulatore e validazione build su SDK 34.

---
### 📅 2026-03-20 23:25 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-002 — IMPLEMENTAZIONE CORE SCREENING E ROOM DB
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creata l'infrastruttura nativa Android per la gestione chiamate.
  1. Configurazione progetto Gradle minimale con App compatibile S22 Ultra (Target 34).
  2. Aggiunto `ContactCacheManager` per gestire la query rubrica con `HashSet` in background ed aggiornamenti a 15 min.
  3. Modificato `CallScreeningServiceImpl` per operare asincronamente. STEP 1 = `respondToCall` sincrono immediato. Nessuna decodifica complessa da Main.
  4. Implementato layer Room DB `StoppAiDatabase` e `CallLogEntry`.
  5. Sviluppata la UI in `MainActivity` col log dei blocchi tramite RecyclerView.
  6. Rimosso ogni riferimento a dialer predefinito e `InCallService`.

---
### 📅 2026-03-20 01:07 | Agente: ARIA (Antigravity)
- **TASK**: TASK-SA-001 — SETUP-STOPPAI
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Setup iniziale progetto StoppAI.
  1. Inizializzato Git e collegato origin.
  2. Creata struttura cartelle richiesta.
  3. Copiati e aggiornati i file markdown dal progetto precedente.
  4. Aggiornato GEMINI.md con la struttura del file e regole per l'agente.
  5. Creato file SKILL.md in .agent/skills/.
  6. Eseguito primo commit su GitHub ("TASK-NS-081").

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-072-CRASH-FIX — SOLVED CRITICAL DB CRASH
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Risolto crash fatale nelle chiamate private/fisse. 
  1. Avvolte tutte le chiamate `logToCrm` in `executor.execute`.
  2. Garantita segregazione thread tra risposta telecom (Main) e storage (Background).
  3. Allineato `handleFixedOrPrivate` al protocollo asincrono.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-071 — DISABLE-INCALLSERVICE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Disabilitato `InCallServiceImpl` nel Manifest (`enabled="false"`) per consolidare il blocco via Screening.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-070 — FIX-BLOCCO-DEFINITIVO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Ripristinato blocco totale (`disallowCall=true`) e inserito log CRM `DA_GESTIRE` sincronizzato.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-069 — DIAGNOSI-SCREENING
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Analisi della latenza e comportamento del framework Telecom. Identificato `disallowCall(false)` come causa squilli residui.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-068 — FIX-BLOCCO-PROTEZIONE-BASE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Risposta telecom resa sincrona (`respondToCall`) per eliminare sfarfallio UI dialer e squilli.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-067 — FIX-SMS-PROTEZIONE-BASE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Implementata routine `sendSmartParsingSms` con delay 5s per invio OTP in Protezione Base.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-066 — FIX-PROTEZIONE-OFF-TOTALE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Bonifica di `InCallServiceImpl` per permettere chiamate normali quando gli switch sono OFF.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-065 — FIX-SWITCH-OFF
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Primo inserimento bypass `respondToCall(ALLOW)` se protezione disattivata.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-064 — FIX-UI-E-NAVIGAZIONE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Aggiornata `ActiveCallActivity` con: 1. Lookup Contatti, 2. Toggle Speaker, 3. Home Navigation, 4. Auto-Redirect fine chiamata.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-063-A — FIX-OTP-E-PROTEZIONE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Implementata funzione `normalizeNumber` (+39) obbligatoria per ogni confronto DB e screening.

---
### 📅 2026-03-18 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-063-WAKE — RISVEGLIO
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Rilettura SKILL ed allineamento operativo.

---
### 📅 2026-03-17 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-049 — COMMIT-VERONICA-CORE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Commit InCallService completo,
  risposta automatica ARIA, registrazione 
  audio chiamante, notifica push, Mini CRM PENDING

---
### 📅 2026-03-17 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-048-PRE — COMMIT-DEFAULT-DIALER-SCREENS
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Commit schermate Default Dialer 
  approvate da Mario (IncomingCall, ActiveCall, Dialer)

---
### 📅 2026-03-17 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-046 — DEFAULT-DIALER-SCREENS
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Predisposizione StoppAI come App Telefono Predefinita.
  1. Realizzata **IncomingCallActivity** (Schermata 1) con DNA Inspector dinamico e badge rischio.
  2. Realizzata **ActiveCallActivity** (Schermata 2) con timer durata chiamata (`Chronometer`) e controlli Mute/Speaker.
  3. Redesign High-Premium del **Dialer** (Schermata 3) con tasti spaziosi da 82dp ispirati a iOS e distribuzione tramite Constraint Chains.
  4. Implementato **InCallServiceImpl** per la gestione degli eventi telefonici e l'apertura delle UI proprietarie.
  5. Aggiornato **AndroidManifest.xml** e **MainActivity.kt** per la richiesta del ruolo ROLE_DIALER di sistema.

---
### 📅 2026-03-17 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-045 — COMMIT-UI-V16.1-CHECKPOINT
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Commit checkpoint UI v16.1 approvata da Mario

---
### 📅 2026-03-17 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-044 — UI-FIX-SHIELD-COMPLETE
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Perfezionamento UI e logica Shield v16.1.
  1. Implementato modulo **DNA Inspector** (Shield screen) con ProgressBar dinamica, badge rischio (SICURO/SOSPETTO/PERICOLOSO) e mock operator/zona.
  2. Aggiunta sezione **DNA Inspector** nel Dettaglio Chiamata, posizionata sopra il Risk Score.
  3. Popolato il **Centro Notifiche** con mock realistici (blocchi, link sospetti, aggiornamenti) completi di trigger azioni.
  4. Bonifica totale della nomenclatura: rimosso "Shield Premium" ovunque, sostituito con **SHIELD** (piani: FREE, PRO, SHIELD).
  5. Aggiunta autorizzazione **App Telefono Predefinita** nelle Impostazioni con icona dedicata e collegamento al sistema.
  6. Risolto bug di compilazione su `NotificationsActivity` (obsoleta) allineandola alla nuova struttura a frammenti.
  7. Verificata integrità della build Gradle e installazione APK v16.1.

**📁 FILE MODIFICATI:**
- activity_shield.xml, activity_call_detail.xml, activity_referral.xml, activity_settings.xml
- item_dna_inspector.xml, item_notification.xml (CREATI)
- ShieldFragment.kt, CallDetailFragment.kt, NotificationsFragment.kt, SettingsFragment.kt
- NotificationsActivity.kt (Cleanup)
- ROADBOOK.md

---
### 📅 2026-03-17 | Agente: Ambrogio (Antigravity)
- **TASK**: TASK-NS-042 — COMMIT-UI-V16-CHECKPOINT
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Commit checkpoint UI v16.0 approvata da Mario prima sviluppo Shield

**📁 FILE MODIFICATI:**
- ROADBOOK.md → root progetto

---
### 📅 2026-03-17 10:30 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-040] — SHIELD-FULL-FUNCTIONAL
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Completamento integrale del piano Shield "Fully Functional".
  1. Migrazione DB a Room (Kotlin) con seed automatico di 20+ chiamate realistiche (AI transcription, risk score, sentiment).
  2. Implementazione Onboarding (primo avvio) e Profile/Stats/AI/Shield/Referral/Notifications/Info.
  3. Refactoring CrmActivity con gestione stati a 3 vie (Checkbox-style) e azioni batch (Friend/Spam/Delete).
  4. Implementazione Dettaglio Chiamata Shield con dati AI completi e salvataggio note manuali.
  5. Refactoring Settings con checklist permessi reale e funzione "Wipe Database".
  6. Integrazione completa dei flussi di navigazione tramite BottomNav e Header.
  7. Build di successo verificata via Gradle.

### 📅 2026-03-16 22:55 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-035] — UI-MAIN-SCREEN-REDESIGN
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Ricostruzione integrale della MainActivity in Kotlin con nuovo linguaggio visivo.
  1. Implementato layout `activity_main.xml` con Header statico, Stats Banner collassabile e Bottom Nav a 5 schede.
  2. Creato registro chiamate (`RecyclerView`) con supporto per stati CRM (Pending/Spam/Allowed) e badge colorati.
  3. Integrata logica per piani **Free/Pro/Shield** con popup informativi bloccanti per funzioni avanzate.
  4. Implementato DNA Inspector (MAIN-001 a MAIN-006) attivabile con 3 tap sul footer.
  5. Migrato il progetto a Kotlin e aggiornata l'infrastruttura Gradle.
  6. Generato APK v16.0 in `./releases/STOPPAI_v16.0_NewUI.apk`.

### 📅 2026-03-16 22:45 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-034] — UPDATE-PIANI-NOMENCLATURA
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Nomenclatura piani aggiornata in tutto il progetto (Free/Pro/Shield).
  1. Aggiornato GEMINI.md con la nuova sezione piani.
  2. Verificato PROJECT_STEPS.md.
  3. Modificato SKILL.md con nuovi prezzi e servizi.
  4. Aggiornato ROADBOOK.md.


### 📅 2026-03-16 21:50 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-032] — BRANCH-NUOVA-UI
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creazione ambiente di sviluppo per la nuova interfaccia.
  1. Creato branch locale `NUOVA-UI-MOBILE-BY-CLAUDE`.
  2. Effettuato push sul repository remoto GitHub.
  3. Verificato posizionamento corretto sul nuovo branch.

### 📅 2026-03-16 11:05 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-030] — CREATE-SKILL-FILE
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creazione della Skill operativa ufficiale del progetto.
  1. Creata cartella `.agent/skills/no-spam-android/`.
  2. Scritto file `SKILL.md` con il protocollo Aldo/Ambrogio, regole Room DB, infrastruttura porte e visione prodotto.

### 📅 2026-03-15 21:05 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-028] — RIUNIONE-MODAL-FIX
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Potenziamento Modalità Riunione con logica PRO e DNA.
  1. Implementato modal complesso con DNA `SCUDO-MODAL-RIUNIONE`.
  2. Aggiunte opzioni timer (2 ore, Personalizza) e sezione deviazione contatti rubrica (PRO).
  3. Aggiunta selezione orario di fine automatico con sbarramento PRO.
  4. Organizzato layout con separatori e switch interattivi simulati.

### 📅 2026-03-15 20:38 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-027] — SCUDO-IFRAME-FIX
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Eliminazione definitiva iframe e ricorsione simulatore.
  1. Modificata logica globale in `dna-inspector.js` per bloccare l'iniezione del simulatore nelle pagine sotto `/app/`.
  2. Bonifica totale di `scudo.html` con verifica assenza tag iframe dinamici o statici.
  3. Ripristinata integrità header `SCUDO-002`.

### 📅 2026-03-15 20:30 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-026] — APP-SIMULATED-DATA
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Resa l'app interattiva con dati simulati e sistema di modal.
  1. Creato sistema di modal `modals.css` e logica `ui-utils.js` per feedback "In sviluppo".
  2. Implementata logica Switch ON/OFF e Banner di stato in `scudo.html`.
  3. Aggiunta sezione "Ultime intercettazioni" e "Modalità Riunione".
  4. Popolato CRM con 8 chiamate simulate e interattività completa.
  5. Estesi i feedback simulati a tutte le pagine dell'applicazione.

### 📅 2026-03-15 19:55 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-025] — ENCODING-IFRAME-FIX
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Bonifica profonda e cleanup del frontend app.
  1. Corretti testi mojibake nei file `abbonamento.html`, `ai.html`, `aiuto.html` e `privacy.html`. Ripristinate tutte le accentate e le emoji corrotte.
  2. Rimosso ogni riferimento a iframe da `scudo.html` (verificato via script).
  3. Completato `index.html` dell'app con supporto DNA Inspector, DEV MODE bar e meta tag corretti.
  4. Verificata l'integrità dell'encoding UTF-8 (senza BOM) su tutti i file modificati.

### 📅 2026-03-15 13:40 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-023] — UTF8-ENCODING-FIX
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Bonifica totale dell'encoding UTF-8 in tutto il progetto frontend. 
  1. Forzato `<meta charset="UTF-8">` come primo elemento nell'head di tutti i file HTML (landing, dashboard, app/*.html).
  2. Aggiunto `@charset "UTF-8";` a tutti i file CSS.
  3. Inserito `// -*- coding: utf-8 -*-` in cima a tutti i file JavaScript.
  4. Corrotte sequenze "mojibake" ripristinate manualmente in emoji e caratteri accentati (🛡️, 📊, 👥, 📞, ⚙️, attività, novità, ecc.).
  5. Configurato Nginx (`docker-compose.yml`) con `charset utf-8;` e `source_charset utf-8;` per garantire la corretta erogazione dei contenuti.
  6. Riavviati i container Docker e verificata la corretta visualizzazione su localhost:3500.

### 📅 2026-03-14 — Sessione pomeriggio/sera
### Agente: Ambrogio (Antigravity)

- **TASK-NS-005**: Fotografia ambiente Hetzner.
  Server Ubuntu 24.04, Docker 29.2.0 attivo,
  136GB disco, 7.6GB RAM. Scoperto bridge FCM
  già attivo su porta 3000.

- **TASK-NS-006**: Security fix Hetzner.
  Token FCM spostato in .env, gitignore creato,
  bridge FCM riavviato e funzionante.

- **TASK-NS-007**: Backend webhook Twilio creato
  su porta 5200. Endpoint /webhook/voice risponde
  con TwiML in italiano voce "alice".
  Health check attivo su /health.

- **TASK-NS-008**: App v15.3 — Switch scudo ON/OFF,
  Carrier Intelligence visibile, Log di sviluppo
  con condivisione. Codici USSD **21* e ##21#
  integrati per deviazione automatica.

- **TASK-NS-009**: Crash fix Samsung OneUI.
  Sostituito MaterialSwitch con SwitchCompat,
  protetti NPE e TelephonyManager.
  APK v15.3.1 generato.

- **TASK-NS-011**: USSD fix ho. Mobile.
  Formato internazionale (00) e fallback (0).
  Avvio silenzioso su Samsung.
  Switch OFF di default.
  APK v15.3.2 generato.

- **TASK-NS-012**: UI Cleanup & Frontend Scaffold.
  Rimosse voci ID Chiamante/Sblocco da Settings Android.
  Log Spostato in Dialog (menu segreto).
  Creato Frontend Locale su localhost:3000 con Mobile Preview.
  Generato APK v15.4.

- **TASK-NS-013**: Docker Port Audit (Localhost).
  Effettuato audit dei container attivi sul PC locale.
  Creato report DOCKER_STATUS_LOCAL.md.

- **TASK-NS-014**: App Frontend Rebuild.
  Spostato Frontend locale su porta 3500 (liberata 3000).
  Ricostruita Landing Page con sezioni Hero, Funzioni e Piani.
  Aggiornati Frontend/Dashboard con menu e Mockup App Android integrato nel pupup di preview.
  Generato PDF a colori della Landing Page per presentazione.

- **TASK-NS-015A**: App Navigation Structure & DNA Inspector.
  Creata struttura app in /frontend/app/ con navigazione a 4 tab e menu hamburger.
  Implementato DNA Inspector (ALT+D) con tooltip e copia in clipboard.
  Scaricate immagini reali da Unsplash in locale per le sezioni Hero.
  Configurato redirect e routing locale su porta 3500/app/.

- **TASK-NS-017**: App UI Fixes & Data Simulation.
  Corretto DNA Inspector (fix cattura ALT+D e visibilità).
  Simulati dati reali in Scudo, Registro Chiamate (5 voci) e Profilo.
  Implementata anteprima Premium "sfumata" nella sezione AI.
  Create pagine Notifiche, Aiuto, Privacy e conferma Logout.

- **TASK-NS-018**: DNA Inspector UI Switch.
  Sostituita scorciatoia ALT+D con uno switch visibile in Impostazioni.
  Implementata persistenza dello stato tramite localStorage (dna_inspector_active).
  Aggiornato dna-inspector.js per caricare lo stato all'avvio su ogni pagina.

- **TASK-NS-019**: Landing Page Restyling & SEO Logic.
  Creata struttura cartelle media/ per asset SEO-friendly.
  Scaricate 11 immagini reali alta qualità da Unsplash in locale.
  Riscritta landing.html con meta tag SEO, OpenGraph e Schema.org.
  Design Premium con navbar sticky, hero fullwidth e sezioni semantiche.
  Ottimizzazione Mobile-First (95% width) e layout accessibile.

- **TASK-NS-020**: Landing UI Polish & Realism.
  Aumentato font-size globale (body 1.1rem, titoli 32px+, card 1.5rem).
  Pulizia Hero: rimosso testo ridondante sovrapposto.
  Aggiornata sezione Funzioni con immagini di persone reali e smartphone.
  Migliorata leggibilità e contrasto in tutte le sezioni.

- **TASK-NS-022**: DNA Preview & Simulator Deep Fix.
  DNA Inspector potenziato con Event Delegation (ispezione di OGNI elemento).
  Tooltip DNA ora mostra il percorso completo (es: CARD > BUTTON) e tag info.
  CTRL+Click implementato per copia rapida info elemento con flash di conferma.
  Simulatore App fluttuante e trascinabile (Drag & Drop) incorporato globalmente.
  Pulsanti di ridimensionamento dinamico (S/M/L) e chiusura [x] nel simulatore.
  Fix Preview: forzato caricamento di localhost:3500/app/ in ogni istanza.
  Mappatura DNA estesa a Dashboard e Frontend Area Riservata.

- **DECISIONI ARCHITETTURALI**:
  * Numero Twilio unico condiviso +13505004274
  * Modello sessione: chiamante + ricevente abbinati
    dal backend
  * Tre modalità operative pianificate:
    NORMALE / RIUNIONE / OFF
  * Modalità Riunione con timer (30min/1h/2h)
  * Punti di valore per landing page documentati
### 📅 2026-03-14 18:35 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-009] — CRASH-FIX-V15.3
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Corretto crash all'avvio su Samsung S22 Ultra. Sostituito `MaterialSwitch` con `SwitchCompat` per compatibilità OneUI. Aggiunti controlli null-safety su binding UI e try-catch su `detectCarrier` (TelephonyManager). Generato APK v15.3.1 in `releases/STOPPAI_v15.3.1_S22_Hotfix.apk`.
---
### 📅 2026-03-14 18:18 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-008] — SHIELD-SWITCH & CARRIER-DISPLAY
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Ripristinato ambiente mobile in `android/`. Implementata sezione SCUDO con switch per deviazione USSD verso Twilio (+13505004274). Aggiunto rilevamento operatore real-time. Creato `DevLogManager` e sezione UI per "Log di Sviluppo" (solo DEBUG) con funzione di condivisione. Risolto bug risorse XML. Generato APK v15.3 in `releases/STOPPAI_v15.3_ShieldLogs.apk`.
---
### 📅 2026-03-14 17:58 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-007] — BACKEND-WEBHOOK-TWILIO
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creato e avviato il backend per il webhook di Twilio su porta 5200. Container `nospam-backend-twilio` attivo in `/opt/nospam-backend/`. Implementato endpoint POST `/webhook/voice` che risponde con TwiML (messaggio di benvenuto in italiano). Verificato funzionamento tramite health check e curl.
---
### 📅 2026-03-14 17:48 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-006] — SECURITY-FIX-HETZNER
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Messi in sicurezza i dati sensibili sul server Hetzner. Creato file `.env` in `/opt/nospam-cloud/` con il token FCM. Modificato `hetzner_fcm_trigger.js` per leggere il token da `process.env`. Aggiornato `docker-compose.yml` per caricare il file `.env`. Creato `.gitignore` per proteggere `.env` e `firebase-service-account.json`. Riavviato container `nospam-fcm-bridge` con successo.
---
### 📅 2026-03-14 17:25 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-005] — HETZNER-ENVIRONMENT-CHECK
- **Autorizzato da**: Mario (CEO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Eseguito audit completo del server Hetzner 46.225.14.90 in sola lettura. Creato `HETZNER_STATUS.md` nella root del progetto con tutti i risultati. Scoperto container `nospam-fcm-bridge` già attivo su porta 3000, directory `/opt/nospam-cloud/` con bridge FCM operativo, Python 3.12.3 sul host, Docker 29.2.0 attivo. Porte 4200 e 5200 libere e pronte. RAM 6.6GB disponibile, disco 136GB libero.
---
### 📅 2026-03-14 10:44 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-NS-006] — SALVA-DIARIO
- **Autorizzato da**: Mario (CEO) + Aldo (CTO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creato `DIARIO-PROGETTO.md` nella root del progetto (v1.0 — Capitolo Fondativo, 9 capitoli).
---
---
### 📅 2026-03-14 10:21 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-STOPPAI-V16.4-PROTOCOLLO-PERMANENTE]
- **Autorizzato da**: Mario (CEO) + Aldo (CTO)
- **STATUS**: ✅ COMPLETATO
- **AZIONI**: Creato `.agent/rules/PROTOCOLLO-MARIO.md` con regole permanenti per ogni sessione (Git, File, Piano, Zero Iniziative, Fine Task).
---
---
### 📅 2026-03-14 09:31 | Agente: Ambrogio (Antigravity)
- **TASK**: [TASK-STOPPAI-V16.2-CLEAN-SETUP] — Pulizia e setup struttura progetto
- **Autorizzato da**: Mario (CEO) + Aldo (CTO)
- **STATUS**: ✅ COMPLETATO

**BACKUP CREATO (`_BACKUP_DOCS/`):**
- GEMINI.md, SICUREZZA.md, PROJECT_STEPS.md, ROADBOOK.md
- docker-compose.yml, regole-core.md, regole-fabbro.md

**CARTELLE ELIMINATE:**
- `_DEV/` (549 file — codice Android su GitHub, branch: `numeri-fissi-e-privati`)
- `mobile_capacitor/`
- `releases/` (14 APK)

**STRUTTURA CREATA:**
- `backend/webhook/twilio_handler.py`
- `backend/api/routes.py`
- `backend/db/models.py`
- `backend/config/settings.py`
- `backend/requirements.txt`
- `backend/Dockerfile`
- `backend/README.md`
- `frontend/src/pages/dashboard.html`
- `frontend/src/components/call-log.html`
- `frontend/src/assets/style.css`
- `frontend/README.md`

**FILE INTATTI (non toccati):** `.agent/`, `.vscode/`, GEMINI.md, SICUREZZA.md,
PROJECT_STEPS.md, docker-compose.yml, up.bat, tmp.java
---
---
### 📅 2026-03-14 01:20 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V15.2 - BONIFICA CLOUD-READY
- **STATUS**: ✅ RISOLTO — BUILD OK
- **AZIONI**:
    1. **FIX CRITICO Zero-Latency**: `respondToCall()` ora chiamato SEMPRE sincrono, prima di qualsiasi operazione DB. Logica Mobile resa sincrona grazie a `allowMainThreadQueries()`. Zero "mezzo squillo" garantito.
    2. **OTP RIMOSSO TOTALMENTE**: `SmsRetrieverReceiver`, `SmsReceiver`, `SmsNotificationListener` ridotti a No-Op stub. Nessun riferimento a `otpCode` nel codice attivo. Campo `otpCode` rimosso da `CallLogEntry`.
    3. **Risk Analyzer DB (v5)**: Aggiunti campi `risk_score` (int 0-100) e `security_notes` (Text) alla tabella `call_log_entries`. Migrazione Room v5.
    4. **Stato Scudo Cloud**: Voce "Sblocco Impostazioni Limitate" sostituita con "Stato Scudo Cloud" (placeholder Twilio AI con dialog informativo).
    5. **Build**: APK `STOPPAI_v15.2_CloudReady.apk` generato e committato.
---
---
### 📅 2026-03-14 00:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V15.1 - CORRETTIVO CRM & SMART FILTER
- **STATUS**: ✅ RISOLTO — BUILD OK
- **AZIONI**:
    1. **UI Bonifica**: Rimossi pulsanti `ESPORTA LOG` e `RE-BIND SERVICE` dalla schermata principale. Footer aggiornato a "v15.1 — Smart Filter".
    2. **Menu Segreto 3-Tap**: 3 tap rapidi sul footer aprono il menu sviluppatore (Re-bind, Esporta, Wipe DB).
    3. **OTP SMS Deprecated**: Modulo invio/ricezione SMS completamente rimosso dal `CallScreeningServiceImpl`. Preparato per integrazione Twilio AI.
    4. **Blocco Zero-Latency**: `blockImmediately()` chiama `respondToCall()` PRIMA del log DB → zero "mezzo squillo" per il chiamante.
    5. **FIX Impostazioni Limitate**: Punta a `ACTION_APPLICATION_DETAILS_SETTINGS` (Info App) per accesso diretto ai permessi dell'app.
    6. **CRM Whitelist Immediata**: Azione "Attendibile" imposta `statusId=1` nel DB; il `CallScreeningService` ammette i numeri con `statusId=1` senza passare dalla rubrica.
    7. **APK**: `STOPPAI_v15.1_SmartFilter.apk` generato e committato.
---
---
### 📅 2026-03-14 00:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V15.0 - MINI CRM OPERATIVO
- **STATUS**: ✅ RISOLTO — BUILD OK
- **AZIONI**:
    1. **DB AI-Ready (v4)**: Aggiornata Entity `CallLogEntry` con campi `statusId`, `transcription`, `sentiment`, `audioUrl`. Migrazione Room v4.
    2. **Mini CRM**: Multi-selezione (Long Press) con Toolbar azioni: ✓Gestito, ✗Spam, 🗑Elimina (con confirm dialog). Badge ⚠️ Pending aggiornato in tempo reale.
    3. **Icone Stato CRM**: `[?]` Da verificare, `[V]` Attendibile, `[X]` Spam, `[-]` Ignorato per ogni riga del Registro.
    4. **Feedback Discreto**: Vibrazione custom (pattern 100-50-100ms) al ricevimento di chiamata da contatto in rubrica mentre l'app è attiva.
    5. **UI Assistente AI**: Card mockup Premium con benefici e CTA in `SettingsActivity` (€4.90/mese, Twilio/Whisper/GPT-4o mini).
    6. **Build**: APK `STOPPAI_v15.0_MiniCRM.apk` generato e committato.
---
---
### 📅 2026-03-14 00:10 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V15 - STRATEGY (Direttiva Architettuale)
- **STATUS**: ✅ ACQUISITA E MEMORIZZATA
- **AZIONI**: Creato `PROJECT_STEPS.md` con roadmap completa V15. Acquisita architettura: CallScreeningService → Rifiuto → Deviazione Twilio → Agente AI → Mini CRM. 4 Fasi schedulate: Mini CRM Locale, Predisposizione DB AI, Feedback Discreto, Integrazione Twilio/AI.
---
---
### 📅 2026-03-13 20:25 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V14.0 - FINAL REFACTOR & DATA INTELLIGENCE
- **STATUS**: ✅ RISOLTO
- **AZIONI**: 
    1. **Sanificazione Rep**: Eliminate classi `VoicemailActivity`, `GhostInCallService`, `CallManager` e rimosso file MP3 `nospam_welcome`. Repo GitHub pulito.
    2. **Filtro Dinamico**: Implementato switch in Settings per scegliere tra **Rifiuto Attivo** (deviazione) e **Silenziamento**.
    3. **Carrier Intelligence**: Migliorato rilevamento operatore (S22 Ultra Dual SIM ready) per mapping segreteria.
    4. **Data Intelligence**: Registro con Checkbox e **Expandable View** (Timestamp FULL + Block Type). Analitica dashboard con breakdown categorie (Private, Fissi, Mobile).
    5. **Database**: Migrazione Room v3 per supporto `callType`.
---
---
### 📅 2026-03-13 18:45 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V13.0 - SMART VOICEMAIL REFACTOR & TO-DO LIST
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Dismessa logica Trojan (Risposta Automatica/MP3). Implementato **Rifiuto Attivo** (`setRejectCall`) per deviare target in segreteria. Aggiunto pulsante **Smart Voicemail** con auto-lookup operatore SIM. Registro trasformato in **To-Do List** interattiva con Checkbox e persistenza stato `isHandled` (Migrazione Room v2). Rimossi permessi invasivi.
---
---
### 📅 2026-03-13 17:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V12.6 - SAMSUNG AUTO-ANSWER FIX (OEM Restricion Bypass)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Ottimizzata la risposta automatica per dispositivi Samsung/OEM. Aggiunta la **5° Voce Checklist** per il permesso `ANSWER_PHONE_CALLS` (Consenso Risposta Automatica). Implementata logica di **Fallback Legacy** nel `ProtectionService`: se `acceptRingingCall` fallisce, il sistema simula un evento hardware `KEYCODE_HEADSETHOOK` per forzare l'aggancio della linea. Tuning dei delay preservato a 1500ms per stabilità iniezione audio.
---
---
### 📅 2026-03-13 16:35 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V12.4 - GHOST ANSWER & CLEANUP (Automatic Voice Injection)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Pulizia UI: rimosso il popup automatico "Watchtower" all'avvio nel `MainActivity`. Implementata la logica Ghost Answer nel `ProtectionService`: risposta automatica silente dopo 1500ms di attesa, iniezione audio del file `nospam_welcome.mp3` forzata su `STREAM_VOICE_CALL` e silenziamento del microfono locale per privacy. Aggiunto backgrounding automatico (ritorno alla Home) subito dopo la risposta per nascondere il Dialer di sistema.
---
---
### 📅 2026-03-13 16:15 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V12.3 - SILENT INTERCEPT FIX (Stealth Call Seizure)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementato il silenziamento forzato dello squillo per numeri Privati e Fissi. Utilizzata combinazione di `setSkipNotification(true)` e `setSilenceCall(true)` nel `CallResponse`. Aggiunto Piano B con `AudioManager.RINGER_MODE_SILENT` per garantire l'assenza di audio prima dell'intercettazione. Inserito Toast diagnostico "Silenziamento in corso..." per feedback immediato al CEO.
---
---
### 📅 2026-03-13 15:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V12.2 - UI CHECKLIST REFACTOR (System Configuration Card)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Refactoring completo della pagina Impostazioni. Creata la card "CONFIGURAZIONE SISTEMA" con checklist dinamica per i 4 permessi critici (Account di Chiamata, Role Screening, Batteria, Restrizioni). Implementata logica `onResume` per l'aggiornamento real-time delle icone stato (🔴/🟢). Collegati i 4 Intent nativi di Android per la configurazione rapida. Rimosse funzionalità SMS personalizzate obsolete.
---
---
### 📅 2026-03-13 11:15 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V12.0 - NEW BRANCH CREATION (Dev Environment Setup)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Creato il nuovo branch di sviluppo `numeri-fissi-e-privati` partendo dalla base stabile v6.6. Pubblicato il branch su GitHub. Aggiornato l'header della UI in **"v6.6 - DEV (Fissi/Privati)"** per identificare chiaramente l'ambiente di sviluppo. Il lavoro sulle nuove logiche per numeri privati e fissi procederà esclusivamente su questo branch.
---
---
### 📅 2026-03-13 11:15 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V11.2 - FINAL BRANCH PURGE (Git Cleanup Done)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Completata la pulizia strutturale del repository. Eliminato il branch di transizione `stable-recovery` sia localmente che remotamente. Il repository GitHub ora ospita esclusivamente il branch `main` allineato alla versione stabile v6.6. Eseguito `git fetch --prune` e generato l'APK finale certificato dal branch `main`.
---
---
### 📅 2026-03-13 11:15 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V11.1 - MERGE & CONSOLIDATE (Main Status Finalized)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Consolidata la stabilità della versione **v6.6 (Stable OTP)** eseguendo il merge del branch `stable-recovery` nel branch `main`. Creato il Git Tag ufficiale `v6.6-OFFICIAL-STABLE`. Verificato il codice sorgente: logica OTP cellulari attiva (2 SMS sequenziali), Whitelist sessione persistente e UI allineata. Generato APK ufficiale dal branch `main`.
---
---
### 📅 2026-03-13 10:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V10.9 - GIT CLEANUP RECOVERY (Purge & Environment Reset)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Eliminato definitivamente il branch instabile `FEATURE-STOPPAI-FISSI-SCONOSCIUTI` sia in locale che sul repository remoto GitHub. Creato il nuovo branch `stable-recovery` partendo dal commit consolidato `b2edf4a`. Consolidato l'ambiente di lavoro sulla versione **v6.6 (Stable OTP)** con un commit di sincronizzazione. Eseguito `git fetch --prune` per pulire i riferimenti obsoleti.
---
---
### 📅 2026-03-13 10:45 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V6.6 - RESTORE STABLE OTP (Full Rollback to b2edf4a)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Eseguito rollback completo al commit `b2edf4a`. Ripristinata la logica stabile di validazione OTP per cellulari sconosciuti (Blocco immediato + 2 SMS). Reinserita la Whitelist temporanea (Sessione) nel database locale senza aggiunta in rubrica. Aggiornata UI e build.gradle alla versione **v6.6 (Stable OTP)**. Eseguito `clean` e generato APK certificato.
---
---
### 📅 2026-03-11 01:15 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V6.5 - SMART PARSING (Logic improvement & Dash logs)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementato nuovo formato SMS `[OTP]-[HASH]`. Aggiornata la logica di ricezione con Smart Parsing basato sul carattere separatore `-`. Inseriti log dettagliati nel Registro Protezione per monitorare l'estrazione dell'OTP e l'esito del confronto (`MATCH OK / FALLITO`). La Dashboard ora persiste correttamente lo storico delle chiamate.
---
---
### 📅 2026-03-11 01:10 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V6.4 - OPERATIONAL DASHBOARD (Rebuild UI & Log Logic)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Rebuild totale della `MainActivity` con rimozione del debug box e implementazione del "Registro Protezione" (RecyclerView dashboard). Aggiornata la logica di sblocco in `SmsRetrieverReceiver` per supportare il match OTP e gli stati operativi (`AUTORIZZATO`, `OTP Errato`, `Bloccato - SMS inviato`). Inserito il supporto per i log tecnici in tempo reale in cima alla lista delle attività.
---
---
### 📅 2026-03-11 00:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V6.2 - SECURITY LOCK (OTP Verification Logic)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementata la verifica dell'OTP durante la ricezione tramite `SmsRetrieverReceiver`. Il sistema estrae le prime 4 cifre del messaggio e le confronta con l'OTP salvato nel database per i numeri in stato PENDING. Sblocco autorizzato solo in caso di match perfetto. Aggiunti log di debug per monitorare i tentativi di sblocco.
---
---
### 📅 2026-03-11 00:45 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V6.1 - ZERO ERROR (One-String SMS optimization)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Ottimizzati i template SMS per eliminare ogni margine di errore dell'utente. Il primo SMS è puramente informativo. Il secondo SMS contiene la stringa magica `[OTP][HASH]` (es. 1234XLjrbbsuadi) senza spazi o testo aggiuntivo, pronta per essere copiata e incollata integralmente. Delay di 5s confermato.
---
---
### 📅 2026-03-11 00:35 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V5.9 - FORCE UPDATE (Clean & Dual SMS Delay)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Eseguito `gradlew clean` e rimossa directory `app/build`. Implementato `Executors.newSingleThreadExecutor()` per gestire il delay di 5s senza bloccare il servizio. Inseriti testi statici precisi per evitare conflitti con vecchi file `strings.xml`. Nuovo formato secondo SMS: `"Codice: [OTP] [HASH]"`.
---
---
### 📅 2026-03-11 00:30 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V5.8 - DYNAMIC SHIELD (Reliable Double SMS & 5s Delay)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Eseguito `gradlew clean` profondo. Implementato delay di 5 secondi tra il primo SMS informativo e il secondo contenente l'OTP+Hash. Aggiornato template SMS per informare l'utente dell'attesa. Il secondo SMS ora contiene `[OTP] [HASH]` per un triggering immediato del Retriever API.
---
---
### 📅 2026-03-11 00:05 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V5.7 - DOUBLE TAP (UX Optimization - Two SMS Logic)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Modificata la logica di invio SMS nel `CallScreeningServiceImpl`. Ora vengono inviati due messaggi: uno informativo e uno contenente solo l'App Hash per facilitare il copia-incolla sul telefono chiamante. Inserito delay di 500ms tra i due inviati.
---
---
### 📅 2026-03-11 23:15 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V5.6 - RETRIEVER SHIELD (Official SmsRetriever & FG Security)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementata l'API `SmsRetriever` per la ricezione SMS automatica. Generato `AppSignatureHelper` per calcolare l'App Hash richiesto da Google. Aggiornato `CallScreeningServiceImpl` per inviare SMS con istruzioni basate sull'Hash. Attivato `ProtectionService` in Foreground all'intercettazione per proteggere il processo su Android 14.
---
---
### 📅 2026-03-10 21:55 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V5.2 - RECOVERY & SHIELD (CallScreening fix, OTP via SmsManager, Foreground Shield)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Ripristinato il `CallScreeningService` come meccanismo primario di intercettazione. Implementato l'invio immediato dell'SMS OTP tramite `SmsManager` con `PendingIntent` per Android 14. Aggiunto l'avvio forzato del `ProtectionService` (Foreground) all'intercettazione per proteggere il processo. Logging dettagliato nel Debug Box per ogni fase (Intercettazione, Invio OTP, Errori).
---
---
### 📅 2026-03-10 21:45 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.7 - DEEP DIAGNOSTIC (Service Rebind & Init Logs)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementato `requestRebind()` in `onListenerDisconnected()` per forzare il servizio su Android 14. Aggiunto log di inizializzazione "[SISTEMA] Watchtower inizializzata" per confermare l'avvio del servizio nel Debug Box. Reinserito il permesso `SEND_SMS` nel Manifest.
---
---
### 📅 2026-03-10 21:20 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.6 - RAW SENSOR (Universal Notification Diagnostic)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Rimosso il filtro package in `SmsNotificationListener` per intercettare TUTTE le notifiche di sistema nel Debug Box. Aggiornato lo stato della Watchtower in `MainActivity` per leggere direttamente la configurazione di sicurezza di Android.
---
---
### 📅 2026-03-10 20:45 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.5 - THE WATCHTOWER (Notification Listener Service)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementato `SmsNotificationListener` per intercettare gli OTP direttamente dalle notifiche, superando i blocchi di Android 14 sugli SMS. Rimossi i vecchi receiver e permessi SMS diretti. Aggiunta logica in `MainActivity` per guidare l'utente all'abilitazione del servizio nelle impostazioni di sistema.
---
---
### 📅 2026-03-10 20:05 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.4 - THE EARDRUM FIX (Runtime SMS Permissions, Manifest Receiver Fixes)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Forzata richiesta permessi `RECEIVE_SMS` e `READ_SMS` all'avvio nel `onCreate` di `MainActivity` con codice richiesta 101. Verificata registrazione `SmsReceiver` in `AndroidManifest.xml` con `exported="true"` e priorità 999. Aggiornato build.gradle a v26.
---
---
### 📅 2026-03-10 18:25 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.3 - SMS DEBUGGER (Debug Box, Raw SMS Interception)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Aggiunta Debug Box in `MainActivity` per visualizzare gli SMS in tempo reale. `SmsReceiver` ora esegue un broadcast immediato dei dati grezzi del mittente e del messaggio per facilitare la diagnosi degli errori di matching. Aggiornato build.gradle a v25.
---
---
### 📅 2026-03-10 17:10 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.2 - LOG & OTP FIX (SmsReceiver Normalization, History UI, Share Intent)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Corretta ricezione OTP con Regex e normalizzazione a 10 cifre in `SmsReceiver`. Aggiunta visualizzazione cronologia espandibile in `MainActivity`. Implementata funzione di condivisione log tramite Intent. Aggiornato build.gradle a v24.
---
---
### 📅 2026-03-10 15:58 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: Compilazione fisica APK V4.0
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Eseguito `gradlew assembleDebug` dopo configurazione `local.properties`. Generato e spostato l'APK in `/releases/STOPPAI_v4.0_DatabaseEra.apk`.
---
---
### 📅 2026-03-10 15:30 | Agente: Ambrogio (Antigravity)
- **RICHIESTA**: V4.0 - THE DATABASE ERA (Room DB, Screening Logic, Statistics UI, Settings)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Implementato Room Database per persistenza. Nuova logica di screening (Contacts -> Verified -> Block/OTP). Restyling UI con 3 statistiche card-based. Creata SettingsActivity per messaggi personalizzati e wipe DB. Aggiornato build.gradle a v22.
---
---
### 📅 2026-03-09 21:19 | Agente: Ambrogio
- **RICHIESTA**: V2.1 - CORE FIX - Salvataggio Milestone (Blocco Suoneria e SMS)
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Completata la logica di blocco chiamate nativo, SMS out-of-band in thread background, aggiornamento SDK a 34 e commit git.
---
### 📅 2026-03-09 | Agente: Ambrogio
- **RICHIESTA**: Upgrade Mobile-Ready (Capacitor Integration).
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Creata cartella `/mobile_capacitor/`. Aggiornate specifiche in GEMINI.md, regole mobile in SICUREZZA.md, regole-fabbro e regole-core.
---
### 📅 2026-03-09 | Agente: Ambrogio
- **RICHIESTA**: Genesi Progetto con specifica porte e IP Hetzner.
- **STATUS**: ✅ RISOLTO
- **AZIONI**: Creazione cartelle e blocco porte 4001/5001. Setup porte 5200/4200.
---
