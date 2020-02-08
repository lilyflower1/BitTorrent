package tracker;

import bencode.BDecoder;
import bencode.BEncodedValue;
import org.apache.commons.codec.binary.Hex;
import peers.PeerClient;
import peers.PeerManager;
import peers.Server;
import pieces.Block;
import pieces.Piece;
import pieces.PieceManager;
import pieces.Torrent;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracker class
 */
public class Tracker {

    private String announceURL;
    protected Logger globalLogger = Logger.getLogger("global");

    public Tracker(String announceURL, Level level) {
        globalLogger.setLevel(level);
        this.announceURL = announceURL;
        globalLogger.info("Tracker : " + announceURL);
    }

    /**
     * Send HTTP GET method to contact Tracker in order to get peers connected
     *
     * @param type         HTTPRequestType
     * @param server       Server
     * @param torrent      Torrent
     * @param manager      PeerManager
     * @param pieceManager PieceManager
     * @throws UnsupportedEncodingException exception
     * @throws MalformedURLException        exception
     */
    public void sendGet(HttpRequestType type, Server server, Torrent torrent, PeerManager manager, PieceManager pieceManager) throws UnsupportedEncodingException, MalformedURLException {
        URL url = createURL(type, server, torrent, pieceManager);
        globalLogger.info("get request from tracker :" + url.toString());
        try {
            // send HTTP GET request to tracker
            HttpURLConnection request = (HttpURLConnection) Objects.requireNonNull(url).openConnection();
            request.setRequestMethod("GET");

                // get tracker response
                BDecoder reader = new BDecoder(request.getInputStream());
                Map<String, BEncodedValue> document = reader.decodeMap().getMap();
                BEncodedValue peers = document.get("peers");
                int nbPeers = peers.getBytes().length;
                globalLogger.info("from tracker, nb peers connected : " + (nbPeers / 6 - 1));
            if (!type.equals(HttpRequestType.COMPLETED)) {
                for (int i = 0; i < nbPeers; i += 6) {
                    byte[] ip = {peers.getBytes()[i], peers.getBytes()[i + 1], peers.getBytes()[i + 2], peers.getBytes()[i + 3]};
                    String ipAddr = InetAddress.getByAddress(ip).getHostAddress();
                    byte[] portArray = {peers.getBytes()[i + 4], peers.getBytes()[i + 5]};
                    int port = Integer.parseInt(Hex.encodeHexString(portArray), 16);
                    if (!equals(server, ipAddr, port) && !manager.containsPeer(ipAddr, port)) {
                        PeerClient p = new PeerClient(ipAddr, port, manager, pieceManager, torrent, globalLogger.getLevel());
                        manager.addNewPeer(p);
                        p.start();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //globalLogger.severe("Error: " + e.getLocalizedMessage());
        }
    }

    /**
     * method checking if IP Address and port with Server
     *
     * @param server Server
     * @param peerIp IP Address
     * @param portIp Port
     * @return true if there are equals and false if not
     */
    private boolean equals(Server server, String peerIp, int portIp) {
        return peerIp.equals(server.getIp()) && portIp == server.getPort();
    }

    /**
     * Method creating URL for HTTP GET REQUEST
     *
     * @param type         HttpRequestType
     * @param server       Server
     * @param torrent      Torrent
     * @param pieceManager PieceManager
     * @return URL
     * @throws UnsupportedEncodingException exception
     * @throws MalformedURLException exception
     */
    private URL createURL(HttpRequestType type, Server server, Torrent torrent, PieceManager pieceManager) throws UnsupportedEncodingException, MalformedURLException {
        String workingURL = announceURL + '?';
        String escapedInfoHash = urlEncode(torrent.getInfoHash());
        String escapedPeerID = URLEncoder.encode(server.getPeerId(), StandardCharsets.UTF_8.toString());

        String left = null;
        String downloaded = null;
        String uploaded = null;
        switch (type) {
            case STARTED:
                left = Long.toString(torrent.getTotalLength()); /*initially set as the size of the file to be downloaded*/
                downloaded = "0";
                uploaded = "0";
                break;
            case COMPLETED:
                left = "0";
                downloaded = Long.toString(torrent.getTotalLength());
                uploaded = "0";
                break;
            case OTHER:
                Map<Piece, List<Block>> leftPieces = pieceManager.getLeftPieces();
                long leftValue = 0;
                for (Piece p : leftPieces.keySet()) {
                    leftValue += p.getSize();
                }
                long downloadedValue = torrent.getTotalLength() - leftValue;
                left = Long.toString(leftValue);
                downloaded = Long.toString(downloadedValue);
                uploaded = Integer.toString(0);
                break;
            default:
                globalLogger.warning("Not implemented yet");
        }
        workingURL = workingURL + "info_hash" + "=" + escapedInfoHash + "&peer_id=" + escapedPeerID + "&port="
                + server.getPort() + "&uploaded=" + uploaded + "&downloaded=" + downloaded + "&left=" + left;

        if (!type.equals(HttpRequestType.OTHER)) {
            workingURL = workingURL + "&event=" + type.toString();
        }

        return new URL(workingURL);
    }

    /**
     * Method which encodes byte array to an URLEncoding version
     *
     * @param rs byte array
     * @return String URLEncoded
     */
    protected static String urlEncode(byte[] rs) {
        String HEXDIGITS = "0123456789ABCDEF";
        StringBuffer result = new StringBuffer(rs.length * 2);

        for (byte r : rs) {
            char c = (char) r;

            switch (c) {
                case '_':
                case '.':
                case '*':
                case '-':
                case '/':
                    result.append(c);
                    break;

                case ' ':
                    result.append('+');
                    break;

                default:
                    if ((c >= 'a' && c <= 'z') ||
                            (c >= 'A' && c <= 'Z') ||
                            (c >= '0' && c <= '9')) {
                        result.append(c);
                    } else {
                        result.append('%');
                        result.append(HEXDIGITS.charAt((c & 0xF0) >> 4));
                        result.append(HEXDIGITS.charAt(c & 0x0F));
                    }
            }

        }
        return result.toString();
    }
}


