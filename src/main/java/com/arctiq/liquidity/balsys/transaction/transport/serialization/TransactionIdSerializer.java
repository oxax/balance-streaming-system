package com.arctiq.liquidity.balsys.transaction.transport.serialization;

import java.io.IOException;

import com.arctiq.liquidity.balsys.transaction.core.TransactionId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class TransactionIdSerializer extends JsonSerializer<TransactionId> {

    @Override
    public void serialize(TransactionId id, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(String.valueOf(id.value()));
    }
}