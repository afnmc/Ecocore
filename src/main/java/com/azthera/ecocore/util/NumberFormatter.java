package com.azthera.ecocore.util;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
 
/**
 * Formats currency and generic numeric values for GUI/chat display,
 * with optional K/M/B/T suffix compression for large economy numbers.
 */
public final class NumberFormatter {
 
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};
 
    private final int decimalPlaces;
    private final boolean useSuffix;
    private final DecimalFormat plainFormat;
 
    public NumberFormatter(int decimalPlaces, boolean useSuffix) {
        this.decimalPlaces = Math.max(0, decimalPlaces);
        this.useSuffix = useSuffix;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        StringBuilder pattern = new StringBuilder("#,##0");
        if (this.decimalPlaces > 0) {
            pattern.append('.');
            pattern.append("0".repeat(this.decimalPlaces));
        }
        this.plainFormat = new DecimalFormat(pattern.toString(), symbols);
    }
 
    public String format(double value) {
        if (!useSuffix) {
            return plainFormat.format(value);
        }
        return formatSuffixed(value);
    }
 
    public String format(BigDecimal value) {
        return format(value.doubleValue());
    }
 
    private String formatSuffixed(double value) {
        double absValue = Math.abs(value);
        int suffixIndex = 0;
        while (absValue >= 1000.0 && suffixIndex < SUFFIXES.length - 1) {
            absValue /= 1000.0;
            suffixIndex++;
        }
        BigDecimal rounded = BigDecimal.valueOf(absValue).setScale(decimalPlaces, RoundingMode.HALF_UP);
        String sign = value < 0 ? "-" : "";
        return sign + plainFormat.format(rounded.doubleValue()) + SUFFIXES[suffixIndex];
    }
 
    public String formatPlain(double value) {
        return plainFormat.format(value);
    }
}
