package za.co.impilodrilling.quotes;

import java.math.BigDecimal;

public final class LineItem {
    public final String item;
    public final String description;
    public final String unit;
    public final boolean editable;
    private BigDecimal quantity;
    private BigDecimal rate;

    public LineItem(String item, String description, String unit, boolean editable,
                    BigDecimal quantity, BigDecimal rate) {
        this.item = item;
        this.description = description;
        this.unit = unit;
        this.editable = editable;
        this.quantity = quantity == null ? BigDecimal.ZERO : quantity;
        this.rate = rate == null ? BigDecimal.ZERO : rate;
    }

    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getRate() { return rate; }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity == null ? BigDecimal.ZERO : quantity;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate == null ? BigDecimal.ZERO : rate;
    }

    public BigDecimal amount() {
        if (!editable) return BigDecimal.ZERO;
        return quantity.multiply(rate);
    }
}
