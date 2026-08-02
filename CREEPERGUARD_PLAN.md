# CreeperGuard — Design- & Umsetzungsplan

Fabric-Mod für **Minecraft 1.21.11**: schützt per Konsolenbefehl definierte Kartenbereiche vor
Explosions-Sachschaden, während Spieler weiterhin Schaden nehmen. Zonen überdauern Neustarts.

---

## 1. Ziel & Umfang

- **Kern:** Benannte, koordinatenbasierte Schutzzonen anlegen/verwalten, in denen Explosionen
  keinen **Sachschaden** (Blöcke, Items, Deko, Fahrzeuge, Tiere, Villager) anrichten.
- **Spieler nehmen IMMER Schaden** — in jeder Zone, bei jedem Preset, nicht abschaltbar.
- **MVP nur Overworld** (Datenmodell trägt die Dimension aber schon mit).
- Zonen sind **persistent** über Serverneustarts.

---

## 2. Plattform & Build

| Punkt | Festlegung |
|---|---|
| Loader | **Fabric** (Server läuft bereits auf Fabric) |
| Ziel-Version | **Minecraft 1.21.11** |
| Mappings | **Yarn** |
| Java | **21** |
| Abhängigkeit | **Fabric API** (u. a. `CommandRegistrationCallback`) |
| Mod-ID | `creeperguard` |
| Package | `com.mpunkth.creeperguard` |
| Environment | `*` — läuft auf Dedicated Server **und** Singleplayer; Vanilla-Clients brauchen nichts |
| Versionspins | Fabric Loader / Fabric API / Yarn-Build für 1.21.11 werden beim Scaffolding aus aktuellen Metadaten geholt (nicht aus dem Gedächtnis) |

---

## 3. Befehle

Wurzelkommando `/creeperguard`, nutzbar aus **Konsole** und durch **Op-Spieler (Level 4)**.

```
/creeperguard add <name> <x1> <z1> <x2> <z2> [modus=creeper] [preset=standard]
/creeperguard remove <name>
/creeperguard set <name> mode <creeper|all>
/creeperguard set <name> <blocks|items|vehicles|animals|villagers|hostiles> <on|off>
/creeperguard list
```

**Regeln / UX:**
- **Nur X/Z-Grundfläche** angeben → Höhe (Y) automatisch komplett (−64…320, Overworld).
- Ecken werden **automatisch normalisiert** (min/max), Reihenfolge egal.
- Zonennamen: ein Wort (Buchstaben/Ziffern/`_`), **case-insensitiv eindeutig**.
- `add` mit existierendem Namen → **Fehler** (kein stilles Überschreiben).
- `remove`/`set` bieten existierende Zonennamen per **Tab-Vervollständigung**.
- `modus`/`preset` per Tab als feste Auswahlwerte.
- `list` zeigt pro Zone: **Name, beide Ecken (X/Z), Modus, aktive Schutzkategorien** + Gesamtzahl.

---

## 4. Datenmodell — `Zone`

| Feld | Typ | Bemerkung |
|---|---|---|
| `name` | String | eindeutig (case-insensitiv) |
| `dimension` | Identifier | MVP fest `minecraft:overworld`; Feld existiert für spätere Erweiterung |
| `minX, maxX, minZ, maxZ` | int | normalisierte Grundfläche |
| `minY, maxY` | int | fest −64…320 (Overworld-Bauhöhe) |
| `mode` | Enum `CREEPER \| ALL` | welche Explosionsquelle |
| `protection` | Set von Kategorien | welche Ziele geschützt sind |

Alle Zonen liegen zentral in **einem** Speicher, der am Overworld-Level hängt.

---

## 5. Achsen: Quelle & Ziel

**Achse 1 — Quelle (`mode`):**
- `creeper` *(Default)* — nur Creeper (normal **und** geladen). Erkennung über
  `explosion.getCausingEntity() instanceof CreeperEntity`.
- `all` — alle Explosionen (TNT, Ghast, End-Kristall, Bett/Wither, …).

**Achse 2 — Ziel (Schutzkategorien):**

| Kategorie | Umfasst |
|---|---|
| `blocks` | alle Blöcke |
| `items` | gedroppte Items, Item-Frames (auch Glow), Rüstungsständer, Gemälde |
| `vehicles` | Boote (auch Truhenboote), Loren (alle, inkl. Truhen-/Hopper-Loren) |
| `animals` | passive/gezähmte Tiere, Haustiere, Golems |
| `villagers` | Dorfbewohner **+ fahrender Händler** (Zombie-Villager zählt zu `hostiles`) |
| `hostiles` | feindliche Mobs (Zombies, Skelette, andere Creeper …) |

**Spieler** sind bewusst **keine** Kategorie — nie schützbar.

