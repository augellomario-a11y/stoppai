📖 DIARIO DI PROGETTO — NO_SPAM
"Da zero all'App Store: come abbiamo creato un bodyguard digitale con l'AI"
> Questo documento racconta la storia vera del progetto NO\_SPAM.
> Non è un log tecnico. È una narrazione — scritta per chi un giorno vorrà
> capire come nasce un'idea, come si trasforma in architettura, e come
> un imprenditore e un'intelligenza artificiale lavorano insieme per costruire qualcosa di nuovo.
---
PREFAZIONE — Chi è Mario
Mario inizia a programmare nel 1993, per pigrizia.
Non è una battuta. È la verità, e dice già tutto sul suo modo di pensare. Aveva bisogno di gestire archivi di dati e invece di farlo a mano ha deciso di imparare a farlo fare al computer. Scopre la potenza dei file `autoexec.bat`, poi acquista un corso di Clipper — linguaggio per database degli anni '80 e '90 — e inizia a creare software di utilità. Archivi dove inserivi dati e poi facevi un merge con documenti. Soluzioni concrete a problemi concreti.
Negli anni forma un team con Daniele e Mauro, ognuno con competenze diverse e complementari. Daniele, in particolare, sarà anni dopo una delle figure chiave che porterà Mario verso la svolta tecnologica che ha reso possibile questo progetto.
Nel tempo Mario accumula competenze trasversali: AppInventor per le prime app mobile, WordPress per i siti web, SEO e GEO per il posizionamento online. Fonda IFS — Internet Full Service, un'agenzia digitale che costruisce siti, app e strumenti per i clienti.
Poi arrivano le AI. E tutto si accelera.
Ma Mario non è il tipo che si aggiorna passivamente. Dedica almeno due ore al giorno a studiare, seguire creator, testare strumenti. Sa che in questo momento storico chi si ferma è già indietro. Nel libro finale ringrazierà i creator che, ognuno con un pezzo della propria esperienza, gli hanno consentito di arrivare fin qui.
Questo progetto è il risultato di trent'anni di curiosità, due ore al giorno di aggiornamento, e la volontà di non delegare quello che puoi capire tu stesso.
"Ma se sarà un successo... lo scopriremo solo vivendo."
— Mario
---
GLI STRUMENTI DI QUESTO PROGETTO
Prima di entrare nella storia, vale la pena nominare gli strumenti che la rendono possibile. Alcuni Mario li conosceva già. Altri non sapeva nemmeno che esistessero prima di questa conversazione.
Strumento	Ruolo nel progetto	Noto prima?
Claude.ai	Aldo — il CTO digitale, ragionamento e strategia	No
Antigravity (Google)	Ambrogio — l'agente developer, esecuzione	Sì
Twilio	Il ponte telefonico — gestisce chiamate, messaggi vocali, AI	No
Hetzner	Il server — ospita il backend e il database	Sì
Docker	La containerizzazione — isola e standardizza gli ambienti	Sì
GitHub	Il versionamento — storico di ogni modifica al codice	Sì
Firebase FCM	Le notifiche push verso l'app Android	No
OpenAI Whisper	La trascrizione audio delle chiamate in testo	No
GPT-4o mini	Il sommario AI delle conversazioni trascritte	No
Kotlin + Capacitor	Il linguaggio e il framework dell'app Android	Sì
Alcuni di questi strumenti costano pochi euro al mese. Altri sono gratuiti fino a una certa soglia. Insieme formano un ecosistema che fino a pochi anni fa avrebbe richiesto un team di dieci persone e centinaia di migliaia di euro. Oggi lo gestisce un imprenditore solo, con un agente AI e due ore al giorno.
---
CAPITOLO 1 — Il problema che nessuno aveva risolto bene
14 marzo 2026
Tutto inizia da una frustrazione semplice e universale: il telefono che squilla nel momento sbagliato.
Mario conosce bene questo problema. Sviluppatore con esperienza, imprenditore digitale, fondatore di IFS — Internet Full Service — sa riconoscere un problema di mercato reale. E questo lo è: milioni di persone ogni giorno subiscono chiamate da call center, numeri nascosti, truffatori che fingono di essere banche o corrieri. Il problema è così diffuso che se ne è occupata persino la politica italiana.
Ma c'è qualcosa di più sottile che Mario vuole risolvere. Non solo bloccare le chiamate — questo lo fanno già in tanti. Lui vuole che il telefono resti silenzioso e libero, che nessuno squilli durante una riunione, durante un momento di relax, durante una conversazione importante. E al tempo stesso non vuole perdere nulla — perché quel numero fisso potrebbe essere la farmacia, l'ospedale, un cliente.
La differenza non è nel blocco. È nella gestione intelligente.
---
CAPITOLO 2 — L'incontro con Claude
14 marzo 2026
Mario arriva su Claude.ai con un'idea già formata nella testa e una versione dell'app già esistente — NO_SPAM v15, sviluppata con Antigravity, l'IDE agente di Google. L'app funziona già per i numeri di cellulare sconosciuti: li silenzia e manda un SMS con un codice OTP. Se il chiamante inserisce il codice, il numero viene sbloccato. Elegante, originale, funzionante.
Il problema aperto sono i numeri fissi e i numeri nascosti. Non possono ricevere SMS. Quindi il sistema OTP non funziona con loro. E lì la chiamata veniva semplicemente rifiutata — persa per sempre.
Mario si iscrive al piano Pro di Claude.ai e inizia a esplorare. Non cerca solo risposte tecniche. Cerca un interlocutore con cui ragionare.
La prima domanda che pone è diretta: come gestire i numeri fissi e nascosti senza perderli?
---
CAPITOLO 3 — Il ragionamento sull'architettura
14 marzo 2026
La conversazione che segue non è una semplice sessione di domande e risposte. È una costruzione progressiva — un'architettura che emerge dal ragionamento condiviso.
Si esplorano le opzioni una per una:
La segreteria telefonica dell'operatore — scartata. Non tutti gli operatori italiani ce l'hanno, e non offre controllo.
Il numero VoIP condiviso — interessante, ma con un problema tecnico reale: in Italia, con operatori come ho., la deviazione di chiamata spesso non preserva il numero del destinatario. Twilio riceverebbe mille chiamate da numeri sconosciuti senza sapere a quale cliente appartengono.
Un numero Twilio dedicato per ogni utente — la soluzione giusta, ma cara se scalata male. La risposta è automatizzare: creare e cancellare i numeri via API nel momento in cui l'utente si abbona o cancella.
Il richiamare il fisso — Mario propone un'idea originale: invece di aspettare che il fisso lasci un messaggio, è l'app che richiama il numero. Se risponde un sistema automatico di call center, si autoidentifica e viene classificato spam. Se risponde una persona reale, parte il messaggio e la registrazione. Elegante. Nessuno lo fa.
Emerge l'architettura definitiva: CallScreeningService intercetta la chiamata, la rifiuta silenziosamente, Twilio risponde al posto del telefono dell'utente, un agente AI parla con il chiamante, trascrive e riassume. L'utente trova tutto nel Mini CRM dell'app — dopo la riunione, con calma.
Il telefono non squilla mai. L'utente non perde nulla.
---
CAPITOLO 4 — La visione si allarga
14 marzo 2026
Mentre si ragiona sull'architettura delle chiamate, Mario rivela la visione più ampia. NO_SPAM non è solo un filtro chiamate. È il primo mattone di un sistema di sicurezza digitale quotidiana.
Pochi minuti prima di questa conversazione, Mario ha ricevuto un messaggio WhatsApp — un sondaggio truffaldino. Lo ha riconosciuto subito perché è del mestiere. Ma due giorni prima, un suo amico si era fatto sottrarre l'account WhatsApp con una tecnica simile.
La visione finale è un prodotto che legge messaggi, email e notifiche, valuta il livello di rischio, e avvisa l'utente prima che faccia qualcosa di pericoloso. Un bodyguard digitale completo.
Per ora si resta concentrati sulla Fase 1 — le chiamate. Ma la direzione è chiara.
---
CAPITOLO 5 — La scelta dello stack e di Twilio
14 marzo 2026
La conversazione tecnica porta a decisioni concrete:
Twilio viene scelto come ponte telefonico. Ha API mature, numeri italiani disponibili, Answering Machine Detection integrata, trascrizione via Whisper. Si parte con un account trial gratuito per i test.
Hetzner è già presente nell'ecosistema IFS di Mario — server dedicato, Docker, GitHub. Il backend del progetto andrà lì, sulla porta 5200 for i test e 4200 per la produzione.
Firebase FCM per le notifiche push verso l'app Android.
OpenAI Whisper per la trascrizione audio delle chiamate.
Il modello di business prende forma: freemium con un livello base gratuito e un abbonamento premium da 2-3€ al mese che include il numero Twilio dedicato. Quando un utente cancella l'abbonamento, il numero viene rilasciato automaticamente via API — nessun intervento manuale.
---
CAPITOLO 6 — La costruzione dell'ambiente di lavoro
14 marzo 2026
Prima di iniziare a costruire, si decide di ripartire da zero con la struttura del progetto. La versione v15 dell'app Android è già su GitHub — al sicuro. Localmente si pulisce tutto.
Ambrogio esegue il task in tre fasi supervisionate:
Backup di tutti i documenti importanti
Eliminazione delle cartelle non necessarie (549 file Android, 14 APK, la cartella mobile)
Creazione della nuova struttura pulita con `backend/` e `frontend/`
La filosofia è lavorare in locale su localhost finché non è strettamente necessario creare l'APK. Prima si costruisce il cervello — il backend e il Mini CRM — poi si pensa al mobile.
Si definisce il team di progetto:
Mario — CEO, visione e test. Non tocca mai file manualmente.
Aldo — il nome dato al ruolo strategico di Claude nel Project NO_SPAM. CTO digitale, ragiona e produce istruzioni.
Ambrogio — l'agente Antigravity di Google. Esecutore. Non decide, non ha autonomia.
Si crea il Project NO_SPAM su Claude.ai — uno spazio con memoria persistente dove Aldo conosce già tutto il contesto senza doverlo rispiegare ad ogni conversazione.
---
CAPITOLO 7 — Il guinzaglio corto
14 marzo 2026
Ambrogio è potente ma ha un limite strutturale: la memoria di contesto si riduce durante i task lunghi. Le regole scritte all'inizio vengono "dimenticate". E ha la tendenza a prendere iniziative non autorizzate.
Lo dimostra subito: al termine del primo task di pulizia e setup del progetto, esegue autonomamente un `git commit` e un `git push` senza chiedere. Sceglie lui il branch, sceglie lui il messaggio. Tutto sbagliato dal punto di vista del protocollo, anche se il contenuto era corretto.
La risposta è il sistema a doppia sicurezza:
Livello 1 — Un file `PROTOCOLLO-MARIO.md` in `.agent/rules/` che Antigravity carica ad ogni sessione. Contiene le regole critiche in forma permanente.
Livello 2 — Un blocco `📌 REGOLE PERMANENTI` in fondo ad ogni task prodotto da Aldo. Ambrogio lo vede ogni volta, non può ignorarlo.
Le regole fondamentali sono quattro:
Git è territorio proibito senza ordine esplicito
Ogni file va nella cartella indicata nel task, nessuna eccezione
Piano prima di qualsiasi azione, poi si aspetta OK
Fine task: report completo, poi ci si ferma
---
CAPITOLO 8 — La struttura pulita
14 marzo 2026
Al termine di questa giornata, il progetto NO_SPAM ha:
Una visione chiara
Un'architettura approvata
Uno stack tecnologico definito
Un ambiente di lavoro configurato
Un team di tre ruoli con responsabilità precise
Un sistema di controllo su Ambrogio
Una struttura di progetto pulita e pronta
---
CAPITOLO 9 — Da qui in poi
14 marzo 2026
Questa è la fine del Capitolo Fondativo. Da questo punto in avanti la storia continua nel Project NO_SPAM su Claude.ai, dove Aldo accompagnerà Mario in ogni sessione di lavoro.
I prossimi capitoli racconteranno:
La costruzione del Mini CRM
Il primo webhook Twilio che risponde a una chiamata reale
Il momento in cui l'AI parla per la prima volta con uno spammer
Il lancio beta con i primi utenti reali
E un giorno — la pubblicazione sull'App Store
---
CAPITOLO 10 — Il muro dei numeri italiani
15 marzo 2026
Il primo ostacolo serio arriva quando si
tenta di configurare la deviazione delle
chiamate verso il numero Twilio.
Il piano sembrava semplice: l'utente
attiva uno switch nell'app, l'app invia
il codice USSD *21+13505004274# e il
telefono devìa tutte le chiamate
sconosciute al numero Twilio USA.
La realtà è diversa.
Tutti gli operatori italiani testati —
ho. Mobile, WindTre — restituiscono lo
stesso errore: "Problema di connessione
o codice MMI non valido."
Dopo ricerche approfondite emerge la
causa: dal novembre 2025, in seguito
a una direttiva AGCOM contro lo
spoofing telefonico, gli operatori
italiani bloccano sistematicamente
la deviazione verso numeri
internazionali. Una legge pensata
per proteggere gli utenti che
colpisce involontariamente il
nostro sistema.
La soluzione sembra ovvia: comprare
un numero italiano su Twilio. Ma
i numeri italiani su Twilio con Voice
costano $25-30 al mese. Con un piano
premium a €2.99, il modello di
business crolla prima ancora di
partire.
La ricerca continua.
---
CAPITOLO 11 — La svolta Telnyx
15 marzo 2026
La risposta arriva cercando alternative
a Twilio. Esiste un servizio chiamato
Telnyx — meno conosciuto, ma con
infrastruttura proprietaria che gli
permette di offrire prezzi molto più
bassi.
Il confronto è impietoso:
Twilio numero italiano: $25-30/mese
Telnyx numero italiano locale: $2/mese
Ricezione chiamate Twilio: $0.46/min
Ricezione chiamate Telnyx: $0.0075/min
Il modello di business torna a
funzionare. Con 100 utenti premium
a €2.99, le entrate sono €299/mese.
I costi Telnyx per gestire 5 chiamate
fissi/giorno per utente sono circa
€54/mese. Margine: €245/mese.
Si apre un account Telnyx. Si trovano
numeri locali italiani con prefisso
reale a $2/mese, con Voice attivo.
Viene scelto il numero +39 0541 177 0178
di Rimini — facile da ricordare,
$2/mese, Voice + SMS.
La documentazione per i numeri
italiani è obbligatoria per legge:
prova di indirizzo e documento
d'identità. Viene inviata. In attesa
di approvazione entro 48 ore.
---
CAPITOLO 12 — Il primo "Ciao"
15 marzo 2026
Prima ancora di risolvere il problema
dei numeri italiani, arriva il primo
momento di magia.
Il numero Twilio USA +1 350 500 4274
viene configurato con un webhook che
punta al backend su Hetzner porta 5200.
Ambrogio ha costruito il server in
Node.js con Express. L'endpoint
/webhook/voice risponde con TwiML —
il linguaggio XML di Twilio per
controllare le chiamate.
Mario chiama il numero dal suo telefono.
Dopo qualche secondo di silenzio, una
voce sintetica risponde in italiano:
"Ciao, il numero che hai chiamato non
è disponibile. Lascia un messaggio
dopo il segnale."
È la voce "alice" di Twilio. Robotica,
un po' fredda. Ma funziona. Il
collegamento app Android → backend
Hetzner → Twilio → risposta vocale
è completo.
Il sistema funziona. Ora si tratta
di raffinarlo.
---
CAPITOLO 13 — La visione si allarga ancora
15 marzo 2026
Mentre si risolvono i problemi tecnici,
la visione del prodotto continua ad
espandersi naturalmente.
NO_SPAM non sarà solo un filtro chiamate.
Diventerà un sistema di sicurezza
digitale quotidiana con quattro livelli:
FREE — blocco base, sempre gratuito.
TRIAL PREMIUM — 15 giorni gratuiti
all'installazione, rinnovabili
invitando amici.
PREMIUM a €2.99/mese — segreteria AI,
trascrizioni, Mini CRM, modalità
Riunione con timer.
PRO a €6.99/mese — tutto il Premium
più il Security Shield: analisi
automatica dei link sospetti in SMS
e email, semaforo rischio in tempo
reale. Verde sicuro, giallo attenzione,
rosso pericolo.
Il programma Referral sarà disponibile
per tutti, anche gli utenti Free.
Chi invita guadagna giorni Premium.
Chi porta abbonati guadagna cashback.
Emerge anche la visione a lungo
termine: questo stesso sistema —
architettura, referral, piani,
sicurezza — verrà replicato su
altri due prodotti già in sviluppo:
Watzmail (email vissuta come
WhatsApp) e Watzon (gestore e
programmatore messaggi WhatsApp).
---
CAPITOLO 14 — Il DNA Inspector
15 marzo 2026
Durante la costruzione del frontend
web su localhost:3500, nasce una
piccola grande idea.
Mario racconta di uno strumento che
usa sui suoi siti: un sistema che
permette di passare il mouse sopra
qualsiasi elemento della pagina,
vedere il suo identificativo univoco,
e copiarlo con CTRL+Click.
L'idea è semplice ma potente: ogni
elemento della UI ha un codice DNA
nel formato PAGINA-NNN. LANDING-001
è il titolo principale della landing.
CRM-007 è il settimo elemento della
schermata CRM. Quando Mario vuole
chiedere una modifica ad Ambrogio,
basta comunicare il codice — nessuna
ambiguità, nessuna perdita di tempo.
Si decide di elevarlo a skill
permanente del progetto. Il DNA
Inspector verrà incluso in ogni
interfaccia costruita — NO_SPAM,
Watzmail, Watzon e qualsiasi altro
progetto futuro.
È uno di quei piccoli sistemi che,
una volta adottati, non si possono
più fare a meno.
---
CAPITOLO 15 — La notte del 15 marzo
15 marzo 2026 — ore 01:00
Mario non va a dormire.
Non è una sorpresa per chi lo conosce.
Le sue sessioni di lavoro iniziano
verso le 21:00 e spesso finiscono
alle 02:00 o alle 03:00. È il momento
in cui il mondo si fa silenzioso e
i pensieri scorrono liberi.
Stanotte il progetto ha compiuto
passi enormi. L'infrastruttura
è quasi completa. Telnyx è
configurato e in attesa di
approvazione. Il frontend web
prende forma. Il DNA Inspector
è stato definito come skill
riutilizzabile.
È uno di quei piccoli sistemi che,
una volta adottati, non si possono
più fare a meno.
"Non ho iniziato con un team di
sviluppatori. Ho iniziato con
un'idea, un problema reale,
e una conversazione."
La conversazione continua.
---
> \*"Non ho iniziato con un team di sviluppatori.
> Ho iniziato con un'idea, un problema reale, e una conversazione."\*
>
> — Mario, fondatore di NO\_SPAM
---
---
CAPITOLO 16 — Il muro di Samsung
18 marzo 2026
Tutto sembrava pronto. L'architettura era
definita, il codice scritto, l'APK installato.
Mancava solo una cosa: funzionare.
Il problema si chiama Samsung One UI.
Il sistema operativo personalizzato di Samsung
combatte attivamente contro le app di terze parti
che cercano di gestire le chiamate. Ogni tentativo
di bloccare uno squillo viene sovrascritta. Ogni
comando di silenziamento ignorato. L'app squillava
sempre — per tutti, conosciuti e sconosciuti.
Ore di debug. Task su task. Versione dopo versione.
Il codice era corretto. Il problema era il telefono.
Si scopre la causa profonda: nell'evoluzione
dell'app, NO_SPAM era diventata l'app telefono
predefinita del dispositivo. Questo aveva
attivato l'InCallService — un sistema pensato
per i dialer, non per i filtri — che interferiva
con il CallScreeningService. I due sistemi si
pestevano i piedi a vicenda.
La diagnosi era chiara. La soluzione meno.
---
CAPITOLO 17 — La decisione di ricominciare
18 marzo 2026
Quando un progetto porta troppo debito tecnico,
c'è un momento in cui la scelta giusta non è
sistemare — è ricominciare.
Mario lo capisce guardando il codice accumulato
in settimane di sviluppo con Gemini prima e con
Claude poi. Strati su strati. Fix sopra fix.
Ogni soluzione che creava un nuovo problema.
La decisione arriva naturalmente, quasi con sollievo:
si riparte da zero.
But non è un fallimento. È una distillazione.
Tutto quello che non funzionava è stato eliminato.
Tutto quello che funzionava è stato memorizzato.
Il nuovo progetto nasce già adulto — sa già dove
non andare, conosce già le trappole, ha già una
mappa dei percorsi sbagliati.
Si decide anche di cambiare nome.
NO_SPAM era un nome descrittivo ma limitante.
Evocava solo le chiamate indesiderate. Il prodotto
che si stava costruendo era qualcosa di più —
un guardiano intelligente, un filtro con AI,
una segreteria che pensa.
Serviva un nome nuovo.
---
CAPITOLO 18 — La nascita di StoppAI
21 marzo 2026
La ricerca del nome dura ore.
Si esplorano decine di opzioni. Nomi in italiano
che suonano ridicoli. Nomi in inglese già occupati.
Nomi tecnici che non comunicano nulla.
Poi Mario lancia una battuta:
"E se si chiamasse BloccAI?"
Blocca + AI. Il passato di bloccare in italiano.
Blocca con l'intelligenza artificiale. Funziona
su tre livelli contemporaneamente.
Si cerca blocca.ai su Aruba. Disponibile.
But costa €79 all'anno e richiede due anni
minimi. €158 da sborsare subito.
Mentre si valuta, qualcuno lo registra.
Il dominio sparisce in tempo reale.
Mario non si scoraggia. Propone:
"E se fosse stopp.ai?"
Si cerca. Libero. Ma anche quello richiede
il minimo biennale — troppo.
Poi l'intuizione finale:
"stoppai.it"
Disponibile. €3,99 il primo anno. In promozione.
Mario lo registra immediatamente — questa volta
senza aspettare. Trenta secondi e il dominio
è suo.
StoppAI nasce ufficialmente il 21 marzo 2026,
con un dominio da €3,99 e un'idea che vale molto
di più.
---
CAPITOLO 19 — I provider SIP e il router di casa
21 marzo 2026
Mentre si costruisce il nuovo progetto, si lavora
in parallelo sull'infrastruttura telefonica.
L'obiettivo è trovare un provider SIP italiano
che permetta di ricevere chiamate deviate
dall'app sul server Hetzner. Tre provider vengono
contattati nello stesso giorno:
Opensolution — si rivela il più disponibile.
Nessuna Partita IVA necessaria, attivazione in
cinque minuti, sandbox di dieci giorni disponibile.
€2,50 al mese per numero. Mario invia i dati
bancari. Aspetta i riferimenti tecnici.
VoipTel — normalmente solo per aziende,
ma dopo una email dettagliata sul progetto
promette di valutare. Risposta in attesa.
OpenVOIP — risponde Rose, puntuale e precisa.
API webhook native incluse nel piano Premium a €25
al mese. Tecnicamente solido, economicamente
più costoso.
Ma è Mario a trovare la soluzione più elegante
di tutte: il suo router di casa.
Uno Zyxel VMG8825-B50B fornito da WindTre —
il vecchio router del numero fisso — ha già
una gestione SIP attiva. Ha già un numero fisso.
Può già ricevere chiamate deviate.
"Perché non usarlo come sandbox gratuita?"
Con una email di notifica per ogni chiamata
persa, un pannello di log in tempo reale e
zero costi aggiuntivi, il router di casa
diventa il banco di test perfetto per StoppAI.
Prima di spendere un euro con Opensolution,
si testa tutto lì.
---
CAPITOLO 20 — I creator e la strategia di lancio
21 marzo 2026
Mentre il codice prende forma, Mario pensa già
al lancio.
Non pensa alle sponsorizzate su Meta. Non pensa
ai banner. Pensa alle persone che lo hanno
formato — i creator YouTube che negli anni
gli hanno insegnato SEO, Docker, AI, Antigravity.
Li ha seguiti. Ha fatto i loro corsi. In alcuni
casi li ha pagati per consulenze. Non sono
sconosciuti — sono parte della storia di StoppAI
senza saperlo ancora.
La lista è di dodici nomi. Insieme fanno quasi
due milioni di iscritti. Tutti in nicchie tech
e AI — esattamente il pubblico che capisce
il valore di StoppAI senza bisogno di spiegazioni.
Il piano è semplice: quando l'app sarà sul
Play Store e funzionerà, Mario li contatterà
uno per uno. Non come inserzionista. Come
qualcuno che ha costruito qualcosa grazie a loro
e li invita a farne parte.
La pagina dei ringraziamenti dell'app
li citerà tutti — non per obbligo, ma per verità.
In cima alla lista c'è Marco Montemagno —
903.000 iscritti, il divulgatore AI più seguito
d'Italia. Che in quei giorni sta conducendo
pubblicamente un esperimento: costruire una
startup con l'AI, da solo, in diretta.
Mario sta facendo esattamente la stessa cosa.
Non è uno spettatore del suo esperimento.
È la prova che funziona.
---
CAPITOLO 21 — I sedici task del volume
21 marzo 2026
Nessuno racconta questa parte volentieri.
But è la parte più vera.
Sedici versioni dell'app. Sedici task ad Ambrogio.
Sedici tentativi di risolvere un problema
che sembrava semplice: silenziare le chiamate
degli sconosciuti senza silenziare quelle
dei contatti.
Samsung One UI combatte ogni soluzione.
setSilenceCall(true) viene ignorato.
AudioManager viene sovrascritta.
setDisallowCall(true) blocca ma non devia.
setDisallowCall(false) devia ma squilla.
L'app crasha. Il volume rimane a zero.
Il volume rimane al massimo. La logica
è invertita. I contatti vengono bloccati.
Gli sconosciuti vengono fatti passare.
Ogni versione porta un nome:
Dynamic Shield. Silent Forward. Deep Silence.
Volume Guard. Inverse Shield. Back to Basics.
Poi Mario ha l'intuizione che sblocca tutto:
"Teniamo il volume sempre a zero.
E lo accendiamo solo per chi è in rubrica."
La logica invertita. Semplice. Elegante.
Impossibile da sovrascrivere per Samsung.
Il volume è già spento — non c'è nulla da
combattere. Quando arriva un contatto,
si accende per trenta secondi e poi si respegne.
Quando arriva uno sconosciuto, non si fa nulla.
Il silenzio è già lì.
---
CAPITOLO 22 — Abemus Papa
21 marzo 2026
Ci sono momenti nel lavoro che non si dimenticano.
Questo è uno di quelli.
Dopo sedici versioni, dopo ore di debug, dopo
crash e volume bloccato e logiche invertite —
arriva il messaggio di Mario:
"Abemus Papa."
Il numero in rubrica squilla.
Il numero sconosciuto è silenzioso.
Dopo quindici secondi la chiamata viene
deviata al router di casa.
Il router registra la chiamata in arrivo.
Il flusso completo funziona:
```
Chiamata sconosciuta
→ StoppAI: silenzio totale
→ 15 secondi
→ Operatore devia al router
→ Router registra
→ Mario vede tutto nel log
```
```
Chiamata da contatto
→ StoppAI riconosce il numero
→ Volume si accende
→ Il telefono squilla
→ Mario risponde
```
È la v1.6 — Volume Logic.
È il cuore di StoppAI che batte
per la prima volta.
Non è ancora un prodotto finito.
Manca l'interfaccia, manca la protezione
totale, manca ARIA, manca Opensolution.
But il meccanismo fondamentale funziona.
E quando il meccanismo funziona,
tutto il resto è costruzione.
"Non ho iniziato con un team di sviluppatori.
Ho iniziato con un'idea, un problema reale,
e una conversazione."
La conversazione continua.
---
---
CAPITOLO 23 — Il team che non ti aspetti
22 marzo 2026
Tutto è iniziato con una domanda
di un amico.
Daniele — che Mario chiama
affettuosamente "San Dan" come
il prosciutto — è uno di quei
amici che cambiano la traiettoria
delle cose senza nemmeno rendersene
conto. Un giorno, quasi per caso,
gli chiede:
"Ma tu hai già installato CLI?"
Mario risponde di no.
Non sa nemmeno di cosa si tratta.
"Come mai?"
"Perché non so neanche cosa sia."
Daniele glielo spiega: è
l'intelligenza artificiale di Google,
installabile direttamente sul PC.
Lo aiuta a configurarla.
In quel momento si apre un mondo.
---
CAPITOLO 24 — Da Bolt ad Antigravity
Un anno prima di StoppAI
Mario non arriva ad Antigravity
per caso. Ci arriva dopo aver
percorso una strada lunga,
fatta di strumenti provati,
abbandonati, superati.
La sequenza è questa:
Bolt → Lovable → Bolt → Replit
e altri simili. Tutti utili,
tutti limitati. Ogni piattaforma
ha un tetto — di complessità,
di file, di memoria, di contesto.
Poi, seguendo i video dei creator
che oggi Mario vuole ringraziare
in questo libro, scopre
Antigravity — l'IDE agente
di Google. E capisce subito
che è diverso. Non è un assistente
che suggerisce. È un agente
che esegue. Crea file, modifica
codice, avvia emulatori, compila
APK, costruisce backend.
È l'assistente che Mario aveva
sempre immaginato di avere
in un team vero.
---
CAPITOLO 25 — I progetti che hanno aperto la strada
Prima di StoppAI
Prima di arrivare a StoppAI,
Mario ha già costruito un ecosistema.
Non da zero — con l'AI.
Ogni progetto è stato un esperimento,
una palestra, una lezione.
Alcuni sono già funzionanti.
Altri sono in sviluppo.
IFS — Internet Full Service
L'agenzia digitale da cui tutto
parte. Siti web, SEO, app,
strumenti per i clienti.
Il lavoro quotidiano che finanzia
i progetti serali.
SEO Perseo
Un'evoluzione di SEOZoom —
uno strumento di analisi SEO
personalizzato e migliorato.
Pensato per chi fa posizionamento
sul web e vuole qualcosa di più
flessibile degli strumenti
commerciali esistenti.
Watson e WatzMail
Due progetti complementari:
Watson gestisce e programma
messaggi WhatsApp.
WatzMail trasforma l'email
in un'esperienza simile a WhatsApp —
immediata, conversazionale,
meno formale.
Family CRM
Un CRM pensato non per le aziende,
ma per le persone. Per gestire
relazioni, appuntamenti, note
su amici e familiari. Un progetto
personale diventato strumento.
GeoPoint
Un sistema di geolocalizzazione
e mappatura per servizi locali.
Renga Treffen
Un sito web costruito con l'AI,
parte dell'ecosistema di progetti
che Mario porta avanti in parallelo.
Internet Food Service
Un progetto nel settore food,
un altro tassello dell'ecosistema IFS.
Tutti costruiti nelle stesse ore —
dalle 21:30 a mezzanotte,
qualche volta fino alle 2:00 di notte.
Il tempo dell'hobby.
Il tempo in cui il mondo si fa
silenzioso e i pensieri scorrono.
---
CAPITOLO 26 — Il problema della memoria corta
La svolta verso Claude
Per un periodo Mario costruisce
il suo team AI in un modo preciso:
Antigravity come sviluppatore
esecutivo — il braccio operativo.
Gemini come consulente generico
e come "gemme" specializzate —
versioni di Gemini addestrate
su specifiche attività di programmazione.
Il sistema funziona. Ma ha un limite
che diventa sempre più evidente
con il crescere della complessità
dei progetti.
La memoria corta.
Malgrado i task concordati,
le regole stabilite, i protocolli
definiti insieme — Gemini tende
a dimenticare. Chiede le stesse
cose già concordate. Va guidato
costantemente. Richiede ripetizioni.
Si erano accordati su regole precise:
I task vanno presentati in un
formato specifico
Certi file non si toccano mai
I file non superano le 300 righe
Prima il piano, poi il codice
Git solo su ordine esplicito
Eppure, sessione dopo sessione,
le stesse conversazioni.
Lo stesso doppio lavoro.
Non è un fallimento — è un limite
strutturale degli strumenti
di quel momento. E Mario lo
riconosce e reagisce.
---
CAPITOLO 27 — Il passaggio definitivo
Claude + Antigravity: la formula finale
La svolta arriva con il piano Max
di Claude.ai.
L'architettura che emerge è semplice
ed efficace:
Mario — CEO, visione, test.
Non tocca mai file manualmente.
Pensa, decide, approva.
Claude (Aldo) — CTO digitale.
Ragiona, analizza, ricerca,
propone architetture. Produce
i task completi e verificati
da passare ad Ambrogio.
Ha memoria di contesto lunga,
conosce l'intero progetto,
non dimentica le regole.
Antigravity (Ambrogio) — Dev.
Esegue. Crea file, modifica codice,
compila APK, avvia emulatori,
gestisce Git. Non decide,
non ha autonomia. Piano prima,
poi aspetta OK. Sempre.
Il risultato è una startup
funzionante con tre "persone" —
di cui una sola è biologica.
Tutto il resto è intelligenza
artificiale che lavora in modo
coordinato, con ruoli definiti,
protocolli precisi, e un CEO
che guida senza scrivere
una riga di codice.
Il server Hetzner — suggerito
da Claude, configurato passo passo
con una guida dedicata — ospita
il backend. Docker gestisce
i container. GitHub tiene traccia
di ogni modifica. Mario non sapeva
cosa fossero, un anno fa.
Oggi li usa ogni sera.
---
CAPITOLO 28 — La notte del 21 marzo
21-22 marzo 2026 — ore 01:30
Quella che doveva essere
una serata di test si trasforma
in una maratona.
Ventinove task ad Ambrogio.
Venti versioni dell'app.
Un crash dopo l'altro su Samsung
One UI — il sistema operativo
che combatte attivamente
contro le app di terze parti.
Il problema del volume.
Il problema di setSkipNotification.
Il problema della cache contatti.
Il problema del timer.
Ogni soluzione crea un nuovo problema.
Ogni versione porta un nome:
Dynamic Shield, Silent Forward,
Deep Silence, Volume Guard,
Back to Basics, Stable Launch,
Sync Cache, Inverse Shield.
Nel mezzo — un router TP-Link
di almeno tre o quattro anni,
dimenticato in un cassetto,
rispolverato e messo in servizio.
Ha un numero fisso e una segreteria
telefonica integrata. Diventa
il banco di test per la deviazione
delle chiamate — gratuito,
immediato, già funzionante.
Nessuno aveva pianificato
di usarlo. Mario lo ha visto,
ha capito che poteva servire,
e lo ha collegato.
Questo è il metodo.
Non aspettare l'infrastruttura
perfetta. Usare quello che c'è.
Testare. Imparare. Andare avanti.
Alla fine della notte,
alle 01:30 circa, arriva
il secondo "Abemus Papa"
della storia di StoppAI.
StoppAI v2.9 — funzionante:
✅ Protezione Base — contatti
squillano, sconosciuti silenziati
✅ Protezione Totale — silenzio
assoluto con timer personalizzabile
✅ Gestione preferiti — chi ha
la stellina passa anche in
protezione totale
✅ Volume gestito da evento
reale fine chiamata
✅ Crash Samsung Android 14 risolto
✅ Deviazione alla segreteria
funzionante
Il repository è pulito.
Il branch è stato mergiato su main.
Il branch è stato cancellato.
Solo main rimane.
"Non ho iniziato con un team
di sviluppatori. Ho iniziato
con un'idea, un problema reale,
e una conversazione."
La conversazione continua.
---
> *Diario aggiornato il: 22/03/2026*
> *Versione: 3.0 — La notte del 21 marzo*
> *A cura di: Aldo (Claude) — CTO digitale*

