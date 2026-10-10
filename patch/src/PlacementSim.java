package com.modelport.ported;

import com.modelport.ModelPort;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Placement table probe that also runs Block.onBlockPlacedBy (many mods set the facing there,
 * not in getStateForPlacement) inside the isolated ProbeWorld. Any failure of the extra step
 * falls back to exactly the value the previous probe produced.
 */
public final class PlacementSim {
    private static final float[] YAWS = new float[]{0.0f, 90.0f, 180.0f, 270.0f};
    private static final long LIMIT_MS = 5000L;
    private static ExecutorService exec;
    private static ProbeWorld world;
    private static EntityPlayer placer;
    private static Method hook;
    private static boolean hookSearched;
    private static int changed;

    private PlacementSim() {
    }

    public static int[] probe(Block block, WorldServer realWorld, BlockPos pos, FakePlayer player) {
        int[] table = new int[24];
        boolean hung = false;
        boolean differs = false;
        try {
            for (EnumFacing face : EnumFacing.values()) {
                for (int i = 0; i < 4; ++i) {
                    player.field_70177_z = YAWS[i];
                    player.field_70759_as = YAWS[i];
                    player.field_70126_B = YAWS[i];
                    player.field_70125_A = 0.0f;
                    int oldMeta = -1;
                    IBlockState state = null;
                    try {
                        state = block.getStateForPlacement((World) realWorld, pos, face, 0.5f, 0.5f, 0.5f, 0, (EntityLivingBase) player, EnumHand.MAIN_HAND);
                        oldMeta = block.func_176201_c(state);
                    } catch (Throwable t) {
                        oldMeta = -1;
                    }
                    int meta = oldMeta;
                    if (!hung) {
                        int sim = -1;
                        try {
                            sim = simulate(block, face, YAWS[i]);
                        } catch (TimeoutException te) {
                            hung = true;
                            reset();
                        } catch (Throwable t) {
                            sim = -1;
                        }
                        if (sim >= 0 && sim <= 15) {
                            if (sim != oldMeta) {
                                differs = true;
                            }
                            meta = sim;
                        }
                    }
                    if (meta < 0 || meta > 15) {
                        return null;
                    }
                    table[face.func_176745_a() * 4 + i] = meta;
                }
            }
            if (differs) {
                ++changed;
                try {
                    ModelPort.LOG.info("modelport: таблица установки {} дополнена результатом onBlockPlacedBy", (Object) block.getRegistryName());
                } catch (Throwable t) {
                    // logging only
                }
            }
            return table;
        } catch (Throwable t) {
            return null;
        }
    }

    private static int simulate(final Block block, final EnumFacing face, final float yaw) throws Exception {
        Method m = hookMethod();
        if (m == null) {
            return -1;
        }
        Future<Integer> f = executor().submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return PlacementSim.simulateNow(block, face, yaw);
            }
        });
        try {
            return f.get(LIMIT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            return -1;
        } catch (TimeoutException e) {
            f.cancel(true);
            throw e;
        }
    }

    private static int simulateNow(Block block, EnumFacing face, float yaw) throws Exception {
        if (world == null) {
            world = new ProbeWorld();
            placer = world.player();
        }
        BlockPos pos = ProbeWorld.center();
        // the block the player clicked on: placement code of wall/side mounted blocks looks for it
        BlockPos support = pos.func_177972_a(face.func_176734_d());
        IBlockState air = Blocks.field_150350_a.func_176223_P();
        placer.func_70012_b((double) pos.func_177958_n() + 0.5, (double) (pos.func_177956_o() - 1), (double) pos.func_177952_p() + 0.5, yaw, 0.0f);
        placer.field_70177_z = yaw;
        placer.field_70759_as = yaw;
        placer.field_70126_B = yaw;
        placer.field_70125_A = 0.0f;
        try {
            world.func_180501_a(pos, air, 2);
            world.func_180501_a(support, Blocks.field_150348_b.func_176223_P(), 2);
            IBlockState state = block.getStateForPlacement((World) world, pos, face, 0.5f, 0.5f, 0.5f, 0, (EntityLivingBase) placer, EnumHand.MAIN_HAND);
            if (state == null || state.func_177230_c() != block) {
                return -1;
            }
            world.func_180501_a(pos, state, 2);
            IBlockState current = world.func_180495_p(pos);
            if (current.func_177230_c() != block) {
                return -1;
            }
            hookMethod().invoke(block, world, pos, current, placer, new ItemStack(block));
            IBlockState after = world.func_180495_p(pos);
            if (after.func_177230_c() != block) {
                return -1;
            }
            return block.func_176201_c(after);
        } finally {
            try {
                world.func_180501_a(pos, air, 2);
                world.func_180501_a(support, air, 2);
            } catch (Throwable t) {
                // next block starts from a fresh world if this one is broken
                world = null;
                placer = null;
            }
        }
    }

    private static synchronized Method hookMethod() {
        if (!hookSearched) {
            hookSearched = true;
            for (String name : new String[]{"func_180633_a", "onBlockPlacedBy"}) {
                try {
                    hook = Block.class.getMethod(name, World.class, BlockPos.class, IBlockState.class, EntityLivingBase.class, ItemStack.class);
                    break;
                } catch (Throwable t) {
                    hook = null;
                }
            }
        }
        return hook;
    }

    private static synchronized ExecutorService executor() {
        if (exec == null) {
            exec = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "modelport-placement");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return exec;
    }

    private static synchronized void reset() {
        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }
        world = null;
        placer = null;
    }

    /** After a capture run: bring the stored tables of all library blocks up to date. */
    public static void refreshAll() {
        try {
            PortedLibrary lib = PortedRegistry.library();
            if (lib == null) {
                return;
            }
            for (String name : new java.util.ArrayList<String>(lib.names())) {
                refresh(lib, lib.get(name));
            }
        } catch (Throwable t) {
            // best effort, stored tables stay as they were
        }
    }

    private static void refresh(PortedLibrary lib, PortedBlock block) {
        try {
            if (block == null || block.reg == null) {
                return;
            }
            int[] fresh = PortedPlacement.get(block.reg);
            if (fresh == null || fresh.length != 24 || Arrays.equals(fresh, block.placement)) {
                return;
            }
            block.placement = fresh;
            lib.rewriteJson(block);
            ModelPort.LOG.info("modelport: таблица установки {} обновлена", (Object) block.reg);
        } catch (Throwable t) {
            // best effort
        }
    }
}
