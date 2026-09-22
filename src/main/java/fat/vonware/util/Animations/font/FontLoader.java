package fat.vonware.util.Animations.font;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import net.minecraftforge.fml.common.FMLLog;

public class FontLoader {
   public static void registerFont(String resourceName) {
      try {
         InputStream is = FontLoader.class.getResourceAsStream("/assets/vonware/fonts/" + resourceName);
         Throwable var2 = null;

         try {
            if (is != null) {
               Font font = Font.createFont(0, is);
               GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
               FMLLog.log.info("[FontLoader] Registered font: {}", font.getFontName());
               return;
            }

            FMLLog.log.error("[FontLoader] Not found: {}", resourceName);
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (is != null) {
               if (var2 != null) {
                  try {
                     is.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  is.close();
               }
            }

         }

      } catch (Exception e) {
         FMLLog.log.error("[FontLoader] Failed to register {}", resourceName, e);
      }
   }
}
