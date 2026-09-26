import { readFileSync } from "fs";
import { join } from "path";

type Ingredient = {
  name: string;
  quantity: number;
  unit?: string;
  optional?: boolean;
};

type Recipe = {
  id: string;
  title: string;
  servings: number;
  ingredients: Ingredient[];
  tags?: string[];
  steps?: string[];
  time?: Record<string, unknown>;
  appliances?: string[];
  allergens?: string[];
};

type InventoryItem = {
  quantity: number;
  unit?: string;
};

type MissingItem = {
  name: string;
  required: number;
  unit?: string;
  available: number;
};

type PlanItem = {
  day: string;
  recipe: Recipe;
  coverage: number;
  missing: MissingItem[];
};

const DAY_NAMES = [
  "Montag",
  "Dienstag",
  "Mittwoch",
  "Donnerstag",
  "Freitag",
  "Samstag",
  "Sonntag",
];

const SUBSTITUTIONS: Record<string, string[]> = {
  Babyspinat: ["TK-Spinat", "Rucola"],
  Tahini: ["Erdnussmus", "Cashewmus"],
  Parmesan: ["Hefeflocken", "Pekorino"],
  Pinienkerne: ["Sonnenblumenkerne", "Mandeln"],
  Tofu: ["Tempeh", "Kichererbsen"],
  Weißfisch: ["Lachs", "Kabeljau"],
  Basilikum: ["Petersilie", "TK-Kräuter"],
  Currypaste: ["Currypulver", "Garam Masala"],
};

function readJson<T>(path: string): T {
  const content = readFileSync(path, "utf-8");
  return JSON.parse(content) as T;
}

function scaleQuantity(quantity: number, recipeServings: number, targetServings: number) {
  const factor = targetServings / recipeServings;
  return Math.round(quantity * factor * 100) / 100;
}

function computeCoverage(
  recipe: Recipe,
  available: Record<string, InventoryItem>,
  targetServings: number
): { coverage: number; missing: MissingItem[] } {
  const required = recipe.ingredients.filter((i) => !i.optional);
  const missing: MissingItem[] = [];
  let covered = 0;

  for (const ingredient of required) {
    const requiredQty = scaleQuantity(ingredient.quantity, recipe.servings, targetServings);
    const entry = available[ingredient.name];
    if (entry && entry.quantity >= requiredQty) {
      covered += 1;
    } else {
      missing.push({
        name: ingredient.name,
        required: requiredQty,
        unit: ingredient.unit,
        available: entry?.quantity ?? 0,
      });
    }
  }

  const coverage = required.length === 0 ? 1 : covered / required.length;
  return { coverage, missing };
}

function planWeek(
  recipes: Recipe[],
  available: Record<string, InventoryItem>,
  days: number,
  targetServings: number
): PlanItem[] {
  const scored = recipes.map((recipe) => {
    const { coverage, missing } = computeCoverage(recipe, available, targetServings);
    return { recipe, coverage, missing };
  });

  scored.sort((a, b) => {
    if (a.coverage === b.coverage) {
      return a.missing.length - b.missing.length;
    }
    return b.coverage - a.coverage;
  });

  let candidates = scored.filter((s) => s.coverage >= 0.8);
  if (candidates.length === 0) {
    candidates = scored.slice(0, 3);
  }

  const plan: PlanItem[] = [];
  for (let i = 0; i < days; i++) {
    const entry = candidates[i % candidates.length];
    plan.push({
      day: DAY_NAMES[i % DAY_NAMES.length],
      recipe: entry.recipe,
      coverage: Math.round(entry.coverage * 100) / 100,
      missing: entry.missing,
    });
  }
  return plan;
}

function buildShoppingList(plan: PlanItem[]) {
  const shopping: Record<string, { quantity: number; unit?: string }> = {};
  for (const item of plan) {
    for (const missing of item.missing) {
      if (!shopping[missing.name]) {
        shopping[missing.name] = { quantity: 0, unit: missing.unit };
      }
      shopping[missing.name].quantity =
        Math.round((shopping[missing.name].quantity + missing.required) * 100) / 100;
    }
  }
  return shopping;
}

function suggestSubstitutions(missing: MissingItem[]) {
  const suggestions: string[] = [];
  for (const item of missing) {
    const subs = SUBSTITUTIONS[item.name];
    if (subs?.length) {
      suggestions.push(`${item.name}: ersetze durch ${subs.join(", ")}`);
    }
  }
  return suggestions;
}

function formatPlan(plan: PlanItem[]) {
  const lines: string[] = [];
  for (const item of plan) {
    lines.push(`${item.day}: ${item.recipe.title} (Abdeckung: ${Math.round(item.coverage * 100)}%)`);
    if (item.missing.length === 0) {
      lines.push("  Alle Zutaten vorhanden.");
    } else {
      lines.push("  Fehlende Zutaten:");
      for (const missing of item.missing) {
        lines.push(
          `    - ${missing.name}: benötigt ${missing.required} ${missing.unit ?? ""}, verfügbar ${missing.available}`
        );
      }
      const subs = suggestSubstitutions(item.missing);
      if (subs.length) {
        lines.push("  Vorschläge für Substitution:");
        subs.forEach((s) => lines.push(`    - ${s}`));
      }
    }
  }
  return lines.join("\n");
}

function formatShopping(shopping: Record<string, { quantity: number; unit?: string }>) {
  const entries = Object.entries(shopping);
  if (!entries.length) {
    return "Keine zusätzlichen Einkäufe nötig.";
  }
  const lines = ["Einkaufsliste (kumuliert):"];
  for (const [name, item] of entries) {
    lines.push(`- ${name}: ${item.quantity} ${item.unit ?? ""}`);
  }
  return lines.join("\n");
}

function parseArgs() {
  const args = process.argv.slice(2);
  let recipes = join("data", "recipes.json");
  let ingredients = join("data", "available_ingredients.json");
  let servings = 2;
  let days = 7;

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case "--recipes":
        recipes = args[++i];
        break;
      case "--ingredients":
        ingredients = args[++i];
        break;
      case "--servings":
        servings = Number(args[++i]);
        break;
      case "--days":
        days = Number(args[++i]);
        break;
      default:
        break;
    }
  }

  return { recipes, ingredients, servings, days };
}

function main() {
  const { recipes, ingredients, servings, days } = parseArgs();
  const recipeData = readJson<Recipe[]>(recipes);
  const available = readJson<Record<string, InventoryItem>>(ingredients);

  const plan = planWeek(recipeData, available, days, servings);
  const shopping = buildShoppingList(plan);

  console.log("Menüplan\n");
  console.log(formatPlan(plan));
  console.log("\n" + formatShopping(shopping));
}

main();
