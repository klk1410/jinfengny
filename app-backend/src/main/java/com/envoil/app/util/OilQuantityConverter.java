package com.envoil.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class OilQuantityConverter {

    private OilQuantityConverter() {
    }

    /**
     * 将用户输入的数量（桶 / 斤 / 升）折算为与仓储一致的标准「桶」当量。
     */
    public static BigDecimal toBuckets(BigDecimal qty, char unit, BigDecimal densityKgPerL, BigDecimal litersPerBucket) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("数量须大于 0");
        }
        if (densityKgPerL == null || densityKgPerL.compareTo(BigDecimal.ZERO) <= 0
                || litersPerBucket == null || litersPerBucket.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("油品密度或每桶升数无效");
        }
        qty = qty.setScale(8, RoundingMode.HALF_UP);
        char u = Character.toUpperCase(unit);
        switch (u) {
            case 'B':
                return qty.setScale(4, RoundingMode.HALF_UP);
            case 'L':
                return qty.divide(litersPerBucket, 6, RoundingMode.HALF_UP);
            case 'J':
                BigDecimal kg = qty.multiply(new BigDecimal("0.5"));
                BigDecimal liters = kg.divide(densityKgPerL, 8, RoundingMode.HALF_UP);
                return liters.divide(litersPerBucket, 6, RoundingMode.HALF_UP);
            default:
                throw new IllegalArgumentException("计量单位无效");
        }
    }

    public static char normalizeOilQtyUnit(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 'B';
        }
        String t = raw.trim();
        if ("桶".equals(t)) {
            return 'B';
        }
        if ("斤".equals(t)) {
            return 'J';
        }
        if ("升".equals(t)) {
            return 'L';
        }
        String u = t.toUpperCase();
        if ("B".equals(u) || "J".equals(u) || "L".equals(u)) {
            return u.charAt(0);
        }
        return t.charAt(0);
    }
}
