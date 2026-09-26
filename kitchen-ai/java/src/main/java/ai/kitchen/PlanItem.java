package ai.kitchen;

import java.util.List;

public class PlanItem {
    private final String day;
    private final Recipe recipe;
    private final double coverage;
    private final List<MissingItem> missing;

    public PlanItem(String day, Recipe recipe, double coverage, List<MissingItem> missing) {
        this.day = day;
        this.recipe = recipe;
        this.coverage = coverage;
        this.missing = missing;
    }

    public String getDay() {
        return day;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public double getCoverage() {
        return coverage;
    }

    public List<MissingItem> getMissing() {
        return missing;
    }
}
