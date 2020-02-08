package peers;

import pieces.Piece;

/**
 * Observer Server interface
 */
public interface ObserverServer {
    void notify(Piece piece);
}

