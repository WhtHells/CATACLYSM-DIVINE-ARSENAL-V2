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
    private static final int MAX_VISUALS = 3;
    public static void beginStrike(Vec3d target) { if (VISUALS.size() >= MAX_VISUALS) VISUALS.remove(0); VISUALS.add(new StrikeVisual(target, System.nanoTime())); StrikeCinematicHud.begin(target); }

    @Override public void onInitializeClient() {
        StrikeCinematicHud.register();
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
            while (iterator.hasNext()) if ((now - iterator.next().started) / 1e9 > 8.0) iterator.remove();
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
        double charge=smooth(0.25,1.8,t)*(1-smooth(3.1,3.5,t));
        if (charge>0) {
            orbitalStation(b,m,x,y+70,z,t,(float)charge);
        }
        if (charge>0) for(int i=0;i<3;i++)
            ring(b,m,x,y+68+i*2,z,6+i*3,0.25,0.4,0.72,1,(float)(charge*0.7),t*(i%2==0?1:-1),64);
        if(charge>0) {
            for(int arm=0;arm<12;arm++) {
                double a=arm*Math.PI/6+t*0.15;
                double inner=8,outer=19;
                double sx=x+Math.cos(a)*inner,sz=z+Math.sin(a)*inner;
                double ex=x+Math.cos(a)*outer,ez=z+Math.sin(a)*outer;
                double h=y+69+Math.sin(arm*2.4+t)*1.5;
                quad(b,m,sx,h-0.8,sz,ex,h-1.5,ez,ex,h+1.5,ez,sx,h+0.8,sz,0.1,0.45,0.8,(float)(charge*0.8));
            }
            for(int k=0;k<3;k++) ring(b,m,x,y+64+k*5,z,18+k*2.5,0.4,0.1,0.9,1,(float)(charge*0.8),-t*(k+1)*0.4,72);
        }
        double beam=smooth(3.12,3.25,t)*(1-smooth(4.05,4.4,t));
        if(beam>0) {
            double pulse=1+0.07*Math.sin(t*35);
            cylinder(b,m,x,y,z,12*pulse,76,0.35,0.03,0.09,(float)(0.10*beam),28);
            cylinder(b,m,x,y,z,9*pulse,76,1,0.07,0.14,(float)(0.15*beam),28);
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
        double impact=smooth(3.2,3.4,t)*(1-smooth(5.2,7.0,t));
        if(impact>0) {
            double redPulse = 1-smooth(3.3,4.8,t);
            if(redPulse>0) {
                cylinder(b,m,x,y,z,10+ageForImpact(t)*8,68,0.9,0.04,0.08,(float)(impact*redPulse*0.15),32);
            }
            double age=Math.max(0,t-3.25), radius=2+age*19;
            ring(b,m,x,y+0.25,z,radius,0.35+age*0.18,0.65,0.87,1,(float)(impact*0.7),0,72);
            ring(b,m,x,y+0.3,z,radius*0.75,0.25,0.9,0.95,1,(float)(impact*0.6),-t,72);
            ring(b,m,x,y+0.4,z,radius*1.1,0.2,0.25,0.75,1,(float)(impact*0.4),t,72);
            for(int i=0;i<24;i++) {
                double a=i*Math.PI/12, rr=radius*(0.8+0.2*Math.sin(i*7.1));
                vertical(b,m,x+Math.cos(a)*rr,y,z+Math.sin(a)*rr,0.16,0.5+age*2.1,0.55,0.63,0.7,(float)(impact*0.14));
            }
        }
    }

    // Procedural hard-surface orbital platform. All geometry is emitted as batched
    // colored quads; no extra textures, draw calls or third-party render libraries.
    private static double ageForImpact(double t) { return Math.max(0,t-3.25); }
    private static void orbitalStation(BufferBuilder b, Matrix4f m, double x, double y, double z, double t, float alpha) {
        double spin=t*0.22;
        // Hexagonal segmented armored torus: 16 hull plates, independent emissive seams.
        for (int i=0;i<16;i++) {
            double a=2*Math.PI*i/16+spin;
            double a2=2*Math.PI*(i+0.82)/16+spin;
            annularPanel(b,m,x,y,z,9.2,14.5,a,a2,0.12,0.2,0.3,alpha*0.85f);
            annularPanel(b,m,x,y+0.08,z,11.65,12.05,a,a2,0.18,0.75,1,alpha);
            // Outer mechanical pylons have a distinct silhouette from a glowing ring.
            double px=x+Math.cos((a+a2)/2)*15.5, pz=z+Math.sin((a+a2)/2)*15.5;
            cuboid(b,m,px-0.7,y-2.8,pz-0.7,px+0.7,y+2.8,pz+0.7,0.13,0.2,0.29,alpha);
            cuboid(b,m,px-0.34,y-2.9,pz-0.34,px+0.34,y+2.9,pz+0.34,0.25,0.85,1,alpha*0.85f);
        }
        // Three concentric gyroscopic rails at distinct altitudes.
        for(int k=0;k<3;k++) {
            double rr=4.5+k*2.25;
            ring(b,m,x,y-4+k*2,z,rr,0.34,0.15,0.68,0.95,alpha,spin*(k%2==0?3:-4),48);
            ring(b,m,x,y-3.85+k*2,z,rr+0.52,0.11,0.75,0.95,1,alpha*0.8f,-spin*2,48);
        }
        // Reactor housing, 8 large faceted cooling vanes.
        cuboid(b,m,x-2.3,y-2.1,z-2.3,x+2.3,y+2.1,z+2.3,0.08,0.14,0.22,alpha);
        cuboid(b,m,x-1.35,y-2.4,z-1.35,x+1.35,y+2.4,z+1.35,0.25,0.78,1,alpha);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4-spin*1.8;
            double dx=Math.cos(a),dz=Math.sin(a);
            double px=x+dx*5.6,pz=z+dz*5.6;
            cuboid(b,m,px-0.45,y-1.8,pz-0.45,px+0.45,y+1.8,pz+0.45,0.14,0.27,0.39,alpha);
            vertical(b,m,px,y-2,pz,0.14,4,0.3,0.9,1,alpha*0.7f);
        }
        // Eight animated emitter lenses.
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4+spin*2;
            double px=x+Math.cos(a)*3.7,pz=z+Math.sin(a)*3.7;
            cuboid(b,m,px-0.55,y-0.8,pz-0.55,px+0.55,y+0.8,pz+0.55,0.5,0.9,1,alpha*0.8f);
        }
    }
    private static void annularPanel(BufferBuilder b,Matrix4f m,double x,double y,double z,double inner,double outer,double a,double a2,double r,double g,double blue,float alpha) {
        quad(b,m,x+Math.cos(a)*inner,y,z+Math.sin(a)*inner,
            x+Math.cos(a2)*inner,y,z+Math.sin(a2)*inner,
            x+Math.cos(a2)*outer,y,z+Math.sin(a2)*outer,
            x+Math.cos(a)*outer,y,z+Math.sin(a)*outer,r,g,blue,alpha);
    }
    private static void cuboid(BufferBuilder b,Matrix4f m,double x1,double y1,double z1,double x2,double y2,double z2,double r,double g,double blue,float alpha) {
        quad(b,m,x1,y1,z1,x2,y1,z1,x2,y2,z1,x1,y2,z1,r,g,blue,alpha);
        quad(b,m,x2,y1,z2,x1,y1,z2,x1,y2,z2,x2,y2,z2,r,g,blue,alpha);
        quad(b,m,x1,y1,z2,x1,y1,z1,x1,y2,z1,x1,y2,z2,r,g,blue,alpha);
        quad(b,m,x2,y1,z1,x2,y1,z2,x2,y2,z2,x2,y2,z1,r,g,blue,alpha);
        quad(b,m,x1,y2,z1,x2,y2,z1,x2,y2,z2,x1,y2,z2,r,g,blue,alpha);
        quad(b,m,x1,y1,z2,x2,y1,z2,x2,y1,z1,x1,y1,z1,r,g,blue,alpha);
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