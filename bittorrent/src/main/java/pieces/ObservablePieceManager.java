package pieces;

import peers.ObserverServer;

/**
 * Observable PieceManager class
 */
public class ObservablePieceManager {
    ObserverServer serverObserver;

    public ObservablePieceManager(){
    }

    public void notifyObserver(Piece piece){
        serverObserver.notify(piece);
    }

    public void setServerObserver(ObserverServer serverObserver) {
        this.serverObserver = serverObserver;
    }
}