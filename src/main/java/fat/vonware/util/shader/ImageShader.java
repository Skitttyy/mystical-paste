package fat.vonware.util.shader;

public class ImageShader extends FrameBufferShader {
   public static final ImageShader FRUITY = new ImageShader("fruity");

   public ImageShader(String name) {
      super(name);
   }

   public void setupUniforms() {
      this.setupUniform("texture");
      this.setupUniform("image");
      this.setupUniform("texelSize");
      this.setupUniform("color");
      this.setupUniform("radius");
      this.setupUniform("resolution");
      this.setupUniform("time");
      this.setupUniform("alpha");
   }

   public void updateUniforms() {
   }
}
