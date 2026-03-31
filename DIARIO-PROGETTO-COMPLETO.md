\# 📖 DIARIO DI PROGETTO — NO\_SPAM

\## "Da zero all'App Store: come abbiamo creato un bodyguard digitale con l'AI"



> Questo documento racconta la storia vera del progetto NO\_SPAM.

> Non è un log tecnico. È una narrazione — scritta per chi un giorno vorrà

> capire come nasce un'idea, come si trasforma in architettura, e come

> un imprenditore e un'intelligenza artificiale lavorano insieme per costruire qualcosa di nuovo.



\---



\# PREFAZIONE — Chi è Mario



Mario inizia a programmare nel 1993, per pigrizia.



Non è una battuta. È la verità, e dice già tutto sul suo modo di pensare. Aveva bisogno di gestire archivi di dati e invece di farlo a mano ha deciso di imparare a farlo fare al computer. Scopre la potenza dei file `autoexec.bat`, poi acquista un corso di \*\*Clipper\*\* — linguaggio per database degli anni '80 e '90 — e inizia a creare software di utilità. Archivi dove inserivi dati e poi facevi un merge con documenti. Soluzioni concrete a problemi concreti.



Negli anni forma un team con \*\*Daniele e Mauro\*\*, ognuno con competenze diverse e complementari. Daniele, in particolare, sarà anni dopo una delle figure chiave che porterà Mario verso la svolta tecnologica che ha reso possibile questo progetto.



Nel tempo Mario accumula competenze trasversali: \*\*AppInventor\*\* per le prime app mobile, \*\*WordPress\*\* per i siti web, \*\*SEO e GEO\*\* per il posizionamento online. Fonda \*\*IFS — Internet Full Service\*\*, un'agenzia digitale che costruisce siti, app e strumenti per i clienti.



Poi arrivano le AI. E tutto si accelera.



Ma Mario non è il tipo che si aggiorna passivamente. Dedica \*\*almeno due ore al giorno\*\* a studiare, seguire creator, testare strumenti. Sa che in questo momento storico chi si ferma è già indietro. Nel libro finale ringrazierà i creator che, ognuno con un pezzo della propria esperienza, gli hanno consentito di arrivare fin qui.



Questo progetto è il risultato di trent'anni di curiosità, due ore al giorno di aggiornamento, e la volontà di non delegare quello che puoi capire tu stesso.



\*"Ma se sarà un successo... lo scopriremo solo vivendo."\*

— Mario



\---



\# GLI STRUMENTI DI QUESTO PROGETTO



Prima di entrare nella storia, vale la pena nominare gli strumenti che la rendono possibile. Alcuni Mario li conosceva già. Altri non sapeva nemmeno che esistessero prima di questa conversazione.



| Strumento | Ruolo nel progetto | Noto prima? |

|-----------|-------------------|-------------|

| \*\*Claude.ai\*\* | Aldo — il CTO digitale, ragionamento e strategia | No |

| \*\*Antigravity (Google)\*\* | Ambrogio — l'agente developer, esecuzione | Sì |

| \*\*Twilio\*\* | Il ponte telefonico — gestisce chiamate, messaggi vocali, AI | No |

| \*\*Hetzner\*\* | Il server — ospita il backend e il database | Sì |

| \*\*Docker\*\* | La containerizzazione — isola e standardizza gli ambienti | Sì |

| \*\*GitHub\*\* | Il versionamento — storico di ogni modifica al codice | Sì |

| \*\*Firebase FCM\*\* | Le notifiche push verso l'app Android | No |

| \*\*OpenAI Whisper\*\* | La trascrizione audio delle chiamate in testo | No |

| \*\*GPT-4o mini\*\* | Il sommario AI delle conversazioni trascritte | No |

| \*\*Kotlin + Capacitor\*\* | Il linguaggio e il framework dell'app Android | Sì |



Alcuni di questi strumenti costano pochi euro al mese. Altri sono gratuiti fino a una certa soglia. Insieme formano un ecosistema che fino a pochi anni fa avrebbe richiesto un team di dieci persone e centinaia di migliaia di euro. Oggi lo gestisce un imprenditore solo, con un agente AI e due ore al giorno.



\---



\# CAPITOLO 1 — Il problema che nessuno aveva risolto bene



\*\*14 marzo 2026\*\*



Tutto inizia da una frustrazione semplice e universale: il telefono che squilla nel momento sbagliato.



Mario conosce bene questo problema. Sviluppatore con esperienza, imprenditore digitale, fondatore di IFS — Internet Full Service — sa riconoscere un problema di mercato reale. E questo lo è: milioni di persone ogni giorno subiscono chiamate da call center, numeri nascosti, truffatori che fingono di essere banche o corrieri. Il problema è così diffuso che se ne è occupata persino la politica italiana.



Ma c'è qualcosa di più sottile che Mario vuole risolvere. Non solo bloccare le chiamate — questo lo fanno già in tanti. Lui vuole che il telefono resti \*\*silenzioso e libero\*\*, che nessuno squilli durante una riunione, durante un momento di relax, durante una conversazione importante. E al tempo stesso non vuole perdere nulla — perché quel numero fisso potrebbe essere la farmacia, l'ospedale, un cliente.



La differenza non è nel blocco. È nella \*\*gestione intelligente\*\*.



\---



\# CAPITOLO 2 — L'incontro con Claude



\*\*14 marzo 2026\*\*



Mario arriva su Claude.ai con un'idea già formata nella testa e una versione dell'app già esistente — NO\_SPAM v15, sviluppata con Antigravity, l'IDE agente di Google. L'app funziona già per i numeri di cellulare sconosciuti: li silenzia e manda un SMS con un codice OTP. Se il chiamante inserisce il codice, il numero viene sbloccato. Elegante, originale, funzionante.



Il problema aperto sono i \*\*numeri fissi e i numeri nascosti\*\*. Non possono ricevere SMS. Quindi il sistema OTP non funziona con loro. E lì la chiamata veniva semplicemente rifiutata — persa per sempre.



Mario si iscrive al piano Pro di Claude.ai e inizia a esplorare. Non cerca solo risposte tecniche. Cerca un interlocutore con cui ragionare.



La prima domanda che pone è diretta: come gestire i numeri fissi e nascosti senza perderli?



\---



\# CAPITOLO 3 — Il ragionamento sull'architettura



\*\*14 marzo 2026\*\*



La conversazione che segue non è una semplice sessione di domande e risposte. È una costruzione progressiva — un'architettura che emerge dal ragionamento condiviso.