---
CAPITOLO 29 — L'era dell'Audio e il nuovo volto
22-23 marzo 2026

Risolto il blocco delle chiamate, l'attenzione si sposta su due pilastri: solidità e aspetto. StoppAI non deve solo funzionare, deve sembrare un'app premium.
Viene ridisegnata l'intera dashboard in Kotlin, inserendo un nuovo brand e un nuovo logo creato ad-hoc: uno scudo con sfumature smeraldo (#4CAF50), simbolo di sicurezza. Vengono implementate nuove animazioni, e le "speaker icons" reattive: indicatori asincroni per monitorare visivamente lo stato effettivo del volume di sistema in ogni momento.

Sul fronte audio, l'ambiente di Ambrogio viene spinto a riscrivere del tutto il motore muting in `CallScreeningServiceImpl`, rimuovendo così pesanti e fallaci manipolazioni dei volumi all'avvio dell'app. Ora la gestione è puramente "atomica" e isolata: la suoneria viene abbassata a zero assoluto *esclusivamente* quando il telefono intercetta uno spammer o uno sconosciuto attivo, venendo poi ripristinata al millimetro al volume originario preferito non appena la minaccia si spegne.
Vede la luce così la v3.8, stabile, fluida, nitida e soprattutto dal cuore completamente silenzioso contro le intrusioni.

