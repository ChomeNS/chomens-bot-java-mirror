package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;
import lombok.Getter;

import java.util.UUID;

public class AuthenticatePacket implements Packet<AuthenticatePacket> {
    @Getter private UUID playerUUID;
    @Getter private UUID secret;

    @Override
    public AuthenticatePacket fromBytes (FriendlyByteBuf buf) {
        AuthenticatePacket packet = new AuthenticatePacket();
        packet.playerUUID = buf.readUUID();
        packet.secret = buf.readUUID();
        return packet;
    }

    @Override
    public void toBytes (FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeUUID(secret);
    }
}
