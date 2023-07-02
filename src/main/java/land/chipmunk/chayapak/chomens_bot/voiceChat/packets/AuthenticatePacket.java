package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;

import java.util.UUID;

public class AuthenticatePacket implements Packet<AuthenticatePacket> {
    public UUID playerUUID;
    public UUID secret;

    public AuthenticatePacket () {}

    public AuthenticatePacket (
            UUID playerUUID,
            UUID secret
    ) {
        this.playerUUID = playerUUID;
        this.secret = secret;
    }

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
