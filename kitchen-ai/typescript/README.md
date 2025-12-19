# Kitchen-AI (TypeScript)

Node/TypeScript-Variante zur Erstellung eines Wochen-Menüplans aus Rezept-JSON und verfügbarer Zutatenliste.

## Setup & Nutzung
```bash
cd kitchen-ai/typescript
npm install
npm run start -- --servings 2 --days 7 \
  --recipes data/recipes.json \
  --ingredients data/available_ingredients.json
```

## Funktionsumfang
- Skaliert Mengen auf gewünschte Portionen.
- Wählt Rezepte mit ≥80% Abdeckung (Fallback auf beste Treffer).
- Listet fehlende Zutaten und Substitutionsvorschläge auf.
- Baut eine kumulierte Einkaufsliste.

## Anpassungen
- Bearbeiten Sie `data/recipes.json` und `data/available_ingredients.json` für eigene Daten.
- Ergänzen Sie Substitutionen im Objekt `SUBSTITUTIONS` in `src/index.ts`.
- Mit `npm run build` entsteht ein kompiliertes Bundle unter `dist/`.
