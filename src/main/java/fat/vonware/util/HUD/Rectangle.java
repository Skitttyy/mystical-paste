package fat.vonware.util.HUD;

public class Rectangle {
   private float x;
   private float y;
   private float width;
   private float height;

   public Rectangle(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public float getX() {
      return this.x;
   }

   public void setX(float x) {
      this.x = x;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public boolean inBound(int mouseX, int mouseY) {
      return (float)mouseX >= this.x && (float)mouseX <= this.x + this.width && (float)mouseY >= this.y && (float)mouseY <= this.y + this.height;
   }

   public String toString() {
      return String.format("Rectangle[x=%.1f, y=%.1f, w=%.1f, h=%.1f]", this.x, this.y, this.width, this.height);
   }
}
