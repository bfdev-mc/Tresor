# Tresor

Paper-Plugin (Minecraft 26.2) für code-gesicherte Tresore.

- **Kleiner Tresor** – wie eine Kiste. Rezept: 1 Kiste + 1 Eisenblock (formlos).
- **Großer Tresor** – wie eine Doppelkiste, belegt aber nur einen Block. Rezept: wie eine Kiste, aber mit 8 Eisenblöcken statt Holz.

Das Design kommt aus einem Resource Pack (`TresorPack.zip`, wird beim Beitritt automatisch an Spieler geschickt, URL und SHA1 stehen in `config.yml`). Der Tresor ist ein ItemDisplay mit eigenem 3D-Modell über einem Eisenblock, das Zahlenfeld hat einen eigenen Hintergrund und Tasten. Ohne Pack sieht man nur einen Eisenblock und ein schlichtes Inventar.

Das Pack wird per `java tools/GeneratePack.java` komplett aus Code erzeugt (Texturen, Modelle, GUI).

## Benutzung

1. Tresor platzieren – ein Zahlenfeld öffnet sich, dort einen Code (4–9 Ziffern) festlegen. Ohne Code wird das Platzieren abgebrochen und du bekommst den Tresor zurück.
2. Rechtsklick auf den Tresor → Code eingeben → der Tresor öffnet sich. Danach bleibt er 2 Minuten für dich entsperrt.
3. Im geöffneten Tresor ist der letzte Slot (unten rechts) der **Abschließen-Button**: sperrt den Tresor für alle wieder zu. Dieser Slot ist deshalb kein Lagerplatz (26 bzw. 53 nutzbare Slots).
4. Abbauen geht nur, wenn der Tresor für dich entsperrt ist (vorher Rechtsklick + Code). Inhalt und Tresor-Item droppen.
5. Nach 5 Fehlversuchen: 30 Sekunden Sperre.

Tresore sind gegen Explosionen und Kolben geschützt. Der Code wird nur als Hash gespeichert (`plugins/Tresor/tresore.yml`).

Permission `tresor.admin` (Standard: OP): Tresore ohne Code öffnen/abbauen.

## Bauen

Java 25 und Gradle:

```
gradle build
```

Die JAR liegt danach in `build/libs/`. Sie gehört in den `plugins`-Ordner eines Paper-26.2-Servers.
