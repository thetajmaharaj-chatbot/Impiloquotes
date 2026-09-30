package za.co.webleadfinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 4201;
    private static final int BG = Color.rgb(7, 17, 31);
    private static final int PANEL = Color.rgb(13, 27, 45);
    private static final int LINE = Color.rgb(35, 59, 82);
    private static final int TEXT = Color.rgb(238, 247, 255);
    private static final int MUTED = Color.rgb(146, 168, 189);
    private static final int GREEN = Color.rgb(53, 224, 161);

    private final ArrayList<JSONObject> leads = new ArrayList<>();
    private ArrayAdapter<String> leadAdapter;
    private final ArrayList<String> rows = new ArrayList<>();
    private SharedPreferences prefs;

    private EditText locationInput;
    private EditText queryInput;
    private EditText urlInput;
    private Spinner timeframeSpinner;
    private Spinner presetSpinner;
    private TextView statusText;
    private TextView statsText;
    private ListView leadList;
    private String pendingCsv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("weblead", MODE_PRIVATE);
        loadLeads();
        buildUi();
        handleShare(getIntent());
        render();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShare(intent);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(10));
        root.setBackgroundColor(BG);

        TextView title = label("WebLead Finder", 24, TEXT, true);
        root.addView(title);
        TextView sub = label("Free website lead hunter • Android • R0 paid API", 12, MUTED, false);
        sub.setPadding(0, 0, 0, dp(12));
        root.addView(sub);

        statsText = label("", 13, GREEN, true);
        statsText.setPadding(dp(12), dp(10), dp(12), dp(10));
        statsText.setBackgroundColor(PANEL);
        root.addView(statsText, full());

        TextView findTitle = label("1. Find people asking for websites", 15, TEXT, true);
        findTitle.setPadding(0, dp(14), 0, dp(7));
        root.addView(findTitle);

        locationInput = input("Location", "South Africa");
        root.addView(locationInput, full());

        timeframeSpinner = spinner(new String[]{"Past 24 hours", "Past 7 days", "Past 30 days", "Any time"});
        root.addView(timeframeSpinner, full());

        presetSpinner = spinner(new String[]{
                "Hot website requests",
                "Website quotation intent",
                "E-commerce / online store",
                "Website redesign / revamp"
        });
        root.addView(presetSpinner, full());

        queryInput = input("Optional custom search, e.g. \"need a website\" Durban", "");
        root.addView(queryInput, full());

        LinearLayout searchRow = row();
        Button google = button("Search Google", true);
        google.setOnClickListener(v -> openSearch("google"));
        searchRow.addView(google, weight());
        Button ddg = button("DuckDuckGo", false);
        ddg.setOnClickListener(v -> openSearch("duckduckgo"));
        searchRow.addView(ddg, weight());
        root.addView(searchRow, full());

        TextView hint = label("Open a promising public result in your browser, then Share → WebLead Finder.", 11, MUTED, false);
        hint.setPadding(0, dp(6), 0, dp(10));
        root.addView(hint);

        TextView analyseTitle = label("2. Analyse a public lead page", 15, TEXT, true);
        analyseTitle.setPadding(0, dp(4), 0, dp(7));
        root.addView(analyseTitle);

        urlInput = input("Paste or share a public URL", "");
        root.addView(urlInput, full());

        Button analyse = button("Analyse & Save Lead", true);
        analyse.setOnClickListener(v -> analyseCurrent());
        root.addView(analyse, full());

        statusText = label("Ready. No paid search API required.", 11, MUTED, false);
        statusText.setPadding(0, dp(7), 0, dp(7));
        root.addView(statusText);

        LinearLayout actionRow = row();
        Button export = button("Export CSV", false);
        export.setOnClickListener(v -> exportCsv());
        actionRow.addView(export, weight());
        Button removeLow = button("Remove low-score", false);
        removeLow.setOnClickListener(v -> {
            int before = leads.size();
            for (int i = leads.size() - 1; i >= 0; i--) if (leads.get(i).optInt("score", 0) < 40) leads.remove(i);
            saveLeads();
            render();
            toast("Removed " + (before - leads.size()) + " low-score leads");
        });
        actionRow.addView(removeLow, weight());
        root.addView(actionRow, full());

        TextView savedTitle = label("Saved leads", 15, TEXT, true);
        savedTitle.setPadding(0, dp(12), 0, dp(6));
        root.addView(savedTitle);

        leadList = new ListView(this);
        leadList.setDividerHeight(dp(8));
        leadList.setBackgroundColor(BG);
        leadAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(TEXT);
                tv.setTextSize(13);
                tv.setPadding(dp(12), dp(11), dp(12), dp(11));
                tv.setBackgroundColor(PANEL);
                tv.setMinHeight(dp(76));
                return tv;
            }
        };
        leadList.setAdapter(leadAdapter);
        leadList.setOnItemClickListener((parent, view, position, id) -> showLead(position));
        root.addView(leadList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void openSearch(String engine) {
        try {
            String custom = queryInput.getText().toString().trim();
            String query = custom.isEmpty() ? presetQuery(presetSpinner.getSelectedItemPosition()) : custom;
            String loc = locationInput.getText().toString().trim();
            if (!loc.isEmpty()) query += " \"" + loc + "\"";
            query += " -jobs -vacancies -course -tutorial";

            String q = URLEncoder.encode(query, "UTF-8");
            int t = timeframeSpinner.getSelectedItemPosition();
            String url;
            if ("duckduckgo".equals(engine)) {
                String df = t == 0 ? "&df=d" : t == 1 ? "&df=w" : t == 2 ? "&df=m" : "";
                url = "https://duckduckgo.com/?q=" + q + df;
            } else {
                String tbs = t == 0 ? "&tbs=qdr:d" : t == 1 ? "&tbs=qdr:w" : t == 2 ? "&tbs=qdr:m" : "";
                url = "https://www.google.com/search?q=" + q + tbs;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("Could not open search");
        }
    }

    private String presetQuery(int position) {
        if (position == 1) return "(\"website quotation\" OR \"quote for a website\" OR \"quotation for a website\")";
        if (position == 2) return "(\"need an ecommerce website\" OR \"need an online store\" OR \"shopify developer needed\")";
        if (position == 3) return "(\"website redesign needed\" OR \"redesign our website\" OR \"website revamp\")";
        return "(\"need a website\" OR \"looking for a web designer\" OR \"website developer needed\" OR \"website designer needed\")";
    }

    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null || text.trim().isEmpty()) return;
        if (urlInput != null) {
            urlInput.setText(text);
            statusText.setText("Shared browser result received. Analysing...");
            analyseCurrent();
        }
    }

    private void analyseCurrent() {
        String raw = urlInput.getText().toString().trim();
        String url = extractUrl(raw);
        if (url == null) {
            toast("Paste or share a valid http/https URL");
            return;
        }
        statusText.setText("Scanning public page for buying intent and contact details...");
        new Thread(() -> analyseUrl(url)).start();
    }

    private void analyseUrl(String source) {
        HttpURLConnection connection = null;
        try {
            URL u = new URL(source);
            connection = (HttpURLConnection) u.openConnection();
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 Chrome/140.0 Mobile Safari/537.36");
            connection.setRequestProperty("Accept-Language", "en-ZA,en;q=0.9");
            int code = connection.getResponseCode();
            if (code >= 400) throw new Exception("Website returned HTTP " + code);

            String finalUrl = connection.getURL().toString();
            String html = readLimited(connection.getInputStream(), 1600000);
            if (html.trim().isEmpty()) throw new Exception("No readable HTML returned");

            String title = extractTitle(html);
            String text = htmlToText(html);
            if (title.isEmpty()) title = Uri.parse(finalUrl).getHost();

            Set<String> emails = matches(html, Pattern.compile("(?i)(?<![A-Z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,24}(?![A-Z0-9._%+-])"), true);
            filterEmails(emails);

            Set<String> phones = new LinkedHashSet<>();
            phones.addAll(matches(html, Pattern.compile("(?i)tel:\\s*([+0-9][0-9\\s().-]{7,20})"), false));
            phones.addAll(matches(text, Pattern.compile("(?<!\\d)(?:\\+27|0)[1-8][0-9](?:[\\s().-]*\\d){7}(?!\\d)"), false));

            Set<String> wa = matches(html, Pattern.compile("(?i)https?://(?:wa\\.me|api\\.whatsapp\\.com|chat\\.whatsapp\\.com)/[^\\s\\\"'<>]+"), false);

            ArrayList<String> reasons = new ArrayList<>();
            int score = score((title + " " + text).toLowerCase(Locale.ROOT), emails, phones, wa, reasons);

            String excerpt = text.replaceAll("\\s+", " ").trim();
            if (excerpt.length() > 420) excerpt = excerpt.substring(0, 420) + "…";

            JSONObject lead = new JSONObject();
            lead.put("title", clean(title));
            lead.put("sourceUrl", finalUrl);
            lead.put("host", Uri.parse(finalUrl).getHost());
            lead.put("score", score);
            lead.put("email", first(emails));
            lead.put("phone", first(phones));
            lead.put("whatsapp", first(wa));
            lead.put("emails", new JSONArray(emails));
            lead.put("phones", new JSONArray(phones));
            lead.put("whatsappLinks", new JSONArray(wa));
            lead.put("reasons", new JSONArray(reasons));
            lead.put("excerpt", excerpt);
            lead.put("createdAt", System.currentTimeMillis());

            runOnUiThread(() -> {
                addOrUpdateLead(lead);
                urlInput.setText("");
                statusText.setText(score >= 80 ? "HOT lead saved: " + score + "/100" : "Lead saved: " + score + "/100");
                render();
                toast(score >= 80 ? "Hot lead saved" : "Lead saved");
            });
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Analysis failed" : e.getMessage();
            runOnUiThread(() -> statusText.setText("Could not analyse: " + msg));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private int score(String t, Set<String> emails, Set<String> phones, Set<String> wa, List<String> reasons) {
        int s = 0;
        if (containsAny(t, new String[]{"looking for a web designer","looking for web designer","need a web designer","need someone to build a website","need someone to build my website","need a website","website developer needed","web developer needed","website designer needed","looking for website developer","website quotation","quote for a website","need an ecommerce website","need an e-commerce website","need an online store","website redesign needed","looking for wordpress developer"})) {
            s += 48; reasons.add("Explicit website-buying intent");
        } else if (containsAny(t, new String[]{"website redesign","redesign our website","new website","build our website","build my website","create a website","web design quote","website quote"})) {
            s += 35; reasons.add("Strong website project language");
        } else if (containsAny(t, new String[]{"website","web design","web developer","wordpress","shopify","online store"})) {
            s += 12; reasons.add("Website-related content");
        }
        if (containsAny(t, new String[]{"urgent","urgently","asap","immediately","this week","today","quote needed","quotation needed"})) {
            s += 14; reasons.add("Urgency language");
        }
        if (containsAny(t, new String[]{"no website","don't have a website","do not have a website","without a website","need our first website"})) {
            s += 16; reasons.add("No existing website indicated");
        }
        if (!emails.isEmpty()) { s += 10; reasons.add("Public email found"); }
        if (!phones.isEmpty()) { s += 8; reasons.add("Public phone found"); }
        if (!wa.isEmpty()) { s += 6; reasons.add("Public WhatsApp found"); }

        if (containsAny(t, new String[]{"we build websites","our web design services","web design agency","website packages","hire our web designers","professional web design services","we design websites"})) {
            s -= 50; reasons.add("Likely website seller");
        }
        if (containsAny(t, new String[]{"job vacancy","vacancy","career opportunity","apply for this job","web developer job","web designer job","salary","cv required"})) {
            s -= 35; reasons.add("Likely employment listing");
        }
        if (containsAny(t, new String[]{"how to build a website","website tutorial","web design course","learn web design"})) {
            s -= 30; reasons.add("Likely educational content");
        }
        return Math.max(0, Math.min(100, s));
    }

    private void addOrUpdateLead(JSONObject lead) {
        String source = lead.optString("sourceUrl");
        String email = lead.optString("email");
        for (int i = 0; i < leads.size(); i++) {
            JSONObject old = leads.get(i);
            if (source.equals(old.optString("sourceUrl")) || (!email.isEmpty() && email.equalsIgnoreCase(old.optString("email")))) {
                leads.set(i, lead);
                saveLeads();
                return;
            }
        }
        leads.add(0, lead);
        saveLeads();
    }

    private void render() {
        int hot = 0, today = 0;
        String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        rows.clear();
        for (JSONObject x : leads) {
            int score = x.optInt("score", 0);
            if (score >= 80) hot++;
            String d = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(x.optLong("createdAt", 0)));
            if (todayKey.equals(d)) today++;
            String tier = score >= 80 ? "HOT" : score >= 60 ? "WARM" : "LEAD";
            String contact = !x.optString("email").isEmpty() ? x.optString("email") : x.optString("phone");
            rows.add(tier + "  " + score + "/100\n" + x.optString("title", "Untitled lead") + (contact.isEmpty() ? "" : "\n" + contact));
        }
        statsText.setText("Hot: " + hot + "     Saved: " + leads.size() + "     Today: " + today);
        if (leadAdapter != null) leadAdapter.notifyDataSetChanged();
    }

    private void showLead(int position) {
        if (position < 0 || position >= leads.size()) return;
        JSONObject x = leads.get(position);
        StringBuilder msg = new StringBuilder();
        msg.append("Score: ").append(x.optInt("score")).append("/100\n\n");
        if (!x.optString("email").isEmpty()) msg.append("Email: ").append(x.optString("email")).append("\n");
        if (!x.optString("phone").isEmpty()) msg.append("Phone: ").append(x.optString("phone")).append("\n");
        if (!x.optString("whatsapp").isEmpty()) msg.append("WhatsApp: ").append(x.optString("whatsapp")).append("\n");
        msg.append("\nSource:\n").append(x.optString("sourceUrl")).append("\n\n");
        msg.append("Signals:\n");
        JSONArray rs = x.optJSONArray("reasons");
        if (rs != null) for (int i = 0; i < rs.length(); i++) msg.append("• ").append(rs.optString(i)).append("\n");
        msg.append("\n").append(x.optString("excerpt"));

        String[] actions = {"Open source", "Email", "Call", "Share", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle(x.optString("title", "Lead"))
                .setMessage(msg.toString())
                .setItems(actions, (d, which) -> {
                    if (which == 0) openExternal(x.optString("sourceUrl"));
                    else if (which == 1) {
                        String email = x.optString("email");
                        if (email.isEmpty()) toast("No email found"); else openExternal("mailto:" + email);
                    } else if (which == 2) {
                        String phone = x.optString("phone");
                        if (phone.isEmpty()) toast("No phone found"); else openExternal("tel:" + phone);
                    } else if (which == 3) shareLead(x);
                    else {
                        leads.remove(position);
                        saveLeads();
                        render();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void shareLead(JSONObject x) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT,
                x.optString("title") + "\nLead score: " + x.optInt("score") + "/100\n" +
                (!x.optString("email").isEmpty() ? "Email: " + x.optString("email") + "\n" : "") +
                (!x.optString("phone").isEmpty() ? "Phone: " + x.optString("phone") + "\n" : "") +
                "Source: " + x.optString("sourceUrl"));
        startActivity(Intent.createChooser(i, "Share lead"));
    }

    private void exportCsv() {
        if (leads.isEmpty()) {
            toast("No leads to export");
            return;
        }
        StringBuilder csv = new StringBuilder();
        csv.append("Score,Business or Page,Email,Phone,WhatsApp,Source URL,Reasons,Saved\n");
        for (JSONObject x : leads) {
            csv.append(cell(x.optInt("score"))).append(",");
            csv.append(cell(x.optString("title"))).append(",");
            csv.append(cell(x.optString("email"))).append(",");
            csv.append(cell(x.optString("phone"))).append(",");
            csv.append(cell(x.optString("whatsapp"))).append(",");
            csv.append(cell(x.optString("sourceUrl"))).append(",");
            csv.append(cell(x.optJSONArray("reasons"))).append(",");
            csv.append(cell(new Date(x.optLong("createdAt")).toString())).append("\n");
        }
        pendingCsv = csv.toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "WebLeadFinder_" + stamp + ".csv");
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXPORT_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null && pendingCsv != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out == null) throw new Exception("No output stream");
                out.write(pendingCsv.getBytes(StandardCharsets.UTF_8));
                out.flush();
                toast("Lead CSV saved");
            } catch (Exception e) {
                toast("Export failed");
            }
            pendingCsv = null;
        }
    }

    private void loadLeads() {
        leads.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("leads", "[]"));
            for (int i = 0; i < a.length(); i++) leads.add(a.getJSONObject(i));
        } catch (Exception ignored) {}
    }

    private void saveLeads() {
        JSONArray a = new JSONArray();
        for (JSONObject x : leads) a.put(x);
        prefs.edit().putString("leads", a.toString()).apply();
    }

    private void openExternal(String url) {
        if (url == null || url.isEmpty()) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast("No app can open this link"); }
    }

    private String extractUrl(String input) {
        if (input == null) return null;
        Matcher m = Pattern.compile("https?://[^\\s<]+", Pattern.CASE_INSENSITIVE).matcher(input);
        if (!m.find()) return null;
        String out = m.group();
        while (out.endsWith(".") || out.endsWith(",") || out.endsWith(")") || out.endsWith("]")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private String readLimited(InputStream input, int max) throws Exception {
        try (BufferedInputStream in = new BufferedInputStream(input); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int total = 0, n;
            while ((n = in.read(buf)) != -1) {
                int allow = Math.min(n, max - total);
                if (allow > 0) out.write(buf, 0, allow);
                total += allow;
                if (total >= max) break;
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String extractTitle(String html) {
        Matcher m = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);
        return m.find() ? clean(Html.fromHtml(m.group(1), Html.FROM_HTML_MODE_LEGACY).toString()) : "";
    }

    private String htmlToText(String html) {
        String x = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ");
        return clean(Html.fromHtml(x, Html.FROM_HTML_MODE_LEGACY).toString());
    }

    private Set<String> matches(String input, Pattern p, boolean lower) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = p.matcher(input == null ? "" : input);
        while (m.find() && out.size() < 20) {
            String v = m.groupCount() >= 1 ? m.group(1) : m.group();
            if (v != null) {
                v = clean(v);
                if (lower) v = v.toLowerCase(Locale.ROOT);
                if (!v.isEmpty()) out.add(v);
            }
        }
        return out;
    }

    private void filterEmails(Set<String> emails) {
        ArrayList<String> bad = new ArrayList<>();
        for (String e : emails) {
            String x = e.toLowerCase(Locale.ROOT);
            if (x.endsWith(".png") || x.endsWith(".jpg") || x.endsWith(".gif") || x.endsWith(".webp") || x.contains("example.com") || x.startsWith("noreply@") || x.startsWith("no-reply@")) bad.add(e);
        }
        emails.removeAll(bad);
    }

    private String first(Set<String> values) {
        for (String x : values) return x;
        return "";
    }

    private boolean containsAny(String text, String[] values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    private String clean(String s) {
        return s == null ? "" : s.replace("\u0000", "").replaceAll("\\s+", " ").trim();
    }

    private String cell(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        return "\"" + s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(13);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(PANEL);
        return e;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(TEXT);
                tv.setBackgroundColor(PANEL);
                tv.setPadding(dp(10), dp(10), dp(10), dp(10));
                return tv;
            }
        };
        s.setAdapter(a);
        s.setBackgroundColor(PANEL);
        return s;
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(4, 30, 25) : TEXT);
        b.setBackgroundColor(primary ? GREEN : Color.rgb(20, 40, 61));
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, dp(7), 0, 0);
        return r;
    }

    private LinearLayout.LayoutParams full() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(4), 0, dp(4));
        return p;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
