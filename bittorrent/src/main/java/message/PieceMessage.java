package message;

import java.nio.ByteBuffer;
import java.util.Arrays;
/**
 * Piece Message class
 */
public class PieceMessage extends Message {
    private static final int LENGTH_PREFIX_SIZE = 4;
    private static final int FIXED_PAYLOAD_SIZE = 9; //  1 (MessageID) + 4 + 4 (index, begin)
    private static final int MESSAGE_ID = 7;
    private int index;
    private int begin;
    private byte[] block;


    public PieceMessage(int index, int beginOffset, byte[] block) {
        super(LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE + block.length);
        this.index = index;
        this.begin = beginOffset;
        this.block = block;
        getBuf().put(createLengthPrefix(block));
        getBuf().put((byte) MESSAGE_ID);
        getBuf().put(createIndex(this.index));
        getBuf().put(createBeginOffset(this.begin));
        getBuf().put(this.block);
        setTypeMessage(TypeMessage.PIECE);
    }

    PieceMessage(){setTypeMessage(TypeMessage.PIECE);}

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        // Getting the block size contained in m
        byte[] lengthPrefix = Arrays.copyOfRange(m, 0, 4);
        int blockSize = ByteBuffer.wrap(lengthPrefix).getInt() - FIXED_PAYLOAD_SIZE;

        byte[] indexArray = Arrays.copyOfRange(m, 5, 9);
        byte[] beginOffset = Arrays.copyOfRange(m, 9, 13);
        byte[] blockArray = Arrays.copyOfRange(m, 13, 13 + blockSize);
        //ByteBuffer.wrap(byte[]).getInt() gives the integer written in byte[] (big-endian by default)
        this.setIndex(ByteBuffer.wrap(indexArray).getInt());
        this.setBegin(ByteBuffer.wrap(beginOffset).getInt());
        this.setBlock(ByteBuffer.wrap(blockArray).array());
        return this;
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        int size = m.getInt();
        m.get();
        this.setIndex(m.getInt());
        this.setBegin(m.getInt());
        byte[] blockData = new byte[size - 9];
        m.get(blockData, 0, size - 9);
        this.setBlock(blockData);
        return this;
    }

    @Override
    public int getSize() {
        return LENGTH_PREFIX_SIZE + FIXED_PAYLOAD_SIZE + block.length;
    }

    private byte[] createLengthPrefix(byte[] block) {
        return ByteBuffer.allocate(4).putInt(9 + block.length).array();
    }

    private byte[] createIndex(int index) {
        return ByteBuffer.allocate(4).putInt(index).array();
    }

    private byte[] createBeginOffset(int offSetBegin){
        return ByteBuffer.allocate(4).putInt(offSetBegin).array();
    }

    private void setIndex(int index) {
        this.index = index;
    }

    private void setBegin(int begin) {
        this.begin = begin;
    }

    private void setBlock(byte[] block) {
        this.block = block;
    }

    public int getIndex() {
        return index;
    }

    public int getBegin() {
        return begin;
    }

    public byte[] getBlock() {
        return block;
    }
}
