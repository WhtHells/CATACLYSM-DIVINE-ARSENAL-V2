package dev.cataclysm.arsenal;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CataclysmClient implements ClientModInitializer {
    private static final List<StrikeVisual> VISUALS = new ArrayList<>();
    public static void beginStrike(Vec3d target) { VISUALS.add(new StrikeVisual(target, System.nanoTime())); }

    @Override public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient && player.getStackInHand(hand).isOf(Cataclysm.SOLAR_LANCE)
                && !player.getItemCooldownManager().isCoolingDown(Cataclysm.SOLAR_LANCE)) {
                Vec3d from = player.getCameraPosVec(1.0f);
                Vec3d to = from.add(player.getRotationVec(1.0f).multiply(110));
                BlockHitResult hit = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
                beginStrike(hit.getType() == HitResult.Type.MISS ? to : hit.getPos());
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) { VISUALS.clear(); return; }
            long now = System.nanoTime();
            Iterator<StrikeVisual> iterator = VISUALS.iterator();
            while (iterator.hasNext()) if ((now - iterator.next().started) / 1e9 > 6.2) iterator.remove();
            if (VISUALS.isEmpty()) return;
            MatrixStack matrices = context.matrixStack();
            Vec3d camera = context.camera().getPos();
            matrices.push();
            matrices.translate(-camera.x, -camera.y, -camera.z);
            Matrix4f transform = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (StrikeVisual visual : VISUALS)
                draw(buffer, transform, visual.target, (now - visual.started) / 1e9);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            matrices.pop();
        });
    }
    private static void draw(BufferBuilder b, Matrix4f m, Vec3d target, double t) {
        double x=target.x,y=target.y+0.08,z=target.z;
        double aim=smooth(0,0.5,t)*(1-smooth(2.6,3.3,t));
        if (aim>0) for(int i=0;i<3;i++)
            ring(b,m,x,y+i*0.15,z,4+i*2.7,0.065,0.14,0.62,1,(float)(aim*0.5),t*(i%2==0?1:-1),64);
        double charge=smooth(1.6,2.8,t)*(1-smooth(3.1,3.5,t));
        if (charge>0) for(int i=0;i<3;i++)
            ring(b,m,x,y+68+i*2,z,6+i*3,0.1,0.4,0.72,1,(float)(charge*0.6),t,64);
        double beam=smooth(3.12,3.25,t)*(1-smooth(4.05,4.4,t));
        if(beam>0) {
            double pulse=1+0.07*Math.sin(t*35);
            cylinder(b,m,x,y,z,7*pulse,76,0.12,0.4,1,(float)(0.11*beam),28);
            cylinder(b,m,x,y,z,4*pulse,76,0.35,0.7,1,(float)(0.22*beam),28);
            cylinder(b,m,x,y,z,1.6*pulse,76,0.9,0.95,1,(float)(0.55*beam),28);
            cylinder(b,m,x,y,z,0.55*pulse,76,1,1,1,(float)(0.95*beam),28);
            for(int j=0;j<4;j++) for(int i=0;i<28;i++) {
                double h=i*2.6, angle=j*1.57+h*0.21+t*(j%2==0?5:-4);
                double radius=3.3+0.5*Math.sin(h*0.2+t*3);
                vertical(b,m,x+Math.cos(angle)*radius,y+h,z+Math.sin(angle)*radius,0.09,2.2,0.5,0.8,1,(float)(beam*0.4));
            }
        }
        double impact=smooth(3.2,3.4,t)*(1-smooth(4.5,5.8,t));
        if(impact>0) {
            double age=Math.max(0,t-3.25), radius=2+age*22;
            ring(b,m,x,y+0.25,z,radius,0.35+age*0.18,0.65,0.87,1,(float)(impact*0.7),0,72);
            for(int i=0;i<24;i++) {
                double a=i*Math.PI/12, rr=radius*(0.8+0.2*Math.sin(i*7.1));
                vertical(b,m,x+Math.cos(a)*rr,y,z+Math.sin(a)*rr,0.16,0.5+age*2.1,0.55,0.63,0.7,(float)(impact*0.14));
            }
        }
    }
    private static double smooth(double a,double b,double x) {
        double v=Math.max(0,Math.min(1,(x-a)/(b-a)));
        return v*v*(3-2*v);
    }
    private static void vertex(BufferBuilder b,Matrix4f m,double x,double y,double z,double r,double g,double blue,float alpha) {
        b.vertex(m,(float)x,(float)y,(float)z).color((float)r,(float)g,(float)blue,alpha).next();
    }
    private static void quad(BufferBuilder b,Matrix4f m,double x1,double y1,double z1,double x2,double y2,double z2,double x3,double y3,double z3,double x4,double y4,double z4,double r,double g,double blue,float alpha) {
        vertex(b,m,x1,y1,z1,r,g,blue,alpha);
        vertex(b,m,x2,y2,z2,r,g,blue,alpha);
        vertex(b,m,x3,y3,z3,r,g,blue,alpha);
        vertex(b,m,x4,y4,z4,r,g,blue,alpha);
    }
    private static void cylinder(BufferBuilder b,Matrix4f m,double x,double y,double z,double radius,double height,double r,double g,double blue,float alpha,int sides) {
        for(int i=0;i<sides;i++) {
            double a=i*2*Math.PI/sides,c=(i+1)*2*Math.PI/sides;
            double x1=x+Math.cos(a)*radius,z1=z+Math.sin(a)*radius;
            double x2=x+Math.cos(c)*radius,z2=z+Math.sin(c)*radius;
            quad(b,m,x1,y,z1,x2,y,z2,x2,y+height,z2,x1,y+height,z1,r,g,blue,alpha);
        }
    }
    private static void ring(BufferBuilder b,Matrix4f m,double x,double y,double z,double radius,double width,double r,double g,double blue,float alpha,double spin,int sides) {
        for(int i=0;i<sides;i++) {
            double a=i*2*Math.PI/sides+spin,c=(i+1)*2*Math.PI/sides+spin;
            double ri=radius-width/2,ro=radius+width/2;
            quad(b,m,x+Math.cos(a)*ri,y,z+Math.sin(a)*ri,
                x+Math.cos(c)*ri,y,z+Math.sin(c)*ri,
                x+Math.cos(c)*ro,y,z+Math.sin(c)*ro,
                x+Math.cos(a)*ro,y,z+Math.sin(a)*ro,r,g,blue,alpha);
        }
    }
    private static void vertical(BufferBuilder b,Matrix4f m,double x,double y,double z,double w,double h,double r,double g,double blue,float alpha) {
        quad(b,m,x-w,y,z,x+w,y,z,x+w,y+h,z,x-w,y+h,z,r,g,blue,alpha);
        quad(b,m,x,y,z-w,x,y,z+w,x,y+h,z+w,x,y+h,z-w,r,g,blue,alpha);
    }
    private record StrikeVisual(Vec3d target,long started) {}
}