package fat.vonware.features.gui.newgui.newcomponents.preview;

public interface IPreviewable {
   PreviewType getPreviewType();

   public static enum PreviewType {
      CHAMS,
      ESP;
   }
}
