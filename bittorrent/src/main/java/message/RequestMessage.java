package message;

import java.nio.ByteBuffer;
import java.util.Arrays;
/**
 * Request Message class
 */
public class RequestMessage extends Message {
    private static final int SIZE = 17; // 4 (LengthPrefix) + 1 (MessageID) + 4 + 4 + 4 (index, begin, length)
    private static final int MESSAGE_ID = 6;
    private int index;
    private int begin;
    private int length;

    public RequestMessage(int index, int beginOffset, int blockLength) {
        super(SIZE);
        this.index = index;
        this.begin = beginOffset;
        this.length = blockLength;
        getBuf().putInt(13);
        getBuf().put((byte) MESSAGE_ID);
        getBuf().putInt(index);
        getBuf().putInt(beginOffset);
        getBuf().putInt(blockLength);
        setTypeMessage(TypeMessage.REQUEST);
    }

    RequestMessage(){ setTypeMessage(TypeMessage.REQUEST);}

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        byte[] id = Arrays.copyOfRange(m, 5, 9);
        byte[] beginOffset = Arrays.copyOfRange(m, 9, 13);
        byte[] pieceLength = Arrays.copyOfRange(m, 13, 17);
        this.setIndex(ByteBuffer.wrap(id).getInt());
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
        return SIZE;
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

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public int getLength() {
        return length;
    }
}
