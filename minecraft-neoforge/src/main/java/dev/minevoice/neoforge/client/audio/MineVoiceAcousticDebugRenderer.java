package dev.minevoice.neoforge.client.audio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws client-side acoustic probe and reflection lines after the world particles render stage. */
public final class MineVoiceAcousticDebugRenderer {
    private MineVoiceAcousticDebugRenderer() {
    }

    public static void render(RenderLevelStageEvent event, AcousticDebugSnapshot snapshot) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || !snapshot.enabled()
                || snapshot.lines().isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        PoseStack.Pose transform = poseStack.last();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        for (AcousticDebugSnapshot.Line line : snapshot.lines()) {
            drawLine(consumer, transform, line);
        }
        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void drawLine(
            VertexConsumer consumer,
            PoseStack.Pose transform,
            AcousticDebugSnapshot.Line line
    ) {
        consumer.addVertex(transform.pose(), (float) line.from().x, (float) line.from().y, (float) line.from().z)
                .setColor(line.red(), line.green(), line.blue(), line.alpha())
                .setNormal(transform, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(transform.pose(), (float) line.to().x, (float) line.to().y, (float) line.to().z)
                .setColor(line.red(), line.green(), line.blue(), line.alpha())
                .setNormal(transform, 0.0F, 1.0F, 0.0F);
    }
}
