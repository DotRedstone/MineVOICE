package dev.minevoice.neoforge.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.minevoice.neoforge.client.MineVoiceClientBootstrap;
import dev.minevoice.neoforge.client.VoiceSpeakerTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public final class ProximityEdgeIndicators {

    
    private static int getTeamColor(UUID playerId, UUID myGroupId) {
        dev.minevoice.neoforge.client.VoicePlayerDirectory directory = MineVoiceClientBootstrap.voiceDirectory();
        if (directory != null) {
            dev.minevoice.neoforge.network.VoiceRosterEntry entry = directory.get(playerId);
            if (entry != null && myGroupId != null && myGroupId.equals(entry.groupId())) {
                return entry.groupColor();
            }
        }
        return 0xAAAAAA; // Gray for public
    }

    private static void drawTriangle(GuiGraphics graphics, float x, float y, float angle, float size, int color, float opacity) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        com.mojang.blaze3d.vertex.PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(angle));
        
        com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        org.joml.Matrix4f matrix = poseStack.last().pose();
        
        float halfSize = size / 1.5f;
        vertexConsumer.addVertex(matrix, size, 0, 0).setColor(r, g, b, opacity);
        vertexConsumer.addVertex(matrix, -halfSize, halfSize, 0).setColor(r, g, b, opacity);
        vertexConsumer.addVertex(matrix, -halfSize, -halfSize, 0).setColor(r, g, b, opacity);
        
        graphics.bufferSource().endBatch();
        poseStack.popPose();
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        dev.minevoice.neoforge.client.ClientAudioSettings settings = dev.minevoice.neoforge.client.MineVoiceClientBootstrap.settings();
        int outOfSightMode = settings.outOfSightIndicatorMode();
        int occludedMode = settings.occludedIndicatorMode();

        VoiceSpeakerTracker tracker = MineVoiceClientBootstrap.speakerTracker();
        List<UUID> activeSpeakers = tracker.activeSpeakers(20);
        if (activeSpeakers.isEmpty()) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        Vector3f forward = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;
        
        float margin = 5.0f;
        float halfW = centerX - margin;
        float halfH = centerY - margin;

        double fovRad = Math.toRadians(minecraft.options.fov().get());
        double f = 1.0 / Math.tan(fovRad / 2.0);

        dev.minevoice.neoforge.client.VoicePlayerDirectory directory = MineVoiceClientBootstrap.voiceDirectory();
        UUID myGroupId = null;
        if (directory != null) {
            dev.minevoice.neoforge.network.VoiceRosterEntry myEntry = directory.get(minecraft.player.getUUID());
            if (myEntry != null) myGroupId = myEntry.groupId();
        }

        for (UUID speakerId : activeSpeakers) {
            if (speakerId.equals(minecraft.player.getUUID())) {
                continue;
            }

            if (myGroupId == null || directory == null) continue;
            dev.minevoice.neoforge.network.VoiceRosterEntry speakerEntry = directory.get(speakerId);
            if (speakerEntry == null || !myGroupId.equals(speakerEntry.groupId())) {
                continue;
            }

            net.minecraft.world.entity.player.Player speakerEntity = minecraft.level.getPlayerByUUID(speakerId);
            if (speakerEntity == null) {
                continue; 
            }

            Vec3 targetPos = speakerEntity.position().add(0, speakerEntity.getEyeHeight(), 0);
            Vec3 dir = targetPos.subtract(cameraPos);
            double distance = dir.length();
            if (distance < 0.1) continue;
            Vec3 dirToTarget = dir.normalize();
            
            float forwardDot = (float) dirToTarget.dot(new Vec3(forward.x(), forward.y(), forward.z()));
            float leftDot = (float) dirToTarget.dot(new Vec3(left.x(), left.y(), left.z()));
            float upDot = (float) dirToTarget.dot(new Vec3(up.x(), up.y(), up.z()));
            
            float size = (float) Mth.clamp(12.0f - (distance / 64.0f) * 6.0f, 6.0f, 12.0f);
            float opacity = (float) Mth.clamp(1.0f - (distance / 64.0f), 0.2f, 0.9f);
            
            float drawX = 0.0f;
            float drawY = 0.0f;

            float dx = -leftDot;
            float dy = -upDot;
            float angle = (float) Mth.atan2(dy, dx);
            
            boolean clampToEdge = false;

            if (forwardDot > 0) {
                double px = dx / forwardDot * f;
                double py = dy / forwardDot * f;
                
                float sx = centerX + (float) (px * centerY);
                float sy = centerY + (float) (py * centerY);

                if (sx < margin || sx > screenWidth - margin || sy < margin || sy > screenHeight - margin) {
                    clampToEdge = true;
                } else {
                    drawX = sx;
                    drawY = sy;
                }
            } else {
                clampToEdge = true;
            }

            boolean lineOfSightClear = minecraft.level.clip(new net.minecraft.world.level.ClipContext(
                cameraPos, targetPos, 
                net.minecraft.world.level.ClipContext.Block.VISUAL, 
                net.minecraft.world.level.ClipContext.Fluid.NONE, 
                minecraft.player
            )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;

            if (!clampToEdge && lineOfSightClear) {
                continue;
            }

            if (clampToEdge && outOfSightMode == 0) continue;
            if (!clampToEdge && !lineOfSightClear && occludedMode == 0) continue;

            if (clampToEdge) {
                float cosA = Mth.cos(angle);
                float sinA = Mth.sin(angle);
                
                float tx = Float.MAX_VALUE;
                if (Math.abs(cosA) > 0.001f) {
                    tx = halfW / Math.abs(cosA);
                }
                float ty = Float.MAX_VALUE;
                if (Math.abs(sinA) > 0.001f) {
                    ty = halfH / Math.abs(sinA);
                }
                
                float t = Math.min(tx, ty);
                drawX = centerX + cosA * t;
                drawY = centerY + sinA * t;
            }

            int color = getTeamColor(speakerId, myGroupId);

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // Draw Avatar
            ResourceLocation texture = DefaultPlayerSkin.get(speakerId).texture();
            if (minecraft.getConnection() != null) {
                PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(speakerId);
                if (playerInfo != null) {
                    texture = playerInfo.getSkin().texture();
                }
            }

            // Draw Border
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            graphics.fill((int)(drawX - size / 2.0f - 1), (int)(drawY - size / 2.0f - 1), (int)(drawX + size / 2.0f + 1), (int)(drawY + size / 2.0f + 1), 0);
            graphics.fill((int)(drawX - size / 2.0f - 1), (int)(drawY - size / 2.0f - 1), (int)(drawX + size / 2.0f + 1), (int)(drawY + size / 2.0f + 1), ((int)(opacity * 255) << 24) | color);

            graphics.setColor(1.0f, 1.0f, 1.0f, opacity);
            net.minecraft.client.gui.components.PlayerFaceRenderer.draw(
                    graphics,
                    texture,
                    (int) (drawX - size / 2.0f),
                    (int) (drawY - size / 2.0f),
                    (int) size
            );
            
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            
            poseStack.popPose();
        }
    }
}
