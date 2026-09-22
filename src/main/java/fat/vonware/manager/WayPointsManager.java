package fat.vonware.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fat.vonware.util.OtherRenderShit.WayPoint;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WayPointsManager {
   private static final File WAYPOINTS_DIR = new File("vonware");
   private static final File WAYPOINTS_FILE = new File("vonware/waypoints.json");
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private final List<WayPoint> waypoints = new ArrayList();

   public void add(WayPoint wp) {
      this.waypoints.add(wp);
      this.save();
   }

   public boolean remove(String name) {
      Iterator<WayPoint> it = this.waypoints.iterator();

      while(it.hasNext()) {
         if (((WayPoint)it.next()).getName().equalsIgnoreCase(name)) {
            it.remove();
            this.save();
            return true;
         }
      }

      return false;
   }

   public WayPoint get(String name) {
      for(WayPoint wp : this.waypoints) {
         if (wp.getName().equalsIgnoreCase(name)) {
            return wp;
         }
      }

      return null;
   }

   public boolean exists(String name) {
      return this.get(name) != null;
   }

   public void clear() {
      this.waypoints.clear();
      this.save();
   }

   public List<WayPoint> getWaypoints() {
      return this.waypoints;
   }

   public void save() {
      try {
         if (!WAYPOINTS_DIR.exists()) {
            WAYPOINTS_DIR.mkdirs();
         }

         JsonArray array = new JsonArray();

         for(WayPoint wp : this.waypoints) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", wp.getName());
            obj.addProperty("x", wp.getX());
            obj.addProperty("y", wp.getY());
            obj.addProperty("z", wp.getZ());
            obj.addProperty("color", wp.getColor());
            array.add(obj);
         }

         FileWriter writer = new FileWriter(WAYPOINTS_FILE);
         Throwable var17 = null;

         try {
            GSON.toJson(array, writer);
         } catch (Throwable var13) {
            var17 = var13;
            throw var13;
         } finally {
            if (writer != null) {
               if (var17 != null) {
                  try {
                     writer.close();
                  } catch (Throwable var12) {
                     var17.addSuppressed(var12);
                  }
               } else {
                  writer.close();
               }
            }

         }
      } catch (Exception e) {
         System.err.println("[Vonware] Failed to save waypoints: " + e.getMessage());
         e.printStackTrace();
      }

   }

   public void load() {
      this.waypoints.clear();
      if (WAYPOINTS_FILE.exists()) {
         try {
            FileReader reader = new FileReader(WAYPOINTS_FILE);
            Throwable var2 = null;

            try {
               for(JsonElement element : (new JsonParser()).parse(reader).getAsJsonArray()) {
                  JsonObject obj = element.getAsJsonObject();
                  String name = obj.get("name").getAsString();
                  double x = obj.get("x").getAsDouble();
                  double y = obj.get("y").getAsDouble();
                  double z = obj.get("z").getAsDouble();
                  String color = obj.has("color") ? obj.get("color").getAsString() : WayPoint.randomColorName();
                  this.waypoints.add(new WayPoint(name, x, y, z, color));
               }
            } catch (Throwable var23) {
               var2 = var23;
               throw var23;
            } finally {
               if (reader != null) {
                  if (var2 != null) {
                     try {
                        reader.close();
                     } catch (Throwable var22) {
                        var2.addSuppressed(var22);
                     }
                  } else {
                     reader.close();
                  }
               }

            }
         } catch (Exception e) {
            System.err.println("[Vonware] Failed to load waypoints: " + e.getMessage());
            e.printStackTrace();
         }

      }
   }
}
