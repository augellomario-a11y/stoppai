# 📋 PROJECT STEPS - StoppAI V16
> Ultimo aggiornamento: 2026-03-17 01:20 | Agente: Antigravity

---

## 🎯 VISIONE STRATEGICA V16

Evoluzione da app di protezione locale a **piattaforma SaaS integrata** con Segreteria AI su Telnyx, Mini CRM e gestione smartphone-libero durante riunioni/chiamate in corso.

---

## 📌 STEP CORRENTE (BASE COMPLETATA - v14.0)

- [x] Branch `NUOVA-UI-MOBILE-BY-CLAUDE` stabilizzato
- [x] `CallScreeningService` con filtro dinamico (Rifiuto Attivo / Silenziamento)
- [x] Registro To-Do List interattivo con Checkbox + stato persistente
- [x] Icone differenziate per tipo (🕵️ Privato, ☎️ Fisso, 📱 Mobile)
- [x] Expandable View nel registro
- [x] Breakdown analitico statistiche (Privati / Fissi / Mobile)
- [x] Smart Voicemail Button con Carrier Intelligence (Dual SIM ready)
- [x] Repository GitHub sanificato

---

## 🚀 PROSSIMI STEP — STRATEGIA DI BUILD: SHIELD FIRST
Costruiamo Shield completo, poi limitiamo a ritroso.
Branch attivo: NUOVA-UI-MOBILE-BY-CLAUDE

---

## 📱 FASE A — UI COMPLETA SHIELD (in corso)

### A1 — Schermata Principale ✅
- [x] Header con nome app + icona scudo
- [x] Switch 1 Protezione Base (Free)
- [x] Switch 2 Protezione Totale + dialog durata (Pro)
- [x] Banner statistiche collassabile
- [x] Filtri chip orizzontali
- [x] Registro chiamate con bordi colorati
- [x] Badge stato CRM (Pending/Attendibile/Spam/Ignorato)
- [x] Lock 🔒 Pro e Shield su funzioni bloccate
- [x] Popup Pro e Shield al tap funzioni bloccate
- [x] Popup azioni CRM al tap voce
- [x] Multi-selezione con long press
- [x] Bottom navigation bar con label
- [x] DNA Inspector (3 tap footer)

### A2 — Schermata Mini CRM 🔲
- [ ] Lista chiamate con stati avanzati
- [ ] Filtri: Tutti / Da gestire / Attendibili / Spam / Ignorati
- [ ] Tap voce → scheda dettaglio chiamata completa
- [ ] Scheda dettaglio: numero, tipo, timestamp, risk score
- [ ] Scheda dettaglio: trascrizione completa (Shield)
- [ ] Scheda dettaglio: sommario AI (Shield)
- [ ] Scheda dettaglio: audio player placeholder (Shield)
- [ ] Whitelist rapida: [✅ Sempre] [🚫 Mai] [⏭ Ignora]
- [ ] Blacklist rapida con conferma
- [ ] Note manuali sulla chiamata (testo libero)
- [ ] Azioni batch: seleziona tutto, elimina, segna gestito

### A3 — Schermata Stats 🔲
- [ ] Grafico chiamate per giorno (ultimi 7 giorni)
- [ ] Breakdown per tipo: Privati / Fissi / Mobili
- [ ] Breakdown per stato: Spam / Attendibili / Pending
- [ ] Risk score medio del periodo
- [ ] Record giornaliero (es. "Giorno peggiore: 12 spam")
- [ ] Esporta report (Shield)

### A4 — Schermata AI 🔲
- [ ] Lista chiamate con trascrizione disponibile
- [ ] Lettore trascrizione completa
- [ ] Sommario AI in evidenza
- [ ] Badge sentiment: 😊 Neutro / ⚠️ Sospetto / 🚨 Pericolo
- [ ] Ricerca nel testo delle trascrizioni
- [ ] Filtro per sentiment
- [ ] Placeholder audio player

### A5 — Schermata Shield 🔲
- [ ] Semaforo rischio in tempo reale:
      🟢 Tutto ok / 🟡 Attenzione / 🔴 Pericolo
