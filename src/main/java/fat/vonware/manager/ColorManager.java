package fat.vonware.manager;

import java.awt.Color;

public class ColorManager {
   private float red = 1.0F;
   private float green = 1.0F;
   private float blue = 1.0F;
   private float alpha = 1.0F;
   private Color color;

   public ColorManager() {
      this.color = new Color(this.red, this.green, this.blue, this.alpha);
   }

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public void updateColor() {
      this.setColor(new Color(this.red, this.green, this.blue, this.alpha));
   }

   public void setColor(int red, int green, int blue, int alpha) {
      this.red = (float)red / 255.0F;
      this.green = (float)green / 255.0F;
      this.blue = (float)blue / 255.0F;
      this.alpha = (float)alpha / 255.0F;
      this.updateColor();
   }

   public void setRed(float red) {
      this.red = red;
      this.updateColor();
   }

   public void setGreen(float green) {
      this.green = green;
      this.updateColor();
   }

   public void setBlue(float blue) {
      this.blue = blue;
      this.updateColor();
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
      this.updateColor();
   }
}
