package dev.cataclysm.arsenal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
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
            }
            player.getItemCooldownManager().set(this, 160);
            return TypedActionResult.success(stack, world.isClient());
        }
    };
    @Override public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "solar_lance"), SOLAR_LANCE);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<PendingStrike> iterator = STRIKES.iterator();
            while (iterator.hasNext()) {
                PendingStrike strike = iterator.next();
                if (--strike.ticks <= 0) {
                    Vec3d p = strike.target;
                    strike.world.createExplosion(null, p.x, p.y, p.z, 7.0f, World.ExplosionSourceType.TNT);
                    iterator.remove();
                }
            }
        });
    }
    private static final class PendingStrike {
        final ServerWorld world;
        final Vec3d target;
        int ticks;
        PendingStrike(ServerWorld world, Vec3d target, int ticks) { this.world = world; this.target = target; this.ticks = ticks; }
    }
}