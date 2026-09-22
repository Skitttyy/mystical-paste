package fat.vonware.util.HUD;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public final class CornerSortUtil {
   private CornerSortUtil() {
   }

   public static <T> List<T> sort(List<T> list, ToIntFunction<T> widthFn, boolean bottomSide) {
      Comparator<T> comparator = bottomSide ? Comparator.comparingInt(widthFn) : Comparator.comparingInt(widthFn).reversed();
      return (List)list.stream().sorted(comparator).collect(Collectors.toList());
   }

   public static <T> void sortInPlace(List<T> list, ToIntFunction<T> widthFn, boolean bottomSide) {
      Comparator<T> comparator = bottomSide ? Comparator.comparingInt(widthFn) : Comparator.comparingInt(widthFn).reversed();
      list.sort(comparator);
   }

   public static boolean isLeftSide(float centerX, int screenWidth) {
      return centerX < (float)screenWidth / 2.0F;
   }

   public static boolean isBottomSide(float centerY, int screenHeight) {
      return centerY > (float)screenHeight / 2.0F;
   }
}