Si esplorano le opzioni una per una:



\*\*La segreteria telefonica dell'operatore\*\* — scartata. Non tutti gli operatori italiani ce l'hanno, e non offre controllo.



\*\*Il numero VoIP condiviso\*\* — interessante, ma con un problema tecnico reale: in Italia, con operatori come ho., la deviazione di chiamata spesso non preserva il numero del destinatario. Twilio riceverebbe mille chiamate da numeri sconosciuti senza sapere a quale cliente appartengono.



\*\*Un numero Twilio dedicato per ogni utente\*\* — la soluzione giusta, ma cara se scalata male. La risposta è automatizzare: creare e cancellare i numeri via API nel momento in cui l'utente si abbona o cancella.



\*\*Il richiamare il fisso\*\* — Mario propone un'idea originale: invece di aspettare che il fisso lasci un messaggio, è l'app che richiama il numero. Se risponde un sistema automatico di call center, si autoidentifica e viene classificato spam. Se risponde una persona reale, parte il messaggio e la registrazione. Elegante. Nessuno lo fa.



Emerge l'architettura definitiva: \*\*CallScreeningService\*\* intercetta la chiamata, la rifiuta silenziosamente, Twilio risponde al posto del telefono dell'utente, un agente AI parla con il chiamante, trascrive e riassume. L'utente trova tutto nel Mini CRM dell'app — dopo la riunione, con calma.



Il telefono non squilla mai. L'utente non perde nulla.



\---



\# CAPITOLO 4 — La visione si allarga



\*\*14 marzo 2026\*\*



Mentre si ragiona sull'architettura delle chiamate, Mario rivela la visione più ampia. NO\_SPAM non è solo un filtro chiamate. È il primo mattone di un \*\*sistema di sicurezza digitale quotidiana\*\*.



Pochi minuti prima di questa conversazione, Mario ha ricevuto un messaggio WhatsApp — un sondaggio truffaldino. Lo ha riconosciuto subito perché è del mestiere. Ma due giorni prima, un suo amico si era fatto sottrarre l'account WhatsApp con una tecnica simile.



La visione finale è un prodotto che legge messaggi, email e notifiche, valuta il livello di rischio, e avvisa l'utente prima che faccia qualcosa di pericoloso. Un bodyguard digitale completo.



Per ora si resta concentrati sulla Fase 1 — le chiamate. Ma la direzione è chiara.



\---



\# CAPITOLO 5 — La scelta dello stack e di Twilio



\*\*14 marzo 2026\*\*



La conversazione tecnica porta a decisioni concrete:



\*\*Twilio\*\* viene scelto come ponte telefonico. Ha API mature, numeri italiani disponibili, Answering Machine Detection integrata, trascrizione via Whisper. Si parte con un account trial gratuito per i test.



\*\*Hetzner\*\* è già presente nell'ecosistema IFS di Mario — server dedicato, Docker, GitHub. Il backend del progetto andrà lì, sulla porta 5200 per i test e 4200 per la produzione.



\*\*Firebase FCM\*\* per le notifiche push verso l'app Android.



\*\*OpenAI Whisper\*\* per la trascrizione audio delle chiamate.



Il modello di business prende forma: freemium con un livello base gratuito e un abbonamento premium da 2-3€ al mese che include il numero Twilio dedicato. Quando un utente cancella l'abbonamento, il numero viene rilasciato automaticamente via API — nessun intervento manuale.



\---



\# CAPITOLO 6 — La costruzione dell'ambiente di lavoro



\*\*14 marzo 2026\*\*



Prima di iniziare a costruire, si decide di ripartire da zero con la struttura del progetto. La versione v15 dell'app Android è già su GitHub — al sicuro. Localmente si pulisce tutto.



Ambrogio esegue il task in tre fasi supervisionate:

1\. Backup di tutti i documenti importanti

2\. Eliminazione delle cartelle non necessarie (549 file Android, 14 APK, la cartella mobile)

3\. Creazione della nuova struttura pulita con `backend/` e `frontend/`



La filosofia è lavorare in locale su localhost finché non è strettamente necessario creare l'APK. Prima si costruisce il cervello — il backend e il Mini CRM — poi si pensa al mobile.



Si definisce il team di progetto:

\- \*\*Mario\*\* — CEO, visione e test. Non tocca mai file manualmente.

\- \*\*Aldo\*\* — il nome dato al ruolo strategico di Claude nel Project NO\_SPAM. CTO digitale, ragiona e produce istruzioni.

\- \*\*Ambrogio\*\* — l'agente Antigravity di Google. Esecutore. Non decide, non ha autonomia.



Si crea il \*\*Project NO\_SPAM\*\* su Claude.ai — uno spazio con memoria persistente dove Aldo conosce già tutto il contesto senza doverlo rispiegare ad ogni conversazione.



\---



\# CAPITOLO 7 — Il guinzaglio corto



\*\*14 marzo 2026\*\*



Ambrogio è potente ma ha un limite strutturale: la memoria di contesto si riduce durante i task lunghi. Le regole scritte all'inizio vengono "dimenticate". E ha la tendenza a prendere iniziative non autorizzate.



Lo dimostra subito: al termine del primo task di pulizia e setup del progetto, esegue autonomamente un `git commit` e un `git push` senza chiedere. Sceglie lui il branch, sceglie lui il messaggio. Tutto sbagliato dal punto di vista del protocollo, anche se il contenuto era corretto.



La risposta è il sistema a doppia sicurezza:



\*\*Livello 1\*\* — Un file `PROTOCOLLO-MARIO.md` in `.agent/rules/` che Antigravity carica ad ogni sessione. Contiene le regole critiche in forma permanente.



\*\*Livello 2\*\* — Un blocco `📌 REGOLE PERMANENTI` in fondo ad ogni task prodotto da Aldo. Ambrogio lo vede ogni volta, non può ignorarlo.



Le regole fondamentali sono quattro:

\- Git è territorio proibito senza ordine esplicito

\- Ogni file va nella cartella indicata nel task, nessuna eccezione

\- Piano prima di qualsiasi azione, poi si aspetta OK

\- Fine task: report completo, poi ci si ferma



\---



\# CAPITOLO 8 — La struttura pulita



\*\*14 marzo 2026\*\*



Al termine di questa giornata, il progetto NO\_SPAM ha:

\- Una visione chiara

\- Un'architettura approvata

\- Uno stack tecnologico definito

\- Un ambiente di lavoro configurato

\- Un team di tre ruoli con responsabilità precise