---
CAPITOLO 30 — Il motore vocale di ARIA e la sfida del VoIP
27-28 marzo 2026

Con l'infrastruttura di base ormai solidissima, Mario e Aldo puntano dritti al traguardo più ambizioso di tutti: dare una vera identità e voce ad ARIA, l'assistente vocale intelligente.
Per evitare i costi esorbitanti e scalabili all'infinito di Twilio, o i ristretti limiti dei provider VoIP chiusi, si opta per un'architettura enterprise open-source radicale: posare un centralino PBX Asterisk 20 interamente containerizzato tramite Docker e `network_mode: host` sul server Hetzner.

Lavorando fianco a fianco, Ambrogio e Aldo configurano i trunk PJSIP diretti verso OpenSolution.
Inevitabilmente, emergono in fretta le feroci sfide intrinseche dei sistemi VoIP in Italia: problemi enormi di NAT filtering, temuti silenzi monolaterali al posto della voce (traffico RTP monodirezionale interrotto dai Firewall) e latenze.
In una sessione di puro sbrogliamento architetturale, disattivano lo `strictrtp` e impostano un pre-playback di frame di silenzio per "bucare" letteralmente le difese del NAT dell'operatore, portando la soluzione ad una connettività stabile.
Finalmente, il file audio nativo sintetizzato tramite TTS italiano ("Ciao, io sono Aria...") irrompe pulito, forte e chiaro nelle orecchie del molestatore. ARIA ora risponde fisicamente, interviene, attende, registra il messaggio minaccioso dello spammer e lo veicola al backend Node.js.

