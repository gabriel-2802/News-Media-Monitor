#!/bin/bash
# Minimal ordered migration runner using cypher-shell.
# Tracks applied migrations as (:__Migration) nodes inside Neo4j itself.
set -euo pipefail

URI="${NEO4J_URI:-bolt://neo4j:7687}"
USER="${NEO4J_USERNAME:-neo4j}"
PASS="${NEO4J_PASSWORD:-secret}"
MIGRATIONS_DIR="${MIGRATIONS_DIR:-/migrations}"

SHELL_BIN="/var/lib/neo4j/bin/cypher-shell"

cypher() {
  "$SHELL_BIN" -a "$URI" -u "$USER" -p "$PASS" --non-interactive "$@"
}

echo "[migrate] Waiting for Neo4j to accept connections..."
until cypher "RETURN 1" > /dev/null 2>&1; do
  sleep 2
done

echo "[migrate] Ensuring migration tracking constraint exists..."
cypher "CREATE CONSTRAINT migration_version_unique IF NOT EXISTS
        FOR (m:\`__Migration\`) REQUIRE m.version IS UNIQUE;"

# Process files in lexicographic order (V1__, V2__, … sorts correctly)
for file in $(ls "$MIGRATIONS_DIR"/V*.cypher 2>/dev/null | sort); do
  filename=$(basename "$file")
  # Extract version token before the first double-underscore (e.g. "V1")
  version="${filename%%__*}"

  applied=$(cypher "MATCH (m:\`__Migration\` {version: '$version'}) RETURN count(m);" \
            | tail -1 | tr -d '[:space:]')

  if [ "$applied" != "0" ]; then
    echo "[migrate] $filename — already applied, skipping."
    continue
  fi

  echo "[migrate] Applying $filename ..."
  cypher --file "$file"
  cypher "CREATE (:__Migration {
            version:    '$version',
            filename:   '$filename',
            applied_at: datetime()
          });"
  echo "[migrate] $filename — done."
done

echo "[migrate] All migrations are up to date."
