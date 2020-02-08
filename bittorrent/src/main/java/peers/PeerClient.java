package peers;

import message.*;
import pieces.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * PeerClient Class
 */
public class PeerClient extends Peer {
    private StateMachine state;
    private SocketChannel socket;
    private List<Integer> bitfield = new ArrayList<>();
    private List<RequestMessage> pieces = new ArrayList<>();
    private AtomicBoolean completed = new AtomicBoolean(false);
    private AtomicBoolean bitfieldSet = new AtomicBoolean(false);

    public PeerClient(String ip, int port, PeerManager peerManager, PieceManager m, Torrent t, Level l) {
        super(ip, port, peerManager, m, t, l);
        state = new StateMachine();
        initMessageReceivedHistory();
        pieceManager.getSelectionPieces().initBitfieldForPeer(this);
    }

    /**
     * Method that builds the hashMap for the message history
     */
    private void initMessageReceivedHistory(){
        this.messageReceivedFromPeerHistory = new HashMap<>();
        for(TypeMessage typMessage : TypeMessage.values()) {
            this.messageReceivedFromPeerHistory.put(typMessage, false);
        }
    }

    @Override
    protected boolean connect(){
        try {
            socket = SocketChannel.open();
            // Set socketChannel to nonblocking
            socket.configureBlocking(false);
            if (!socket.connect(new InetSocketAddress(ip, port))) {
                while (!socket.finishConnect()) {
                    globalLogger.info("... Connexion ...");
                }
                // Test de la connexion
                globalLogger.info("Socket connected ? " + socket.isConnected());
                return true;
            }
            return false;
        } catch (IOException e){
            globalLogger.info("Connection refused");
            return false;
        }
    }