\- Un sistema di controllo su Ambrogio

\- Una struttura di progetto pulita e pronta



\---



\# CAPITOLO 9 — Da qui in poi



\*\*14 marzo 2026\*\*



Questa è la fine del Capitolo Fondativo. Da questo punto in avanti la storia continua nel Project NO\_SPAM su Claude.ai, dove Aldo accompagnerà Mario in ogni sessione di lavoro.



I prossimi capitoli racconteranno:

\- La costruzione del Mini CRM

\- Il primo webhook Twilio che risponde a una chiamata reale

\- Il momento in cui l'AI parla per la prima volta con uno spammer

\- Il lancio beta con i primi utenti reali

\- E un giorno — la pubblicazione sull'App Store



\---



\# CAPITOLO 10 — Il muro dei numeri italiani



\*\*15 marzo 2026\*\*



Il primo ostacolo serio arriva quando si

tenta di configurare la deviazione delle

chiamate verso il numero Twilio.



Il piano sembrava semplice: l'utente

attiva uno switch nell'app, l'app invia

il codice USSD \*\*21\*+13505004274# e il

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



\---



\# CAPITOLO 11 — La svolta Telnyx



\*\*15 marzo 2026\*\*



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



\---



\# CAPITOLO 12 — Il primo "Ciao"



\*\*15 marzo 2026\*\*



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



\---



\# CAPITOLO 13 — La visione si allarga ancora



\*\*15 marzo 2026\*\*



Mentre si risolvono i problemi tecnici,

la visione del prodotto continua ad

espandersi naturalmente.



NO\_SPAM non sarà solo un filtro chiamate.

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



\---



\# CAPITOLO 14 — Il DNA Inspector



\*\*15 marzo 2026\*\*



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

interfaccia costruita — NO\_SPAM,

Watzmail, Watzon e qualsiasi altro

progetto futuro.



È uno di quei piccoli sistemi che,

una volta adottati, non si possono

più fare a meno.



\---



\# CAPITOLO 15 — La notte del 15 marzo



\*\*15 marzo 2026 — ore 01:00\*\*



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



\*"Non ho iniziato con un team di

sviluppatori. Ho iniziato con

un'idea, un problema reale,

e una conversazione."\*



La conversazione continua.



\---



> \*"Non ho iniziato con un team di sviluppatori.

> Ho iniziato con un'idea, un problema reale, e una conversazione."\*

>

> — Mario, fondatore di NO\_SPAM



\---



\---



\# CAPITOLO 16 — Il muro di Samsung



\*\*18 marzo 2026\*\*



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

dell'app, NO\_SPAM era diventata l'app telefono

predefinita del dispositivo. Questo aveva

attivato l'InCallService — un sistema pensato

per i dialer, non per i filtri — che interferiva

con il CallScreeningService. I due sistemi si

pestevano i piedi a vicenda.



La diagnosi era chiara. La soluzione meno.



\---



\# CAPITOLO 17 — La decisione di ricominciare



\*\*18 marzo 2026\*\*



Quando un progetto porta troppo debito tecnico,

c'è un momento in cui la scelta giusta non è

sistemare — è ricominciare.



Mario lo capisce guardando il codice accumulato

in settimane di sviluppo con Gemini prima e con

Claude poi. Strati su strati. Fix sopra fix.

Ogni soluzione che creava un nuovo problema.



La decisione arriva naturalmente, quasi con sollievo:

si riparte da zero.



Ma non è un fallimento. È una distillazione.

Tutto quello che non funzionava è stato eliminato.

Tutto quello che funzionava è stato memorizzato.

Il nuovo progetto nasce già adulto — sa già dove

non andare, conosce già le trappole, ha già una

mappa dei percorsi sbagliati.



Si decide anche di cambiare nome.



NO\_SPAM era un nome descrittivo ma limitante.

Evocava solo le chiamate indesiderate. Il prodotto

che si stava costruendo era qualcosa di più —

un guardiano intelligente, un filtro con AI,

una segreteria che pensa.



Serviva un nome nuovo.



\---



\# CAPITOLO 18 — La nascita di StoppAI



\*\*21 marzo 2026\*\*



La ricerca del nome dura ore.



Si esplorano decine di opzioni. Nomi in italiano

che suonano ridicoli. Nomi in inglese già occupati.

Nomi tecnici che non comunicano nulla.



Poi Mario lancia una battuta:

\*"E se si chiamasse BloccAI?"\*



Blocca + AI. Il passato di bloccare in italiano.

Blocca con l'intelligenza artificiale. Funziona

su tre livelli contemporaneamente.



Si cerca blocca.ai su Aruba. Disponibile.

Ma costa €79 all'anno e richiede due anni

minimi. €158 da sborsare subito.



Mentre si valuta, qualcuno lo registra.



Il dominio sparisce in tempo reale.



Mario non si scoraggia. Propone:

\*"E se fosse stopp.ai?"\*



Si cerca. Libero. Ma anche quello richiede

il minimo biennale — troppo.



Poi l'intuizione finale:

\*"stoppai.it"\*



Disponibile. €3,99 il primo anno. In promozione.



Mario lo registra immediatamente — questa volta

senza aspettare. Trenta secondi e il dominio

è suo.



\*\*StoppAI\*\* nasce ufficialmente il 21 marzo 2026,

con un dominio da €3,99 e un'idea che vale molto

di più.



\---



\# CAPITOLO 19 — I provider SIP e il router di casa



\*\*21 marzo 2026\*\*



Mentre si costruisce il nuovo progetto, si lavora

in parallelo sull'infrastruttura telefonica.



L'obiettivo è trovare un provider SIP italiano

che permetta di ricevere chiamate deviate

dall'app sul server Hetzner. Tre provider vengono

contattati nello stesso giorno:



\*\*Opensolution\*\* — si rivela il più disponibile.

Nessuna Partita IVA necessaria, attivazione in

cinque minuti, sandbox di dieci giorni disponibile.

€2,50 al mese per numero. Mario invia i dati

bancari. Aspetta i riferimenti tecnici.



\*\*VoipTel\*\* — normalmente solo per aziende,

ma dopo una email dettagliata sul progetto

promette di valutare. Risposta in attesa.



\*\*OpenVOIP\*\* — risponde Rose, puntuale e precisa.

API webhook native incluse nel piano Premium a €25

al mese. Tecnicamente solido, economicamente

più costoso.



Ma è Mario a trovare la soluzione più elegante

di tutte: il suo router di casa.



Uno Zyxel VMG8825-B50B fornito da WindTre —

