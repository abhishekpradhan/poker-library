

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a game of Texas hold'em.
 *
 * @author andyehou@gmail.com (Andy Hou)
 */
public class TexasHoldemGame {
  // The deck of cards to draw cards from.
  private Deck deck;
  // The players in the game (myself is index 0).
  private List<Player> players;
  // The cards on the board.
  private List<Card> boardCards;
  // The player who is acting as the button for the current hand.
  private Player button;
  // The player whose turn it is to act.
  private Player actor;
  // The player who last bet or raised. Set to the first actor at the start of a round of betting.
  private Player lastRaised;
  // The players who won the current hand.
  private List<Player> winners;
  // The number of chips in the pot.
  private int pot;
  // The amount of chips needed to call in a round of betting.
  private int toCallAmount;
  // True if we are hosting (ground truth for the simulation), vs acting as a guest or observer.
  private boolean isHost;
  // The players that we are to deal in on the next hand.
  private List<Player> newHandPlayers;
  
  // The state of one hand of poker.
  private State state;
  
  // The possible states of one hand of poker in play order.
  // The state always progresses in monotonically increasing enum order, except when
  // the hand is won, in which case it jumps to the HAND_DONE state. It can also jump to
  // NONE if the communication channel is disconnected. 
  public enum State {
    NONE,
    BET_1,
    BET_2,
    BET_3,
    BET_4,
    HAND_DONE
  }
  
  private static final int BUY_IN_CHIPS = 20;
  private static final int SMALL_BLIND  = 1;
  private static final int BIG_BLIND    = 2;
  
  private static final String LOG_TAG = TexasHoldemGame.class.getSimpleName();
  
  /**
   * Creates a new TexasHoldemGame.
   * @param handler The handler to send messages back to the UI activity.
   */
  public TexasHoldemGame() {
    players = new ArrayList<Player>();
    boardCards = new ArrayList<Card>();
    newHandPlayers = new ArrayList<Player>();
    setState(State.NONE);
  }
  
  private void setState(State state) {
    this.state = state;
  }
  
  public State getState() {
    return state;
  }
  
  public String getStateDescription() {
    switch (state) {
      case BET_1:
        return "Preflop";
      case BET_2:
        return "Flop";
      case BET_3:
        return "Turn";
      case BET_4:
        return "River";
    }
    return "";
  }
  
  /**
   * Sets up the game after an initial connection is made.
   * @param isHost true if we are acting as the host.
   */
  public void connect(boolean isHost) {
    this.isHost = isHost;
  }
  
  /**
   * Clean up member variables when game is disconnected.
   */
  public void disconnect() {
    isHost = false;
    players.clear();
    cleanupHand();
    button = null;
    setState(State.NONE);
  }
  
  /**
   * @return true if the game has started after a call to connect().
   */
  public boolean isStarted() {
    return state != State.NONE;
  }
  
  /**
   * @return true if the game is over.
   */
  public boolean isGameOver() {
    return state == State.HAND_DONE && getPlayersWithChips().size() < 2;
  }
  
  /**
   * @return the player who won the game or null if the game is not over.
   */
  public Player getGameWinner() {
    if (state != State.HAND_DONE || !isGameOver()) {
      return null;
    }
    for (Player player : players) {
      if (player.getChips() > 0) {
        return player;
      }
    }
    return null;
  }
  
  public List<Player> getPlayers() {
    return players;
  }
  
