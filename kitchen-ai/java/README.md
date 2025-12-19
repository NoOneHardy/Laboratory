# Kitchen-AI (Java)

CLI-Anwendung, die aus Rezept-JSON und verfügbaren Zutaten einen Wochenplan erzeugt und fehlende Zutaten inkl. Substitutionen ausweist.

## Bauen und ausführen
```bash
cd kitchen-ai/java
mvn package
java -jar target/kitchen-ai-1.0-SNAPSHOT.jar --servings 3 --days 7 \
  --recipes src/main/resources/recipes.json \
  --ingredients src/main/resources/available_ingredients.json
```

## Funktionsweise
- Skaliert Mengen auf die gewünschte Portionenzahl.
- Wählt Rezepte mit ≥80% Zutatenabdeckung (sonst die besten Treffer).
- Zeigt fehlende Zutaten und Substitutionsvorschläge an.
- Aggregiert eine Einkaufsliste über alle geplanten Tage.

## Dateien anpassen
- `src/main/resources/recipes.json`: Rezeptstammdaten (siehe Python-Beispiel).
- `src/main/resources/available_ingredients.json`: Vorratsliste.
- Zusätzliche Substitutionen in `MenuPlanner.SUBSTITUTIONS` ergänzen.
