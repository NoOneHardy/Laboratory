package ai.kitchen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MenuPlanner {
    private static final List<String> DAY_NAMES = List.of(
            "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
    );

    private static final Map<String, List<String>> SUBSTITUTIONS = Map.of(
            "Babyspinat", List.of("TK-Spinat", "Rucola"),
            "Tahini", List.of("Erdnussmus", "Cashewmus"),
            "Parmesan", List.of("Hefeflocken", "Pekorino"),
            "Pinienkerne", List.of("Sonnenblumenkerne", "Mandeln"),
            "Tofu", List.of("Tempeh", "Kichererbsen"),
            "Weißfisch", List.of("Lachs", "Kabeljau"),
            "Basilikum", List.of("Petersilie", "TK-Kräuter"),
            "Currypaste", List.of("Currypulver", "Garam Masala")
    );

    private record CoverageResult(double coverage, List<MissingItem> missing) {}

    public static void main(String[] args) throws IOException {
        Path recipesPath = Paths.get("src/main/resources/recipes.json");
        Path ingredientsPath = Paths.get("src/main/resources/available_ingredients.json");
        int servings = 2;
        int days = 7;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--recipes" -> recipesPath = Paths.get(args[++i]);
                case "--ingredients" -> ingredientsPath = Paths.get(args[++i]);
                case "--servings" -> servings = Integer.parseInt(args[++i]);
                case "--days" -> days = Integer.parseInt(args[++i]);
                default -> {
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Recipe> recipes = loadRecipes(mapper, recipesPath);
        Map<String, InventoryItem> available = loadInventory(mapper, ingredientsPath);

        List<PlanItem> plan = planWeek(recipes, available, days, servings);
        Map<String, ShoppingEntry> shopping = buildShoppingList(plan);

        System.out.println("Menüplan\n");
        System.out.println(formatPlan(plan));
        System.out.println();
        System.out.println(formatShopping(shopping));
    }

    private static List<Recipe> loadRecipes(ObjectMapper mapper, Path path) throws IOException {
        return mapper.readValue(path.toFile(), new TypeReference<>() {});
    }

    private static Map<String, InventoryItem> loadInventory(ObjectMapper mapper, Path path) throws IOException {
        return mapper.readValue(path.toFile(), new TypeReference<>() {});
    }

    private static CoverageResult computeCoverage(Recipe recipe, Map<String, InventoryItem> available, int targetServings) {
        List<Ingredient> required = recipe.getIngredients().stream()
                .filter(i -> !i.isOptional())
                .toList();

        List<MissingItem> missing = new ArrayList<>();
        int covered = 0;

        for (Ingredient ingredient : required) {
            double requiredQty = scaleQuantity(ingredient.getQuantity(), recipe.getServings(), targetServings);
            InventoryItem entry = available.get(ingredient.getName());
            if (entry != null && entry.getQuantity() >= requiredQty) {
                covered += 1;
            } else {
                double availableQty = entry == null ? 0 : entry.getQuantity();
                missing.add(new MissingItem(ingredient.getName(), requiredQty, ingredient.getUnit(), availableQty));
            }
        }
        double coverage = required.isEmpty() ? 1.0 : (double) covered / required.size();
        return new CoverageResult(coverage, missing);
    }

    private static double scaleQuantity(double quantity, int recipeServings, int targetServings) {
        double factor = (double) targetServings / recipeServings;
        return Math.round(quantity * factor * 100.0) / 100.0;
    }

    private static List<PlanItem> planWeek(List<Recipe> recipes, Map<String, InventoryItem> available, int days, int targetServings) {
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Recipe recipe : recipes) {
            CoverageResult result = computeCoverage(recipe, available, targetServings);
            Map<String, Object> entry = new HashMap<>();
            entry.put("recipe", recipe);
            entry.put("coverage", result.coverage());
            entry.put("missing", result.missing());
            scored.add(entry);
        }

        scored.sort(Comparator
                .comparing((Map<String, Object> m) -> (Double) m.get("coverage"))
                .thenComparing(m -> -((List<?>) m.get("missing")).size())
                .reversed());

        List<Map<String, Object>> candidates = scored.stream()
                .filter(m -> (Double) m.get("coverage") >= 0.8)
                .toList();
        if (candidates.isEmpty()) {
            candidates = scored.stream().limit(3).toList();
        }

        List<PlanItem> plan = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            Map<String, Object> entry = candidates.get(i % candidates.size());
            Recipe recipe = (Recipe) entry.get("recipe");
            double coverage = Math.round(((Double) entry.get("coverage")) * 100.0) / 100.0;
            @SuppressWarnings("unchecked")
            List<MissingItem> missing = (List<MissingItem>) entry.get("missing");
            plan.add(new PlanItem(DAY_NAMES.get(i % DAY_NAMES.size()), recipe, coverage, missing));
        }
        return plan;
    }

    private static Map<String, ShoppingEntry> buildShoppingList(List<PlanItem> plan) {
        Map<String, ShoppingEntry> shopping = new HashMap<>();
        for (PlanItem item : plan) {
            for (MissingItem missing : item.getMissing()) {
                ShoppingEntry entry = shopping.getOrDefault(missing.getName(), new ShoppingEntry(missing.getUnit(), 0));
                entry.addQuantity(missing.getRequired());
                shopping.put(missing.getName(), entry);
            }
        }
        return shopping;
    }

    private static List<String> suggestSubstitutions(List<MissingItem> missing) {
        List<String> suggestions = new ArrayList<>();
        for (MissingItem item : missing) {
            List<String> subs = SUBSTITUTIONS.get(item.getName());
            if (subs != null && !subs.isEmpty()) {
                suggestions.add(item.getName() + ": ersetze durch " + String.join(", ", subs));
            }
        }
        return suggestions;
    }

    private static String formatPlan(List<PlanItem> plan) {
        StringBuilder builder = new StringBuilder();
        for (PlanItem item : plan) {
            builder.append(String.format("%s: %s (Abdeckung: %d%%)%n",
                    item.getDay(), item.getRecipe().getTitle(), (int) (item.getCoverage() * 100)));
            if (item.getMissing().isEmpty()) {
                builder.append("  Alle Zutaten vorhanden.\n");
            } else {
                builder.append("  Fehlende Zutaten:\n");
                for (MissingItem missing : item.getMissing()) {
                    builder.append(String.format(
                            "    - %s: benötigt %.2f %s, verfügbar %.2f%n",
                            missing.getName(), missing.getRequired(), missing.getUnit(), missing.getAvailable()
                    ));
                }
                List<String> substitutions = suggestSubstitutions(item.getMissing());
                if (!substitutions.isEmpty()) {
                    builder.append("  Vorschläge für Substitution:\n");
                    substitutions.forEach(sub -> builder.append("    - ").append(sub).append("\n"));
                }
            }
        }
        return builder.toString();
    }

    private static String formatShopping(Map<String, ShoppingEntry> shopping) {
        if (shopping.isEmpty()) {
            return "Keine zusätzlichen Einkäufe nötig.";
        }
        return "Einkaufsliste (kumuliert):\n" + shopping.entrySet().stream()
                .map(entry -> String.format("- %s: %.2f %s", entry.getKey(), entry.getValue().quantity(), entry.getValue().unit()))
                .collect(Collectors.joining("\n"));
    }

    private static class ShoppingEntry {
        private final String unit;
        private double quantity;

        ShoppingEntry(String unit, double quantity) {
            this.unit = unit;
            this.quantity = quantity;
        }

        void addQuantity(double add) {
            this.quantity = Math.round((this.quantity + add) * 100.0) / 100.0;
        }

        public String unit() {
            return unit;
        }

        public double quantity() {
            return quantity;
        }
    }
}
