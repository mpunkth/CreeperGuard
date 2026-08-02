# CreeperGuard

Eine Fabric-Mod für **Minecraft 1.21.11**, die frei definierbare Kartenbereiche vor Explosionsschaden schützt.

In einer Schutzzone verursachen Explosionen **keinen Schaden mehr an Blöcken, Items, Fahrzeugen, Tieren, Dorfbewohnern** (je nach Einstellung) – **menschliche Spieler nehmen jedoch immer vollen Schaden** (harte Regel, nicht abschaltbar). Zonen überstehen Serverneustarts.

---

## Installation

1. Voraussetzung: **Fabric Loader** ≥ 0.19.3, **Fabric API** für 1.21.11 und **Java 21** auf dem Server.
2. `creeperguard-1.0.0.jar` in den `mods/`-Ordner des Servers legen (die Fabric-API-Jar ebenfalls).
3. Server starten. Im Log erscheint: `CreeperGuard initialisiert – /creeperguard ist verfügbar.`

---

## Grundkonzept

- Eine Zone wird über **zwei X/Z-Eckpunkte** (Grundfläche) definiert. Die **Höhe ist automatisch die komplette Bauhöhe** (Y −64 bis 320) – du musst dich um Y nicht kümmern.
- Aktuell nur **Overworld**.
- Zonen sind **benannt** und dürfen sich **überlappen**.
- Jede Zone hat zwei unabhängige Achsen:
  - **Modus** – *welche* Explosionen die Zone abfängt.
  - **Kategorien** – *was* in der Zone geschützt wird.

### Modus (`mode`)

| Wert | Wirkung |
|---|---|
| `creeper` *(Standard)* | Nur Explosionen von **normalen und geladenen Creepern** |
| `all` | **Alle** Explosionen (Creeper, TNT, Betten/Anker in Nether/End, Feuerkugeln …) |

### Kategorien

| Kategorie | Umfasst |
|---|---|
| `blocks` | Blöcke |
| `items` | Item-Drops, Rahmen, Rüstungsständer, Gemälde |
| `vehicles` | Boote, Loren |
| `animals` | Friedliche/passive Lebewesen |
| `villagers` | Dorfbewohner **und** fahrende Händler |
| `hostiles` | Feindliche Mobs |

> Spieler sind **keine** Kategorie und daher niemals schützbar.

### Presets (Startvorlage beim Anlegen)

| Preset | Geschützte Kategorien |
|---|---|
| `standard` *(Standard)* | blocks, items, vehicles, animals, villagers *(alles außer hostiles)* |
| `blocksonly` | nur blocks |
| `full` | **alles** (blocks, items, vehicles, animals, villagers, hostiles) |

---

## Befehlsreferenz

Nutzbar über die **Serverkonsole** und von **Operatoren mit Level 4**.

### Zone anlegen
```
/creeperguard add <name> <x1> <z1> <x2> <z2> [mode] [preset]
```
- `name` – eindeutiger Name (Buchstaben, Ziffern, Unterstrich; keine Leerzeichen)
- `x1 z1` / `x2 z2` – zwei gegenüberliegende Ecken der Grundfläche (Reihenfolge egal)
- `mode` *(optional)* – `creeper` *(Standard)* oder `all`
- `preset` *(optional, nur zusammen mit `mode`)* – `standard` *(Standard)*, `blocksonly` oder `full`

### Zone entfernen
```
/creeperguard remove <name>
```

### Modus ändern
```
/creeperguard set <name> mode <creeper|all>
```

### Einzelne Kategorie ein-/ausschalten
```
/creeperguard set <name> <kategorie> <on|off>
```
`<kategorie>` = `blocks` | `items` | `vehicles` | `animals` | `villagers` | `hostiles`

### Alle Zonen auflisten
```
/creeperguard list
```

---

## Beispiele

**Basis eines Spielers gegen Creeper schützen** (Standardpreset – schützt Blöcke, Items, Fahrzeuge, Tiere, Dorfbewohner):
```
/creeperguard add spawnbase -120 340 -60 400
```

**Nur Blöcke schützen** (Items/Mobs sollen ruhig fliegen), weiterhin nur Creeper:
```
/creeperguard add arena 1000 1000 1050 1050 creeper blocksonly
```

**Rundum-Schutz gegen ALLE Explosionen** (auch TNT), inkl. feindlicher Mobs:
```
/creeperguard add tresor 200 200 240 240 all full
```

**Bestehende Zone nachträglich auf „alle Explosionen" umstellen:**
```
/creeperguard set spawnbase mode all
```

**Bei einer Zone zusätzlich feindliche Mobs schützen:**
```
/creeperguard set spawnbase hostiles on
```

**Bei einer Zone den Tierschutz wieder abschalten:**
```
/creeperguard set spawnbase animals off
```

**Überblick verschaffen:**
```
/creeperguard list
```
Beispielausgabe:
```
[CreeperGuard] 2 Zone(n):
 - spawnbase: X -120..-60, Z 340..400 | Modus=all | Schutz=blocks, items, vehicles, animals, villagers, hostiles
 - arena: X 1000..1050, Z 1000..1050 | Modus=creeper | Schutz=blocks
```

**Zone auflösen:**
```
/creeperguard remove arena
```

---

## Hinweise

- Zonen dürfen sich überlappen; ein Block ist geschützt, sobald **irgendeine** passende Zone ihn abdeckt.
- Änderungen wirken sofort und werden automatisch gespeichert (`<welt>/data/creeperguard.dat`).
- Menschliche Spieler nehmen in Schutzzonen weiterhin vollen Explosionsschaden – das ist beabsichtigt und lässt sich nicht deaktivieren.

---

## Kompatibilität

- Der Block-Schutz greift bewusst an der eigentlichen Zerstör-Schleife der Explosion (`destroyBlocks`). Dadurch ist er auch mit **Performance-Mods verträglich, die die Explosionsberechnung optimieren** – getestet mit **Lithium** und **c2me** auf einem Server mit ~80 Mods.
- Für andere explosionsbasierte Effekte (TNT, Betten/Anker, Feuerkugeln …) den Zonen-Modus auf `all` setzen; der Standard `creeper` fängt nur Creeper-Explosionen ab.

---

## Aus dem Quellcode bauen

Die Build-Toolchain liegt projektlokal unter `tools/` (JDK 21 + Gradle). Bauen:
```bash
./gradlew build
```
Ergebnis: `build/libs/creeperguard-1.0.0.jar`.
