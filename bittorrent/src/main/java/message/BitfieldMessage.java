package message;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Bitfield Message class
 */
public class BitfieldMessage extends Message {
    private static final int LENGTH_PREFIX_SIZE = 4;
    private static final int FIXED_PAYLOAD_SIZE = 1; //  messageID (1)
    private static final int MESSAGE_ID = 5;
    private byte[] bitfield;

    public BitfieldMessage(byte[] bitfield) {
        super(LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE + bitfield.length);
        this.bitfield = bitfield;
        getBuf().putInt(bitfield.length + 1);
        getBuf().put((byte) MESSAGE_ID);
        getBuf().put(this.bitfield);
        setTypeMessage(TypeMessage.BITFIELD);
    }

    BitfieldMessage(){ setTypeMessage(TypeMessage.BITFIELD);}

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        byte[] lengthPrefix = Arrays.copyOfRange(m, 0, 4);
        // On récupère la taille du bitfield
        int bitfieldLength = ByteBuffer.wrap(lengthPrefix).getInt();
        byte[] bitfieldMessage = Arrays.copyOfRange(m, 5, 5 + bitfieldLength);
        this.setBitfield(bitfieldMessage);
        return this;
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        int bitfieldLength = m.getInt();        // size
        m.get();                                // ID
        byte[] payload = new byte[bitfieldLength - 1];
        m.get(payload, 0, bitfieldLength - 1);
        setBitfield(payload);
        return this;
    }

    @Override
    public int getSize() {
        return LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE + bitfield.length;
    }

    private void setBitfield(byte[] bitfield) {
        this.bitfield = bitfield;
    }

    public byte[] getBitfield() {
        return bitfield;
    }
}
