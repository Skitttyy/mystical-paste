package fat.vonware.util;

import fat.vonware.features.modules.client.Manager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.Display;

public class IconUtil {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public static void setWindowIcon(Manager.Icons icon) {
      if (icon.getFiles() != null) {
         InputStream inputstream16 = null;
         InputStream inputstream32 = null;

         try {
            inputstream16 = mc.getResourceManager().getResource(new ResourceLocation(icon.getFiles()[0])).getInputStream();
            inputstream32 = mc.getResourceManager().getResource(new ResourceLocation(icon.getFiles()[1])).getInputStream();
            if (inputstream16 != null && inputstream32 != null) {
               Display.setIcon(new ByteBuffer[]{readImageToBuffer(inputstream16), readImageToBuffer(inputstream32)});
            }
         } catch (IOException e) {
            System.out.println("Couldn't set icon: " + e.getMessage());
         } finally {
            IOUtils.closeQuietly(inputstream16);
            IOUtils.closeQuietly(inputstream32);
         }

      }
   }

   public static ByteBuffer readImageToBuffer(InputStream imageStream) throws IOException {
      BufferedImage bufferedimage = ImageIO.read(imageStream);
      int[] aint = bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), (int[])null, 0, bufferedimage.getWidth());
      ByteBuffer bytebuffer = ByteBuffer.allocate(4 * aint.length);

      for(int i : aint) {
         bytebuffer.putInt(i << 8 | i >> 24 & 255);
      }

      bytebuffer.flip();
      return bytebuffer;
   }
}
