package ai.kitchen;

public class InventoryItem {
    private double quantity;
    private String unit;

    public InventoryItem() {}

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit == null ? "" : unit;
    }
}
