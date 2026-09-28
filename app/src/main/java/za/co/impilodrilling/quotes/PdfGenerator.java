package za.co.impilodrilling.quotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class PdfGenerator {
    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;

    // Reference canvas is the A4 render of the supplied Excel estimate.
    private static final float REF_W = 2480f;
    private static final float REF_H = 3508f;

    private PdfGenerator() { }

    private static float x(float px) { return px * PAGE_W / REF_W; }
    private static float y(float px) { return px * PAGE_H / REF_H; }

    public static File generate(Context context, QuoteData data) throws Exception {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ImpiloQuotes");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create quotation folder");
        }

        String customer = firstNonBlank(data.company, data.contactPerson, data.projectArea, "Customer");
        String number = clean(data.estimateNumber).isEmpty() ? "Draft" : clean(data.estimateNumber);
        File output = new File(dir, "Estimate_" + safe(number) + "_" + safe(customer) + ".pdf");

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create();
        PdfDocument.Page page = document.startPage(info);
        Canvas c = page.getCanvas();

        c.drawColor(Color.WHITE);
        drawTemplate(c, data);

        document.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(output)) {
            document.writeTo(out);
        } finally {
            document.close();
        }
        return output;
    }

    private static void drawTemplate(Canvas c, QuoteData data) {
        Paint thin = stroke(0.28f);
        Paint medium = stroke(0.62f);

        // Excel outer frame and main section separators.
        c.drawLine(x(1), y(1), x(2478), y(1), medium);
        c.drawLine(x(1), y(1), x(1), y(3040), medium);
        c.drawLine(x(2478), y(1), x(2478), y(3040), medium);
        c.drawLine(x(1), y(370), x(2478), y(370), medium);

        Paint headerSmall = textPaint(6.1f, false);
        Paint headerSmallBold = textPaint(6.1f, true);
        Paint title = textPaint(16.8f, true);

        draw(c, "Buka Afrika Visuals t/a", 5, 108, headerSmallBold, Paint.Align.LEFT);
        draw(c, "Vat no. 4300 275 841", 5, 158, headerSmall, Paint.Align.LEFT);
        draw(c, "IMPILO DRILLING", REF_W / 2f, 100, title, Paint.Align.CENTER);
        draw(c, "Registration Number: 2008/123094/23", REF_W / 2f + 110, 156, headerSmallBold, Paint.Align.CENTER);
        draw(c, "LEVEL 1 BEE COMPLIANT COMPANY", REF_W / 2f + 110, 202, headerSmallBold, Paint.Align.CENTER);
        draw(c, "MEMBER OF GROUND WATER ASSOCIATION & BOREHOLE WATER ASSOCIATION OF S.A.",
                REF_W / 2f + 80, 246, headerSmallBold, Paint.Align.CENTER);
        draw(c, "8 Victory Road, Port Shepstone, info@impilodrilling.co.za",
                REF_W / 2f + 95, 290, headerSmallBold, Paint.Align.CENTER);
        draw(c, "www.impilodrilling.co.za             MOBILE: 083 419 2100",
                REF_W / 2f + 95, 334, headerSmallBold, Paint.Align.CENTER);

        Paint body = textPaint(5.7f, false);
        Paint bodyBold = textPaint(5.9f, true);

        drawMixed(c, 5, 438, "Estimate  No. ", clean(data.estimateNumber), bodyBold, bodyBold);
        drawMixed(c, 2006, 438, "DATE: ", clean(data.date), body, body);
        drawMixed(c, 5, 484, "Company:  ", clean(data.company), body, body);
        drawMixed(c, 5, 530, "Contact Person: ", clean(data.contactPerson), body, body);
        drawMixed(c, 5, 576, "Address:  ", clean(data.address), body, body);
        drawMixed(c, 5, 622, "Mobile: ", clean(data.mobile), body, body);
        drawMixed(c, 5, 668, "Email: ", clean(data.email), body, body);
        drawMixed(c, 5, 714, "Project Area:  ", clean(data.projectArea), body, body);
        drawMixed(c, 5, 760, "Vat no:  ", clean(data.vatNumber), body, body);

        // Table geometry copied from the supplied Excel render.
        final float[] cols = {1, 143, 1555, 1829, 2004, 2232, 2478};
        final float tableTop = 770;
        final float rowH = 46;

        // Section shading (Excel rows 22 and 30).
        Paint sectionFill = new Paint();
        sectionFill.setColor(Color.rgb(222, 218, 196));
        c.drawRect(x(cols[0]), y(tableTop + 5 * rowH), x(cols[6]), y(tableTop + 6 * rowH), sectionFill);
        c.drawRect(x(cols[0]), y(tableTop + 13 * rowH), x(cols[6]), y(tableTop + 14 * rowH), sectionFill);

        for (float col : cols) {
            c.drawLine(x(col), y(tableTop), x(col), y(tableTop + 28 * rowH), thin);
        }
        for (int i = 0; i <= 28; i++) {
            c.drawLine(x(cols[0]), y(tableTop + i * rowH), x(cols[6]), y(tableTop + i * rowH), thin);
        }

        // Outer table sides match the medium Excel border.
        c.drawLine(x(cols[0]), y(tableTop), x(cols[0]), y(tableTop + 28 * rowH), medium);
        c.drawLine(x(cols[6]), y(tableTop), x(cols[6]), y(tableTop + 28 * rowH), medium);

        Paint tableHeader = textPaint(5.7f, true);
        float headerBase = tableTop + 34;
        draw(c, "ITEM", mid(cols[0], cols[1]), headerBase, tableHeader, Paint.Align.CENTER);
        draw(c, "DESCRIPTION", mid(cols[1], cols[2]), headerBase, tableHeader, Paint.Align.CENTER);
        draw(c, "UNIT", mid(cols[2], cols[3]), headerBase, tableHeader, Paint.Align.CENTER);
        draw(c, "QUANT", mid(cols[3], cols[4]), headerBase, tableHeader, Paint.Align.CENTER);
        draw(c, "RATE", mid(cols[4], cols[5]), headerBase, tableHeader, Paint.Align.CENTER);
        draw(c, "TOTAL", mid(cols[5], cols[6]), headerBase, tableHeader, Paint.Align.CENTER);

        Paint table = textPaint(5.35f, false);
        Paint tableBold = textPaint(5.35f, true);

        int row = 18;
        for (LineItem item : data.items) {
            float rowTop = tableTop + (row - 17) * rowH;
            float base = rowTop + 34;
            Paint p = item.editable ? table : tableBold;

            draw(c, item.item, mid(cols[0], cols[1]), base, p, Paint.Align.CENTER);
            drawFit(c, item.description, cols[1] + 8, base, cols[2] - cols[1] - 16, p, 4.1f);
            if (!clean(item.unit).isEmpty()) {
                draw(c, item.unit, mid(cols[2], cols[3]), base, p, Paint.Align.CENTER);
            }

            if (item.editable) {
                draw(c, quantity(item.getQuantity()), mid(cols[3], cols[4]), base, table, Paint.Align.CENTER);
                draw(c, rate(item.getRate()), cols[5] - 8, base, table, Paint.Align.RIGHT);
                draw(c, lineTotal(item.amount()), cols[6] - 8, base, table, Paint.Align.RIGHT);
            } else {
                draw(c, "0", cols[6] - 8, base, table, Paint.Align.RIGHT);
            }
            row++;
        }

        // Row 45 and total box.
        float row45Top = tableTop + 28 * rowH;
        draw(c, "0.00", cols[5] - 8, row45Top + 34, table, Paint.Align.RIGHT);

        float totalTop = row45Top + rowH;
        for (int i = 0; i < 3; i++) {
            float yy = totalTop + i * rowH;
            c.drawLine(x(cols[4]), y(yy), x(cols[6]), y(yy), thin);
        }
        c.drawLine(x(cols[4]), y(totalTop), x(cols[4]), y(totalTop + 3 * rowH), thin);
        c.drawLine(x(cols[5]), y(totalTop), x(cols[5]), y(totalTop + 3 * rowH), thin);
        c.drawLine(x(cols[6]), y(totalTop), x(cols[6]), y(totalTop + 3 * rowH), medium);

        BigDecimal subtotal = QuoteCalculator.subtotal(data);
        BigDecimal vat = QuoteCalculator.vat(data);
        BigDecimal total = QuoteCalculator.grandTotal(data);
        Paint totalPaint = textPaint(5.7f, true);

        draw(c, "SUB TOTAL", cols[4] + 8, totalTop + 34, totalPaint, Paint.Align.LEFT);
        draw(c, noGroupingMoney(subtotal), cols[6] - 8, totalTop + 34, totalPaint, Paint.Align.RIGHT);
        draw(c, "VAT", cols[4] + 8, totalTop + rowH + 34, totalPaint, Paint.Align.LEFT);
        draw(c, groupedMoney(vat), cols[6] - 8, totalTop + rowH + 34, totalPaint, Paint.Align.RIGHT);
        draw(c, "TOTAL", cols[4] + 8, totalTop + 2 * rowH + 34, totalPaint, Paint.Align.LEFT);
        draw(c, groupedMoney(total), cols[6] - 8, totalTop + 2 * rowH + 34, totalPaint, Paint.Align.RIGHT);

        // Banking block.
        Paint yellow = new Paint();
        yellow.setColor(Color.YELLOW);
        c.drawRect(x(1), y(2244), x(1555), y(2290), yellow);
        c.drawLine(x(1), y(2290), x(1555), y(2290), medium);
        c.drawLine(x(1), y(2290), x(1), y(2477), medium);
        c.drawLine(x(1555), y(2290), x(1555), y(2477), medium);
        c.drawLine(x(1), y(2477), x(1555), y(2477), medium);

        Paint bank = textPaint(5.0f, false);
        Paint bankBold = textPaint(5.1f, true);
        draw(c, "BANKING DETAILS:", 3, 2278, bankBold, Paint.Align.LEFT);
        draw(c, "Impilo Drilling", 3, 2330, bankBold, Paint.Align.LEFT);
        draw(c, "First National Bank", 3, 2376, bank, Paint.Align.LEFT);
        draw(c, "BRANCH: Shelly Beach, 250062", 3, 2422, bank, Paint.Align.LEFT);
        draw(c, "ACCOUNT: 631 808 944 88", 3, 2468, bank, Paint.Align.LEFT);

        // Terms & Conditions from the same Excel estimate.
        Paint terms = textPaint(4.8f, false);
        Paint termsBold = textPaint(4.9f, true);
        draw(c, "Terms & Conditions:", 3, 2515, termsBold, Paint.Align.LEFT);
        draw(c, "Once any payment is received towards this estimate, you will have entered and accepted our terms and conditions.",
                3, 2561, terms, Paint.Align.LEFT);
        draw(c, "R10 000 DEPOSIT, BALANCE TO BE PAID ONCE THE TEAM ARRIVES ON SITE AND PRIOR TO SETUP.",
                3, 2607, terms, Paint.Align.LEFT);

        String description = clean(data.estimateDescription);
        if (!description.isEmpty()) {
            drawFit(c, description, 3, 2653, 2420, terms, 3.6f);
        }

        draw(c, "Trencing, for float switch and connecting to clients tank is at an additional cost.",
                3, 2699, terms, Paint.Align.LEFT);
        draw(c, "Once we drilled to the specified depth & theres no water, you will be given two hours to decide to drill deeper or to stop. NO REFUND.",
                3, 2745, terms, Paint.Align.LEFT);
        draw(c, "In the event we have to cease operations due to any circumstances by yourselves a cost of R2500 per hour will be chargeable.",
                3, 2791, terms, Paint.Align.LEFT);
        draw(c, "Impilo Drilling will not be held responsible for any damages to you or your neigbouring property and any injury to oneself whilst we are based on your site.",
                3, 2837, terms, Paint.Align.LEFT);
        draw(c, "Impilo Drilling is not responsible for applying for any necessary permissions from your local municipality, authorities or governing body to drill a borehole.",
                3, 2883, terms, Paint.Align.LEFT);
        draw(c, "Impilo Drilling will not be held responsible for any site or road cleaning.",
                3, 2929, terms, Paint.Align.LEFT);
        draw(c, "In the event our vehicles are stuck on your site and requires a recovery vehicle (tow truck) or any other means of recovery, this cost will be to the client account.",
                3, 2975, terms, Paint.Align.LEFT);
        draw(c, "Clients needs to make sure that the road is stable with wide road access in and out of the drill site.",
                3, 3021, terms, Paint.Align.LEFT);
        draw(c, "All materials remain the property of Impilo Drilling until fully paid for. We have the right to uplift any of materials or goods until fully paid.",
                3, 3067, terms, Paint.Align.LEFT);
    }

    private static float mid(float a, float b) { return (a + b) / 2f; }

    private static Paint textPaint(float size, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(Color.BLACK);
        p.setTextSize(size);
        p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private static Paint stroke(float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        return p;
    }

    private static void draw(Canvas c, String text, float px, float py, Paint p, Paint.Align align) {
        Paint.Align old = p.getTextAlign();
        p.setTextAlign(align);
        c.drawText(text == null ? "" : text, x(px), y(py), p);
        p.setTextAlign(old);
    }

    private static void drawMixed(Canvas c, float px, float py, String label, String value,
                                  Paint labelPaint, Paint valuePaint) {
        float xx = x(px);
        float yy = y(py);
        c.drawText(label, xx, yy, labelPaint);
        c.drawText(value, xx + labelPaint.measureText(label), yy, valuePaint);
    }

    private static void drawFit(Canvas c, String text, float px, float py, float maxPx,
                                Paint p, float minSize) {
        float old = p.getTextSize();
        float max = x(maxPx);
        while (p.measureText(text) > max && p.getTextSize() > minSize) {
            p.setTextSize(p.getTextSize() - 0.12f);
        }
        c.drawText(text, x(px), y(py), p);
        p.setTextSize(old);
    }

    private static String quantity(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private static String rate(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String lineTotal(BigDecimal value) {
        if (value == null) return "0";
        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
        if (rounded.stripTrailingZeros().scale() <= 0) {
            return rounded.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return rounded.toPlainString();
    }

    private static String noGroupingMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String groupedMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    private static String safe(String value) {
        return clean(value).replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("_+", "_");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "Customer";
    }
}