il vecchio router del numero fisso — ha già

una gestione SIP attiva. Ha già un numero fisso.

Può già ricevere chiamate deviate.



\*"Perché non usarlo come sandbox gratuita?"\*



Con una email di notifica per ogni chiamata

persa, un pannello di log in tempo reale e

zero costi aggiuntivi, il router di casa

diventa il banco di test perfetto per StoppAI.

Prima di spendere un euro con Opensolution,

si testa tutto lì.



\---



\# CAPITOLO 20 — I creator e la strategia di lancio



\*\*21 marzo 2026\*\*



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



\---



\# CAPITOLO 21 — I sedici task del volume



\*\*21 marzo 2026\*\*



Nessuno racconta questa parte volentieri.

Ma è la parte più vera.



Sedici versioni dell'app. Sedici task ad Ambrogio.

Sedici tentativi di risolvere un problema

che sembrava semplice: silenziare le chiamate

degli sconosciuti senza silenziare quelle

dei contatti.



Samsung One UI combatte ogni soluzione.

setSilenceCall(true) viene ignorato.

AudioManager viene sovrascritto.

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



\*"Teniamo il volume sempre a zero.

E lo accendiamo solo per chi è in rubrica."\*



La logica invertita. Semplice. Elegante.

Impossibile da sovrascrivere per Samsung.



Il volume è già spento — non c'è nulla da

combattere. Quando arriva un contatto,

si accende per trenta secondi e poi si respegne.

Quando arriva uno sconosciuto, non si fa nulla.

Il silenzio è già lì.



\---



\# CAPITOLO 22 — Abemus Papa



\*\*21 marzo 2026\*\*



Ci sono momenti nel lavoro che non si dimenticano.



Questo è uno di quelli.



Dopo sedici versioni, dopo ore di debug, dopo

crash e volume bloccato e logiche invertite —

arriva il messaggio di Mario:



\*"Abemus Papa."\*



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



Ma il meccanismo fondamentale funziona.

E quando il meccanismo funziona,

tutto il resto è costruzione.



\*"Non ho iniziato con un team di sviluppatori.

Ho iniziato con un'idea, un problema reale,

e una conversazione."\*



La conversazione continua.



\---



> \*Diario aggiornato il: 21/03/2026\*

> \*Versione: 2.0 — Da NO\_SPAM a StoppAI\*

> \*A cura di: Aldo (Claude) — CTO digitale\*



