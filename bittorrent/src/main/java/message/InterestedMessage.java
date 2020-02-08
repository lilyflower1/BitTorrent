package message;

import java.nio.ByteBuffer;
/**
 * Interested Message class
 */
public class InterestedMessage extends Message {
    private static final int LENGTH_PREFIX_SIZE = 4;
    private static final int FIXED_PAYLOAD_SIZE = 1;
    private static final int MESSAGE_ID = 2;


    public InterestedMessage() {
        super(LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE);
        getBuf().put(createLengthPrefix());
        getBuf().put((byte) MESSAGE_ID);
        setTypeMessage(TypeMessage.INTERESTED);
    }

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        return this;
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        return this;
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
        lengthPrefix[3] = 1; // lengthPrefix = 0001
        return lengthPrefix;
    }
}
