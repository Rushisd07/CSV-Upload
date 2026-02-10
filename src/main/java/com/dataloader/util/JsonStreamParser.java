package com.dataloader.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Streaming JSON parser using Jackson Streaming API.
 * Reads JSON arrays element by element - no full load into memory.
 * Supports: { "data": [...] } or top-level array [...].
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonStreamParser {

    private final ObjectMapper objectMapper;

    /**
     * Stream a JSON array of type T.
     * Supports both:
     *   [ {...}, {...} ]
     *   { "data": [ {...}, {...} ] }
     *
     * @param inputStream source stream (not closed here)
     * @param clazz       target type per element
     * @param batchSize   number of records per batch
     * @param batchConsumer callback per batch
     * @param totalRowCounter callback with total count at end
     */
    public <T> void streamArray(InputStream inputStream,
                                Class<T> clazz,
                                int batchSize,
                                Consumer<List<T>> batchConsumer,
                                Consumer<Long> totalRowCounter) throws IOException {

        JsonFactory factory = objectMapper.getFactory();
        long totalRows = 0;

        try (JsonParser jsonParser = factory.createParser(inputStream)) {
            JsonToken token = jsonParser.nextToken();

            // If root is an object, descend into the first array field
            if (token == JsonToken.START_OBJECT) {
                while (jsonParser.nextToken() != null) {
                    if (jsonParser.currentToken() == JsonToken.START_ARRAY) {
                        break; // found the array
                    }
                }
            } else if (token != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("JSON must start with an object or array");
            }

            List<T> batch = new ArrayList<>(batchSize);

            while (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode node = objectMapper.readTree(jsonParser);
                T item = objectMapper.treeToValue(node, clazz);
                batch.add(item);
                totalRows++;

                if (batch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(batch));
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                batchConsumer.accept(batch);
            }
        }

        totalRowCounter.accept(totalRows);
    }
}