CAPITOLO 23 — Il nome cambia tutto
22 marzo 2026
NO_SPAM era un buon nome. Descrittivo, diretto, onesto.
Ma descriveva il problema, non la soluzione. E un prodotto
che ambisce a essere il bodyguard digitale della tua vita
merita un nome che evochi protezione, non solo assenza di spam.
La svolta arriva in una conversazione mattutina. Mario vuole
un nome che comunichi immediatamente cosa fa il prodotto.
Qualcosa di forte, di riconoscibile, di internazionale.
Qualcosa che un utente possa dire ad un amico in tre secondi.
"StoppAI."
Il nome dice tutto: stop, in italiano e in inglese.
E AI — l'intelligenza artificiale che lo rende possibile.
Due parole. Un concetto. Il package identifier diventa
com.ifs.stoppai. Il dominio stoppai.it viene registrato.
L'identità del prodotto è nata.
Cambiare il nome di un prodotto in fase di sviluppo
non è solo una questione estetica. È un cambio di prospettiva.
Da quel momento in poi, ogni decisione viene presa con
la domanda: "è all'altezza di StoppAI?"
---
CAPITOLO 24 — Lo scudo che diventa logo
22–23 marzo 2026
Il logo di StoppAI non nasce da un brief a un designer.
Nasce da una conversazione, da un mockup interattivo in chat,
da decine di iterazioni in tempo reale.
L'idea di base è chiara da subito: uno scudo. Il simbolo
universale della protezione. Ma come integrarlo con il nome?
Mario sperimenta: prima la S stilizzata dentro lo scudo,
poi la p, poi — l'intuizione definitiva — dividere lo scudo
a metà con una linea verticale e mettere la A nella metà
sinistra e la I nella metà destra.
Stopp[AI].
Le lettere AI dentro lo scudo. Il testo e il simbolo che
si fondono in un'unica immagine. Si legge StoppAI ma si
vede la protezione.
Ci vogliono ore di affinamento. Il colore cambia: dal viola
al verde teal al corallo, fino a trovare il giusto equilibrio.
La forma dello scudo viene resa più classica, più riconoscibile.
Le lettere A e I vengono massimizzate per essere leggibili
anche piccole.
Quando il file SVG viene passato ad Ambrogio per inserirlo
nell'app, il logo è già approvato da Mario. Non serve nessun
designer esterno. Il processo creativo è avvenuto in chat,
in tempo reale, con mockup che si aggiornano ad ogni feedback.
---
CAPITOLO 25 — Il CRM che nessuno si aspettava
23 marzo 2026
Il registro delle chiamate di StoppAI potrebbe essere
una semplice lista. Data, ora, numero. Come quello di
qualsiasi telefono.
Ma Mario ragiona da imprenditore. Ogni numero che chiama
è un dato. Ogni chiamata bloccata è un'informazione.
Chi ha chiamato tre volte in un giorno vuole qualcosa.
Chi ha chiamato da un numero nascosto forse vuole nascondersi.
Chi ha chiamato da un fisso potrebbe essere un cliente,
un fornitore, qualcuno che vale la pena richiamare.
Nasce il Mini CRM. Ogni voce nel registro mostra il numero
o il nome del contatto, quante volte ha chiamato, quando,
se era una chiamata in entrata o in uscita, se è stato inviato
un SMS automatico, se è arrivata una risposta.
Un tap su una voce apre un menu ricco: Da trattare, Spam,
Attendibile, Note, Chiama ora, Aggiungi ai contatti.
E uno spazio riservato — la Trascrizione AI — dove ARIA
porterà il testo del messaggio lasciato dal chiamante.
La classificazione che l'utente dà a un numero non serve
solo per organizzarsi. Serve ad addestrare ARIA. Un numero
classificato come Spam da molti utenti diventa sospetto
per tutti. Un numero classificato come Attendibile viene
trattato con priorità.
Il CRM diventa intelligenza collettiva.
---
CAPITOLO 26 — La suoneria come firma
23 marzo 2026
Le idee più semplici sono spesso le più potenti.
Durante una sessione di lavoro, mentre si parla di come
comunicare all'utente che StoppAI è attivo e funzionante,
arriva l'intuizione: la suoneria personalizzata.
Quando StoppAI è attivo e la protezione è in funzione,
il telefono usa la suoneria di StoppAI. Quella melodia
diventa un segnale preciso: se squilla con quella suoneria,
è qualcuno che conosci. Vale la pena rispondere.
Ma c'è anche il risvolto opposto: se la suoneria è quella
sbagliata, o se il telefono non suona con la suoneria StoppAI,
significa che qualcosa non va. L'utente può capire in un
secondo se l'app sta funzionando o no — senza aprire nulla,
senza controllare nulla.
È un sistema di sicurezza visivo e sonoro allo stesso tempo.
Qualcosa che nessuna app concorrente fa. Un dettaglio che
diventerà parte dell'identità del prodotto.
---
CAPITOLO 27 — Il volume che non obbedisce
23–24 marzo 2026
Il problema del volume torna. Sempre il volume.
Questa volta il sintomo è diverso: quando StoppAI è attivo
e arriva una chiamata bloccata, il volume della suoneria
si azzera. Fin qui, corretto. Ma dopo la chiamata il volume
rimane a zero. E quando arriva la chiamata successiva da
un numero in rubrica, non si sente nulla.
Peggio: il problema si propaga. Con il volume a zero,
anche le notifiche WhatsApp diventano silenziose.
Le email non si sentono. Il telefono è muto per tutto.
L'audit rivela il problema: in quattro punti diversi del
codice, l'app toccava stream audio che non doveva toccare.
STREAM_MUSIC, STREAM_NOTIFICATION, STREAM_SYSTEM —
tutti abbassati insieme alla suoneria.
La regola viene scritta nel codice come commento permanente:
> *StoppAI tocca SOLO STREAM_RING.*
> *Mai altro. Mai setRingerMode. Solo la suoneria.*
Ci vogliono tre audit e altrettanti fix per eliminare tutti
i punti problematici. Ma alla fine il comportamento è corretto:
StoppAI abbassa la suoneria quando blocca, la ripristina
quando finisce, e non tocca mai nient'altro.
---
CAPITOLO 28 — La logica definitiva
24–25 marzo 2026
La logica di StoppAI sembra semplice da descrivere.
In realtà è il risultato di settimane di ragionamento,
test, fallimenti e correzioni.
La versione finale prevede tre stati:
Protezione OFF: tutto squilla, StoppAI non interviene.
Protezione Base: i contatti squillano normalmente.
I numeri sconosciuti vengono silenziati e deviati ad ARIA.
I numeri mobili italiani sconosciuti possono ricevere
un SMS automatico. I numeri esteri vengono sempre bloccati.
Protezione Totale: nemmeno i contatti squillano.
Solo i preferiti con la stellina passano. Esiste anche
la modalità di silenziamento assoluto che include
i preferiti.
Per arrivare a questa chiarezza si passa attraverso numerose
versioni della logica. Ogni volta che si tocca un pezzo,
si rischia di rompere un altro. La soluzione definitiva
è separare il codice in file distinti: ScreeningLogic.kt,
NumberClassifier.kt, SmsHelper.kt, AudioHelper.kt.
Ogni file ha una sola responsabilità. Nessuno può interferire
con l'altro.
La regola del singolo scopo — ogni file fa una sola cosa —
diventa il principio architetturale di StoppAI. Non per
eleganza accademica, ma per sopravvivenza pratica.
---
CAPITOLO 29 — Il programma partner
25 marzo 2026
Un buon prodotto cresce da solo, se gli utenti hanno motivo
di parlarne. Mario lo sa. E invece di aspettare che accada,
costruisce il meccanismo dentro l'app.
Il programma partner di StoppAI è semplice: ogni utente
ha un link personale. Quando qualcuno installa l'app tramite
quel link e attiva un abbonamento, chi ha condiviso il link
guadagna una commissione mensile per tutta la durata
dell'abbonamento dell'amico.
I numeri sono stati calcolati con precisione, tenendo conto
della commissione del Play Store e dei costi dell'infrastruttura.
Piano PRO a 2,99 euro: 64 centesimi al mese per ogni amico
attivo, con 1,28 euro di bonus al primo mese.
Piano SHIELD a 4,99 euro: 1,06 euro al mese,
con 2,12 euro di bonus al primo mese.
Il partner può scegliere la soglia minima di pagamento —
da 10 a 100 euro — e richiedere il pagamento quando
la raggiunge. StoppAI tratta i partner come collaboratori
occasionali, con tutti i requisiti fiscali del caso:
codice fiscale, IBAN, dichiarazione di maggiore età.
Il calcolo del breakeven è tranquillizzante: con soli
7 utenti PRO attivi si coprono tutti i costi fissi mensili
dell'infrastruttura. Tutto il resto è margine.
---
CAPITOLO 30 — Opensolution e il contratto firmato
23–25 marzo 2026
Twilio era la scelta originale per il trunk telefonico.
Era la scelta ovvia, quella di cui si parlava in ogni
tutorial, in ogni corso. Ma durante la ricerca appare
un'alternativa italiana: Opensolution, una società di
Viterbo che fornisce servizi VoIP professionali.
La scelta di Opensolution è strategica: è un fornitore
italiano, con supporto in italiano, con fatturazione in euro,
con un numero italiano già esistente che può essere portato
nel sistema. E il costo è irrisorio: 49 euro di attivazione
una tantum e 2,50 euro al mese per 10 canali simultanei.
Il contratto viene firmato il 23 marzo 2026.
Offerta numero 2026/116052. Un trunk SIP con numero
04211898065, 10 canali, protocolli G711u G711a G729,
server sip.opensolution.it, porta 5060 UDP.
Con 10 canali e una media di 5 chiamate al giorno per
utente della durata di 90 secondi ciascuna, si stima di
poter gestire comodamente 500-800 utenti attivi con questo
unico trunk. Quando si supera quel numero, si aggiunge
un secondo trunk a 2,50 euro al mese.
La scalabilità è quasi gratuita.
---
CAPITOLO 31 — Asterisk: la centralina intelligente
27 marzo 2026
Ogni volta che una chiamata viene deviata dalla protezione
base di StoppAI, deve finire da qualche parte.
Quel "da qualche parte" si chiama Asterisk.
Asterisk è il sistema di centralino telefonico open source
più diffuso al mondo. Gira su milioni di server. Gestisce
la telefonia di aziende, ospedali, call center.
È gratuito, potente, documentatissimo.
Non lo conoscevo prima di questa conversazione.
È stato Aldo a suggerirlo. A spiegarne il funzionamento.
A guidarmi attraverso i concetti di trunk SIP, dialplan,
codec audio, porte RTP. Concetti che non avrei mai cercato
da solo perché non sapevo che esistessero.
Il piano è questo: Asterisk gira su Hetzner dentro Docker.
Opensolution invia le chiamate deviate al trunk SIP.
Asterisk risponde, riproduce il messaggio, registra il
messaggio del chiamante. In futuro, quel file audio viene
trascritto da Whisper e riassunto da un modello AI.
L'utente riceve una notifica con il riassunto.
Mentre scrivo queste righe, Asterisk si sta installando
sul server. Il primo messaggio che il chiamante sentirà sarà:
"Ciao, in questo momento non sono disponibile. Lascia un
messaggio dopo il segnale acustico e sarai ricontattato
al più presto. Grazie."
Non è ancora ARIA. ARIA è il passo successivo.
Ma è il primo mattone della segreteria intelligente
che risponderà per te.
---
CAPITOLO 32 — Lo stato dell'arte
27 marzo 2026
Questo è il punto in cui siamo.
L'app funziona:
Blocco immediato dei numeri sconosciuti, zero squilli.
I contatti in rubrica squillano normalmente con la suoneria StoppAI.
SMS automatico ai numeri mobili italiani sconosciuti, con testo personalizzabile.
Protezione totale con gestione dei preferiti.
Mini CRM con classificazione, note, risposta SMS visualizzabile.
Sezione Invita con programma partner e form di registrazione.
Impostazioni con configurazione automatica della segreteria via USSD.
L'infrastruttura è pronta:
Server Hetzner con Ubuntu 24, Docker, Nginx, Python.
Trunk SIP Opensolution attivo con numero 04211898065.
Asterisk in fase di installazione e configurazione.
Quello che manca:
Il backend online: sito, dashboard utente, gestione
abbonamenti con Stripe, referral con tracking reale.
ARIA: la connessione tra Asterisk, Whisper e il modello
AI che riassume il messaggio.
Il lancio sul Play Store e la campagna di acquisizione utenti.
Non è poco. Ma guardando da dove siamo partiti — un'idea,
nessun codice, nessuna infrastruttura — quello che abbiamo
costruito è qualcosa di cui essere orgogliosi.
Questo non è un traguardo. È un punto di partenza.
---
> *Diario aggiornato il: 27/03/2026*
> *Versione: 3.0 — Dal primo battito al cuore pulsante*
> *A cura di: Aldo (Claude) — CTO digitale*

