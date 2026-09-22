package fat.vonware.ducks;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import javax.annotation.Nullable;
import net.minecraft.network.Packet;

public interface INetworkManager {
   void sendPacketNoEvent(Packet<?> var1);

   void sendFast(Packet<?> var1, @Nullable GenericFutureListener<? extends Future<? super Void>>[] var2);
}
