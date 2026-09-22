package fat.vonware.util.holeesp;

import fat.vonware.util.BlockUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class HolePathFinder {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private static final int MAX_ITERATIONS = 2500;
   private static final int MAX_FALL_BLOCKS = 5;
   public static final double REACH_THRESHOLD = 0.32;
   private int maxStepUp = 1;
   private List<Vec3d> waypoints = Collections.emptyList();
   private List<BlockPos> rawWaypoints = Collections.emptyList();
   private int wpIndex = 0;
   private boolean valid = false;
   private static final int[][] DIRS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

   public void setMaxStepUp(int blocks) {
      this.maxStepUp = Math.max(0, Math.min(2, blocks));
   }

   public int getMaxStepUp() {
      return this.maxStepUp;
   }

   public boolean computePath(BlockPos start, BlockPos goal) {
      this.reset();
      List<BlockPos> raw = this.aStar(start, goal);
      if (raw != null && !raw.isEmpty()) {
         this.rawWaypoints = raw;
         List<Vec3d> wps = new ArrayList(raw.size());

         for(BlockPos bp : raw) {
            wps.add(new Vec3d((double)bp.getX() + (double)0.5F, (double)bp.getY(), (double)bp.getZ() + (double)0.5F));
         }

         this.waypoints = wps;
         this.valid = true;
         return true;
      } else {
         return false;
      }
   }

   public void invalidate() {
      this.reset();
   }

   public Vec3d getCurrentWaypoint() {
      return this.valid && this.wpIndex < this.waypoints.size() ? (Vec3d)this.waypoints.get(this.wpIndex) : null;
   }

   public boolean tick(Vec3d playerPos) {
      if (this.valid && !this.waypoints.isEmpty()) {
         Vec3d wp = (Vec3d)this.waypoints.get(this.wpIndex);
         if (Math.hypot(playerPos.x - wp.x, playerPos.z - wp.z) <= 0.32) {
            if (this.wpIndex >= this.waypoints.size() - 1) {
               return true;
            }

            ++this.wpIndex;
         }

         return false;
      } else {
         return true;
      }
   }

   public boolean isValid() {
      return this.valid;
   }

   public boolean isPathStillValid() {
      if (!this.valid) {
         return false;
      } else {
         int end = Math.min(this.wpIndex + 4, this.rawWaypoints.size());

         for(int i = this.wpIndex; i < end; ++i) {
            BlockPos bp = (BlockPos)this.rawWaypoints.get(i);
            if (!BlockUtil.isAir(bp) || !BlockUtil.isAir(bp.up())) {
               return false;
            }
         }

         return true;
      }
   }

   public int remainingWaypoints() {
      return this.valid ? Math.max(0, this.waypoints.size() - this.wpIndex) : 0;
   }

   public List<Vec3d> getWaypoints() {
      return Collections.unmodifiableList(this.waypoints);
   }

   private List<BlockPos> aStar(BlockPos start, BlockPos goal) {
      PriorityQueue<Node> open = new PriorityQueue();
      Map<BlockPos, Double> best = new HashMap();
      Set<BlockPos> closed = new HashSet();
      open.add(new Node(start, (double)0.0F, this.heuristic(start, goal), (Node)null));
      best.put(start, (double)0.0F);

      for(int it = 0; it < 2500 && !open.isEmpty(); ++it) {
         Node cur = (Node)open.poll();
         if (!closed.contains(cur.pos)) {
            closed.add(cur.pos);
            if (this.isGoal(cur.pos, goal)) {
               return this.buildPath(cur);
            }

            for(Node nb : this.expand(cur, goal)) {
               if (!closed.contains(nb.pos) && nb.g < (Double)best.getOrDefault(nb.pos, Double.MAX_VALUE)) {
                  best.put(nb.pos, nb.g);
                  open.add(nb);
               }
            }
         }
      }

      return null;
   }

   private List<Node> expand(Node cur, BlockPos goal) {
      List<Node> out = new ArrayList(10);
      BlockPos p = cur.pos;
      if (!BlockUtil.isAir(p.up())) {
         return out;
      } else {
         for(int[] d : DIRS) {
            BlockPos adj = p.add(d[0], 0, d[1]);
            if (BlockUtil.isAir(adj) && BlockUtil.isAir(adj.up())) {
               if (!BlockUtil.isAir(adj.down())) {
                  out.add(this.node(cur, adj, (double)1.0F, goal));
               } else {
                  this.addFallNeighbors(cur, adj, goal, out);
               }
            } else if (!BlockUtil.isAir(adj)) {
               for(int h = 1; h <= this.maxStepUp; ++h) {
                  boolean wallSolid = true;

                  for(int w = 0; w < h; ++w) {
                     if (BlockUtil.isAir(adj.up(w))) {
                        wallSolid = false;
                        break;
                     }
                  }

                  if (!wallSolid) {
                     break;
                  }

                  BlockPos newFeet = adj.up(h);
                  BlockPos newHead = adj.up(h + 1);
                  if (BlockUtil.isAir(newFeet) && BlockUtil.isAir(newHead)) {
                     boolean approachClear = true;

                     for(int a = 2; a <= h + 1; ++a) {
                        if (!BlockUtil.isAir(p.up(a))) {
                           approachClear = false;
                           break;
                        }
                     }

                     if (approachClear) {
                        out.add(this.node(cur, newFeet, (double)1.0F + (double)h * (double)0.5F, goal));
                     }
                  }
               }
            }
         }

         if (BlockUtil.isAir(p.down())) {
            this.addFallNeighbors(cur, p, goal, out);
         }

         return out;
      }
   }

   private void addFallNeighbors(Node cur, BlockPos column, BlockPos goal, List<Node> out) {
      for(int f = 1; f <= 5; ++f) {
         BlockPos land = column.down(f);
         if (!BlockUtil.isAir(land) || !BlockUtil.isAir(land.up())) {
            break;
         }

         double cost = (double)1.0F + (double)f * 0.15;
         if (this.isGoal(land, goal)) {
            out.add(this.node(cur, land, cost, goal));
            break;
         }

         if (!BlockUtil.isAir(land.down())) {
            out.add(this.node(cur, land, cost, goal));
            break;
         }
      }

   }

   private Node node(Node parent, BlockPos pos, double stepCost, BlockPos goal) {
      return new Node(pos, parent.g + stepCost, this.heuristic(pos, goal), parent);
   }

   private boolean isGoal(BlockPos pos, BlockPos goal) {
      return pos.getX() == goal.getX() && pos.getZ() == goal.getZ() && pos.getY() <= goal.getY();
   }

   private double heuristic(BlockPos a, BlockPos goal) {
      return (double)(Math.abs(a.getX() - goal.getX()) + Math.abs(a.getZ() - goal.getZ())) + (double)Math.abs(a.getY() - goal.getY()) * 0.3;
   }

   private List<BlockPos> buildPath(Node end) {
      LinkedList<BlockPos> path = new LinkedList();

      for(Node n = end; n != null; n = n.parent) {
         path.addFirst(n.pos);
      }

      return new ArrayList(path);
   }

   private void reset() {
      this.waypoints = Collections.emptyList();
      this.rawWaypoints = Collections.emptyList();
      this.wpIndex = 0;
      this.valid = false;
   }

   private static final class Node implements Comparable<Node> {
      final BlockPos pos;
      final double g;
      final double h;
      final Node parent;

      Node(BlockPos pos, double g, double h, Node parent) {
         this.pos = pos;
         this.g = g;
         this.h = h;
         this.parent = parent;
      }

      double f() {
         return this.g + this.h;
      }

      public int compareTo(Node o) {
         return Double.compare(this.f(), o.f());
      }
   }
}
