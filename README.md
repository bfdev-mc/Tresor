# Tresor

Paper-Plugin (Minecraft 26.2) für code-gesicherte Tresore.

- **Kleiner Tresor** – 27 Slots (wie eine Kiste). Rezept: 1 Kiste + 1 Eisenblock (formlos).
- **Großer Tresor** – 54 Slots (wie eine Doppelkiste), belegt zwei Blöcke nebeneinander. Rezept: wie eine Kiste, aber mit 8 Eisenblöcken statt Holz.

Die Tresore nutzen den Vault-Block aus den Trial Chambers (der große ist die dunkle "ominous"-Variante), brauchen also kein Resource Pack.

## Benutzung

1. Tresor platzieren – ein Zahlenfeld öffnet sich, dort einen Code (4–8 Ziffern) festlegen. Ohne Code wird das Platzieren abgebrochen und du bekommst den Tresor zurück.
2. Rechtsklick auf den Tresor → Code eingeben → der Tresor öffnet sich. Danach bleibt er 2 Minuten für dich entsperrt.
3. Abbauen geht nur, wenn der Tresor für dich entsperrt ist (vorher Rechtsklick + Code). Inhalt und Tresor-Item droppen.
4. Nach 5 Fehlversuchen: 30 Sekunden Sperre.

Tresore sind gegen Explosionen und Kolben geschützt. Der Code wird nur als Hash gespeichert (`plugins/Tresor/tresore.yml`).

Permission `tresor.admin` (Standard: OP): Tresore ohne Code öffnen/abbauen.

## Bauen

Java 25 und Gradle:

```
gradle build
```

Die JAR liegt danach in `build/libs/`. Sie gehört in den `plugins`-Ordner eines Paper-26.2-Servers.
