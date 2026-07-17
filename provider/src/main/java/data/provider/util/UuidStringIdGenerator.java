package data.provider.util;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.neo4j.core.schema.IdGenerator;

public class UuidStringIdGenerator implements IdGenerator<String> {

    @Override
    @NullMarked
    public String generateId(final String primaryLabel, final Object entity) {
        return UUID.randomUUID().toString();
    }
}
