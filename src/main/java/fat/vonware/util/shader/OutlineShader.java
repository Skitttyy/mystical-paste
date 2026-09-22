package fat.vonware.util.shader;

public class OutlineShader extends FrameBufferShader {
   public static final OutlineShader OUTLINE_SHADER = new OutlineShader();

   public OutlineShader() {
      super("differents");
   }

   public void setupUniforms() {
      this.setupUniform("texture");
      this.setupUniform("texelSize");
      this.setupUniform("color");
      this.setupUniform("divider");
      this.setupUniform("radius");
      this.setupUniform("maxSample");
      this.setupUniform("startingHeight");
      this.setupUniform("viewPos");
      this.setupUniform("resolution");
      this.setupUniform("time");
   }

   public void updateUniforms() {
   }
}
