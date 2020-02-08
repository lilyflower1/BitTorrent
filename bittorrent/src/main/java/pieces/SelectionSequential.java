package pieces;

import peers.PeerClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selection Sequential class
 * L’algorithme devra respecter les contraintes suivantes :
 * <p>
 * ne pas envoyer de requête pour une pièce qu’un peer ne possède pas
 * ne pas envoyer des requêtes pour le même bloc à des peers différents
 * ne pas requérir une pièce que vous avez déjà
 */
public class SelectionSequential extends Selection {
    SelectionSequential() {
        super();
    }

    /**
     * select pieces to ask for every peer
     *
     * @param piecesToDownload left pieces to download
     */
    @Override
    public synchronized void selectPiecesToAsk(Map<Piece, List<Block>> piecesToDownload) {
        for (Map.Entry<PeerClient, Map<Piece, List<Block>>> entry : piecesToAsk.entrySet()) {
            entry.setValue(new ConcurrentHashMap<>());
        }
        for (Map.Entry<PeerClient, List<Piece>> entry : pieces.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                for (Piece p : entry.getValue()) {
                    if (piecesToDownload.containsKey(p)) {
                        piecesToAsk.get(entry.getKey()).put(p, piecesToDownload.get(p));
                        piecesToDownload.remove(p);
                    }
                }
            } else {
                entry.getKey().setCompleted(true);
            }
        }
        setCompleted();
        setDone();
    }

    /**
     * set peer selection completed
     */
    @Override
    public synchronized void setCompleted() {
        for (Map.Entry<PeerClient, Map<Piece, List<Block>>> entry : piecesToAsk.entrySet()) {
            if (entry.getValue().size() == 0) {
                entry.getKey().setCompleted(true);
            }
        }
    }

    /**
     * pieces to ask fro a special peer
     *
     * @param p PeerClient
     */
    @Override
    public synchronized Map<Piece, List<Block>> selectPiecesToAskFromPeer(PeerClient p) {
        return this.piecesToAsk.get(p);
    }
}
