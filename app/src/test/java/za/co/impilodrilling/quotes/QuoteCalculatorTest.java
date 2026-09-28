package za.co.impilodrilling.quotes;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class QuoteCalculatorTest {
    @Test
    public void matchesExcelEstimate8632Totals() {
        QuoteData data = new QuoteData();
        setQty(data, "1", "1");
        setQty(data, "3", "1");
        setQty(data, "4.1", "18");
        setQty(data, "4.2.1", "102");
        setQty(data, "5.2", "18");
        setQty(data, "7", "1");
        setQty(data, "9", "1");
        setQty(data, "11", "1");
        setQty(data, "12", "1");

        assertEquals("125640.00", QuoteCalculator.subtotal(data).setScale(2).toPlainString());
        assertEquals("18846.00", QuoteCalculator.vat(data).setScale(2).toPlainString());
        assertEquals("144486.00", QuoteCalculator.grandTotal(data).setScale(2).toPlainString());
    }

    private static void setQty(QuoteData data, String itemNo, String qty) {
        for (LineItem item : data.items) {
            if (item.item.equals(itemNo)) {
                item.setQuantity(new BigDecimal(qty));
                return;
            }
        }
        throw new AssertionError("Missing item " + itemNo);
    }
}
