

import java.util.UUID;

/**
 * Represents a message sent by a player describing an action or game state.
 * Can be serialized and deserialized from a string.
 *
 * @author andyehou@gmail.com (Andy Hou)
 */
public class GameMessage {
  // The UID of the player sending the message.
  private final UUID playerUid;
  
  // The type of game message.
  private final Type type;
  
  // The possible types of game messages.
  public enum Type {
    SYNC_GAME_STATE,
    PLAYER_ACTION,
    PLAYER_JOINING,
    PLAYER_LEAVING,
    NEW_HAND,
    REQUEST_NEW_HAND
  }
  
  // The type of action, if the message type is PLAYER_ACTION.
  private final ActionType actionType;
  
  // The possible types of player game actions.
  public enum ActionType {
    NONE,
    FOLD,
    BET
  }
  
  // The number corresponding to:
  //   player bet action        (Type == PLAYER_ACTION)
  //   isHost                   (Type == PLAYER_JOINING)
  //   number of players        (Type == NEW_HAND)
  private final int number;
  
  // The string corresponding to:
  //   player nickname          (Type == PLAYER_JOINING)
  //   new hand game sync data  (Type == NEW_HAND)
  private final String data;
  
  private static final String LOG_TAG = GameMessage.class.getSimpleName();
  
  public GameMessage(UUID playerUid, Type type, ActionType actionType, int number, String data) {
    this.playerUid = playerUid;
    this.type = type;
    this.actionType = actionType;
    this.number = number;
    this.data = data;
  }
  
  public UUID getPlayerUid() {
    return playerUid;
  }
  
  public Type getType() {
    return type;
  }
  
  public ActionType getActionType() {
    return actionType;
  }
  
  public int getNumber() {
    return number;
  }
  
  public String getData() {
    return data;
  }
  
  public String toString() {
    return
        "[Player = " + playerUid +
        ", Type = " + type +
        ", ActionType = " + actionType +
        ", number = " + number +
        ", data = " + data + "]";
  }
  
  /**
   * Serializes this GameMessage into a string.
   * @return a string representing this GameMessage.
   */
  public String serialize() {
    return
        playerUid + "," +
        type.ordinal() + "," +
        actionType.ordinal() + "," +
        number + "," +
        data;
  }
  
  /**
   * Deserializes a string to create a new GameMessage.
   * @param s The string to deserialize.
   * @return a new GameMessage.
   */
  public static GameMessage deserialize(String s) {
    String parts[] = s.split(",", 5);
    
    UUID playerUid = UUID.fromString(parts[0]);
    int typeOrdinal = Integer.parseInt(parts[1]);
    Type type = Type.values()[typeOrdinal];
    int actionTypeOrdinal = Integer.parseInt(parts[2]);
    ActionType actionType = ActionType.values()[actionTypeOrdinal];
    int amount = Integer.parseInt(parts[3]);
    String data = parts[4];
    return new GameMessage(playerUid, type, actionType, amount, data);
  }
}
