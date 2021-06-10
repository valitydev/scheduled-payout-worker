package com.rbkmoney.scheduledpayoutworker.converter;

public interface BinaryConverter<T> {

    T convert(byte[] bin, Class<T> clazz);

}
