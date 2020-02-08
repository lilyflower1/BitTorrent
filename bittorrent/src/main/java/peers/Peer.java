package peers;

import message.Message;
import message.TypeMessage;
import pieces.PieceManager;
import pieces.Torrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Peer abstract class
 */
public abstract class Peer extends Thread{
    protected String id;
    protected String ip;
    protected int port;
    protected Torrent file;
    protected PieceManager pieceManager;
    protected PeerManager peerManager;
    Logger globalLogger = Logger.getLogger("global");
    protected HashMap<TypeMessage, Boolean> messageReceivedFromPeerHistory;


    public Peer(String ip, int port, PeerManager peerManager, PieceManager m, Torrent t, Level level) {
        globalLogger.setLevel(level);
        do {
            this.id = this.generatePeerId();
        }
        while (!(peerManager.peerIdAvailable(this.generatePeerId())));
        this.ip = ip;
        this.port = port;
        file = t;
        pieceManager = m;
        this.peerManager = peerManager;
    }


    /**
     * reverse string character by character
     *
     * @param str string word
     * @return reversed word
     */
    static String reverseWord(String str) {
        String[] words = str.split("\\s");
        StringBuilder reverseWord = new StringBuilder();
        for (String w : words) {
            StringBuilder sb = new StringBuilder(w);
            sb.reverse();
            reverseWord.append(sb.toString()).append(" ");
        }
        return reverseWord.toString().trim();
    }

    /**
     * generate a random peer id
     *
     * @return peer id
     */
    private String generatePeerId() {
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(20);

        for (int i = 0; i < 20; i++) {
            int index = (int) (alphaNumericString.length() * Math.random());
            sb.append(alphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    /**
     * connect method
     *
     * @return true if success and false if failure
     * @throws IOException exception
     */
    protected abstract boolean connect() throws IOException;

    /**
     * send method which writes the message on the socket
     *
     * @param message message
     * @param socket  socket
     * @throws IOException exception
     */
    protected void send(Message message, SocketChannel socket) throws IOException {
        ByteBuffer m = message.createByteArray();
        while (m.hasRemaining()) {
            socket.write(m);
        }
    }

    public String getPeerId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public PeerManager getPeerManager() {
        return peerManager;
    }
}
