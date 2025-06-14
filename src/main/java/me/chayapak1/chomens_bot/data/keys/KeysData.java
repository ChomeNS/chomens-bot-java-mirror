package me.chayapak1.chomens_bot.data.keys;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record KeysData(
        @JsonProperty ArrayList<Key> keys,
        @JsonProperty String userId
) {
}
