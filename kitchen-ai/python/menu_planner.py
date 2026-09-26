import argparse
import json
import math
from pathlib import Path
from typing import Dict, List, Tuple

Ingredient = Dict[str, object]
Recipe = Dict[str, object]

DAY_NAMES = ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"]
SUBSTITUTIONS = {
    "Babyspinat": ["TK-Spinat", "Rucola"],
    "Tahini": ["Erdnussmus", "Cashewmus"],
    "Parmesan": ["Hefeflocken", "Pekorino"],
    "Pinienkerne": ["Sonnenblumenkerne", "Mandeln"],
    "Tofu": ["Tempeh", "Kichererbsen"],
    "Weißfisch": ["Lachs", "Kabeljau"],
    "Basilikum": ["Petersilie", "TK-Kräuter"],
    "Currypaste": ["Currypulver", "Garam Masala"],
}


def load_json(path: Path):
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def scale_quantity(quantity: float, recipe_servings: int, target_servings: int) -> float:
    factor = target_servings / recipe_servings
    return round(quantity * factor, 2)


def compute_coverage(
    recipe: Recipe, available: Dict[str, Dict[str, object]], target_servings: int
) -> Tuple[float, List[Dict[str, object]]]:
    required = [i for i in recipe["ingredients"] if not i.get("optional")]
    missing: List[Dict[str, object]] = []
    covered = 0

    for ingredient in required:
        name = ingredient["name"]
        required_qty = scale_quantity(float(ingredient["quantity"]), recipe["servings"], target_servings)
        available_entry = available.get(name)
        if available_entry and float(available_entry.get("quantity", 0)) >= required_qty:
            covered += 1
        else:
            missing.append(
                {
                    "name": name,
                    "required": required_qty,
                    "unit": ingredient.get("unit", ""),
                    "available": available_entry.get("quantity") if available_entry else 0,
                }
            )
    coverage = covered / len(required) if required else 1.0
    return coverage, missing


def plan_week(recipes: List[Recipe], available: Dict[str, Dict[str, object]], days: int, target_servings: int) -> List[Dict[str, object]]:
    scored = []
    for recipe in recipes:
        coverage, missing = compute_coverage(recipe, available, target_servings)
        scored.append({"recipe": recipe, "coverage": coverage, "missing": missing})

    scored.sort(key=lambda x: (x["coverage"], -len(x["missing"])), reverse=True)

    candidates = [entry for entry in scored if entry["coverage"] >= 0.8]
    if not candidates:
        candidates = scored[:3]

    plan = []
    for day_index in range(days):
        entry = candidates[day_index % len(candidates)]
        plan.append({
            "day": DAY_NAMES[day_index % len(DAY_NAMES)],
            "recipe": entry["recipe"],
            "coverage": round(entry["coverage"], 2),
            "missing": entry["missing"],
        })
    return plan


def build_shopping_list(plan: List[Dict[str, object]]) -> Dict[str, Dict[str, float]]:
    shopping: Dict[str, Dict[str, float]] = {}
    for item in plan:
        for missing in item["missing"]:
            name = missing["name"]
            if name not in shopping:
                shopping[name] = {"quantity": 0, "unit": missing.get("unit", "")}
            shopping[name]["quantity"] += missing.get("required", 0)
    return shopping


def suggest_substitutions(missing: List[Dict[str, object]]) -> List[str]:
    suggestions = []
    for item in missing:
        subs = SUBSTITUTIONS.get(item["name"])
        if subs:
            suggestions.append(f"{item['name']}: ersetze durch {', '.join(subs)}")
    return suggestions


def format_plan(plan: List[Dict[str, object]]):
    lines = []
    for item in plan:
        recipe = item["recipe"]
        lines.append(f"{item['day']}: {recipe['title']} (Abdeckung: {int(item['coverage'] * 100)}%)")
        if item["missing"]:
            lines.append("  Fehlende Zutaten:")
            for missing in item["missing"]:
                lines.append(
                    f"    - {missing['name']}: benötigt {missing['required']} {missing.get('unit', '')}, verfügbar {missing.get('available', 0)}"
                )
            subs = suggest_substitutions(item["missing"])
            if subs:
                lines.append("  Vorschläge für Substitution:")
                for sub in subs:
                    lines.append(f"    - {sub}")
        else:
            lines.append("  Alle Zutaten vorhanden.")
    return "\n".join(lines)


def format_shopping(shopping: Dict[str, Dict[str, float]]):
    if not shopping:
        return "Keine zusätzlichen Einkäufe nötig."
    lines = ["Einkaufsliste (kumuliert):"]
    for name, detail in shopping.items():
        qty = round(detail["quantity"], 2)
        unit = detail.get("unit", "")
        lines.append(f"- {name}: {qty} {unit}")
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Erzeuge einen Wochen-Menüplan aus Rezept-JSON und verfügbaren Zutaten.")
    parser.add_argument("--recipes", default="recipes.json", type=Path, help="Pfad zur Rezept-JSON")
    parser.add_argument("--ingredients", default="available_ingredients.json", type=Path, help="Pfad zur Zutaten-JSON")
    parser.add_argument("--servings", default=2, type=int, help="Zielportionen pro Rezept")
    parser.add_argument("--days", default=7, type=int, help="Anzahl der Tage für den Plan")
    args = parser.parse_args()

    recipes = load_json(args.recipes)
    available = load_json(args.ingredients)

    plan = plan_week(recipes, available, args.days, args.servings)
    shopping_list = build_shopping_list(plan)

    print("Menüplan:\n")
    print(format_plan(plan))
    print("\n" + format_shopping(shopping_list))


if __name__ == "__main__":
    main()
