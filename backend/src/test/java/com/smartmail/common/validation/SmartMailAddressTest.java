package com.smartmail.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartMailAddressTest {
    @Test
    void acceptsOnlyNormalizedSmartMailAddresses() {
        assertThat(SmartMailAddress.isAllowed(" User.Name@SMAIL.COM ")).isTrue();
        assertThat(SmartMailAddress.normalize(" User.Name@SMAIL.COM "))
                .isEqualTo("user.name@smail.com");
        assertThat(SmartMailAddress.isAllowed("user@example.com")).isFalse();
        assertThat(SmartMailAddress.isAllowed("user@sub.smail.com")).isFalse();
        assertThat(SmartMailAddress.isAllowed("@smail.com")).isFalse();
    }
}
