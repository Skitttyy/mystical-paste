package fat.vonware.util.Announcer;

public enum Language {
   ENGLISH("I just moved {blocks} blocks thanks to Mystic!", "I just placed {amount} {name} thanks to Mystic!", "I just broke {amount} {name} thanks to Mystic!", "I just ate {amount} {name} thanks to Mystic!"),
   SPANISH("Acabo de moverme {blocks} bloques gracias a Mystic!", "Acabo de colocar {amount} {name} gracias a Mystic!", "Acabo de romper {amount} {name} gracias a Mystic!", "Acabo de comer {amount} {name} gracias a Mystic!"),
   RUSSIAN("\u042f \u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u043e \u043f\u0435\u0440\u0435\u0435\u0445\u0430\u043b {blocks} \u0431\u043b\u0430\u0433\u043e\u0434\u0430\u0440\u044f Mystic!", "\u042f \u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u043e \u0440\u0430\u0437\u043c\u0435\u0441\u0442\u0438\u043b {amount} {name} \u0431\u043b\u0430\u0433\u043e\u0434\u0430\u0440\u044f Mystic!", "\u042f \u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u043e \u0440\u0430\u0437\u0431\u0438\u043b {amount} {name} \u0431\u043b\u0430\u0433\u043e\u0434\u0430\u0440\u044f Mystic! ", "\u044f \u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u043e \u0441\u044a\u0435\u043b {amount} {name} \u0431\u043b\u0430\u0433\u043e\u0434\u0430\u0440\u044f Mystic!"),
   GERMAN("Ich habe gerade {blocks} Bl\u00f6cke verschoben, dank Mystic!", "Ich habe gerade {amount} {name} dank Mystic!", "Ich habe gerade {amount} {name} dank Mystic!", "Ich habe gerade {amount} {name} dank Mystic!"),
   ARABIC("\u0644\u0642\u062f \u0642\u0645\u062a \u0644\u0644\u062a\u0648 \u0628\u0646\u0642\u0644 {blocks} \u0645\u0646 \u0627\u0644\u0643\u062a\u0644 \u0628\u0641\u0636\u0644 Mystic!", "\u0644\u0642\u062f \u0642\u062f\u0645\u062a \u0644\u0644\u062a\u0648 {amount} {name} \u0628\u0641\u0636\u0644 Mystic!", "\u0644\u0642\u062f \u0643\u0633\u0631\u062a {amount} {name} \u0628\u0641\u0636\u0644 Mystic!", "\u0644\u0642\u062f \u0623\u0643\u0644\u062a \u0644\u0644\u062a\u0648 {amount} {name} \u0628\u0641\u0636\u0644 Mystic!"),
   FRENCH("Je viens de bouger {blocks} pieds gr\u00e2ce \u00e0 Mystic!", "Je viens de placer {amount} {name} gr\u00e2ce \u00e0 Mystic!", "Je viens de casser {amount} {name} gr\u00e2ce \u00e0 Mystic!", "Je viens de manger {amount} {name} gr\u00e2ce \u00e0 Mystic!"),
   SWEDISH("Jag har just flyttat {blocks} fot tack vare Mystic", "Jag har just placerat {amount} {name} tack vare Mystic", "Jag f\u00f6rst\u00f6rde just {amount} {name} tack vare Mystic", "Jag \u00e5t just {amount} {name} tack vare Mystic"),
   DotGod("I just flew {blocks} meters like a Butterfly thanks to DotGod.CC!", "I just placed {amount} {name} thanks to DotGod.CC!", "I just mined {amount} {name} thanks to DotGod.CC!", "I just ate {amount} {name} thanks to DotGod.CC!"),
   Vonware("I just moved {blocks} blocks thanks to Vonware!", "I just placed {amount} {name} thanks to Vonware!", "I just broke {amount} {name} thanks to Vonware!", "I just ate {amount} {name} thanks to Vonware!");

   private final String walkMessage;
   private final String placeMessage;
   private final String breakMessage;
   private final String eatMessage;

   private Language(String walkMessage, String placeMessage, String breakMessage, String eatMessage) {
      this.walkMessage = walkMessage;
      this.placeMessage = placeMessage;
      this.breakMessage = breakMessage;
      this.eatMessage = eatMessage;
   }

   public String getWalkMessage() {
      return this.walkMessage;
   }

   public String getPlaceMessage() {
      return this.placeMessage;
   }

   public String getBreakMessage() {
      return this.breakMessage;
   }

   public String getEatMessage() {
      return this.eatMessage;
   }
}
