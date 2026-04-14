package com.example.apitool.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {

    @Valid
    @NotNull
    private TierSettings tiers = new TierSettings();

    public TierSettings getTiers() {
        return tiers;
    }

    public void setTiers(TierSettings tiers) {
        this.tiers = tiers;
    }

    public TierRateLimitConfig resolve(UserTier tier) {
        return tier == UserTier.PREMIUM ? tiers.getPremium() : tiers.getFree();
    }

    public static class TierSettings {

        @Valid
        @NotNull
        private TierRateLimitConfig free = new TierRateLimitConfig();

        @Valid
        @NotNull
        private TierRateLimitConfig premium = new TierRateLimitConfig();

        public TierRateLimitConfig getFree() {
            return free;
        }

        public void setFree(TierRateLimitConfig free) {
            this.free = free;
        }

        public TierRateLimitConfig getPremium() {
            return premium;
        }

        public void setPremium(TierRateLimitConfig premium) {
            this.premium = premium;
        }
    }
}
