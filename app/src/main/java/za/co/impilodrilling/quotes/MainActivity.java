package za.co.impilodrilling.quotes;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private QuoteData data;
    private TextView subtotalView;
    private TextView vatView;
    private TextView totalView;
    private Button previewButton;
    private Button shareButton;
    private Button printButton;
    private File lastPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = new QuoteData();
        data.date = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH)
                .format(new Date()).toUpperCase(Locale.ENGLISH);
        setContentView(buildScreen());
        updateTotals();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(36));
        root.setBackgroundColor(Color.WHITE);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("IMPILO DRILLING", 25, true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView subtitle = text("Borehole Estimate", 16, true);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle);

        root.addView(sectionTitle("Estimate details"));
        root.addView(field("Estimate No.", "", false, value -> data.estimateNumber = value));
        root.addView(field("Date", data.date, false, value -> data.date = value));

        root.addView(sectionTitle("Customer / project details"));
        root.addView(field("Company", "", false, value -> data.company = value));
        root.addView(field("Contact Person", "", false, value -> data.contactPerson = value));
        root.addView(field("Address", "", true, value -> data.address = value));
        root.addView(field("Mobile", "", false, value -> data.mobile = value));
        root.addView(field("Email", "", false, value -> data.email = value));
        root.addView(field("Project Area", "", false, value -> data.projectArea = value));
        root.addView(field("Vat no.", "", false, value -> data.vatNumber = value));

        root.addView(sectionTitle("Excel estimate items"));
        TextView instruction = text("Enter quantity and rate exactly as you would in the Excel sheet. Totals calculate automatically.", 12, false);
        instruction.setPadding(0, 0, 0, dp(8));
        root.addView(instruction);

        for (LineItem item : data.items) {
            if (!item.editable) root.addView(sectionRow(item));
            else root.addView(itemRow(item));
        }

        LinearLayout totals = new LinearLayout(this);
        totals.setOrientation(LinearLayout.VERTICAL);
        totals.setPadding(dp(10), dp(12), dp(10), dp(12));
        totals.setBackgroundColor(Color.rgb(245, 245, 245));
        subtotalView = totalLine("SUB TOTAL");
        vatView = totalLine("VAT");
        totalView = totalLine("TOTAL");
        totalView.setTextSize(17);
        totals.addView(subtotalView);
        totals.addView(vatView);
        totals.addView(totalView);
        root.addView(totals, fullWidth(dp(12)));

        root.addView(sectionTitle("Estimate wording"));
        EditText description = editText("Same one-line job description used in the Excel estimate", true);
        description.setMinLines(2);
        description.addTextChangedListener(watcher(v -> data.estimateDescription = v));
        root.addView(description, fullWidth(dp(12)));

        Button generate = actionButton("GENERATE A4 PDF", true);
        generate.setOnClickListener(v -> generatePdf());
        root.addView(generate, fullWidth(dp(10)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        previewButton = actionButton("Preview", false);
        shareButton = actionButton("Email / Share", false);
        printButton = actionButton("Print", false);
        previewButton.setEnabled(false);
        shareButton.setEnabled(false);
        printButton.setEnabled(false);
        previewButton.setOnClickListener(v -> previewPdf());
        shareButton.setOnClickListener(v -> sharePdf());
        printButton.setOnClickListener(v -> printPdf());
        actions.addView(previewButton, weightedButton());
        actions.addView(shareButton, weightedButton());
        actions.addView(printButton, weightedButton());
        root.addView(actions, fullWidth(0));

        TextView foot = text("PDF output: fixed A4 layout matching the Excel estimate template.", 11, false);
        foot.setGravity(Gravity.CENTER_HORIZONTAL);
        foot.setPadding(0, dp(14), 0, 0);
        root.addView(foot);

        return scroll;
    }

    private View itemRow(LineItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackgroundColor(Color.rgb(250, 250, 250));

        TextView name = text(item.item + "   " + item.description, 13, true);
        card.addView(name);
        TextView unit = text("UNIT: " + item.unit, 11, false);
        unit.setTextColor(Color.DKGRAY);
        card.addView(unit);

        LinearLayout inputs = new LinearLayout(this);
        inputs.setOrientation(LinearLayout.HORIZONTAL);
        inputs.setGravity(Gravity.CENTER_VERTICAL);

        EditText qty = numberEdit("Qty", "0");
        EditText rate = numberEdit("Rate", item.getRate().setScale(2).toPlainString());
        TextView amount = text("R 0.00", 12, true);
        amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        TextWatcher recalc = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                item.setQuantity(decimal(qty.getText().toString()));
                item.setRate(decimal(rate.getText().toString()));
                amount.setText("R " + money(item.amount()));
                lastPdf = null;
                setOutputButtons(false);
                updateTotals();
            }
        };
        qty.addTextChangedListener(recalc);
        rate.addTextChangedListener(recalc);

        inputs.addView(qty, weightedField());
        inputs.addView(rate, weightedField());
        inputs.addView(amount, weightedField());
        card.addView(inputs);

        LinearLayout.LayoutParams params = fullWidth(dp(4));
        params.setMargins(0, dp(3), 0, dp(3));
        card.setLayoutParams(params);
        return card;
    }

    private View sectionRow(LineItem item) {
        TextView view = text(item.item + "   " + item.description, 13, true);
        view.setPadding(dp(10), dp(9), dp(10), dp(9));
        view.setBackgroundColor(Color.rgb(224, 221, 196));
        LinearLayout.LayoutParams p = fullWidth(0);
        p.setMargins(0, dp(3), 0, dp(3));
        view.setLayoutParams(p);
        return view;
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 15, true);
        view.setPadding(0, dp(18), 0, dp(7));
        return view;
    }

    private View field(String label, String initial, boolean multiline, ValueReceiver receiver) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView l = text(label, 12, true);
        EditText e = editText(label, multiline);
        e.setText(initial);
        e.addTextChangedListener(watcher(value -> {
            receiver.receive(value);
            lastPdf = null;
            setOutputButtons(false);
        }));
        box.addView(l);
        box.addView(e);
        LinearLayout.LayoutParams p = fullWidth(dp(6));
        box.setLayoutParams(p);
        return box;
    }

    private EditText editText(String hint, boolean multiline) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setTextSize(15);
        edit.setPadding(dp(10), dp(8), dp(10), dp(8));
        edit.setSingleLine(!multiline);
        if (multiline) {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return edit;
    }

    private EditText numberEdit(String hint, String initial) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(initial);
        edit.setSelectAllOnFocus(true);
        edit.setTextSize(13);
        edit.setGravity(Gravity.CENTER);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return edit;
    }

    private TextView totalLine(String label) {
        TextView view = text(label, 14, true);
        view.setGravity(Gravity.END);
        return view;
    }

    private Button actionButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(primary ? 14 : 12);
        b.setAllCaps(false);
        b.setPadding(dp(6), dp(9), dp(6), dp(9));
        if (primary) {
            b.setTextColor(Color.WHITE);
            b.setBackgroundColor(Color.BLACK);
        }
        return b;
    }

    private void updateTotals() {
        if (subtotalView == null) return;
        subtotalView.setText("SUB TOTAL     R " + money(QuoteCalculator.subtotal(data)));
        vatView.setText("VAT (15%)     R " + money(QuoteCalculator.vat(data)));
        totalView.setText("TOTAL     R " + money(QuoteCalculator.grandTotal(data)));
    }

    private void generatePdf() {
        Toast.makeText(this, "Generating A4 estimate…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                File file = PdfGenerator.generate(this, data);
                runOnUiThread(() -> {
                    lastPdf = file;
                    setOutputButtons(true);
                    Toast.makeText(this, "PDF ready: " + file.getName(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Could not generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void previewPdf() {
        if (lastPdf == null) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", lastPdf);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdf() {
        if (lastPdf == null) return;
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", lastPdf);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Impilo Drilling Estimate " + data.estimateNumber);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Email / share estimate"));
    }

    private void printPdf() {
        if (lastPdf == null) return;
        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build();
        manager.print("Impilo Drilling Estimate", new PdfPrintAdapter(lastPdf), attributes);
    }

    private void setOutputButtons(boolean enabled) {
        if (previewButton != null) previewButton.setEnabled(enabled);
        if (shareButton != null) shareButton.setEnabled(enabled);
        if (printButton != null) printButton.setEnabled(enabled);
    }

    private TextView text(String value, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(Color.BLACK);
        view.setTextSize(size);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private TextWatcher watcher(ValueReceiver receiver) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { receiver.receive(s.toString()); }
        };
    }

    private BigDecimal decimal(String value) {
        try {
            String cleaned = value == null ? "" : value.trim().replace(",", "");
            return cleaned.isEmpty() ? BigDecimal.ZERO : new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String money(BigDecimal value) {
        DecimalFormat f = new DecimalFormat("#,##0.00");
        return f.format(value);
    }

    private LinearLayout.LayoutParams fullWidth(int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = bottomMargin;
        return p;
    }

    private LinearLayout.LayoutParams weightedField() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface ValueReceiver { void receive(String value); }
}
