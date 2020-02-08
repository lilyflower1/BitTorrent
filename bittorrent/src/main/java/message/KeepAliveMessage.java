package message;

import java.nio.ByteBuffer;
/**
 * KeepAlive Message class
 */
public class KeepAliveMessage extends Message {
    private static final int LENGTH_PREFIX_SIZE = 4;
    private static final int FIXED_PAYLOAD_SIZE = 1;
    private static final int MESSAGE_ID = 0;


    private KeepAliveMessage() {
        super(LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE);
        getBuf().put(createLengthPrefix());
        getBuf().put((byte) MESSAGE_ID);

    }

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        return new KeepAliveMessage();
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        return null;
    }

    @Override
    public int getSize() {
        return LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE;
    }

    private byte[] createLengthPrefix() {
        byte[] lengthPrefix = new byte[4];
        for (int i = 0; i < 4; ++i) {
            lengthPrefix[i] = 0;
        }
        return lengthPrefix;
    }
}



