package ai.kitchen;

public class MissingItem {
    private final String name;
    private final double required;
    private final String unit;
    private final double available;

    public MissingItem(String name, double required, String unit, double available) {
        this.name = name;
        this.required = required;
        this.unit = unit;
        this.available = available;
    }

    public String getName() {
        return name;
    }

    public double getRequired() {
        return required;
    }

    public String getUnit() {
        return unit;
    }

    public double getAvailable() {
        return available;
    }
}
