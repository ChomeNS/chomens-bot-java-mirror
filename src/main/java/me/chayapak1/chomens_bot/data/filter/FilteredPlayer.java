package me.chayapak1.chomens_bot.data.filter;

import me.chayapak1.chomens_bot.data.player.PlayerEntry;

public record FilteredPlayer(PlayerEntry player, String reason) {
}
