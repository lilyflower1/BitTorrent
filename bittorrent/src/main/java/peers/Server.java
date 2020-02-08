package peers;

import message.*;
import pieces.Piece;
import pieces.PieceManager;
import pieces.Torrent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Level;

/**
 * Server class
 */
public class Server extends Peer implements ObserverServer {
    private ServerSocketChannel socket;
    private Selector selector;
    private static final int TIMEOUT = 30000;
    private static final int BUFF_SIZE = 10000;
    private List<HaveMessage> broadcastHave = Collections.synchronizedList(new ArrayList<>());
    private LinkedList clients;

    public Server(String ip, int port, PeerManager peerManager, PieceManager pieceManager, Torrent torrentObject, Level l) throws IOException {
        super(ip, port, peerManager, pieceManager, torrentObject, l);
        pieceManager.setServerObserver(this);
        clients = new LinkedList();
    }

    @Override
    public void run() {
        try {
            this.connect();
            if(this.socket.isOpen()) {
                while (true) {
                    if (this.selector.select(TIMEOUT) == 0) {
                        globalLogger.info("...");
                        continue;
                    }
                    Iterator<SelectionKey> keyIterator = this.selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey currentKey = keyIterator.next();
                        if (currentKey.isAcceptable()) {
                            this.accept(currentKey);
                        }
                        if (currentKey.isReadable()) {
                            this.read(currentKey);
                            broadcast();
                        }
                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            globalLogger.severe(e.getMessage());
        }
    }

    @Override
    protected boolean connect() {
        try {
            // Create a selector to listen sockets and connections
            this.selector = Selector.open();
            this.socket = ServerSocketChannel.open();
            this.socket.socket().bind(new InetSocketAddress(this.port));
            this.socket.configureBlocking(false);
            // Register selector with channel
            this.socket.register(selector, SelectionKey.OP_ACCEPT);
            globalLogger.info("ServerSocketChannel opened ? " + this.socket.isOpen());
            return true;
        } catch (IOException e) {
            globalLogger.severe(e.getMessage());
            return false;
        }
    }

    /**
     * Handle accepting connection
     *
     * @param key selection key
     * @throws IOException exception
     */
    private void accept(SelectionKey key) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(BUFF_SIZE));
        String remoteAddress = socketChannel.getRemoteAddress().toString();
        String[] remoteSplit = remoteAddress.split(":");
        int remotePort = Integer.parseInt(remoteSplit[1]);
        String remoteIp = remoteSplit[0].substring(1);
        this.peerManager.addNewPeer(new PeerClient(remoteIp, remotePort, this.peerManager, this.pieceManager, this.file, globalLogger.getLevel()));
        clients.add(socketChannel);
        globalLogger.info("connection accepted from " + remoteIp + ":" + remotePort);
    }

    /**
     * handle read
     *
     * @param key selection key
     * @throws IOException          exception
     * @throws InterruptedException exception
     */
    private void read(SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        long bytesRead = socketChannel.read(buffer);
        if (bytesRead == -1) {
            socketChannel.close();
        } else if (bytesRead > 0) {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            // We get the peerId of the peer who sent us a message(s)
            String peerId = getPeerId(socketChannel);
            if (bytesRead == 68) {
                buffer.flip();
                HandshakeMessage m = new HandshakeMessage();
                m = (HandshakeMessage) m.getMessageFromByteBuffer(buffer);
                if (Arrays.equals(m.getInfoHash(), file.getInfoHash())){
                    HandshakeMessage handshake = new HandshakeMessage(this.file.getInfoHash(), this.getPeerId());
                    // We build the bitfield
                    byte[] bitfield = pieceManager.getBitfield();
                    BitfieldMessage bitfieldMessage = new BitfieldMessage(bitfield);
                    send(handshake, socketChannel);
                    send(bitfieldMessage, socketChannel);
                    buffer.compact();
                }
            } else {
                // We process the message(s) sent
                processMessagesReceived(buffer, socketChannel, peerId);
            }
        }
    }

    /**
     * broadcast have message over network if we have ones
     *
     * @throws IOException exception
     */
    private void broadcast() throws IOException {
        for (Object client : clients) {
            SocketChannel channel = (SocketChannel) client;
            for (HaveMessage m : broadcastHave) {
                send(m, channel);
            }
        }
        clients.clear();
    }