  /**
   * @param playerUid The UID of the player to find.
   * @return the player with the given UID.
   */
  public Player getPlayer(UUID playerUid) {
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).getUid().equals(playerUid)) {
        return players.get(i);
      }
    }
    return null;
  }
  
  public Player getCurrentActor() {
    return actor;
  }
  
  public Player getButton() {
    return button;
  }
  
  public Player getLastRaised() {
    return lastRaised;
  }
  
  public List<Card> getBoardCards() {
    return boardCards;
  }
  
  /**
   * @return true if the hand is in the DONE state and all players except one folded.
   */
  public boolean isWinDueToFolding() {
    return state == State.HAND_DONE && getPlayersInHand().size() == 1;
  }
  
  public List<Player> getWinners() {
    return winners;
  }
  
  /**
   * @return a list of cards in the winning hands.
   */
  public List<Card> getWinningCards() {
    Set<Card> cards = new HashSet<Card>();
    if (state == State.HAND_DONE) {
      for (int i = 0; i < winners.size(); i++) {
        Player winner = winners.get(i);
        if (winner.getBestHand() != null) {
          cards.addAll(winner.getBestHand().getCards());
        }
      }
    }
    return new ArrayList<Card>(cards);
  }
  
  /**
   * @return the size of the pot, including chips in the current round of betting.
   */
  public int getPotSize() {
    int bets = 0;
    for (int i = 0; i < players.size(); i++) {
      bets += players.get(i).getBet();
    }
    return pot + bets;
  }
  
  /**
   * @return the minimum pot contribution among the active players. This should be the same
   *     amount for each active player, unless some have gone all-in.
   */
  public int getPotContribution() {
    int minPotContribution = Integer.MAX_VALUE;
    for (Player player : getPlayersInHand()) {
      if (player.getPotContribution() < minPotContribution) {
        minPotContribution = player.getPotContribution();
      }
    }
    return minPotContribution;
  }
  
  /**
   * @return the amount needed to call. This is the number of chips to call to in the current
   *     round of betting and does not include any pot contribution from previous betting rounds.
   */
  public int getToCallAmount() {
    return toCallAmount;
  }
  
  /**
   * @return the minimum amount needed to raise. This is the number of chips to raise to in the
   *     current round of betting and does not include any pot contribution from previous betting
   *     rounds. The player can also raise by going all-in if they do not have enough chips.
   */
  public int getToRaiseAmount() {
    return Math.max(BIG_BLIND, getToCallAmount() * 2 + getPotContribution());
  }
  
  /**
   * Handles a message about the game, such as a player taking an action or
   * an message containing game state.
   * @param message The message about the game.
   */
  public void handleGameMessage(GameMessage message) {
    //Log.i(LOG_TAG, "Handling game message: " + message);
    switch (message.getType()) {
      
      case SYNC_GAME_STATE:
        break;
        
      case PLAYER_ACTION:
        // Check that the player sending the message is the current player to act.
        if (actor != null && actor.getUid().equals(message.getPlayerUid())) {
          switch (message.getActionType()) {
            case NONE:
              // Should not happen. Treat as fold.
              System.out.println(LOG_TAG + " Got PLAYER_ACTION message, but action type is NONE.");
            case FOLD:
              if (actor.getBet() < toCallAmount && actor.getBet() < actor.getMaxBet()) {
                actor.fold();
              }
              continueGame();
              break;
            case BET:
              int amount = message.getNumber();
              if (amount > toCallAmount) {
                // Bet or raise.
                lastRaised = actor;
                toCallAmount = amount;
                actor.setBet(amount);
              } else if (amount < toCallAmount && actor.getMaxBet() > amount) {
                // Illegal action. Player did not bet at least the call amount
                // and has more chips (so not going all-in). Treat as fold.
                System.out.println(LOG_TAG + " Player BET is less than the call amount.");
                actor.fold();
              } else {
                // Check or call.
                actor.setBet(amount);
              }
              continueGame();
              break;
          }
        } else {
          System.out.println(LOG_TAG + " Got PLAYER_ACTION message from an unexpected player = " +
              message.getPlayerUid() + ", not the actor = " +
              (actor == null ? "NONE" : actor.getUid()));
        }
        break;
        
      case PLAYER_JOINING:
        if (isHost) {
          // Check that the player has not already joined.
          if (getPlayer(message.getPlayerUid()) == null) {
            Player newPlayer = new Player(message.getPlayerUid(), message.getData(), BUY_IN_CHIPS);
            newPlayer.setIsReady(true);
            if (!isStarted()) {
              players.add(newPlayer);
              setNextPlayers();
              
              if (isAllPlayersReadyToStart()) {
                // Deal a new hand and broadcast sync data to other players.
                dealNewHand();
              }
            } else {
              // Keep track of new players to deal in on the next hand.
              newHandPlayers.add(newPlayer);
            }
          } else {
            System.out.println(LOG_TAG + " Got PLAYER_JOINING message from a player already in the game: " +
                message.getPlayerUid());
          }
        }
        break;
        
      case PLAYER_LEAVING:
        if (isHost) {
          Player player = getPlayer(message.getPlayerUid());
          if (player != null) {
            players.remove(player);
            setNextPlayers();
            // TODO: return player bets.
            dealNewHand();
          }
        }
        break;
        
      case NEW_HAND:
        if (!isHost) {
          setNewHand(message.getNumber(), message.getData());
        }
        break;
        
      case REQUEST_NEW_HAND:
        if (!isStarted() || state == State.HAND_DONE) {
          Player player = getPlayer(message.getPlayerUid());
          if (player != null) {
            player.setIsReady(true);
            if (isHost && isAllPlayersReadyToStart()) {
              dealNewHand();
            }
          }
        }
        break;
    }
  }
  
  /**
   * Starts a new hand of poker.
   */
  public void dealNewHand() {
    if (!isHost) {
      System.out.println(LOG_TAG + " Trying to deal a new hand, but not the host player.");
      return;
    }
    
    cleanupHand();
    
    // Add any new players.
    for (int i = 0; i < newHandPlayers.size(); i++) {
      players.add(newHandPlayers.get(i));
    }
    setNextPlayers();
    newHandPlayers.clear();
    
    // Shuffle the deck.
    deck = new Deck();
    deck.shuffle();
    
    // Rotate the button. May be null for first hand.
    if (button == null) {
      button = players.get(0);
    } else {
      button = button.getNextPlayerNotBroke();
    }
    
    setupHand();
  }
  
  /**
   * Sets member variables for a new hand from the given synchronization data.
   * @param playerCount The number of players in the hand.
   * @param data The synchronization data as a string.
   */
  public void setNewHand(int playerCount, String data) {
    if (isHost) {
      System.out.println(LOG_TAG + " Trying to set a new hand, but am the host player.");
      return;
    }
    
    cleanupHand();
    deserializeNewHand(playerCount, data);
    
    setupHand();
  }
  
  /**
   * @return a string that can be used to synchronize the initial game state.
   */
  private String serializeNewHand() {
    // Serialize the deck.
    String data = deck.serialize() + ",";
    
    // Serialize the button.
    data += button.getUid() + ",";
    
    // Serialize the players.
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      data += player.getUid() + "," +
          player.getName() + "," +
          player.getChips();
      if (i < players.size() - 1) {
        data += ",";
      }
    }

    return data;
  }
  
  /**
   * Sets member variables for a new hand from the given synchronization data.
   * @param data The new hand synchronization data as a string.
   */
  private void deserializeNewHand(int playerCount, String data) {
    String parts[] = data.split(",", 3);
    
    // Set the deck.
    deck = Deck.deserialize(parts[0]);
    
    // Set the players.
    String playerParts[] = parts[2].split(",", -1);
    players.clear();
    if (playerParts.length != playerCount * 3) {
      System.out.println(LOG_TAG + " Bad NEW_HAND message.");
    }
    for (int i = 0; i < playerCount; i++) {
      UUID uid = UUID.fromString(playerParts[i*3]);
      String name = playerParts[i*3 + 1];
      int chips = Integer.parseInt(playerParts[i*3 + 2]);
      players.add(new Player(uid, name, chips));
    }
    setNextPlayers();
    
    // Set the button.
    button = getPlayer(UUID.fromString(parts[1]));
  }
  
  /**
   * Resets member variables for a new hand.
   */
  private void cleanupHand() {
    boardCards.clear();
    pot = 0;
    toCallAmount = 0;
    actor = null;
    lastRaised = null;
    winners = null;
    
    for (int i = 0; i < players.size(); i++) {
      players.get(i).cleanupHand();
    }
  }
  
  /**
   * Sets up a new hand of poker.
   */
  private void setupHand() {
    // Draw cards for each player in the hand.
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      if (!player.isBroke()) { 
        players.get(i).drawCards(deck);
      }
    }
    
    // Set up the blinds.
    Player smallBlind = button.getNextPlayerNotBroke();
    Player bigBlind = smallBlind.getNextPlayerNotBroke();
    smallBlind.setBet(Math.min(SMALL_BLIND, smallBlind.getMaxBet()));
    bigBlind.setBet(Math.min(BIG_BLIND, bigBlind.getMaxBet()));
    
    // Start the first round of betting.
    actor = bigBlind.getNextPlayerNotBroke();
    lastRaised = actor;
    toCallAmount = BIG_BLIND;
    
    setState(State.BET_1);
  }
  
  /**
   * Continues the game after the current actor takes an action.
   */
  private void continueGame() {
    actor = getNextActor();
    List<Player> playersInHand = getPlayersInHand();
    if (playersInHand.size() == 1) {
      // Hand ends due to only one player remaining.
      concludeBettingRound();
      winners = playersInHand;
      awardWinners();
      clearPlayersReadyStatus();
      actor = null;
      setState(State.HAND_DONE);
      
    } else if (actor == null) {
      concludeBettingRound();
      
      switch (state) {
        case BET_1:
          // Deal the flop.
          boardCards.add(deck.drawCard());
          boardCards.add(deck.drawCard());
          boardCards.add(deck.drawCard());
          startBettingRound();
          setState(State.BET_2);
          break;
          
        case BET_2:
          // Deal the turn.
          boardCards.add(deck.drawCard());
          startBettingRound();
          setState(State.BET_3);
          break;
          
        case BET_3:
          // Deal the river.
          boardCards.add(deck.drawCard());
          startBettingRound();
          setState(State.BET_4);
          break;
          
        case BET_4:
          // Award winners by showdown.
          winners = getWinnersByShowdown();
          awardWinners();
          clearPlayersReadyStatus();
          actor = null;
          setState(State.HAND_DONE);
          break;
      }
    }
  }
  
  /**
   * Concludes a round of betting by setting the necessary member variables.
   */
  private void concludeBettingRound() {
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      pot += player.getBet();
      player.concludeBettingRound();
    }
  }
  
  /**
   * Starts a round of betting by setting the necessary member variables.
   * There must be at least two players who have not folded.
   */
  private void startBettingRound() {
    actor = button.getNextPlayerNotBroke();
    while (actor.isFolded()) {
      actor = actor.getNextPlayerNotBroke();
    }
    lastRaised = actor;
    toCallAmount = 0;
  }
  
  /**
   * @return the winning players determined by show down.
   */
  private List<Player> getWinnersByShowdown() {
    Hand bestHand = null;
    List<Player> winners = new ArrayList<Player>();
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      if (!player.isFolded()) {
        player.constructBestHand(boardCards);
        if (bestHand == null || bestHand.compare(player.getBestHand()) < 0) {
          bestHand = player.getBestHand();
          winners.clear();
          winners.add(player);
        } else if (bestHand.compare(player.getBestHand()) == 0) {
          winners.add(player);
        }
      }
    }
    return winners;
  }
  
  /**
   * Awards the winners of a hand the chips in the pot.
   * There may be more than one winner if the best hand values are equal.
   */
  private void awardWinners() {
    
    // Determine the lowest pot contribution among the winners.
    int lowestPotContribution = Integer.MAX_VALUE;
    for (int i = 0; i < winners.size(); i++) {
      if (winners.get(i).getPotContribution() < lowestPotContribution) {
        lowestPotContribution = winners.get(i).getPotContribution();
      }
    }
    
    // Return any extra chips to players.
    int mainPot = 0;
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      if (!player.isBroke()) {
        if (player.getPotContribution() > lowestPotContribution) {
          player.awardChips(player.getPotContribution() - lowestPotContribution);
          mainPot += lowestPotContribution;
        } else {
          mainPot += player.getPotContribution();
        }
      }
    }
    
    // Award chips in the main pot.
    int chipsEach = mainPot / winners.size();
    int remainder = mainPot - chipsEach * winners.size();
    for (int i = 0; i < winners.size(); i++) {
      int chipsAwarded = chipsEach;
      if (remainder > 0) {
        chipsAwarded++;
        remainder--;
      }
      winners.get(i).awardChips(chipsAwarded);
    }
  }
  
  /**
   * Clears the ready status for all players.
   */
  private void clearPlayersReadyStatus() {
    for (int i = 0; i < players.size(); i++) {
      players.get(i).setIsReady(false);
    }
  }
  
  /**
   * @return true if there are enough players for a new hand and the players who are not broke
   *     are ready for a new hand to start.
   */
  private boolean isAllPlayersReadyToStart() {
    List<Player> playersWithChips = getPlayersWithChips();
    if (playersWithChips.size() < 2) {
      return false;
    }
    for (int i = 0; i < playersWithChips.size(); i++) {
      if (!playersWithChips.get(i).isReady()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Sets an ordering on the players by setting each player's next player.
   */
  private void setNextPlayers() {
    for (int i = 0; i < players.size(); i++) {
      Player player = players.get(i);
      if (i > 0) {
        players.get(i - 1).setNextPlayer(player);
      }
    }
    players.get(players.size() - 1).setNextPlayer(players.get(0));
  }
  
  /**
   * @return the next player to act in the current round of betting or null if the current round
   *     of betting has concluded. The current round of betting concludes when the current actor
   *     returns to the player who made the last raise.
   */
  private Player getNextActor() {
    Player nextActor = actor.getNextPlayerNotBroke();
    while (nextActor.isFolded()) {
      if (nextActor == lastRaised) {
        // Should only happen during the first betting round due to blinds.
        return null;
      }
      nextActor = nextActor.getNextPlayerNotBroke();
    }
    if (nextActor == lastRaised) {
      return null;
    }
    return nextActor;
  }
  
  /**
   * @return the players who are still in the hand (have not folded).
   */
  private List<Player> getPlayersInHand() {
    List<Player> playersInHand = new ArrayList<Player>();
    for (int i = 0; i < players.size(); i++) {
      if (!players.get(i).isFolded() && !players.get(i).isBroke()) {
        playersInHand.add(players.get(i));
      }
    }
    return playersInHand;
  }
  
  /**
   * @return the players who still have chips (have not gone broke).
   */
  private List<Player> getPlayersWithChips() {
    List<Player> playersWithChips = new ArrayList<Player>();
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).getChips() > 0) {
        playersWithChips.add(players.get(i));
      }
    }
    return playersWithChips;
  }
}
