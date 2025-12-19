# Kitchen-AI (Python)

Dieses kleine CLI erzeugt aus einer Rezept-JSON und den verfügbaren Zutaten einen Wochen-Menüplan und leitet eine Einkaufsliste ab.

## Features
- Skaliert Mengen auf gewünschte Portionenzahl.
- Wählt Rezepte mit mindestens 80% Zutatenabdeckung (sonst Top-Treffer als Fallback).
- Markiert fehlende Zutaten und schlägt einfache Substitutionen vor.
- Aggregiert fehlende Zutaten zu einer Einkaufsliste.

## Schnellstart
```bash
cd kitchen-ai/python
python menu_planner.py --recipes recipes.json --ingredients available_ingredients.json --servings 2 --days 7
```

Output enthält den Menüplan je Wochentag sowie eine konsolidierte Einkaufsliste.

## Eigene Daten verwenden
- Passen Sie `recipes.json` an (Schema: `id`, `title`, `servings`, `ingredients`, `tags`, `steps`, `time`, `appliances`, `allergens`).
- Tragen Sie Ihren Vorrat in `available_ingredients.json` ein (`{ "Zutat": {"quantity": Zahl, "unit": "g/ml/stk"} }`).
- Nutzen Sie zusätzliche Substitutionen in `SUBSTITUTIONS` innerhalb von `menu_planner.py`.