- [ ] Ultimo evento di rischio rilevato
- [ ] Lista link sospetti ricevuti via SMS
- [ ] Analisi link: verde sicuro / giallo attenzione / rosso pericolo
- [ ] Storico analisi sicurezza
- [ ] Impostazioni soglia rischio personalizzabile

### A6 — Schermata Impostazioni 🔲
- [ ] Checklist permessi (🔴/🟢 per ogni permesso)
- [ ] Sezione piano attivo con badge Free/Pro/Shield
- [ ] Pulsante upgrade piano → PlansActivity
- [ ] Numero Telnyx dedicato (Pro/Shield)
- [ ] Impostazioni deviazione chiamate
- [ ] Smart Voicemail con Carrier Intelligence Dual SIM
- [ ] Filtro dinamico: Rifiuto Attivo / Silenziamento
- [ ] Messaggio segreteria personalizzabile (Shield)
- [ ] Wipe DB con conferma
- [ ] Menu sviluppatore (3 tap footer)

### A7 — Schermata Piani (PlansActivity) 🔲
- [ ] Card Free con funzioni incluse
- [ ] Card Pro con funzioni incluse + prezzo €2.99/mese
- [ ] Card Shield con funzioni incluse + prezzo €6.99/mese
- [ ] Card Shield evidenziata come "Consigliato"
- [ ] Pulsante acquisto → placeholder pagamento
- [ ] Toggle mensile/annuale con sconto
- [ ] Trial 15 giorni gratis per Pro e Shield

---

## 🔧 FASE B — BACKEND + TELNYX 🔲
- [ ] Webhook server su Hetzner porta 5500
- [ ] Risposta TwiML con voce italiana
- [ ] Registrazione chiamata con Telnyx
- [ ] Answering Machine Detection (AMD)
- [ ] Pipeline: Audio → Whisper → Testo → DB
- [ ] AI Summary: Testo → GPT-4o mini → DB
- [ ] Firebase FCM: notifica push post-call
- [ ] Risk Score calcolato dal backend
- [ ] Sincronizzazione app via polling

---

## 🛡️ FASE C — SECURITY SHIELD 🔲
- [ ] Analisi link sospetti in SMS
- [ ] Semaforo rischio in tempo reale
- [ ] Alert automatico per link pericolosi
- [ ] Database pattern phishing aggiornabile
- [ ] Notifica immediata per rischio alto

---

## ✂️ FASE D — LIMITAZIONI A RITROSO 🔲

### Da Shield → Pro (cosa si toglie)
- [ ] Rimuovere analisi link sospetti
- [ ] Rimuovere semaforo rischio
- [ ] Rimuovere schermata Shield
- [ ] Rimuovere esporta report
- [ ] Rimuovere messaggio segreteria personalizzabile
- [ ] Limitare storico a 30 giorni

### Da Pro → Free (cosa si toglie)
- [ ] Rimuovere numero Telnyx dedicato
- [ ] Rimuovere trascrizioni AI
- [ ] Rimuovere sommari AI
- [ ] Rimuovere schermata AI
- [ ] Rimuovere Mini CRM avanzato
- [ ] Rimuovere Switch 2 Protezione Totale
- [ ] Limitare registro a 7 giorni
- [ ] Limitare statistiche a base

---

## 📌 PROSSIMO TASK: A2 — Schermata Mini CRM

---

## 🔧 DIPENDENZE TECNICHE

| Tecnologia | Scopo | Nota |
|---|---|---|
| **Telnyx** | Segreteria AI e Recording | Account SaaS, porta 5500 |
| **Whisper API** | Trascrizione audio | OpenAI |
| **GPT-4o mini** | Sintesi AI | OpenAI |
| **Hetzner** | Backend Webhook | 46.225.14.90 |

---

## 🏛️ REGOLE ARCHITETTURALI

1. **Il telefono non squilla mai per fissi/privati**: L'intercettazione deve essere zero-latency.
2. **Nessun dato vocale locale**: Audio e trascrizioni risiedono su Cloud. L'app mostra solo testo.
3. **La rubrica è il Whitelist**: L'aggiunta di un contatto in rubrica sblocca automaticamente il numero.
4. **Privacy first**: Nessun dato condiviso senza autorizzazione.
