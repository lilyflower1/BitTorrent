package message;

import java.nio.ByteBuffer;
import java.util.Arrays;
/**
 * Handshake Message class
 */
public class HandshakeMessage extends Message {
    // handshake: <pstrlen><pstr><reserved><info_hash><peer_id> Bittorrent version 1.0
    private static final int SIZE = 68; // 49 + pstrlen
    private byte[] infoHash;


    public HandshakeMessage(byte[] infoHash, String peerId){
        super(SIZE);
        byte pstrlen = 19;
        getBuf().put(pstrlen);          // <pstrlen>
        String pstr = "BitTorrent protocol";
        getBuf().put(pstr.getBytes());  // <pstr>
        getBuf().put(createReserved());         // <reserved>
        getBuf().put(infoHash);
        getBuf().put(peerId.getBytes());
        setTypeMessage(TypeMessage.HANDSHAKE);
    }

    public HandshakeMessage(){
        super(SIZE);
        setTypeMessage(TypeMessage.HANDSHAKE);
    }

    @Override
    public ByteBuffer createByteArray() {
        getBuf().flip();        // prepare for writing message to network
        return getBuf();
    }

    @Override
    public Message getMessageFromByteArray(byte[] m) {
        byte[] infoHashArray = Arrays.copyOfRange(m, 28, 48);
        this.setInfoHash(infoHashArray);
        return this;
    }

    @Override
    public Message getMessageFromByteBuffer(ByteBuffer m) {
        m.position(28);
        int size = 20;
        byte[] infoHashArray = new byte[size];
        byte[] peerIdArray = new byte[size];
        m.get(infoHashArray, 0, size);
        m.get(peerIdArray, 0, size);
        setInfoHash(infoHashArray);
        return this;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    private byte[] createReserved() {
        byte[] reserved = new byte[8];
        for (int i = 0; i < 8; ++i) {
            reserved[i] = 0;
        }
        return reserved;
    }

    private void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }
}
