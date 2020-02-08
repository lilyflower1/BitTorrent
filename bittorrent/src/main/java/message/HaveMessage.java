package message;

import java.nio.ByteBuffer;
import java.util.Arrays;
/**
 * Have Message class
 */
public class HaveMessage extends Message {
    private static final int LENGTH_PREFIX_SIZE = 4;
    private static final int FIXED_PAYLOAD_SIZE = 5; // 1 (MessageID) + 4 (pieceIndex)
    private static final int MESSAGE_ID = 4;
    private int pieceIndex;



    public HaveMessage(int pieceIndex) {
        super(LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE);
        this.pieceIndex = pieceIndex;
        getBuf().put(createLengthPrefix());
        getBuf().put((byte) MESSAGE_ID);
        getBuf().put(createPieceIndex(this.pieceIndex));
        setTypeMessage(TypeMessage.HAVE);

    }

    HaveMessage(){
        super(LENGTH_PREFIX_SIZE);
        setTypeMessage(TypeMessage.HAVE);
    }

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        byte[] index = Arrays.copyOfRange(m, 5, 9);
        this.setPieceIndex(ByteBuffer.wrap(index).getInt());
        return this;
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        m.getInt();
        m.get();
        this.setPieceIndex(m.getInt());
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
        lengthPrefix[3] = 5; // lengthPrefix = 0005
        return lengthPrefix;
    }

    private byte[] createPieceIndex(double index){
        return ByteBuffer.allocate(4).putInt((int) index).array();
    }

    private void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }
}