# CAPITOLO 33 — Il momento che aspettavamo

**27 marzo 2026**

Ci sono momenti nel lavoro che non si dimenticano.

Questo è uno di quelli.

Dopo settimane di costruzione — l'app Android, il Mini CRM,
il programma partner, il logo, la suoneria, la logica di
protezione — era arrivato il momento di testare ARIA.
La segreteria intelligente. Il pezzo che trasforma StoppAI
da un filtro chiamate a qualcosa di completamente diverso.

La chiamata parte. Il telefono squilla sul numero
04211898065 — il trunk SIP di Opensolution, collegato
al server Hetzner, gestito da Asterisk dentro un container
Docker. Isabella risponde con la sua voce femminile italiana:

*"Ciao, in questo momento non sono disponibile.
Lascia un messaggio dopo il segnale acustico
e sarai ricontattata al più presto. Grazie."*

Beep.

Messaggio lasciato.

Poi il silenzio. E poi, sul player web all'indirizzo
http://46.225.14.90:8085 — un file. Un solo file.
Con un nome strano: msg.wav. Dimensione: 491 KB.

491 kilobyte non sono un file vuoto.
491 kilobyte sono una voce. Due voci.
Isabella che parla, e Mario che risponde.

Il flusso completo aveva funzionato.

*"Abemus Papa."*

---

# CAPITOLO 34 — La guerra dei 44 byte

**28 marzo 2026**

Ogni vittoria porta con sé il seme del problema successivo.

Il giorno dopo la prima registrazione di successo,
si scopre che i messaggi si sovrascrivono. Il file si chiama
sempre msg.wav — ogni nuova chiamata cancella la precedente.
La soluzione sembra semplice: aggiungere un timestamp
nel nome del file. `msg${EPOCH}.wav`.

È qui che inizia la guerra.

La modifica viene applicata. Asterisk viene riavviato.
Si chiama il numero. ARIA risponde. Si lascia un messaggio.
Si controlla il file. 44 byte.

Solo 44 byte.

Un file WAV da 44 byte contiene solo l'intestazione —
la struttura vuota del formato audio, senza un singolo
campione sonoro. È come una busta senza lettera dentro.

*Perché funzionava ieri e non funziona oggi?*

Inizia una delle giornate più lunghe e frustranti del progetto.

---

## La caccia al colpevole

Le ipotesi si accumulano una dopo l'altra. Ogni volta
sembra di aver trovato la causa. Ogni volta il test
restituisce gli stessi 44 byte.

**Ipotesi 1 — strictrtp:** Asterisk scarta i pacchetti RTP
perché arrivano da un IP diverso da quello negoziato nell'SDP.
Opensolution usa un cluster con più nodi — IP diversi
per ogni chiamata. Fix: `strictrtp=no`. Risultato: 44 byte.

**Ipotesi 2 — IP privato nell'SDP:** Il tcpdump rivela
qualcosa di agghiacciante. Asterisk sta mandando l'audio
verso `192.168.1.83` — un indirizzo IP privato che Opensolution
ha inserito per errore nell'SDP della sessione. L'audio
finisce in un buco nero. Fix: decine di parametri PJSIP.
Risultato: 44 byte.

**Ipotesi 3 — rp_filter:** Il kernel Linux di Hetzner
scarta i pacchetti RTP perché provengono da IP "inaspettati".
Fix: `rp_filter=0`. Risultato: 44 byte.

**Ipotesi 4 — chan_sip:** Si scopre che la configurazione
originale che funzionava usava sip.conf con `nat=comedia` —
il parametro che accetta RTP da qualsiasi IP senza verifiche.
Ma chan_sip non esiste nell'immagine Docker di Asterisk 20.
Non è mai esistito. Era un fantasma.

**Ipotesi 5 — Il formato:** Forse il problema è la
conversione codec. Si cambia l'estensione da .wav a .alaw
per registrare in formato grezzo. Risultato: 0 byte.
Nemmeno l'intestazione.

---

