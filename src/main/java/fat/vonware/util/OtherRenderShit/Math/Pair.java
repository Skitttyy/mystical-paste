package fat.vonware.util.OtherRenderShit.Math;

public class Pair<A, B> {
   private A a;
   private B b;

   public Pair(A a, B b) {
      this.a = a;
      this.b = b;
   }

   public A getA() {
      return this.a;
   }

   public void setA(A a) {
      this.a = a;
   }

   public B getB() {
      return this.b;
   }

   public void setB(B b) {
      this.b = b;
   }
}
