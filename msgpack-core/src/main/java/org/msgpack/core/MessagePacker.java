//
// MessagePack for Java
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.msgpack.core;

import java.math.BigInteger;
import java.io.IOException;
import java.nio.ByteBuffer;
import static org.msgpack.core.MessagePack.Code.*;

/**
 * Writer of message packed data.
 *
 * <p>
 * MessagePacker provides packXXX methods for writing values in the message pack format.
 * To write raw string or binary data, first use packRawStringHeader or packBinaryHeader to specify the data length,
 * then call writePayload(...) method.
 * </p>
 *
 * <p>
 * MessagePacker class has no guarantee to produce the correct message-pack format data if it is not used correctly:
 * packXXX methods of primitive values always produce the correct format, but
 * packXXXHeader (e.g. array, map, ext) must be followed by correct number of array/map/ext type values.
 * packRawStringHeader(length) and packBinaryHeader(length) must be followed by writePayload( ... length) to supply
 * the binary data of the specified length in the header.
 * </p>
 *
 */
public class MessagePacker {

    private final MessageBufferOutput out;
    private final MessageBuffer buffer;
    private int position;

    public MessagePacker(MessageBufferOutput out) {
        this(out, 8 * 1024);
    }

    public MessagePacker(MessageBufferOutput out, int bufferSize) {
        assert(out != null);
        this.out = out;
        this.buffer = MessageBuffer.newDirectBuffer(bufferSize);
        this.position = 0;
    }

    public void flush() throws IOException {
        out.flush(buffer, 0, position);
        position = 0;
    }

    public void close() throws IOException {
        try {
            flush();
        }
        finally {
            out.close();
        }
    }

    private void ensureCapacity(int numBytesToWrite) throws IOException {
        if(position + numBytesToWrite < buffer.size())
            return;

        flush();
    }


    private void writeByte(byte b) throws IOException {
        ensureCapacity(1);
        buffer.putByte(position++, b);
    }


    private void writeByteAndByte(byte b, byte v) throws IOException {
        ensureCapacity(2);
        buffer.putByte(position++, b);
        buffer.putByte(position++, v);
    }

    private void writeByteAndShort(byte b, short v) throws IOException {
        ensureCapacity(3);
        buffer.putByte(position++, b);
        buffer.putShort(position, v);
        position += 2;
    }

    private void writeByteAndInt(byte b, int v) throws IOException {
        ensureCapacity(5);
        buffer.putByte(position++, b);
        buffer.putInt(position, v);
        position += 4;
    }

    private void writeByteAndFloat(byte b, float v) throws IOException {
        ensureCapacity(5);
        buffer.putByte(position++, b);
        buffer.putFloat(position, v);
        position += 4;
    }

    private void writeByteAndDouble(byte b, double v) throws IOException {
        ensureCapacity(9);
        buffer.putByte(position++, b);
        buffer.putDouble(position, v);
        position += 8;
    }

    private void writeByteAndLong(byte b, long v) throws IOException {
        ensureCapacity(9);
        buffer.putByte(position++, b);
        buffer.putLong(position, v);
        position += 8;
    }

    private void writeShort(short v) throws IOException {
        ensureCapacity(2);
        buffer.putShort(position, v);
        position += 2;
    }

    private void writeInt(int v) throws IOException {
        ensureCapacity(4);
        buffer.putInt(position, v);
        position += 4;
    }

    private void writeLong(long v) throws IOException {
        ensureCapacity(8);
        buffer.putLong(position, v);
        position += 8;
    }

    public MessagePacker packNil() throws IOException {
        writeByte(NIL);
        return this;
    }

    public MessagePacker packBoolean(boolean b) throws IOException {
        writeByte(b ? TRUE : FALSE);
        return this;
    }


    public MessagePacker packByte(byte b) throws IOException {
        if(b < -(1 << 5)) {
            writeByteAndByte(INT8, b);
        } else {
            writeByte(b);
        }
        return this;
    }

    public MessagePacker packShort(short v) throws IOException {
        if(v < -(1 << 5)) {
            if(v < -(1 << 7)) {
                writeByteAndShort(INT16, v);
            } else {
                writeByteAndByte(INT8, (byte) v);
            }
        } else if(v < (1 << 7)) {
            writeByte((byte) v);
        } else {
            if(v < (1 << 8)) {
                writeByteAndByte(UINT8, (byte) v);
            }
            else {
                writeByteAndShort(UINT16, v);
            }
        }
        return this;
    }

    public MessagePacker packInt(int r) throws IOException {
        if (r < -(1 << 5)) {
            if (r < -(1 << 15)) {
                writeByteAndInt(INT32, r);
            } else if (r < -(1 << 7)) {
                writeByteAndShort(INT16, (short) r);
            } else {
                writeByteAndByte(INT8, (byte) r);
            }
        } else if (r < (1 << 7)) {
            writeByte((byte) r);
        } else {
            if (r < (1 << 8)) {
                writeByteAndByte(UINT8, (byte) r);
            } else if (r < (1 << 16)) {
                writeByteAndShort(UINT16, (short) r);
            } else {
                // unsigned 32
                writeByteAndInt(UINT32, r);
            }
        }
        return this;
    }