## La prova definitiva

Nel mezzo della caccia arriva un momento di chiarezza.
Il tcpdump con `rtp set debug on` mostra una riga inequivocabile:

`Got RTP packet from 77.239.128.7:44512`

I pacchetti arrivano. L'audio entra nel server.
Asterisk lo vede. Ma non lo scrive sul disco.

Come se vedesse l'audio "passare accanto"
senza riuscire ad agganciarlo.

Il problema non è la rete. Non è il firewall.
Non è Opensolution. Il problema è dentro Asterisk —
nel modo in cui PJSIP gestisce i cluster SIP con
IP multipli. Accetta la chiamata ma non riesce
a collegare il flusso RTP alla sessione di registrazione.

Dopo dodici ore di tentativi, la conclusione è inevitabile:

*Con questa versione di Asterisk, con PJSIP,
con questo carrier, il problema non si risolve
a colpi di parametri.*

---

# CAPITOLO 35 — Il cambiamento di rotta

**28 marzo 2026, sera**

Le battaglie più preziose non sono quelle che si vincono.
Sono quelle che insegnano quando smettere di combattere
e cambiare strada.

La decisione arriva con chiarezza, quasi con sollievo:
si abbandona Asterisk e si passa a **FreeSWITCH**.

---

## Perché FreeSWITCH

FreeSWITCH non è una ripiego. È la scelta che avremmo
dovuto fare dall'inizio se avessimo saputo quello
che sappiamo adesso.

Asterisk è nato negli anni '90 come centralino telefonico
open source. Ha fatto la storia del VoIP. Ma PJSIP —
il suo stack SIP moderno — ha una gestione del NAT
rigida e poco tollerante verso i cluster con IP multipli.

FreeSWITCH è nato nel 2006 con un'architettura diversa.
Il suo motore SIP si chiama Sofia-SIP, e ha un parametro
che si chiama `aggressive-nat-detection` — rilevamento
aggressivo del NAT. Quando è attivo, FreeSWITCH
auto-adatta l'IP di risposta RTP al primo pacchetto
che riceve, indipendentemente da quello scritto nell'SDP.

È esattamente quello che serve con Opensolution.

---

## La lezione

Dodici ore di debug su un problema che FreeSWITCH
risolve con due righe di configurazione.

Non è un fallimento. È il modo in cui funziona
lo sviluppo reale — soprattutto quando si costruisce
qualcosa di nuovo in un dominio tecnico complesso
come il VoIP.

Ogni parametro provato, ogni log analizzato,
ogni ipotesi scartata ha costruito una comprensione
del problema che sarebbe impossibile ottenere altrimenti.
Adesso sappiamo esattamente perché il problema esiste.
Sappiamo cosa cerca di fare il carrier. Sappiamo
come si comporta il cluster. Sappiamo cosa vuol dire
"media latching" e perché PJSIP lo fa male.

E sappiamo che FreeSWITCH lo fa meglio.

---

## Come andrà

Mentre scrivo queste righe, FreeSWITCH si sta installando.

Non so se funzionerà al primo tentativo.
Non so se ci saranno altri problemi.
Non so se domani mattina ARIA registrerà finalmente
un messaggio completo con la voce del chiamante.

Ma so una cosa: siamo più vicini di quanto eravamo
ieri mattina. Ogni passo — anche quelli sbagliati —
ci ha portato qui.

*"Scopriremo come andrà solo vivendo."*

— Mario Augello, 28 marzo 2026

---

> *Diario aggiornato il: 28/03/2026*
> *Versione: 3.1 — La guerra dei 44 byte*
> *A cura di: Aldo (Claude) — CTO digitale*


