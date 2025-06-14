package me.chayapak1.chomens_bot.data.keys;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.chayapak1.chomens_bot.command.TrustLevel;
import org.jetbrains.annotations.NotNull;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record Key(
        @JsonProperty TrustLevel trustLevel,
        @JsonProperty String key,
        @JsonProperty long createdAt
) {
    @Override
    public @NotNull String toString () {
        return "Key{" +
                "trustLevel=" + trustLevel +
                ", key=************" +
                ", createdAt=" + createdAt +
                '}';
    }
}
