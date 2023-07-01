package land.chipmunk.chayapak.chomens_bot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class EvalOutput {
    @Getter private final boolean isError;
    @Getter private final String output;
}
