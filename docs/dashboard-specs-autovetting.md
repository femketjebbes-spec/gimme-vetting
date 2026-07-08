# Gimme AutoVetting Pro — Dashboard Specificaties & Hand-off

Dit document bevat de volledige specificaties en design tokens van het interactieve Gimme AutoVetting Pro dashboard. Hiermee kan de frontend-ontwikkelaar de applicatie exact reproduceren in de gewenste professionele B2B-stijl.

---

## 1. Samenvatting van de Ontwerpsessie

In deze sessie hebben we de initiële mockup getransformeerd van een generieke slide-deck/applicatie naar een high-density, invoice-centric workflow tool speciaal ontworpen voor Gimme Case Analysts.

De belangrijkste functionele beslissingen die we hebben genomen:

- **Volledig Schermvullend Overzicht**: De tabel met openstaande taken bedekt het gehele scherm om direct overzicht te bieden over een database van 1000+ dossiers.
- **Dunne, Compacte Rijen**: De tabel is slank opgezet zodat er maximaal aantal invoices onder elkaar getoond kunnen worden.
- **Slide-over Detailvenster (Drawer)**: In plaats van een vast gesplitst scherm schuift er nu van rechts een paneel over de tabel heen zodra een analyst op een invoice klikt. Dit voorkomt dat de tabel wordt samengedrukt.
- **Klant ≠ Debiteur**: In de tabel is alleen de Klant (de Gimme-cliënt) zichtbaar. De Debiteur (altijd een echte menselijke naam) staat exclusief in de detail-drawer, samen met zijn/haar adres en IBAN.
- **Opgeschoond Bestandenoverzicht**: Gekoppelde documenten worden nu clean gepresenteerd zonder statusvinkjes of incompleet-badges. Er is een subtiele "Bekijken" interactie aanwezig om het brondocument of de correspondentie in te zien.
- **Gereduceerde Handmatige Vetting**: De vetting is teruggebracht tot exact 2 checkboxes:
  1. "Is de debtor informatie correct"
  2. "Is het POC (Proof of Correspondence) correct"
- **Dynamische Validatie-vrijgave**: De knop "Validatie afronden" wordt pas actief en klikbaar zodra beide handmatige checkpoints zijn aangevinkt. Een statusindicator (drawer-actions-needed) toont live de voortgang ("2 stappen resterend" of "Klaar voor validatie!").
- **Terugsturen & Afwijzen met Toelichting**: Het terugsturen of afwijzen van een invoice triggert een apart pop-up venster (action-reason-modal) waarin de analist verplicht een reden of toelichting moet invoeren. Dit wordt direct gesimuleerd als notificatie naar de cliënt.
- **Functiescheiding & Archivering**: Zodra een analist de vetting afrondt, krijgt de invoice de status "Gevalideerd" en verdwijnt deze direct uit de actieve werklijst naar de "Dossiers" tab in de linkersidebar.
- **Opnieuw Ingediend Icoontje**: Invoices die voor de tweede keer worden ingestuurd, tonen een donkerblauw her-indieningsicoon direct achter de status.

---

## 2. Design Tokens & Kleurenpalet

Het kleurenpalet is zorgvuldig samengesteld op basis van de warme okergele Gimme-branding, maar vertaald naar een strakke, minimalistische en zakelijke B2B SaaS-omgeving met koele Slate-grijstinten voor optimaal contrast en rust.

### Primaire Merkkleuren

| Element | CSS / Tailwind Class | Hex-code | Toepassing |
|---------|---------------------|----------|------------|
| Gimme Okergeel | `bg-brand-ochre` / `text-brand-ochre` | `#C59B27` | Logo, actieve tabs, primaire buttons, succes-indicatoren |
| Okergeel Hover | `hover:bg-brand-ochreHover` | `#A8801D` | Hover-toestand voor buttons |
| Subtiel Okergeel | `bg-brand-ochreLight` | `#FDF9ED` | Achtergrond voor actieve geselecteerde tabelrijen, badges |
| Gedempt Okergeel | `border-brand-ochreMuted` | `#EEDCA8` | Subtiele randen rondom okergele badges |

### Neutrale & Contrasterende Kleuren (Slate)

