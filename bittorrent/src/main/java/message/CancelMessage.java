package message;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Cancel Message class
 */
public class CancelMessage extends Message {
    private static final int LENGTH_PREFIX_SIZE = 4;
    private static final int FIXED_PAYLOAD_SIZE = 13; // 1 (MessageID) + 4 + 4 + 4 (index, begin, length)
    private static final int MESSAGE_ID = 8;
    private int index;
    private int begin;
    private int length;


    public CancelMessage(int index, int beginOffset, int pieceLength) {
        super(LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE);
        this.index = index;
        this.begin = beginOffset;
        this.length = pieceLength;
        getBuf().put(createLengthPrefix());
        getBuf().put((byte) MESSAGE_ID);
        getBuf().put(createIndex(this.index));
        getBuf().put(createBeginOffset(this.begin));
        getBuf().put(createPieceLength(this.length));
        setTypeMessage(TypeMessage.CANCEL);
    }

    CancelMessage(){setTypeMessage(TypeMessage.CANCEL);}

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        byte[] index = Arrays.copyOfRange(m, 5, 9);
        byte[] beginOffset = Arrays.copyOfRange(m, 9, 13);
        byte[] pieceLength = Arrays.copyOfRange(m, 13, 17);
        //ByteBuffer.wrap(byte[]).getInt() gives the integer written in byte[] (big-endian by default)
        this.setIndex(ByteBuffer.wrap(index).getInt());
        this.setBegin(ByteBuffer.wrap(beginOffset).getInt());
        this.setLength(ByteBuffer.wrap(pieceLength).getInt());
        return this;
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        m.getInt();
        m.get();
        this.setIndex(m.getInt());
        this.setBegin(m.getInt());
        this.setLength(m.getInt());
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
        lengthPrefix[3] = 13; // lengthPrefix = 00013
        return lengthPrefix;
    }

    private byte[] createIndex(double index){
        return ByteBuffer.allocate(4).putDouble(index).array();
    }

    private byte[] createBeginOffset(int offSetBegin){
        return ByteBuffer.allocate(4).putInt(offSetBegin).array();
    }

    private byte[] createPieceLength(int pieceLength){
        return ByteBuffer.allocate(4).putInt(pieceLength).array();
    }

    private void setIndex(int index) {
        this.index = index;
    }

    private void setBegin(int begin) {
        this.begin = begin;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