    /**
     * Method that gets a peerId given the socket on which we communicate with him
     *
     * @param socketChannel socket channel
     * @return the PeerId of the remote peer
     * @throws IOException exception
     */
    private String getPeerId(SocketChannel socketChannel) throws IOException {
        String peerId = null;
        String remoteAddress = socketChannel.getRemoteAddress().toString();
        String[] remoteSplit = remoteAddress.split(":");
        String remoteIp = remoteSplit[0].substring(1);
        for (PeerClient peer : peerManager.getPeers()) {
            if ((peer.ip).equals(remoteIp)) {
                peerId = peer.id;
            }
        }
        return peerId;
    }


    /**
     * Method that gets a list of messages, and replies to them if they are consistent
     * @param bytesBufferReceived the list of message as a byte[]
     * @param socket the socket on which the server communicates with the remote peer
     * @param peerID the ID of the remote peer
     */
    private void processMessagesReceived(ByteBuffer bytesBufferReceived, SocketChannel socket, String peerID) throws IOException, InterruptedException {
        List<Message> messages = Message.parserMessages(bytesBufferReceived);
        for (Message m : messages){
            if (checkMessageConsistency(m, peerID)){
                // We update the history of the messages received
                updateMessageHistory(peerID, m.getTypeMessage());
                List<Message> answer = new ArrayList<>();
                // We look at the received message type and create the appropriate answer message
                switch (m.getTypeMessage()) {
                    // If we receive an interested message, we reply with an unchoke message
                    case INTERESTED:
                        answer.add(new UnchokeMessage());
                        break;
                    // If we receive a request for a piece, we reply with the piece
                    case REQUEST:
                        RequestMessage requestMessage = (RequestMessage) m;
                        int pieceIndex = requestMessage.getIndex();
                        Piece pieceRequested = this.pieceManager.getPieces().get(pieceIndex);
                        //We build the piece to send
                        int indexBlock = requestMessage.getBegin()/Piece.BLOCK_SIZE;
                        byte[] block = pieceRequested.getBlocks().get(indexBlock).getBlockData();
                        answer.add(new PieceMessage(pieceIndex, requestMessage.getBegin(), block));
                        break;
                    // If we receive a not interested message, we reply with a choke and close the connection
                    case NOT_INTERESTED:
                        ChokeMessage chokeMessage = new ChokeMessage();
                        try {
                            send(chokeMessage, socket);
                            // Since the connection with this peer is finished, we clear the message history
                            clearMessageHistory(peerID);
                            // We close the socket since the peer is not interested
                            socket.close();
                        } catch (IOException e) {
                            globalLogger.severe(e.getMessage());
                        }
                        break;
                    default:
                        break;
                }
                for (Message message : answer){
                    send(message, socket);
                }
            }
        }
    }

    /**
     * Method that updates the Received Messages History from a certain peer
     * @param peerID The received message's peer ID
     * @param typeMessage message type
     */
    private void updateMessageHistory(String peerID, TypeMessage typeMessage){
        for(PeerClient peer : peerManager.getPeers()){
            // We search the corresponding peer in the PeerManager list
            if(peerID.equals(peer.id)){
                peer.updateMessageReceivedFromPeerHistory(typeMessage, true);
            }
        }
    }

    /**
     * Method that resets the history of the peer
     * @param peerID peer ID
     */
    private void clearMessageHistory(String peerID){
        for(PeerClient peer : peerManager.getPeers()){
            // We search the corresponding peer in the PeerManager list
            if(peerID.equals(peer.getPeerId())){
                // We reset the history of the peer
                for(TypeMessage typeMessage : TypeMessage.values()) {
                    peer.updateMessageReceivedFromPeerHistory(typeMessage, false);
                }
            }
        }
    }


    /**
     * Method that checks if the received message is consistent regarding Bittorrent protocol specification
     * @param message message
     * @param peerID peer ID
     * @return True if it is consistent, false if not
     */
    private boolean checkMessageConsistency(Message message, String peerID){
        boolean messageConsistency = false;
        switch (message.getTypeMessage()){
            case HANDSHAKE:
                HandshakeMessage messageReceived = (HandshakeMessage) message;
                if(Arrays.equals(messageReceived.getInfoHash(), this.file.getInfoHash())) {
                    messageConsistency = true;
                }
                break;
            case REQUEST:
                PeerClient peer = this.peerManager.findPeer(peerID);
                // We check that the peer in question sent an INTERESTED message before
                if(peer.getMessageReceivedFromPeerHistory().get(TypeMessage.INTERESTED)){
                    messageConsistency = true;
                }
                break;
            default: messageConsistency = true;

        }
        return messageConsistency;
    }

    @Override
    public void notify(Piece piece) {
        if (broadcastHave.size() > 4)
            broadcastHave.clear();
        broadcastHave.add(new HaveMessage(piece.getIndex()));
    }
}