    @Override
    public void run() {
        try {
            if (connect()){
                // send Handshake
                send (new HandshakeMessage(file.getInfoHash(), id), this.socket);
                // receive handshake
                if (receiveHandshake()){
                    globalLogger.info("handshake reçu de " + port);
                    // receive Bitfield ou Have
                    receive();
                    globalLogger.info("Pieces disponibles : " + bitfield + " de " + port);
                    if (!bitfield.isEmpty()){
                        // set bitfield of this peer to the selection class with the combined information, we will know which pieces to ask from this peer
                        setBitfield();
                        // send interested
                        state.sendInterested(this);
                        // receive unchoke
                        state.receiveUnChoke(this);
                        // request pieces from this peer
                        state.getPiece(this);
                        // send not interested
                        state.sendNotInterested(this);
                    }
                }
            }
            quit();
        } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
            globalLogger.severe(e.getMessage());
            try {
                quit();
            } catch (IOException ex) {
                globalLogger.severe(ex.getMessage());
            }
        }
    }

    /**
     * get bits from byte array
     *
     * @param data byte array
     * @return string
     */
    private static String getBit(byte[] data) {
        StringBuilder bitsString = new StringBuilder();
        for (byte b : data) {
            for (int mask = 0x01; mask != 0x100; mask <<= 1) {
                boolean value = (b & mask) != 0;
                if (value) {
                    bitsString.append(1);
                } else {
                    bitsString.append(0);
                }

            }
        }
        return bitsString.toString();
    }

    /**
     * request pieces method which iterate over pieces to ask
     */
    public void requestPieces() throws IOException, InterruptedException, NoSuchAlgorithmException {
        while (!completed.get()) {
            while (!pieceManager.getSelectionPieces().getDone().get()) {
            }
            Iterator<Map.Entry<Piece, List<Block>>> it = pieceManager.getSelectionPieces().selectPiecesToAskFromPeer(this).entrySet().iterator();
            int size = pieceManager.getSelectionPieces().selectPiecesToAskFromPeer(this).entrySet().size();
            requestMaximumPieces(it, size);
            if (size <= pieceManager.getMaxPiecesToAskEachTime() && size != 0) {
                requestLastPieces(it);
            }
        }
    }

    /**
     * request last pieces method which request all the last pieces and after wait for receive
     *
     * @param it iterator over pieces to ask
     * @throws IOException              exception
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     */
    private void requestLastPieces(Iterator<Map.Entry<Piece, List<Block>>> it) throws IOException, InterruptedException, NoSuchAlgorithmException {
        while (it.hasNext()) {
            if (!pieceManager.getSelectionPieces().getMaj(this)) {
                Map.Entry<Piece, List<Block>> pair = it.next();
                requestPiece(pair.getKey().getIndex(), pair.getValue());
                it.remove();
            } else {
                pieceManager.getSelectionPieces().setMaj(this, false);
                pieces.clear();
                break;
            }
        }
        if (!it.hasNext()) {
            receive();
        }
    }

    /**
     * request maximum pieces method which request max pieces in one shot and wait for receive after
     *
     * @param it   iterator over pieces to ask
     * @param size asking pieces map size
     * @throws IOException              exception
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     */
    private void requestMaximumPieces(Iterator<Map.Entry<Piece, List<Block>>> it, int size) throws IOException, InterruptedException, NoSuchAlgorithmException {
        int i = 0;
        while (it.hasNext() && size > pieceManager.getMaxPiecesToAskEachTime()) {
            Map.Entry<Piece, List<Block>> pair = it.next();
            if (!pieceManager.getSelectionPieces().getMaj(this)) {
                if (i < pieceManager.getMaxPiecesToAskEachTime()) {
                    requestPiece(pair.getKey().getIndex(), pair.getValue());
                    it.remove();
                    i++;
                } else {
                    receive();
                    i = 0;
                }
            } else {
                pieceManager.getSelectionPieces().setMaj(this, false);
                pieces.clear();
                break;
            }
        }
    }

    /**
     * set bitfield for this peer
     */
    private void setBitfield() {
        for (Integer i : bitfield) {
            pieceManager.getSelectionPieces().setBitfield(pieceManager.getPieces().get(i), this);
        }
        bitfieldSet.getAndSet(true);
    }

    /**
     * send request messages for a specific piece
     *
     * @param p     piece index
     * @param value blocks within the piece
     * @throws IOException exception
     */
    private void requestPiece(Integer p, List<Block> value) throws IOException {
        for (Block val : value) {
            val.setBlockState(BlockState.PENDING);
            send(new RequestMessage(p, val.getId() * Piece.BLOCK_SIZE, (int) val.getBlockSize()), this.socket);
            pieces.add(new RequestMessage(p, val.getId() * Piece.BLOCK_SIZE, (int) val.getBlockSize()));
        }
    }

    /**
     * quitting method
     *
     * @throws IOException exception
     */
    private void quit() throws IOException {
        setCompleted(true);
        pieceManager.getSelectionPieces().removePeer(this);
        peerManager.removePeer(this);
        socket.close();
    }

    /**
     * receive method which listens on the socket
     *
     * @throws IOException              exception
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     */
    void receive() throws IOException, InterruptedException, NoSuchAlgorithmException {
        boolean complete = false;
        long time = System.currentTimeMillis();
        long newTime = System.currentTimeMillis();
        ByteBuffer buf = ByteBuffer.allocateDirect(PieceManager.MAX_SIZE);
        while (!complete) {
            long dif = newTime - time;
            if (dif > 10000) {
                break;
            }
            int bitsReceived = socket.read(buf);
            if (bitsReceived == -1) {
                return;
            }
            if (bitsReceived > 0) {
                for (Message message : Message.parserMessages(buf)) {
                    if (message instanceof BitfieldMessage) {
                        complete = getBitfield(message);
                        break;
                    } else if (message instanceof ChokeMessage)
                        complete = getChoke();
                    else if (message instanceof UnchokeMessage)
                        complete = true;
                    else if (message instanceof HaveMessage)
                        getHave(message);
                    else if (message instanceof PieceMessage)
                        complete = getPiece(message);
                }
            } else {
                newTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * receiving choke message and updating our current state
     *
     * @return true
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     * @throws IOException              exception
     */
    private boolean getChoke() throws InterruptedException, NoSuchAlgorithmException, IOException {
        state.receiveChoke();
        state.receiveUnChoke(this);
        pieces.clear();
        return true;
    }

    /**
     * receiving bloc message and adding this bloc to PieceManager
     *
     * @param m pieceMessage
     * @return true if we have finished to receive pieces
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     * @throws IOException              exception
     */
    private boolean getPiece(Message m) throws InterruptedException, NoSuchAlgorithmException, IOException {
        removeBlockFromAsked((PieceMessage) m);
        pieceManager.addBlock((PieceMessage) m);
        return checkCompleted();
    }

    /**
     * receiving have message and updating bitfield
     *
     * @param m HaveMessage
     */
    private void getHave(Message m) {
        bitfield.add(((HaveMessage) m).getPieceIndex());
        setBitfield();
    }

    /**
     * receiving bitfield message and updating peer bitfield
     *
     * @param m BitfieldMessage
     * @return true
     */
    private boolean getBitfield(Message m) {
        long sizeBitfield = (long) Math.ceil(pieceManager.getNbPieces() / 8f);
        byte[] field = Arrays.copyOfRange(((BitfieldMessage) m).getBitfield(), 0, (int) sizeBitfield);
        String b;
        if (field.length != sizeBitfield) {
            b = getBit(field);
        } else {
            b = getBit(field).substring(0, pieceManager.getNbPieces());
        }
        String bitfieldString = reverseWord(b);
        int i = 0;
        for (char c : bitfieldString.toCharArray()) {
            if (c == '1') {
                bitfield.add(i);
            }
            i++;
        }
        return true;
    }

    /**
     * remove block from asking pieces when we receive one
     *
     * @param m PieceMessage
     */
    private void removeBlockFromAsked(PieceMessage m) {
        int index = m.getIndex();
        int begin = m.getBegin();
        int blockLength = m.getBlock().length;
        pieces.removeIf(req -> req.getIndex() == index && req.getBegin() == begin && req.getLength() == blockLength);
    }

    /**
     * check if we have finished to receive pieces
     *
     * @return true if asking pieces is empty
     */
    private boolean checkCompleted() {
        return pieces.isEmpty();
    }

    /**
     * handle receiving an handshake message
     *
     * @return true if handshake message is correct
     * @throws IOException exception
     */
    private boolean receiveHandshake() throws IOException {
        try {
            ByteBuffer buf = ByteBuffer.allocate(68);
            long time = System.currentTimeMillis();
            long newTime = System.currentTimeMillis();
            while (true) {
                long dif = newTime - time;
                if (dif > 10000)
                    break;
                int bitsRecus = socket.read(buf);
                if (bitsRecus == -1) {
                    return false;
                } else if (bitsRecus == 68) {
                    buf.flip();
                    HandshakeMessage m = new HandshakeMessage();
                    m = (HandshakeMessage) m.getMessageFromByteBuffer(buf);
                    return Arrays.equals(m.getInfoHash(), file.getInfoHash());
                }
            }
        } catch (SocketTimeoutException e){
            return false;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Peer)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        Peer c = (Peer) o;

        // Compare the data members and return accordingly
        return (ip.compareTo(c.ip) == 0) && (port == c.port);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + ip.hashCode();
        result = 31 * result + port;
        result = 31 * result + id.hashCode();
        return result;
    }

    HashMap<TypeMessage, Boolean> getMessageReceivedFromPeerHistory() {
        return messageReceivedFromPeerHistory;
    }

    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }

    void updateMessageReceivedFromPeerHistory(TypeMessage typeMessage, Boolean b) {
        this.messageReceivedFromPeerHistory.put(typeMessage, b);
    }

    public AtomicBoolean getBitfieldSet() {
        return bitfieldSet;
    }

    public SocketChannel getSocket() {
        return socket;
    }
}