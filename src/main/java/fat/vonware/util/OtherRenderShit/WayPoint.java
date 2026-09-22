package fat.vonware.util.OtherRenderShit;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class WayPoint {
   private String name;
   private double x;
   private double y;
   private double z;
   private String color;
   private static final Map<String, Color> COLOR_MAP = new LinkedHashMap();
   private static final String[] COLOR_NAMES;
   private static final Random RANDOM = new Random();

   public WayPoint(String name, double x, double y, double z, String color) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.z = z;
      this.color = color;
   }

   public WayPoint(String name, double x, double y, double z) {
      this(name, x, y, z, randomColorName());
   }

   public String getName() {
      return this.name;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public String getColor() {
      return this.color;
   }

   public Color getAwtColor() {
      return (Color)COLOR_MAP.getOrDefault(this.color, Color.WHITE);
   }

   public static String randomColorName() {
      return COLOR_NAMES[RANDOM.nextInt(COLOR_NAMES.length)];
   }

   public static Color resolveColor(String name) {
      return (Color)COLOR_MAP.getOrDefault(name, Color.WHITE);
   }

   static {
      COLOR_MAP.put("Red", new Color(255, 75, 75));
      COLOR_MAP.put("Orange", new Color(255, 165, 0));
      COLOR_MAP.put("Yellow", new Color(255, 255, 75));
      COLOR_MAP.put("Green", new Color(75, 255, 75));
      COLOR_MAP.put("Cyan", new Color(75, 255, 255));
      COLOR_MAP.put("Blue", new Color(75, 125, 255));
      COLOR_MAP.put("Purple", new Color(180, 75, 255));
      COLOR_MAP.put("Pink", new Color(255, 105, 180));
      COLOR_NAMES = (String[])COLOR_MAP.keySet().toArray(new String[0]);
   }
}
