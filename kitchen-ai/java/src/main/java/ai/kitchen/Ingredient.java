package ai.kitchen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ingredient {
    private String name;
    private double quantity;
    private String unit;
    @JsonProperty("optional")
    private boolean optional;

    public Ingredient() {}

    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit == null ? "" : unit;
    }

    public boolean isOptional() {
        return optional;
    }
}
