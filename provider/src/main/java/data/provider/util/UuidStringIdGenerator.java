package data.provider.util;

import java.util.UUID;
import org.springframework.data.neo4j.core.schema.IdGenerator;

/**
 * Generates a UUID and stores it as a real {@code id} node property.
 *
 * Unlike the default {@code InternalIdGenerator} (which uses Neo4j's
 * internal element ID and is never persisted as a property) or
 * {@code GeneratedValue.UUIDGenerator} (which produces a {@link UUID}
 * object that doesn't fit a {@code String} field), this is what custom
 * Cypher matching on {@code {id: $storyId}} actually needs.
 */
public class UuidStringIdGenerator implements IdGenerator<String> {

    @Override
    public String generateId(final String primaryLabel, final Object entity) {
        return UUID.randomUUID().toString();
    }
}