| Element | CSS / Tailwind Class | Hex-code | Toepassing |
|---------|---------------------|----------|------------|
| Premium Dark | `text-brand-dark` / `bg-brand-sidebar` | `#0F172A` | Hoofdtekstkleur, titels, donkere sidebar, her-indieningsicoon (Slate 900) |
| Slate Gray | `text-brand-darkMuted` | `#64748B` | Subtiele labels, headers van tabellen, helpteksten (Slate 500) |
| Schoon Wit | `bg-white` | `#FFFFFF` | Kaarten, tabelachtergrond, headerbalk |
| Lichte Achtergrond | `bg-brand-lightBg` | `#F8FAFC` | Tabel-headers, knoppenbalken, subtiele achtergronden (Slate 50) |
| Randen | `border-brand-border` | `#E2E8F0` | Dunne, strakke scheidingslijnen (Slate 200) |

---

## 3. Typografie

| Element | Font | Beschrijving |
|---------|------|-------------|
| Koppen & Headers | **Poppins** (sans-serif) | Geeft een modern, premium en licht speels karakter aan de titels |
| Body & Data | **Arimo** (sans-serif) | Een zeer helder en neutraal lettertype dat perfect leesbaar blijft op kleine schaal en hoge dichtheid |

---

## 4. Iconen & Elementen

### FontAwesome 6 Iconen

De interface maakt gebruik van de FontAwesome 6 bibliotheek voor alle vector-iconen:

| Element | FontAwesome Icon |
|---------|-----------------|
| Invoices tab in sidebar | `<i class="fa-solid fa-file-invoice"></i>` |
| Dossiers tab in sidebar | `<i class="fa-solid fa-briefcase"></i>` |
| Statistieken | `<i class="fa-solid fa-chart-line"></i>` |
| Instellingen | `<i class="fa-solid fa-gear"></i>` |
| Te Controleren Badge | `<i class="fa-solid fa-circle-exclamation"></i>` |
| Gevalideerd Badge | `<i class="fa-solid fa-circle-check"></i>` |
| Terugsturen actie | `<i class="fa-solid fa-reply"></i>` |
| Invoice afwijzen | `<i class="fa-solid fa-circle-xmark"></i>` |

### Custom Opnieuw Ingediend Icoon (Curved Arrow SVG)

Gebaseerd op het aangeleverde ontwerp, is dit specifieke icoon direct als inline SVG verwerkt, ingekleurd in het donkerblauw (`#0F172A`) van de sidebar:

```svg
<svg class="w-4 h-4 text-brand-sidebar inline-block ml-2 shrink-0 align-middle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" title="Opnieuw ingediend">
    <path d="M9 14L4 9l5-5"/>
    <path d="M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5v0a5.5 5.5 0 0 1-5.5 5.5H11"/>
</svg>
```

**Beveelde locatie voor het SVG bestand:** `src/frontend/assets/icons/resubmitted.svg`

---

## 5. Overzicht van de Workflow-Knoppen & Modals

Onderdeel van de vetting beslissing zijn drie functionele knoppen en een interactieve toelichtingsmodal:

### Validatie afronden
- **Toestand**: `disabled` (inactief grijs) bij het openen. Wordt pas geactiveerd (`enabled` okergeel) zodra beide checklists in stap 2 zijn afgevinkt.
- **Kleur**: `#C59B27` (Okergeel)
- **Actie**: Zet de status op "Gevalideerd", opent de succes-modal en verplaatst de invoice naar het archief (Dossiers).

### Invoice terugsturen
- **Toestand**: Altijd actief
- **Actie**: Opent de toelichtingsmodal (`action-reason-modal`) met een okergele verzendknop. Na invoer van de reden wordt de invoice gemarkeerd als "Returned" en uit de actieve wachtrij verwijderd.

### Invoice afwijzen
- **Toestand**: Altijd actief
- **Kleur**: Rode rand (`border-red-200`) met rode tekst (`text-red-600`) voor een waarschuwend maar zakelijk karakter
- **Actie**: Opent de toelichtingsmodal met een rode verzendknop. Na invoer van de reden wordt de invoice gemarkeerd als "Rejected" en uit de actieve wachtrij verwijderd.

### Toelichtingsmodal (action-reason-modal)
- **Interactie**: Vereist een minimale tekstinvoer in de textarea (`action-reason-text`). Indien leeg gelaten bij het verzenden, krijgt het invoerveld een rode foutrand (`border-red-500`) en wordt er een waarschuwingstoast getoond.
