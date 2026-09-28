package za.co.impilodrilling.quotes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class QuoteData {
    public String estimateNumber = "";
    public String date = "";
    public String company = "";
    public String contactPerson = "";
    public String address = "";
    public String mobile = "";
    public String email = "";
    public String projectArea = "";
    public String vatNumber = "";
    public String estimateDescription = "";
    public final List<LineItem> items = new ArrayList<>();

    public QuoteData() {
        add("*", "GEOSURVEY INVESTIGATION", "NO", "10000.00");
        add("1", "ESTABLISHMENT & DE - ESTABLISHMENT", "ONCE", "15000.00");
        add("2", "INTERMOVE", "PER KM", "28.00");
        add("3", "SET UP OF DRILLING MACHINE - INCLUDING DRILLING LUBE", "PER HOLE", "500.00");
        section("4", "ROTARY PERCUSSION DRILLING");
        add("4.1", "PERCUSSION REAMING", "PER METER", "400.00");
        add("4.2.1", "ROTARY: PERCUSSION DRILLING", "PER METER", "350.00");
        add("4.2.2", "ROTARY: PERCUSSION DRILLING (100 METERS ONWARDS)", "PER METER", "380.00");
        add("4.3", "ROTARY: CLAY / SAND / MUD DRILLING /ABRASIVE ROCK", "PER METER", "500.00");
        add("4.4", "SYMMETRIX DRILLING OR DRILL AND DRIVE", "PER METER", "1250.00");
        add("4.5", "SYMMETRIX SHOE", "EACH", "12000.00");
        add("4.6", "INSERTING AND REMOVING OF DRILL RODS", "PER METER", "98.00");
        section("5", "SUPPLY AND INSTALLATION OF CASING");
        add("5.1", "194mm STEEL CASINGS", "PER METER", "1200.00");
        add("5.2", "177mm STEEL CASINGS", "PER METER", "680.00");
        add("5.3", "152mm STEEL CASINGS", "PER METER", "600.00");
        add("5.4", "140mm STEEL CASINGS", "PER METER", "600.00");
        add("5.5", "UPVC CASINGS - CLASS 12", "PER METER", "480.00");
        add("6", "GRAVEL PACK", "PER BAG", "320.00");
        add("7", "DEVELOPMENT", "PER HOUR", "3000.00");
        add("8", "YIELD TEST", "ONCE", "6000.00");
        add("9", "SUPPLY AND INSTALL DAB SUBMERSIBLE PUMP AT POINT OF DRILLING ONLY", "ONCE", "48000.00");
        add("10", "TRENCHING & CONNECTING TO WATER STORAGE TANK", "PER METER", "300.00");
        add("11", "SUPPLYING AND CONNECTING FLOAT SWITCH", "ONCE", "1500.00");
        add("12", "SUPPLY AND INSTALL MANHOLE COVER FROM", "ONCE", "2500.00");
        add("13", "SANS 241 WATER ANAYSIS", "ONCE", "4800.00");
        add("14", "DELIVERY OF STEEL & UPVC CASINGS", "PER KM", "8.90");
    }

    private void add(String item, String description, String unit, String rate) {
        items.add(new LineItem(item, description, unit, true, BigDecimal.ZERO, new BigDecimal(rate)));
    }

    private void section(String item, String description) {
        items.add(new LineItem(item, description, "", false, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}
