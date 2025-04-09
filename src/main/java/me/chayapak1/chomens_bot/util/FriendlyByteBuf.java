package me.chayapak1.chomens_bot.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class FriendlyByteBuf extends ByteBuf {

    private final ByteBuf buf;

    public FriendlyByteBuf (final ByteBuf byteBuf) {
        buf = byteBuf;
    }

    public FriendlyByteBuf () {
        this(Unpooled.buffer());
    }

    public void writeByteArray (final byte[] bs) {
        writeVarInt(bs.length);
        writeBytes(bs);
    }

    public byte[] readByteArray () {
        return readByteArray(readableBytes());
    }

    public byte[] readByteArray (final int i) {
        final int j = readVarInt();
        if (j > i) {
            throw new DecoderException("ByteArray with size " + j + " is bigger than allowed " + i);
        } else {
            final byte[] bs = new byte[j];
            readBytes(bs);
            return bs;
        }
    }

    public int readVarInt () {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return i;
    }

    public void writeUUID (final UUID uUID) {
        writeLong(uUID.getMostSignificantBits());
        writeLong(uUID.getLeastSignificantBits());
    }

    public UUID readUUID () {
        return new UUID(readLong(), readLong());
    }

    public void writeVarInt (int i) {
        while ((i & -128) != 0) {
            writeByte(i & 127 | 128);
            i >>>= 7;
        }

        writeByte(i);
    }

    public String readUtf (final int i) {
        final int j = readVarInt();
        if (j > i * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i * 4 + ')');
        } else if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            final String string = toString(readerIndex(), j, StandardCharsets.UTF_8);
            readerIndex(readerIndex() + j);
            if (string.length() > i) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + j + " > " + i + ')');
            } else {
                return string;
            }
        }
    }

    public void writeUtf (final String string, final int i) {
        final byte[] bs = string.getBytes(StandardCharsets.UTF_8);
        if (bs.length > i) {
            throw new EncoderException("String too big (was " + bs.length + " bytes encoded, max " + i + ')');
        } else {
            writeVarInt(bs.length);
            writeBytes(bs);
        }
    }

    public String readUtf () {
        return readUtf(32767);
    }

    public void writeUtf (final String string) {
        writeUtf(string, 32767);
    }

    @Override
    public int capacity () {
        return buf.capacity();
    }

    @Override
    public ByteBuf capacity (final int i) {
        return buf.capacity(i);
    }

    @Override
    public int maxCapacity () {
        return buf.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc () {
        return buf.alloc();
    }

    @Override
    @Deprecated
    public ByteOrder order () {
        return buf.order();
    }

    @Override
    @Deprecated
    public ByteBuf order (final ByteOrder byteOrder) {
        return buf.order(byteOrder);
    }

    @Override
    public ByteBuf unwrap () {
        return buf.unwrap();
    }

    @Override
    public boolean isDirect () {
        return buf.isDirect();
    }

    @Override
    public boolean isReadOnly () {
        return buf.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly () {
        return buf.asReadOnly();
    }

    @Override
    public int readerIndex () {
        return buf.readerIndex();
    }

    @Override
    public ByteBuf readerIndex (final int i) {
        return buf.readerIndex(i);
    }

    @Override
    public int writerIndex () {
        return buf.writerIndex();
    }

    @Override
    public ByteBuf writerIndex (final int i) {
        return buf.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex (final int i, final int j) {
        return buf.setIndex(i, j);
    }

    @Override
    public int readableBytes () {
        return buf.readableBytes();
    }

    @Override
    public int writableBytes () {
        return buf.writableBytes();
    }

    @Override
    public int maxWritableBytes () {
        return buf.maxWritableBytes();
    }

    @Override
    public boolean isReadable () {
        return buf.isReadable();
    }

    @Override
    public boolean isReadable (final int i) {
        return buf.isReadable(i);
    }

    @Override
    public boolean isWritable () {
        return buf.isWritable();
    }

    @Override
    public boolean isWritable (final int i) {
        return buf.isWritable(i);
    }

    @Override
    public ByteBuf clear () {
        return buf.clear();
    }

    @Override
    public ByteBuf markReaderIndex () {
        return buf.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex () {
        return buf.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex () {
        return buf.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex () {
        return buf.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes () {
        return buf.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes () {
        return buf.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable (final int i) {
        return buf.ensureWritable(i);
    }

    @Override
    public int ensureWritable (final int i, final boolean bl) {
        return buf.ensureWritable(i, bl);
    }

    @Override
    public boolean getBoolean (final int i) {
        return buf.getBoolean(i);
    }

    @Override
    public byte getByte (final int i) {
        return buf.getByte(i);
    }

    @Override
    public short getUnsignedByte (final int i) {
        return buf.getUnsignedByte(i);
    }

    @Override
    public short getShort (final int i) {
        return buf.getShort(i);
    }

    @Override
    public short getShortLE (final int i) {
        return buf.getShortLE(i);
    }

    @Override
    public int getUnsignedShort (final int i) {
        return buf.getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE (final int i) {
        return buf.getUnsignedShortLE(i);
    }

    @Override
    public int getMedium (final int i) {
        return buf.getMedium(i);
    }

    @Override
    public int getMediumLE (final int i) {
        return buf.getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium (final int i) {
        return buf.getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE (final int i) {
        return buf.getUnsignedMediumLE(i);
    }

    @Override
    public int getInt (final int i) {
        return buf.getInt(i);
    }

    @Override
    public int getIntLE (final int i) {
        return buf.getIntLE(i);
    }

    @Override
    public long getUnsignedInt (final int i) {
        return buf.getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE (final int i) {
        return buf.getUnsignedIntLE(i);
    }

    @Override
    public long getLong (final int i) {
        return buf.getLong(i);
    }

    @Override
    public long getLongLE (final int i) {
        return buf.getLongLE(i);
    }

    @Override
    public char getChar (final int i) {
        return buf.getChar(i);
    }

    @Override
    public float getFloat (final int i) {
        return buf.getFloat(i);
    }

    @Override
    public double getDouble (final int i) {
        return buf.getDouble(i);
    }

    @Override
    public ByteBuf getBytes (final int i, final ByteBuf byteBuf) {
        return buf.getBytes(i, byteBuf);
    }

    @Override
    public ByteBuf getBytes (final int i, final ByteBuf byteBuf, final int j) {
        return buf.getBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf getBytes (final int i, final ByteBuf byteBuf, final int j, final int k) {
        return buf.getBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf getBytes (final int i, final byte[] bs) {
        return buf.getBytes(i, bs);
    }

    @Override
    public ByteBuf getBytes (final int i, final byte[] bs, final int j, final int k) {
        return buf.getBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf getBytes (final int i, final ByteBuffer byteBuffer) {
        return buf.getBytes(i, byteBuffer);
    }

    @Override
    public ByteBuf getBytes (final int i, final OutputStream outputStream, final int j) throws IOException {
        return buf.getBytes(i, outputStream, j);
    }

    @Override
    public int getBytes (final int i, final GatheringByteChannel gatheringByteChannel, final int j) throws IOException {
        return buf.getBytes(i, gatheringByteChannel, j);
    }

    @Override
    public int getBytes (final int i, final FileChannel fileChannel, final long l, final int j) throws IOException {
        return buf.getBytes(i, fileChannel, l, j);
    }

    @Override
    public CharSequence getCharSequence (final int i, final int j, final Charset charset) {
        return buf.getCharSequence(i, j, charset);
    }

    @Override
    public ByteBuf setBoolean (final int i, final boolean bl) {
        return buf.setBoolean(i, bl);
    }

    @Override
    public ByteBuf setByte (final int i, final int j) {
        return buf.setByte(i, j);
    }

    @Override
    public ByteBuf setShort (final int i, final int j) {
        return buf.setShort(i, j);
    }

    @Override
    public ByteBuf setShortLE (final int i, final int j) {
        return buf.setShortLE(i, j);
    }

    @Override
    public ByteBuf setMedium (final int i, final int j) {
        return buf.setMedium(i, j);
    }

    @Override
    public ByteBuf setMediumLE (final int i, final int j) {
        return buf.setMediumLE(i, j);
    }

    @Override
    public ByteBuf setInt (final int i, final int j) {
        return buf.setInt(i, j);
    }

    @Override
    public ByteBuf setIntLE (final int i, final int j) {
        return buf.setIntLE(i, j);
    }

    @Override
    public ByteBuf setLong (final int i, final long l) {
        return buf.setLong(i, l);
    }

    @Override
    public ByteBuf setLongLE (final int i, final long l) {
        return buf.setLongLE(i, l);
    }

    @Override
    public ByteBuf setChar (final int i, final int j) {
        return buf.setChar(i, j);
    }

    @Override
    public ByteBuf setFloat (final int i, final float f) {
        return buf.setFloat(i, f);
    }

    @Override
    public ByteBuf setDouble (final int i, final double d) {
        return buf.setDouble(i, d);
    }

    @Override
    public ByteBuf setBytes (final int i, final ByteBuf byteBuf) {
        return buf.setBytes(i, byteBuf);
    }

    @Override
    public ByteBuf setBytes (final int i, final ByteBuf byteBuf, final int j) {
        return buf.setBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf setBytes (final int i, final ByteBuf byteBuf, final int j, final int k) {
        return buf.setBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf setBytes (final int i, final byte[] bs) {
        return buf.setBytes(i, bs);
    }

    @Override
    public ByteBuf setBytes (final int i, final byte[] bs, final int j, final int k) {
        return buf.setBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf setBytes (final int i, final ByteBuffer byteBuffer) {
        return buf.setBytes(i, byteBuffer);
    }

    @Override
    public int setBytes (final int i, final InputStream inputStream, final int j) throws IOException {
        return buf.setBytes(i, inputStream, j);
    }

    @Override
    public int setBytes (final int i, final ScatteringByteChannel scatteringByteChannel, final int j) throws IOException {
        return buf.setBytes(i, scatteringByteChannel, j);
    }

    @Override
    public int setBytes (final int i, final FileChannel fileChannel, final long l, final int j) throws IOException {
        return buf.setBytes(i, fileChannel, l, j);
    }

    @Override
    public ByteBuf setZero (final int i, final int j) {
        return buf.setZero(i, j);
    }

    @Override
    public int setCharSequence (final int i, final CharSequence charSequence, final Charset charset) {
        return buf.setCharSequence(i, charSequence, charset);
    }

    @Override
    public boolean readBoolean () {
        return buf.readBoolean();
    }

    @Override
    public byte readByte () {
        return buf.readByte();
    }

    @Override
    public short readUnsignedByte () {
        return buf.readUnsignedByte();
    }

    @Override
    public short readShort () {
        return buf.readShort();
    }

    @Override
    public short readShortLE () {
        return buf.readShortLE();
    }

    @Override
    public int readUnsignedShort () {
        return buf.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE () {
        return buf.readUnsignedShortLE();
    }

    @Override
    public int readMedium () {
        return buf.readMedium();
    }

    @Override
    public int readMediumLE () {
        return buf.readMediumLE();
    }

    @Override
    public int readUnsignedMedium () {
        return buf.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE () {
        return buf.readUnsignedMediumLE();
    }

    @Override
    public int readInt () {
        return buf.readInt();
    }

    @Override
    public int readIntLE () {
        return buf.readIntLE();
    }

    @Override
    public long readUnsignedInt () {
        return buf.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE () {
        return buf.readUnsignedIntLE();
    }

    @Override
    public long readLong () {
        return buf.readLong();
    }

    @Override
    public long readLongLE () {
        return buf.readLongLE();
    }

    @Override
    public char readChar () {
        return buf.readChar();
    }

    @Override
    public float readFloat () {
        return buf.readFloat();
    }

    @Override
    public double readDouble () {
        return buf.readDouble();
    }

    @Override
    public ByteBuf readBytes (final int i) {
        return buf.readBytes(i);
    }

    @Override
    public ByteBuf readSlice (final int i) {
        return buf.readSlice(i);
    }

    @Override
    public ByteBuf readRetainedSlice (final int i) {
        return buf.readRetainedSlice(i);
    }

    @Override
    public ByteBuf readBytes (final ByteBuf byteBuf) {
        return buf.readBytes(byteBuf);
    }

    @Override
    public ByteBuf readBytes (final ByteBuf byteBuf, final int i) {
        return buf.readBytes(byteBuf, i);
    }

    @Override
    public ByteBuf readBytes (final ByteBuf byteBuf, final int i, final int j) {
        return buf.readBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf readBytes (final byte[] bs) {
        return buf.readBytes(bs);
    }

    @Override
    public ByteBuf readBytes (final byte[] bs, final int i, final int j) {
        return buf.readBytes(bs, i, j);
    }

    @Override
    public ByteBuf readBytes (final ByteBuffer byteBuffer) {
        return buf.readBytes(byteBuffer);
    }

    @Override
    public ByteBuf readBytes (final OutputStream outputStream, final int i) throws IOException {
        return buf.readBytes(outputStream, i);
    }

    @Override
    public int readBytes (final GatheringByteChannel gatheringByteChannel, final int i) throws IOException {
        return buf.readBytes(gatheringByteChannel, i);
    }

    @Override
    public CharSequence readCharSequence (final int i, final Charset charset) {
        return buf.readCharSequence(i, charset);
    }

    @Override
    public int readBytes (final FileChannel fileChannel, final long l, final int i) throws IOException {
        return buf.readBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf skipBytes (final int i) {
        return buf.skipBytes(i);
    }

    @Override
    public ByteBuf writeBoolean (final boolean bl) {
        return buf.writeBoolean(bl);
    }

    @Override
    public ByteBuf writeByte (final int i) {
        return buf.writeByte(i);
    }

    @Override
    public ByteBuf writeShort (final int i) {
        return buf.writeShort(i);
    }

    @Override
    public ByteBuf writeShortLE (final int i) {
        return buf.writeShortLE(i);
    }

    @Override
    public ByteBuf writeMedium (final int i) {
        return buf.writeMedium(i);
    }

    @Override
    public ByteBuf writeMediumLE (final int i) {
        return buf.writeMediumLE(i);
    }

    @Override
    public ByteBuf writeInt (final int i) {
        return buf.writeInt(i);
    }

    @Override
    public ByteBuf writeIntLE (final int i) {
        return buf.writeIntLE(i);
    }

    @Override
    public ByteBuf writeLong (final long l) {
        return buf.writeLong(l);
    }

    @Override
    public ByteBuf writeLongLE (final long l) {
        return buf.writeLongLE(l);
    }

    @Override
    public ByteBuf writeChar (final int i) {
        return buf.writeChar(i);
    }

    @Override
    public ByteBuf writeFloat (final float f) {
        return buf.writeFloat(f);
    }

    @Override
    public ByteBuf writeDouble (final double d) {
        return buf.writeDouble(d);
    }

    @Override
    public ByteBuf writeBytes (final ByteBuf byteBuf) {
        return buf.writeBytes(byteBuf);
    }

    @Override
    public ByteBuf writeBytes (final ByteBuf byteBuf, final int i) {
        return buf.writeBytes(byteBuf, i);
    }

    @Override
    public ByteBuf writeBytes (final ByteBuf byteBuf, final int i, final int j) {
        return buf.writeBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf writeBytes (final byte[] bs) {
        return buf.writeBytes(bs);
    }

    @Override
    public ByteBuf writeBytes (final byte[] bs, final int i, final int j) {
        return buf.writeBytes(bs, i, j);
    }

    @Override
    public ByteBuf writeBytes (final ByteBuffer byteBuffer) {
        return buf.writeBytes(byteBuffer);
    }

    @Override
    public int writeBytes (final InputStream inputStream, final int i) throws IOException {
        return buf.writeBytes(inputStream, i);
    }

    @Override
    public int writeBytes (final ScatteringByteChannel scatteringByteChannel, final int i) throws IOException {
        return buf.writeBytes(scatteringByteChannel, i);
    }

    @Override
    public int writeBytes (final FileChannel fileChannel, final long l, final int i) throws IOException {
        return buf.writeBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf writeZero (final int i) {
        return buf.writeZero(i);
    }

    @Override
    public int writeCharSequence (final CharSequence charSequence, final Charset charset) {
        return buf.writeCharSequence(charSequence, charset);
    }

    @Override
    public int indexOf (final int i, final int j, final byte b) {
        return buf.indexOf(i, j, b);
    }

    @Override
    public int bytesBefore (final byte b) {
        return buf.bytesBefore(b);
    }

    @Override
    public int bytesBefore (final int i, final byte b) {
        return buf.bytesBefore(i, b);
    }

    @Override
    public int bytesBefore (final int i, final int j, final byte b) {
        return buf.bytesBefore(i, j, b);
    }

    @Override
    public int forEachByte (final ByteProcessor byteProcessor) {
        return buf.forEachByte(byteProcessor);
    }

    @Override
    public int forEachByte (final int i, final int j, final ByteProcessor byteProcessor) {
        return buf.forEachByte(i, j, byteProcessor);
    }

    @Override
    public int forEachByteDesc (final ByteProcessor byteProcessor) {
        return buf.forEachByteDesc(byteProcessor);
    }

    @Override
    public int forEachByteDesc (final int i, final int j, final ByteProcessor byteProcessor) {
        return buf.forEachByteDesc(i, j, byteProcessor);
    }

    @Override
    public ByteBuf copy () {
        return buf.copy();
    }

    @Override
    public ByteBuf copy (final int i, final int j) {
        return buf.copy(i, j);
    }

    @Override
    public ByteBuf slice () {
        return buf.slice();
    }

    @Override
    public ByteBuf retainedSlice () {
        return buf.retainedSlice();
    }

    @Override
    public ByteBuf slice (final int i, final int j) {
        return buf.slice(i, j);
    }

    @Override
    public ByteBuf retainedSlice (final int i, final int j) {
        return buf.retainedSlice(i, j);
    }

    @Override
    public ByteBuf duplicate () {
        return buf.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate () {
        return buf.retainedDuplicate();
    }

    @Override
    public int nioBufferCount () {
        return buf.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer () {
        return buf.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer (final int i, final int j) {
        return buf.nioBuffer(i, j);
    }

    @Override
    public ByteBuffer internalNioBuffer (final int i, final int j) {
        return buf.internalNioBuffer(i, j);
    }

    @Override
    public ByteBuffer[] nioBuffers () {
        return buf.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers (final int i, final int j) {
        return buf.nioBuffers(i, j);
    }

    @Override
    public boolean hasArray () {
        return buf.hasArray();
    }

    @Override
    public byte[] array () {
        return buf.array();
    }

    @Override
    public int arrayOffset () {
        return buf.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress () {
        return buf.hasMemoryAddress();
    }

    @Override
    public long memoryAddress () {
        return buf.memoryAddress();
    }

    @Override
    public String toString (final Charset charset) {
        return buf.toString(charset);
    }

    @Override
    public String toString (final int i, final int j, final Charset charset) {
        return buf.toString(i, j, charset);
    }

    @Override
    public int hashCode () {
        return buf.hashCode();
    }

    @Override
    public boolean equals (final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final FriendlyByteBuf that = (FriendlyByteBuf) o;
        return Objects.equals(buf, that.buf);
    }

    @Override
    public int compareTo (final ByteBuf byteBuf) {
        return buf.compareTo(byteBuf);
    }

    @Override
    public String toString () {
        return buf.toString();
    }

    @Override
    public ByteBuf retain (final int i) {
        return buf.retain(i);
    }

    @Override
    public ByteBuf retain () {
        return buf.retain();
    }

    @Override
    public ByteBuf touch () {
        return buf.touch();
    }

    @Override
    public ByteBuf touch (final Object object) {
        return buf.touch(object);
    }

    @Override
    public int refCnt () {
        return buf.refCnt();
    }

    @Override
    public boolean release () {
        return buf.release();
    }

    @Override
    public boolean release (final int i) {
        return buf.release(i);
    }
}
