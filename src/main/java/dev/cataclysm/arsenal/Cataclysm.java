package dev.cataclysm.arsenal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Cataclysm implements ModInitializer {
    public static final String MOD_ID = "cataclysm";
    private static final List<PendingStrike> STRIKES = new ArrayList<>();
    public static final Item SOLAR_LANCE = new Item(new Item.Settings().maxCount(1)) {
        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
            ItemStack stack = player.getStackInHand(hand);
            if (player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
            Vec3d from = player.getCameraPosVec(1.0f);
            Vec3d to = from.add(player.getRotationVec(1.0f).multiply(110));
            BlockHitResult hit = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            Vec3d target = hit.getType() == HitResult.Type.MISS ? to : hit.getPos();
            if (world.isClient) {
                // Client rendering is triggered by a dedicated client-side item hook.
            } else if (world instanceof ServerWorld serverWorld) {
                STRIKES.add(new PendingStrike(serverWorld, target, 64));
                serverWorld.playSound(null, BlockPos.ofFloored(target), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.6f);
            }
            player.getItemCooldownManager().set(this, 240);
            return TypedActionResult.success(stack, world.isClient());
        }
    };
    @Override public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "solar_lance"), SOLAR_LANCE);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<PendingStrike> iterator = STRIKES.iterator();
            while (iterator.hasNext()) {
                PendingStrike strike = iterator.next();
                if (strike.ticks > 0 && strike.ticks % 12 == 0) {
                    strike.world.spawnParticles(ParticleTypes.END_ROD, strike.target.x, strike.target.y + 2, strike.target.z, 18, 3.5, 2.0, 3.5, 0.08);
                    strike.world.playSound(null, BlockPos.ofFloored(strike.target), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.75f, 0.7f + (64-strike.ticks)*0.012f);
                }
                if (--strike.ticks == 0) {
                    Vec3d p = strike.target;
                    strike.world.createExplosion(null, p.x, p.y, p.z, 9.0f, World.ExplosionSourceType.TNT);
                    strike.world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 1, p.z, 1, 0, 0, 0, 0);
                    strike.world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 2, p.z, 130, 7, 4, 7, 0.35);
                    strike.world.playSound(null, BlockPos.ofFloored(p), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 3.0f, 0.55f);
                    strike.prepareCrater();
                    strike.ticks = -1;
                } else if (strike.ticks < 0) {
                    if (strike.digCrater()) iterator.remove();
                }
            }
        });
    }
    private static final class PendingStrike {
        final ServerWorld world;
        final Vec3d target;
        int ticks;
        int index;
        final List<BlockPos> blocks = new ArrayList<>();
        void prepareCrater() {
            BlockPos center = BlockPos.ofFloored(target);
            for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) {
                double radius = Math.sqrt(x * x + z * z);
                if (radius > 16) continue;
                int depth = (int)Math.ceil(9 * (1 - radius * radius / 256));
                for (int y = -depth; y <= 2; y++) blocks.add(center.add(x, y, z));
            }
        }
        boolean digCrater() {
            int budget = 200;
            while (index < blocks.size() && budget-- > 0) {
                BlockPos p = blocks.get(index++);
                if (world.isChunkLoaded(p) && world.isInBuildLimit(p) && !world.getBlockState(p).isOf(Blocks.BEDROCK) && !world.getBlockState(p).isAir() && !world.getBlockState(p).hasBlockEntity())
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            }
            return index >= blocks.size();
        }
        PendingStrike(ServerWorld world, Vec3d target, int ticks) { this.world = world; this.target = target; this.ticks = ticks; }
    }
}