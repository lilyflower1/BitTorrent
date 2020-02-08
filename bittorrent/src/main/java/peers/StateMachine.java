package peers;

import message.InterestedMessage;
import message.NotInterestedMessage;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * StateMachine class
 */
public class StateMachine {
    private enum State {CHOKE_NOT_INTERESTED, UNCHOKE_NOT_INTERESTED, UNCHOKE_INTERESTED, CHOKE_INTERESTED}

    private State state;

    public StateMachine() {
        this.state = State.CHOKE_NOT_INTERESTED;
    }

    /**
     * event send Interested
     *
     * @param p peer client
     * @throws IOException exception
     */
    public void sendInterested(PeerClient p) throws IOException {
        switch (this.state) {
            case CHOKE_NOT_INTERESTED:
                p.send(new InterestedMessage(), p.getSocket());
                this.state = State.CHOKE_INTERESTED;
                break;
            case UNCHOKE_NOT_INTERESTED:
                p.send(new InterestedMessage(), p.getSocket());
                this.state = State.UNCHOKE_INTERESTED;
                break;
            default:
                break;
        }
    }

    /**
     * event send not interested
     *
     * @param p peer client
     * @throws IOException exception
     */
    public void sendNotInterested(PeerClient p) throws IOException {
        switch (this.state) {
            case CHOKE_INTERESTED:
                p.send(new NotInterestedMessage(), p.getSocket());
                this.state = State.CHOKE_NOT_INTERESTED;
                break;
            case UNCHOKE_INTERESTED:
                p.send(new NotInterestedMessage(), p.getSocket());
                this.state = State.UNCHOKE_NOT_INTERESTED;
                break;
            default:
                break;
        }
    }

    /**
     * event receive a choke message
     */
    public void receiveChoke() {
        switch (this.state) {
            case UNCHOKE_NOT_INTERESTED:
                this.state = State.CHOKE_NOT_INTERESTED;
                break;
            case UNCHOKE_INTERESTED:
                this.state = State.CHOKE_INTERESTED;
                break;
            default:
                break;
        }
    }

    /**
     * event receive an unchoke message
     *
     * @param p peer client
     * @throws IOException              exception
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     */
    public void receiveUnChoke(PeerClient p) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if (this.state == State.CHOKE_INTERESTED) {
            p.receive();
            this.state = State.UNCHOKE_INTERESTED;
        }
    }

    /**
     * event get pieces to request pieces
     *
     * @param p peer client
     * @throws IOException              exception
     * @throws InterruptedException     exception
     * @throws NoSuchAlgorithmException exception
     */
    public void getPiece(PeerClient p) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if (this.state == State.UNCHOKE_INTERESTED) {
            p.requestPieces();
        }
    }
}