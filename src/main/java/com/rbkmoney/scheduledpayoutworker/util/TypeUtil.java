package com.rbkmoney.scheduledpayoutworker.util;

import java.util.Optional;

public class TypeUtil {

    public static <T extends Enum<T>> T toEnumField(String name, Class<T> enumType) {
        return Optional.ofNullable(name)
                .map(value -> Enum.valueOf(enumType, name))
                .orElse(null);
    }

    private TypeUtil() {
        throw new UnsupportedOperationException("Unable to instantiate utility class!");
    }

}