    public MessagePacker packLong(long v) throws IOException {
        if (v < -(1L << 5)) {
            if (v < -(1L << 15)) {
                if (v < -(1L << 31)) {
                    writeByteAndLong(INT64, v);
                } else {
                    writeByteAndInt(INT32, (int) v);
                }
            } else {
                if (v < -(1 << 7)) {
                    writeByteAndShort(INT16, (short) v);
                } else {
                    writeByteAndByte(INT8, (byte) v);
                }
            }
        } else if (v < (1 << 7)) {
            // fixnum
            writeByte((byte) v);
        } else {
            if (v < (1L << 16)) {
                if (v < (1 << 8)) {
                    writeByteAndByte(UINT8, (byte) v);
                } else {
                    writeByteAndShort(UINT16, (short) v);
                }
            } else {
                if (v < (1L << 32)) {
                    writeByteAndInt(UINT32, (int) v);
                } else {
                    writeByteAndLong(UINT64, v);
                }
            }
        }
        return this;
    }

    public MessagePacker packBigInteger(BigInteger bi) throws IOException {
        if(bi.bitLength() <= 63) {
            writeLong(bi.longValue());
        } else if(bi.bitLength() == 64 && bi.signum() == 1) {
            writeByteAndLong(UINT64, bi.longValue());
        } else {
            throw new IllegalArgumentException("Messagepack cannot serialize BigInteger larger than 2^64-1");
        }
        return this;
    }
    
    public MessagePacker packFloat(float v) throws IOException {
        writeByteAndFloat(FLOAT32, v);
        return this;
    }
 
    public MessagePacker packDouble(double v) throws IOException {
        writeByteAndDouble(FLOAT64, v);
        return this;
    }

    /**
     * Pack the input String in UTF-8 encoding
     *
     * @param s
     * @return
     * @throws IOException
     */
    public MessagePacker packString(String s) throws IOException {
        byte[] utf8 = s.getBytes(MessagePack.UTF8);
        packRawStringHeader(utf8.length);
        writePayload(utf8, 0, utf8.length);
        return this;
    }

    public MessagePacker packArrayHeader(int arraySize) throws IOException {
        if(arraySize < 0)
            throw new IllegalArgumentException("array size must be >= 0");

        if(arraySize < (1 << 5)) {
            writeByte((byte) (FIXARRAY_PREFIX | arraySize));
        } else if(arraySize < (1 << 16)) {
            writeByteAndShort(ARRAY16, (short) arraySize);
        } else {
            writeByteAndInt(ARRAY32, arraySize);
        }
        return this;
    }

    public MessagePacker packMapHeader(int mapSize) throws IOException {
        if(mapSize < 0)
            throw new IllegalArgumentException("map size must be >= 0");

        if(mapSize < (1 << 5)) {
            writeByte((byte) (FIXMAP_PREFIX | mapSize));
        } else if(mapSize < (1 << 16)) {
            writeByteAndShort(MAP16, (short) mapSize);
        } else {
            writeByteAndInt(MAP32, mapSize);
        }
        return this;
    }

    public MessagePacker packExtendedTypeHeader(int extType, int dataLen) throws IOException {
        if(dataLen < (1 << 8)) {
            if(dataLen > 0 && (dataLen & (dataLen - 1)) == 0) { // check whether dataLen == 2^x
                if(dataLen == 1) {
                    writeByteAndByte(FIXEXT1, (byte) extType);
                } else if(dataLen == 2){
                    writeByteAndByte(FIXEXT2, (byte) extType);
                } else if(dataLen == 4) {
                    writeByteAndByte(FIXEXT4, (byte) extType);
                } else if(dataLen == 8) {
                    writeByteAndByte(FIXEXT8, (byte) extType);
                } else {
                    writeByteAndByte(FIXEXT16, (byte) extType);
                }
            } else {
                writeByteAndByte(EXT8, (byte) dataLen);
                writeByte((byte) extType);
            }
        } else if(dataLen < (1 << 16)) {
            writeByteAndShort(EXT16, (short) dataLen);
            writeByte((byte) extType);
        } else {
            writeByteAndInt(EXT32, dataLen);
            writeByte((byte) extType);

            // TODO support dataLen > 2^31 - 1
        }
        return this;
    }

    public MessagePacker packBinaryHeader(int len) throws IOException {
        if(len < (1 << 8)) {
            writeByteAndByte(BIN8, (byte) len);
        } else if(len < (1 << 16)) {
            writeByteAndShort(BIN16, (short) len);
        } else {
            writeByteAndInt(BIN32, len);
        }
        return this;
    }

    public MessagePacker packRawStringHeader(int len) throws IOException {
        if(len < (1 << 5)) {
            writeByte((byte) (FIXSTR_PREFIX | len));
        } else if(len < (1 << 8)) {
            writeByteAndByte(STR8, (byte) len);
        } else if(len < (1 << 16)) {
            writeByteAndShort(STR16, (short) len);
        } else {
            writeByteAndInt(STR32, len);
        }
        return this;
    }

    public MessagePacker writePayload(ByteBuffer bb) throws IOException {
        while(bb.remaining() > 0) {
            if(position >= buffer.size())
                flush();
            int writeLen = Math.min(buffer.size() - position, bb.remaining());
            buffer.putByteBuffer(position, bb, writeLen);
            position += writeLen;
            bb.position(bb.position() + writeLen);
        }
        return this;
    }

    public MessagePacker writePayload(byte[] o, int off, int len) throws IOException {
        int cursor = 0;
        while(cursor < len) {
            if(position >= buffer.size())
                flush();
            int writeLen = Math.min(buffer.size() - position, len - cursor);
            buffer.putBytes(position, o, off + cursor, writeLen);
            position += writeLen;
            cursor += writeLen;
        }
        return this;
    }


}