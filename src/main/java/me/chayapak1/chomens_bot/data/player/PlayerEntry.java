package me.chayapak1.chomens_bot.data.player;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

public class PlayerEntry {
    public final GameProfile profile;
    public GameMode gamemode;
    public int latency;
    public Component displayName;
    public final long expiresAt;
    public PublicKey publicKey;
    public final byte[] keySignature;
    public PersistingData persistingData;

    public PlayerEntry (
            final GameProfile profile,
            final GameMode gamemode,
            final int latency,
            final Component displayName,
            final long expiresAt,
            final PublicKey publicKey,
            final byte[] keySignature,
            final boolean listed
    ) {
        this.profile = profile;
        this.gamemode = gamemode;
        this.latency = latency;
        this.displayName = displayName;
        this.expiresAt = expiresAt;
        this.publicKey = publicKey;
        this.keySignature = keySignature;
        this.persistingData = new PersistingData(listed);
    }

    public PlayerEntry (final PlayerListEntry entry) {
        this(entry.getProfile(), entry.getGameMode(), entry.getLatency(), entry.getDisplayName(), entry.getExpiresAt(), entry.getPublicKey(), entry.getKeySignature(), entry.isListed());
    }

    @Override
    public String toString () {
        return "PlayerEntry{" +
                "profile=" + profile +
                ", gamemode=" + gamemode +
                ", latency=" + latency +
                ", persistingData=" + persistingData +
                '}';
    }

    public static final class PersistingData {
        public final List<String> usernames = new ObjectArrayList<>();
        public boolean listed;
        public String ip = null;
        public TrustLevel authenticatedTrustLevel = TrustLevel.PUBLIC;

        public PersistingData (final boolean listed) {
            this.listed = listed;
        }

        public PersistingData (final PersistingData friend) {
            this.usernames.addAll(friend.usernames);
            this.listed = friend.listed;
            this.ip = friend.ip;
            this.authenticatedTrustLevel = friend.authenticatedTrustLevel;
        }

        public PersistingData (final PlayerEntry oldEntry) {
            final PersistingData friend = oldEntry.persistingData;

            this.usernames.addAll(oldEntry.persistingData.usernames);
            this.usernames.addLast(oldEntry.profile.getName());
            this.listed = friend.listed;
            this.ip = friend.ip;
            this.authenticatedTrustLevel = friend.authenticatedTrustLevel;
        }

        @Override
        public @NotNull String toString () {
            return "PersistingData{" +
                    "usernames=" + usernames +
                    ", listed=" + listed +
                    ", ip='" + ip + '\'' +
                    ", authenticatedTrustLevel=" + authenticatedTrustLevel +
                    '}';
        }
    }
}
