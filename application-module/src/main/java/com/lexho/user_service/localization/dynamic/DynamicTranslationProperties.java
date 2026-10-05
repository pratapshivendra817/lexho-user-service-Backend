package com.lexho.user_service.localization.dynamic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.dynamic-translation")
public class DynamicTranslationProperties {

    private Duration cacheTtl = Duration.ofDays(7);
    private Provider provider = new Provider();

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl == null ? Duration.ofDays(7) : cacheTtl;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider == null ? new Provider() : provider;
    }

    public static class Provider {
        private boolean enabled;
        private String name = "external";
        private String url;
        private String apiKey;
        private String model;
        private Duration timeout = Duration.ofSeconds(3);
        private int maxAttempts = 2;
        private Duration backoff = Duration.ofMillis(200);
        private List<String> allowedHosts = List.of();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = Math.max(1, Math.min(maxAttempts, 5)); }
        public Duration getBackoff() { return backoff; }
        public void setBackoff(Duration backoff) { this.backoff = backoff == null ? Duration.ofMillis(200) : backoff; }
        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts == null ? List.of() : allowedHosts; }
    }
}
