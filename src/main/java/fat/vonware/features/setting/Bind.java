package fat.vonware.features.setting;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Bind {
   private int key;

   public Bind(int key) {
      this.key = key;
   }

   public static Bind none() {
      return new Bind(-1);
   }

   public int getKey() {
      return this.key;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public boolean isEmpty() {
      return this.key < 0;
   }

   public String toString() {
      if (this.isEmpty()) {
         return "None";
      } else if (this.key >= 256) {
         int mouseButton = this.key - 256;
         return this.getMouseButtonName(mouseButton);
      } else {
         return this.key < 0 ? "None" : this.capitalise(Keyboard.getKeyName(this.key));
      }
   }

   public boolean isDown() {
      if (this.isEmpty()) {
         return false;
      } else if (this.key >= 256) {
         int mouseButton = this.key - 256;
         return Mouse.isButtonDown(mouseButton);
      } else {
         return Keyboard.isKeyDown(this.getKey());
      }
   }

   private String getMouseButtonName(int button) {
      switch (button) {
         case 0:
            return "LMB";
         case 1:
            return "RMB";
         case 2:
            return "MMB";
         case 3:
            return "Mouse4";
         case 4:
            return "Mouse5";
         default:
            return "Mouse" + button;
      }
   }

   private String capitalise(String str) {
      return str.isEmpty() ? "" : Character.toUpperCase(str.charAt(0)) + (str.length() != 1 ? str.substring(1).toLowerCase() : "");
   }

   public static class BindConverter extends Converter<Bind, JsonElement> {
      public JsonElement doForward(Bind bind) {
         return new JsonPrimitive(bind.toString());
      }

      public Bind doBackward(JsonElement jsonElement) {
         String s = jsonElement.getAsString();
         if (s.equalsIgnoreCase("None")) {
            return Bind.none();
         } else if (!s.toUpperCase().startsWith("MOUSE") && !s.equalsIgnoreCase("LMB") && !s.equalsIgnoreCase("RMB") && !s.equalsIgnoreCase("MMB")) {
            int key = -1;

            try {
               key = Keyboard.getKeyIndex(s.toUpperCase());
            } catch (Exception var5) {
            }

            return key == 0 ? Bind.none() : new Bind(key);
         } else {
            int mouseButton = this.parseMouseButton(s);
            return mouseButton >= 0 ? new Bind(mouseButton + 256) : Bind.none();
         }
      }

      private int parseMouseButton(String s) {
         switch (s.toUpperCase()) {
            case "LMB":
               return 0;
            case "RMB":
               return 1;
            case "MMB":
               return 2;
            case "MOUSE4":
               return 3;
            case "MOUSE5":
               return 4;
            default:
               if (s.startsWith("MOUSE")) {
                  try {
                     return Integer.parseInt(s.substring(5));
                  } catch (Exception var5) {
                     return -1;
                  }
               } else {
                  return -1;
               }
         }
      }
   }
}