**Presets (beim `add`):**

| Preset | Geschützt |
|---|---|
| `standard` *(Default)* | `blocks`, `items`, `vehicles`, `animals`, `villagers` — **nicht** `hostiles` |
| `blocksonly` | nur `blocks` |
| `full` | alles außer Spieler: `blocks`, `items`, `vehicles`, `animals`, `villagers`, `hostiles` |

---

## 6. Persistenz

- **`PersistentState`** (1.21.x-API: über `PersistentStateType` = id + Konstruktor + **Codec** + DataFixTypes),
  geladen/erzeugt via `PersistentStateManager` des **Overworld**-Levels.
- Speicherort: **Welt-Ordner** (`world/data/creeperguard.dat`, NBT) → reist mit dem Welt-Save mit,
  korrekt an *diese* Welt gebunden (nicht global über alle Welten).
- Speichern automatisch über Minecrafts Welt-Autosave + beim Stoppen; bei jeder Änderung `markDirty()`.

---

## 7. Explosions-Interception (Mixin)

**Ziel-Klasse:** `ExplosionImpl` (Yarn; Mojmap `ServerExplosion`), Implementierung des `Explosion`-Interface.

**Zwei kleine, rein *entfernende* Injections — kein Neuschreiben von Logik:**

1. **Blöcke:** aus der von Vanilla berechneten Liste der zu zerstörenden `BlockPos` alle Positionen
   **entfernen**, die in einer Zone liegen, deren `blocks`-Schutz aktiv ist und deren `mode` zur Quelle passt.
2. **Entities:** in der Entity-Wirkungsschleife jede Entity **überspringen**, die in einer passenden Zone
   liegt und deren Kategorie geschützt ist. (Schaden **und** Rückstoß entfallen dann gemeinsam.)

**Prüf-Logik pro Block/Entity (Modell A — positionsbasiert):**
- Quelle passt? (`mode == ALL` **oder** auslösende Entity ist Creeper)
- Position liegt in einer Zone der passenden Dimension?
- Kategorie dieses Ziels in der Zone geschützt?
→ nur dann aus der Liste streichen.

Creeper-Erkennung, Position, Welt kommen fertig aus dem `Explosion`-Interface
(`getCausingEntity()`, `getEntity()`, `getPosition()`, `getWorld()`).

---

## 8. Verhalten & Nebenwirkungen (bewusst so)

- Explosion bleibt **sicht- und hörbar**; **Spieler-Schaden + Rückstoß** voll Vanilla → Creeper bleibt gefährlich.
- Geschützte Entities: **weder Schaden noch Rückstoß** (bleiben unbewegt).
- Nicht zerstörte Blöcke → **keine Drops**, bleiben stehen.
- Zonen dürfen sich **überlappen** (doppelter Schutz ist harmlos, keine Extra-Prüfung).
- Positionsbasiert: Creeper *in* der Zone kann Blöcke *außerhalb* weiter zerstören und umgekehrt.

---

## 9. Geplante Projektstruktur

```
build.gradle / settings.gradle / gradle.properties      # Loom, Java 21, Pins für 1.21.11
src/main/resources/fabric.mod.json                      # Mod-Metadaten, entrypoints, mixins-Verweis
src/main/resources/creeperguard.mixins.json             # Mixin-Config
src/main/java/com/mpunkth/creeperguard/
    CreeperGuard.java                                    # ModInitializer: Command-Registrierung
    zone/Zone.java                                       # Datenmodell (+ Codec)
    zone/ProtectionCategory.java                         # Enum der 6 Kategorien
    zone/ExplosionMode.java                              # Enum CREEPER | ALL
    zone/ZoneStore.java                                  # PersistentState (Codec), Lookup-Helfer
    command/CreeperGuardCommand.java                     # add/remove/set/list + Tab-Completion
    mixin/ExplosionImplMixin.java                        # Block- & Entity-Filter
    util/EntityClassifier.java                           # Entity → Kategorie
```

---

## 10. Spätere Erweiterungen (Backlog, nicht im MVP)

- Optionaler **Dimensionsparameter** im Befehl (Datenmodell trägt Dimension bereits).
- Optionaler **Y-Bereich** im Befehl (statt fester ganzer Säule).
- Ausweitung der Quellen (z. B. eigene `mode`-Werte pro Explosionstyp).
- `info <name>` für Einzelzonen-Details.

---

## 11. Beim Scaffolding zu erledigen

- Exakte Build-Nummern (Fabric Loader / Fabric API / Yarn) für **1.21.11** aus aktuellen Metadaten ziehen.
- Yarn-interne Methodennamen der Block-/Entity-Listen in `ExplosionImpl` gegen die 1.21.11-Mappings
  final abgleichen (Interface-Methoden sind bereits bestätigt).
