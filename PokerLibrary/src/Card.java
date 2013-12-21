

/**
 * Represents a single playing card.
 *
 * @author andyehou@gmail.com (Andy Hou)
 */
public class Card {
  private final int rank;
  private final Suit suit;
  
  public enum Suit {
    DIAMOND,
    CLUB,
    HEART,
    SPADE,
  }
  
  private static final char RANK_CHARS[] = {
    'A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A'
  };
  
  private static final char SUIT_CHARS[] = {
    '\u2666', '\u2663', '\u2665', '\u2660'
  };
  
  private Card(int rank, Suit suit) {
    this.rank = rank;
    this.suit = suit;
  }
  
  public int getRank() {
    return rank;
  }
  
  public Suit getSuit() {
    return suit;
  }
  
  public boolean isFaceCard() {
    return rank == 0 || rank >= 10;
  }
  
  /**
   * @param index The index between 0 and 51 inclusive.
   * @return a new card from the given index.
   */
  public static Card fromIndex(int index) {
    return new Card(index % 13, Suit.values()[index / 13]);
  }
  
  /**
   * @return the index corresponding to this card.
   */
  public int toIndex() {
    return suit.ordinal() * 13 + rank;
  }
  
  public String toString() {
    return "" + getRankChar(rank) + getSuitChar(suit);
  }
  
  public boolean equals(Object other) {
    if (other instanceof Card) {
      Card that = (Card)other;
      return this.rank == that.rank && this.suit == that.suit;
    }
    return false;
  }
  
  /**
   * @param rank The rank.
   * @return a character that represents the rank.
   */
  public static char getRankChar(int rank) {
    return RANK_CHARS[rank];
  }
  
  /**
   * @param suit The suit.
   * @return a character that represents the suit.
   */
  public static char getSuitChar(Suit suit) {
    return SUIT_CHARS[suit.ordinal()];
  }
}
