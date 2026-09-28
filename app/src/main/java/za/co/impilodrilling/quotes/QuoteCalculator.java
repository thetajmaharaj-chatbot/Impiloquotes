package za.co.impilodrilling.quotes;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class QuoteCalculator {
    public static final BigDecimal VAT_RATE = new BigDecimal("0.15");

    private QuoteCalculator() { }

    public static BigDecimal subtotal(QuoteData data) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : data.items) total = total.add(item.amount());
        return total;
    }

    public static BigDecimal vat(QuoteData data) {
        return subtotal(data).multiply(VAT_RATE);
    }

    public static BigDecimal grandTotal(QuoteData data) {
        return subtotal(data).add(vat(data));
    }

    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
