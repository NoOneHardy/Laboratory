package ai.kitchen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe {
    private String id;
    private String title;
    private int servings;
    private List<Ingredient> ingredients;
    private List<String> tags;
    private List<String> steps;
    private Map<String, Object> time;
    private List<String> appliances;
    private List<String> allergens;

    public Recipe() {}

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getServings() {
        return servings;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getSteps() {
        return steps;
    }

    public Map<String, Object> getTime() {
        return time;
    }

    public List<String> getAppliances() {
        return appliances;
    }

    public List<String> getAllergens() {
        return allergens;
    }
}