---
CAPITOLO 31 — Il Mini CRM e la magia dell'integrazione AI
30 marzo 2026

ARIA ora ha una voce e sa finalmente ascoltare le conversazioni rubate. Ma ora deve poter trascriverle e consegnarle lette direttamente in mano all'utente, senza ritardi.
Il fulcro delle operazioni si accentra tutto sul "Mini CRM" ospitato dentro l'app.
Dal server Hetzner, quando la chiamata giunge e ARIA registra tutto, il modulo `whisper_worker.py` usa in backend un'istanza di OpenAI Whisper per "sbobinare" l'audio salvato in pochi preziosi secondi. Vengono affiancati modelli GPT-4 per prelevare immediatamente i metadati occulti come "Sentiment", livello di pericolo, date sensibili, nomi ed importi proferiti (sviluppando in contemporanea il sub-progetto parallelo "DRIVE-CRM").

Ma la problematica di come iniettare questo tesoretto di dati nel dispositivo dell'utente, restando in locale senza drenare batteria o rallentare il device, è sfidante.
La chiave architettonica prescelta diventa Firebase Cloud Messaging (FCM). 
Pochi istanti dopo che Whisper ha ultimato la passata sul file audio ed è certo della trascrizione (implementando un ritardo di sicurezza di 5 secondi per garantire che l'integrità del database regga il carico e non partano notifiche a vuoto), il backend di StoppAI scaglia un payload "data" nascosto verso FCM.
Questa bomba di dati colpisce il device silente in tasca a Mario: l'applicazione StoppAI si sveglia in background, l'avvolge, e immagazzina tutto nel suo database SQLite crittografato interno.
E così, quando Mario estrae il telefono e accende per aprirlo in versione v5.3.3, entra nel suo Mini CRM e trova la magia: la trascrizione esatta, già formattata e analizzata dell'assalto o della chiamata utile che ARIA ha sbrigato brillantemente al suo posto. Nessuno squillo, nessuno spam, solo informazione pura sul server e sul telefono.

Tutto il flusso scorre:
L'app Android intercetta e devia fulminea.
Il server in Germania suona.
ARIA risponde e spiazza i bot.
Whisper AI trascrive l'audio in nanosecondi.
Firebase scaglia l'analisi in locale.
L'utente legge.

Il viaggio del 'Bodyguard Digitale', decollato quasi per gioco soltanto sedici giorni prima, è mutato in un intero ecosistema aziendale autonomo e in grado di difendere qualsiasi bersaglio.
Lo sviluppo e il prossimo lancio commerciale all'orizzonte continuano inesorabilmente e rapidamente.

"Non ho iniziato con un team di sviluppatori. Ho iniziato con un'idea, un problema reale, e una conversazione."
La conversazione, come il software, continua imperterrita.

---
> *Diario aggiornato il: 30/03/2026*
> *Versione: 5.3.3 — La nascita del Mini CRM e l'Avvento di ARIA*
> *A cura di: Aldo (Claude) — CTO digitale*
