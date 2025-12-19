# Kitchen-AI – Menüplanung mit ChatGPT-4o-konformen Prompts

Dieser Ordner bündelt drei Beispielprojekte (Python, Java, TypeScript), die zeigen, wie sich ein Menüplan aus eigenen Rezept-JSON-Dateien und einer Zutateninventur automatisiert erstellen lässt. Jede Variante:
- liest `recipes.json` und `available_ingredients.json`,
- skaliert Zutaten auf gewünschte Portionenzahl,
- prüft eine Mindestabdeckung von 80% der benötigten Zutaten (Fallback auf beste Treffer),
- listet fehlende Zutaten samt einfachen Substitutionen und erzeugt eine Einkaufsliste.

## Struktur
- `python/` – leichtgewichtige CLI ohne externe Dependencies.
- `java/` – Maven-Projekt mit Jackson für JSON-Verarbeitung.
- `typescript/` – Node/TypeScript-Projekt mit ts-node.

Jedes Unterverzeichnis enthält eine eigene README mit Build-/Run-Hinweisen und denselben Beispieldaten.
