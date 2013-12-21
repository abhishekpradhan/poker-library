

import java.util.Collections;
import java.util.List;

public class Pocket {
  // The 2 cards that make up the pocket.
  private List<Card> pocket;
  
  // The category of the pocket.
  private Category category;
  
  public enum Category {
    UNSUITED,
    SUITED,
    PAIR
  }
  
  private static final String LOG_TAG = Pocket.class.getSimpleName();
  
  /**
   * Constructs a pocket with the given cards.
   * @param pocket The 2 cards that make up the pocket.
   */
  public Pocket(List<Card> pocket) {
    this.pocket = pocket;
    computeCategory();
  }
  
  /**
   * @return the category of this pocket.
   */
  public Category getCategory() {
    return category;
  }
  
  public Card getLowCard() {
    return pocket.get(0);
  }
  
  public Card getHighCard() {
    return pocket.get(1);
  }
  
  public String toString() {
    String s = "[";
    for (int i = 0; i < pocket.size(); i++) {
      s += pocket.get(i).toString();
      if (i != pocket.size() - 1) {
        s += " ";
      }
    }
    return s + "]";
  }
  
  /**
   * Computes the category of this hand and reorders the pocket cards to place the
   * higher ranking card first.
   */
  private void computeCategory() {
    if (pocket.size() != 2) {
      System.out.println(LOG_TAG + "Pocket does not contain 2 cards: " + toString());
      return;
    }
    
    if (pocket.get(0).getRank() == pocket.get(1).getRank()) {
      category = Category.PAIR;
    } else if (pocket.get(0).getSuit() == pocket.get(1).getSuit()) {
      category = Category.SUITED;
    } else {
      category = Category.UNSUITED;
    }
    
    if (pocket.get(0).getRank() < pocket.get(1).getRank()) {
      Collections.reverse(pocket);
    }
  }
}