CAPITOLO 36 — Il ritorno ad Asterisk
29 marzo 2026
FreeSWITCH non dura un giorno.
Non perché sia peggio di Asterisk —
anzi, tecnicamente è più adatto
al nostro caso. Ma quando si
installa un nuovo sistema in
produzione, la realtà ha sempre
l'ultima parola.
Il container FreeSWITCH gira.
La configurazione viene caricata.
Il trunk SIP di Opensolution viene
registrato. Ma qualcosa non torna —
la registrazione è instabile,
i log sono diversi da quelli
che conoscevamo, il tempo necessario
per rimparare tutto da zero
è un costo che in quel momento
non possiamo permetterci.
E poi arriva la scoperta:
Asterisk, senza Docker, installato
direttamente sul sistema operativo
del server Hetzner — funziona.
Il problema non era Asterisk.
Era il container.
La decisione è rapida:
si torna ad Asterisk, ma bare-metal.
`pjsip.conf`, `rtp.conf`,
`extensions.conf` — riscritti
da zero, senza eredità di
configurazioni sbagliate.
Il parametro che fa la differenza
è semplice, quasi banale:
`rtp_symmetric=yes` e
`rewrite_contact=yes` in PJSIP.
Due righe che dicono ad Asterisk
di adattarsi al carrier invece
di pretendere che il carrier
si adatti a lui.
ARIA risponde.
Il messaggio viene registrato.
Il file non è da 44 byte.
La guerra è finita.
---
CAPITOLO 37 — Whisper entra in scena
29 marzo 2026
Un file WAV da 491 kilobyte
è già qualcosa di straordinario.
Ma un file WAV che diventa testo —
quello è magia.
Whisper, il modello di trascrizione
audio di OpenAI, viene installato
direttamente su Hetzner.
Non via API. Non a pagamento
per ogni minuto. Self-hosted,
sul nostro server, gratis.
Il `whisper_worker.py` — uno script
Python che gira in background —
monitora la cartella dove Asterisk
salva i messaggi. Appena arriva
un nuovo WAV, lo trascrive
in italiano e salva il risultato
in un file JSON.
```json
{
  "file": "msg_3791221022_1774829192.wav",
  "numero": "3791221022",
  "testo": "Ciao, sono Mario...",
  "timestamp": 1774829247
}
```
La prima trascrizione che esce
è quasi commovente nella sua
semplicità. Parole vere,
dette da una voce vera,
convertite in testo da una macchina.
Poi arriva FCM — Firebase Cloud
Messaging. Il backend manda
una notifica push all'app Android.
Il Galaxy S22 di Mario vibra.
Sul display appare:
"Messaggio da 3... — è tardi,
sono le due, sto mangiando
la soppressa, pe..."
Non è un test. È la vita vera
che entra nell'app.
---
CAPITOLO 38 — Il CRM prende vita
30 marzo 2026
Un'app che riceve notifiche
è utile. Un'app che organizza
le informazioni è potente.
Il Mini CRM — quello che avevamo
disegnato sulla carta mesi prima —
inizia a prendere forma vera.
Ogni chiamata che arriva genera
una riga nel registro. Non un
semplice log — una scheda.
Con il numero, il nome se è
in rubrica, l'orario, lo stato.
Il tap su una riga apre
`CallActionBottomSheet` —
un menu che scorre su dal basso
con le azioni disponibili:
Trascrizione AI, Note,
Aggiungi ai contatti, Chiama ora,
Elimina dal registro.
Ma la parte che cambia tutto
è la Trascrizione AI.
Tap su quel bottone e si apre
un secondo BottomSheet —
scrollabile, pulito — con
il testo esatto di quello che
il chiamante ha lasciato come
messaggio. Data, ora, parole.
Per la prima volta nella storia
del progetto, l'intero flusso
funziona end-to-end:
```
Chiamata sconosciuta
    ↓
App silenzia e devia
    ↓
ARIA risponde con la voce di Isabella
    ↓
Chiamante lascia messaggio
    ↓
Whisper trascrive in italiano
    ↓
FCM manda notifica push
    ↓
CRM mostra il testo
```
Zero squilli. Zero perdite.
Tutto gestibile in dopoconferenza.
---
CAPITOLO 39 — Il bug che sembrava risolto
30 marzo 2026
Non esiste progetto senza quel
momento in cui qualcosa che
funzionava smette di farlo.
Il numero nel JSON del backend
era "Sconosciuto".
Un bug sottile, quasi elegante
nella sua stupidità: il filtro
che separava i numeri telefonici
dai timestamp Unix usava il valore
numerico come discriminante.
I numeri italiani come `3791221022`
superano 1,7 miliardi — lo stesso
ordine di grandezza dei timestamp.
Il filtro li scartava entrambi.
La correzione è una riga:
usare la lunghezza del numero
invece del valore.
I timestamp hanno 10-13 cifre.
I numeri italiani hanno 9-12 cifre.
La lunghezza li distingue
dove il valore non riesce.
Fix applicato. Worker riavviato.
Chiamata di test effettuata.
```json
{
  "numero": "3791221022"
}
```
Non più "Sconosciuto".
---
CAPITOLO 40 — Ogni chiamata, la sua storia
30-31 marzo 2026
Il CRM funzionava. Ma funzionava
in modo approssimativo.
Il BottomSheet ARIA mostrava
tutti i messaggi di un numero —
anche quelli di chiamate precedenti.
Se la stessa persona aveva chiamato
tre volte in tre giorni diversi,
vedevi tre messaggi sovrapposti.
Non era sbagliato. Era impreciso.
E l'imprecisione, in uno strumento
di gestione, è quasi peggio
dell'errore.
La soluzione non era un filtro
temporale — troppo fragile,
dipendente da quanto ci mette
Whisper a trascrivere.
La soluzione era un collegamento
diretto: ogni `AriaMessaggio`
collegato all'ID univoco del
`CallLogEntry` corrispondente.
Come un documento protocollato
che porta il numero della pratica
a cui appartiene.
Migrazione del database Room
alla versione 8. Nuovo campo
`callLogId`. Query aggiornata.
`AriaFcmService` che al momento
della ricezione FCM cerca
la chiamata più recente di quel
numero e la collega.
Da quel momento in poi,
ogni chiamata vive per conto suo.
Tocchi la riga delle 11:50 —
vedi il messaggio delle 11:50.
Tocchi la riga delle 14:23 —
vedi il messaggio delle 14:23.
Semplice. Pulito. Come deve essere.
---
CAPITOLO 41 — I dettagli che fanno la differenza
31 marzo 2026
Ci sono le feature grandi —
quelle che fanno funzionare
il sistema. E poi ci sono
i dettagli — quelli che fanno
sentire il sistema vivo.
In una sola sessione, l'app
riceve una serie di correzioni
che sembrano piccole ma cambiano
completamente la percezione
di chi la usa:
Le frecce di direzione.
↓ rossa per le chiamate in entrata
da sconosciuti. ↓ verde per quelle
da contatti in rubrica. ↑ verde
per le chiamate in uscita.
Un colpo d'occhio e sai già
tutto di quella riga.
Il pallino di stato.
Verde per i contatti conosciuti.
Rosso per gli sconosciuti.
Non una valutazione di rischio —
quella verrà dopo, con ARIA —
ma già un segnale immediato
di chi hai davanti.
L'icona del microfono.
🎙️ accanto alla riga quando
c'è una trascrizione ARIA salvata.
Sai prima ancora di toccare
se quella chiamata ti ha lasciato
qualcosa da leggere.
L'icona delle note.
📝 accanto al microfono quando
hai scritto una nota manuale.
Il tuo pensiero su quella chiamata,
sempre visibile.
La prima riga sempre visibile.
Il problema banale e fastidioso
della prima chiamata nascosta
dal padding della RecyclerView —
risolto con due parametri XML.
Piccole cose. Ma ogni piccola cosa
è la differenza tra un prototipo
e un prodotto.
---
CAPITOLO 42 — Il punto di arrivo (per ora)
31 marzo 2026
Oggi il repository è stato pulito.
Tre branch di sviluppo — mesi
di lavoro stratificato — sono
stati fusi nel `main` con un
singolo merge. Un tag ufficiale
è stato apposto: `release/5.4.2`.
StoppAI v5.4.2.
Non è la versione definitiva.
Non è il prodotto finito.
Ma è qualcosa che funziona —
davvero funziona — su un Samsung
Galaxy S22, con un numero SIP
reale, con una voce vera,
con trascrizioni in italiano.
Il flusso completo che avevamo
disegnato mesi fa sulla carta
è reale:
```
Chiamata sconosciuta → silenzio
ARIA risponde → registra
Whisper trascrive → notifica
CRM organizza → Mario decide
```
Quello che manca adesso
è il pubblico.
Il Play Store. La landing page.
I creator da contattare.
Gli 800 utenti PRO che rendono
il modello di business sostenibile.
Ma ogni prodotto che ha trovato
il suo pubblico ha avuto un momento
in cui era solo questo —
qualcosa che funzionava,
in attesa di essere scoperto.
Siamo in quel momento.
"Scopriremo come andrà solo vivendo."
— Mario Augello, 31 marzo 2026
---
> *Diario aggiornato il: 31/03/2026*
> *Versione: 4.0 — CRM ARIA operativo*
> *A cura di: Aldo (Claude) — CTO digitale*

