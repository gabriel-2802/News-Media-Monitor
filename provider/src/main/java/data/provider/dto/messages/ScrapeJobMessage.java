package data.provider.dto.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScrapeJobMessage(
        String name,

        @JsonProperty("retry_count")
        int retryCount) {

    public ScrapeJobMessage(final String name) {
        this(name, 0);
    }
}
