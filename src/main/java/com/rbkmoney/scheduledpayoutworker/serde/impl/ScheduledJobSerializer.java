package com.rbkmoney.scheduledpayoutworker.serde.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.scheduledpayoutworker.model.ScheduledJobContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ScheduledJobSerializer {

    private final ObjectMapper mapper;

    public byte[] writeByte(ScheduledJobContext obj) {
        try {
            return mapper.writeValueAsBytes(obj);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public ScheduledJobContext read(byte[] data) {
        try {
            return mapper.readValue(data, ScheduledJobContext.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

