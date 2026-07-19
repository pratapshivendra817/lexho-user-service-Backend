package com.lexho.user_service.dispatch.service;

public final class GeoHashUtil {
    private static final char[] BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray();

    private GeoHashUtil() {
    }

    public static String encode(double latitude, double longitude, int precision) {
        int safePrecision = Math.max(4, Math.min(12, precision));
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};
        StringBuilder geohash = new StringBuilder(safePrecision);
        boolean evenBit = true;
        int bit = 0;
        int ch = 0;

        while (geohash.length() < safePrecision) {
            if (evenBit) {
                double mid = (lonRange[0] + lonRange[1]) / 2.0;
                if (longitude >= mid) {
                    ch |= 1 << (4 - bit);
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2.0;
                if (latitude >= mid) {
                    ch |= 1 << (4 - bit);
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }
            evenBit = !evenBit;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32[ch]);
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }
}

