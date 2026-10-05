package com.lexho.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class DispatchProperties {

    private final Partner partner = new Partner();
    private final Booking booking = new Booking();

    public Partner getPartner() {
        return partner;
    }

    public Booking getBooking() {
        return booking;
    }

    public static class Partner {
        private double initialSearchRadiusKm = 1.0d;
        private double maxSearchRadiusKm = 2.0d;

        public double getInitialSearchRadiusKm() {
            return initialSearchRadiusKm;
        }

        public void setInitialSearchRadiusKm(double initialSearchRadiusKm) {
            this.initialSearchRadiusKm = initialSearchRadiusKm;
        }

        public double getMaxSearchRadiusKm() {
            return maxSearchRadiusKm;
        }

        public void setMaxSearchRadiusKm(double maxSearchRadiusKm) {
            this.maxSearchRadiusKm = maxSearchRadiusKm;
        }
    }

    public static class Booking {
        private int maxEtaMinutes = 15;
        private int preDispatchMinutes = 30;

        public int getMaxEtaMinutes() {
            return maxEtaMinutes;
        }

        public void setMaxEtaMinutes(int maxEtaMinutes) {
            this.maxEtaMinutes = maxEtaMinutes;
        }

        public int getPreDispatchMinutes() {
            return preDispatchMinutes;
        }

        public void setPreDispatchMinutes(int preDispatchMinutes) {
            this.preDispatchMinutes = preDispatchMinutes;
        }
    }
}
