package peers;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class PeerManager
 */
public class PeerManager {
    private Logger globalLogger = Logger.getLogger("global");
    private Vector<PeerClient> peers;


    public PeerManager(Level l) {
        globalLogger.setLevel(l);
        this.peers = new Vector<>();
    }

    /**
     * Add a new peers.PeerClient to the list of existing peers
     *
     * @param peer peer
     */
    public void addNewPeer(PeerClient peer) {
        this.peers.add(peer);
    }

    /**
     * Remove a peer from the list of existing peers
     * @param peer peer
     */
    public void removePeer(PeerClient peer) {
        this.peers.remove(peer);
    }

    /**
     * Return true if the peer id is already use for another peer
     * @param peerId peer id
     * @return true if the peer id is already use, false if it's available
     */
    boolean peerIdAvailable(String peerId) {
        for(PeerClient p : this.peers) {
            if (p.getPeerId().equals(peerId)) {
                return false;
            }
        }
        return true;
    }

    public synchronized List<PeerClient> getPeers() {
        return peers;
    }

    /**
     * find peer client by its peerID
     *
     * @param peerID peer id
     * @return peer client
     */
    public PeerClient findPeer(String peerID) {
        for (PeerClient peer : this.peers) {
            if (peerID.equals(peer.getPeerId())) {
                return peer;
            }
        }
        globalLogger.warning("Peer not found");
        return null;
    }


    /**
     * check if peer is present in peerManager
     *
     * @param ipAddr IP address
     * @param port   port
     * @return true if present
     */
    public boolean containsPeer(String ipAddr, int port) {
        for (PeerClient p : peers) {
            if (p.getIp().equals(ipAddr) && p.getPort() == port) {
                return true;
            }
        }
        return false;
    }
}
