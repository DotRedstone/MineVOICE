package dev.minevoice.neoforge.client.audio;

import com.mojang.logging.LogUtils;
import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.neoforge.client.ClientAudioSettings;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-speaker OpenAL playback with an optional EFX direct-filter and room-reverb path.
 */
public final class OpenAlVoicePlaybackBackend implements VoicePlaybackBackend {
    public static final String NAME = "openal";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int AL_FALSE = 0;
    private static final int AL_TRUE = 1;
    private static final int AL_NO_ERROR = 0;
    private static final int AL_FORMAT_MONO16 = 0x1101;
    private static final int AL_POSITION = 0x1004;
    private static final int AL_DIRECTION = 0x1005;
    private static final int AL_GAIN = 0x100A;
    private static final int AL_CONE_INNER_ANGLE = 0x1001;
    private static final int AL_CONE_OUTER_ANGLE = 0x1002;
    private static final int AL_CONE_OUTER_GAIN = 0x1022;
    private static final int AL_ORIENTATION = 0x100F;
    private static final int AL_SOURCE_STATE = 0x1010;
    private static final int AL_PLAYING = 0x1012;
    private static final int AL_BUFFERS_QUEUED = 0x1015;
    private static final int AL_BUFFERS_PROCESSED = 0x1016;
    private static final int AL_SOURCE_RELATIVE = 0x202;
    private static final int AL_ROLLOFF_FACTOR = 0x1021;
    private static final int AL_FILTER_TYPE = 0x8001;
    private static final int AL_FILTER_LOWPASS = 0x0001;
    private static final int AL_LOWPASS_GAIN = 0x0001;
    private static final int AL_LOWPASS_GAINHF = 0x0002;
    private static final int AL_EFFECT_TYPE = 0x8001;
    private static final int AL_EFFECT_REVERB = 0x0001;
    private static final int AL_REVERB_DENSITY = 0x0001;
    private static final int AL_REVERB_DIFFUSION = 0x0002;
    private static final int AL_REVERB_GAIN = 0x0003;
    private static final int AL_REVERB_GAINHF = 0x0004;
    private static final int AL_REVERB_DECAY_TIME = 0x0005;
    private static final int AL_REVERB_DECAY_HFRATIO = 0x0006;
    private static final int AL_EFFECTSLOT_EFFECT = 0x0001;
    private static final int AL_DIRECT_FILTER = 0x20005;
    private static final int AL_AUXILIARY_SEND_FILTER = 0x20006;

    private final long device;
    private final long context;
    private final Map<UUID, SourceState> sources = new HashMap<>();
    private ByteBuffer transferBuffer = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder());
    private FloatBuffer orientationBuffer = ByteBuffer.allocateDirect(Float.BYTES * 6).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private boolean efxEnabled;
    private int auxiliarySlot = -1;
    private int reverbEffect = -1;
    private float appliedReverbGain = -1.0F;
    private float appliedReverbDecay = -1.0F;
    private boolean closed;

    private OpenAlVoicePlaybackBackend(long device, long context) {
        this.device = device;
        this.context = context;
    }

    public static boolean available() {
        return classPresent("org.lwjgl.openal.AL")
                && classPresent("org.lwjgl.openal.ALC")
                && classPresent("org.lwjgl.openal.AL10")
                && classPresent("org.lwjgl.openal.ALC10");
    }

    public static OpenAlVoicePlaybackBackend open(ClientAudioSettings settings) {
        if (!available()) {
            throw new UnsupportedOperationException("LWJGL OpenAL classes are unavailable");
        }
        long device = alcOpenDevice();
        if (device == 0L) {
            throw new IllegalStateException("failed to open OpenAL device");
        }
        long context = 0L;
        try {
            IntBuffer attrs = null;
            if (settings != null && settings.hrtfEnabled()) {
                if (alcIsExtensionPresent(device, "ALC_SOFT_HRTF")) {
                    ByteBuffer bb = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder());
                    attrs = bb.asIntBuffer();
                    attrs.put(new int[] { 0x1992, 1, 0 });
                    attrs.flip();
                    LOGGER.info("MineVOICE OpenAL HRTF: Requested ALC_SOFT_HRTF (Enabled)");
                } else {
                    LOGGER.warn("MineVOICE OpenAL HRTF: ALC_SOFT_HRTF extension is not supported by this device");
                }
            } else if (settings != null && !settings.hrtfEnabled()) {
                if (alcIsExtensionPresent(device, "ALC_SOFT_HRTF")) {
                    ByteBuffer bb = ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder());
                    attrs = bb.asIntBuffer();
                    attrs.put(new int[] { 0x1992, 0, 0 });
                    attrs.flip();
                    LOGGER.info("MineVOICE OpenAL HRTF: Requested ALC_SOFT_HRTF (Disabled)");
                }
            }
            context = alcCreateContext(device, attrs);
            if (context == 0L || !alcMakeContextCurrent(context)) {
                throw new IllegalStateException("failed to create OpenAL context");
            }
            createCapabilities(device);
            checkError("OpenAL capabilities");
            OpenAlVoicePlaybackBackend backend = new OpenAlVoicePlaybackBackend(device, context);
            backend.initializeEfx();
            LOGGER.info("MineVOICE OpenAL playback initialized: efx={}", backend.efxEnabled);
            return backend;
        } catch (RuntimeException exception) {
            if (context != 0L) {
                alcDestroyContext(context);
            }
            alcCloseDevice(device);
            throw exception;
        }
    }

    @Override
    public String backendName() {
        return efxEnabled ? NAME + "-efx" : NAME;
    }

    @Override
    public void start() {
        makeContextCurrent();
    }

    @Override
    public void writeStereoFrame(byte[] pcm, int offset, int length) {
        throw new UnsupportedOperationException("OpenAL backend expects per-source mono frames");
    }

    @Override
    public boolean matches(ClientAudioSettings settings) {
        return !closed && NAME.equals(VoicePlaybackBackendFactory.normalizeBackendName(settings.audioPlaybackBackend()));
    }

    @Override
    public boolean supportsSourcePlayback() {
        return true;
    }

    @Override
    public void updateListener(VoiceListenerSnapshot listener) {
        makeContextCurrent();
        alListener3f(AL_POSITION, (float) listener.x(), (float) listener.y(), (float) listener.z());
        float yawRad = (float) Math.toRadians(listener.yaw());
        float pitchRad = (float) Math.toRadians(listener.pitch());
        float atX = -((float) (Math.sin(yawRad) * Math.cos(pitchRad)));
        float atY = -((float) Math.sin(pitchRad));
        float atZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        
        float upPitchRad = (float) Math.toRadians(listener.pitch() - 90.0f);
        float upX = -((float) (Math.sin(yawRad) * Math.cos(upPitchRad)));
        float upY = -((float) Math.sin(upPitchRad));
        float upZ = (float) (Math.cos(yawRad) * Math.cos(upPitchRad));
        
        orientationBuffer.clear();
        orientationBuffer.put(atX).put(atY).put(atZ).put(upX).put(upY).put(upZ).flip();
        alListenerfv(AL_ORIENTATION, orientationBuffer);
        if (efxEnabled) {
            applyRoomReverb(listener);
        }
    }

    @Override
    public void writeSourceFrame(UUID speakerId, VoiceChannel channel, VoiceSourceSnapshot source, byte[] pcm, int sampleRate, float gain) {
        makeContextCurrent();
        SourceState state = sources.computeIfAbsent(speakerId, ignored -> createSource());
        cleanupProcessedBuffers(state);
        boolean positional = channel == VoiceChannel.PROXIMITY && source.known();
        alSourcei(state.source, AL_SOURCE_RELATIVE, positional ? AL_FALSE : AL_TRUE);
        if (positional) {
            alSource3f(state.source, AL_POSITION, (float) source.x(), (float) source.y(), (float) source.z());
            float yawRad = (float) Math.toRadians(source.yaw());
            float pitchRad = (float) Math.toRadians(source.pitch());
            float dirX = -((float) (Math.sin(yawRad) * Math.cos(pitchRad)));
            float dirY = -((float) Math.sin(pitchRad));
            float dirZ = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
            alSource3f(state.source, AL_DIRECTION, dirX, dirY, dirZ);
            alSourcef(state.source, AL_CONE_INNER_ANGLE, 120.0F);
            alSourcef(state.source, AL_CONE_OUTER_ANGLE, 240.0F);
            alSourcef(state.source, AL_CONE_OUTER_GAIN, 0.15F);
        } else {
            alSource3f(state.source, AL_POSITION, 0.0F, 0.0F, 0.0F);
            alSource3f(state.source, AL_DIRECTION, 0.0F, 0.0F, 0.0F);
            alSourcef(state.source, AL_CONE_INNER_ANGLE, 360.0F);
            alSourcef(state.source, AL_CONE_OUTER_ANGLE, 360.0F);
            alSourcef(state.source, AL_CONE_OUTER_GAIN, 1.0F);
        }
        alSourcef(state.source, AL_GAIN, clamp(gain, 0.0F, 2.0F));
        if (efxEnabled) {
            applySourceEffects(state, source);
        }

        Integer freeBuffer = state.freeBuffers.poll();
        int buffer = freeBuffer != null ? freeBuffer : alGenBuffers();
        if (transferBuffer.capacity() < pcm.length) {
            transferBuffer = ByteBuffer.allocateDirect(pcm.length).order(ByteOrder.nativeOrder());
        }
        transferBuffer.clear();
        transferBuffer.put(pcm).flip();
        alBufferData(buffer, AL_FORMAT_MONO16, transferBuffer, sampleRate);
        alSourceQueueBuffers(state.source, buffer);
        state.queuedBuffers.add(buffer);
        if (alGetSourcei(state.source, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(state.source);
        }
        checkError("OpenAL source write");
    }

    @Override
    public void retainSources(Set<UUID> activeSpeakers) {
        makeContextCurrent();
        Iterator<Map.Entry<UUID, SourceState>> iterator = sources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SourceState> entry = iterator.next();
            if (!activeSpeakers.contains(entry.getKey())) {
                deleteSource(entry.getValue());
                iterator.remove();
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            makeContextCurrent();
            for (SourceState source : sources.values()) {
                deleteSource(source);
            }
            sources.clear();
            closeEfx();
        } finally {
            alcMakeContextCurrent(0L);
            alcDestroyContext(context);
            alcCloseDevice(device);
        }
    }

    private void initializeEfx() {
        if (!classPresent("org.lwjgl.openal.EXTEfx")) {
            return;
        }
        try {
            auxiliarySlot = alGenAuxiliaryEffectSlots();
            reverbEffect = alGenEffects();
            alEffecti(reverbEffect, AL_EFFECT_TYPE, AL_EFFECT_REVERB);
            alAuxiliaryEffectSloti(auxiliarySlot, AL_EFFECTSLOT_EFFECT, reverbEffect);
            checkError("OpenAL EFX initialization");
            efxEnabled = true;
        } catch (RuntimeException exception) {
            closeEfx();
            LOGGER.info("MineVOICE OpenAL EFX is unavailable; continuing with positional sources only: {}", exception.getMessage());
        }
    }

    private void applyRoomReverb(VoiceListenerSnapshot listener) {
        float gain = listener.environmentKnown() ? clamp(listener.reverbGain(), 0.0F, 0.65F) : 0.0F;
        float decay = listener.environmentKnown() ? clamp(listener.reverbDecaySeconds(), 0.25F, 2.5F) : 0.7F;
        if (Math.abs(gain - appliedReverbGain) < 0.01F && Math.abs(decay - appliedReverbDecay) < 0.02F) {
            return;
        }
        alEffectf(reverbEffect, AL_REVERB_DENSITY, clamp(gain * 2.0F, 0.0F, 1.0F));
        alEffectf(reverbEffect, AL_REVERB_DIFFUSION, 0.72F);
        alEffectf(reverbEffect, AL_REVERB_GAIN, gain);
        alEffectf(reverbEffect, AL_REVERB_GAINHF, 0.72F);
        alEffectf(reverbEffect, AL_REVERB_DECAY_TIME, decay);
        alEffectf(reverbEffect, AL_REVERB_DECAY_HFRATIO, 0.65F);
        alAuxiliaryEffectSloti(auxiliarySlot, AL_EFFECTSLOT_EFFECT, reverbEffect);
        checkError("OpenAL room reverb update");
        appliedReverbGain = gain;
        appliedReverbDecay = decay;
    }

    private void applySourceEffects(SourceState state, VoiceSourceSnapshot source) {
        alFilterf(state.directFilter, AL_LOWPASS_GAIN, 1.0F);
        alFilterf(state.directFilter, AL_LOWPASS_GAINHF, clamp(source.highFrequencyGain(), 0.02F, 1.0F));
        alSourcei(state.source, AL_DIRECT_FILTER, state.directFilter);
        alFilterf(state.reverbFilter, AL_LOWPASS_GAIN, clamp(source.reflectionGain(), 0.0F, 0.75F));
        alFilterf(state.reverbFilter, AL_LOWPASS_GAINHF, 1.0F);
        alSource3i(state.source, AL_AUXILIARY_SEND_FILTER, auxiliarySlot, 0, state.reverbFilter);
    }

    private SourceState createSource() {
        int source = alGenSources();
        alSourcef(source, AL_ROLLOFF_FACTOR, 0.0F);
        if (!efxEnabled) {
            return new SourceState(source, -1, -1);
        }
        int directFilter = alGenFilters();
        alFilteri(directFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        int reverbFilter = alGenFilters();
        alFilteri(reverbFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        return new SourceState(source, directFilter, reverbFilter);
    }

    private void cleanupProcessedBuffers(SourceState state) {
        int processed = alGetSourcei(state.source, AL_BUFFERS_PROCESSED);
        while (processed > 0) {
            int buffer = alSourceUnqueueBuffers(state.source);
            state.queuedBuffers.remove((Integer) buffer);
            state.freeBuffers.add(buffer);
            processed--;
        }
    }

    private void deleteSource(SourceState state) {
        alSourceStop(state.source);
        int queued = alGetSourcei(state.source, AL_BUFFERS_QUEUED);
        for (int index = 0; index < queued; index++) {
            int buffer = alSourceUnqueueBuffers(state.source);
            alDeleteBuffers(buffer);
        }
        for (Integer buffer : state.freeBuffers) {
            alDeleteBuffers(buffer);
        }
        state.freeBuffers.clear();
        state.queuedBuffers.clear();
        if (state.directFilter >= 0) {
            alDeleteFilters(state.directFilter);
        }
        if (state.reverbFilter >= 0) {
            alDeleteFilters(state.reverbFilter);
        }
        alDeleteSources(state.source);
    }

    private void closeEfx() {
        if (reverbEffect >= 0) {
            alDeleteEffects(reverbEffect);
            reverbEffect = -1;
        }
        if (auxiliarySlot >= 0) {
            alDeleteAuxiliaryEffectSlots(auxiliarySlot);
            auxiliarySlot = -1;
        }
        efxEnabled = false;
        appliedReverbGain = -1.0F;
        appliedReverbDecay = -1.0F;
    }

    private void makeContextCurrent() {
        if (!alcMakeContextCurrent(context)) {
            throw new IllegalStateException("failed to make OpenAL context current");
        }
        createCapabilities(device);
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, OpenAlVoicePlaybackBackend.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long alcOpenDevice() {
        return (Long) invoke("org.lwjgl.openal.ALC10", "alcOpenDevice", new Class<?>[]{ByteBuffer.class}, new Object[]{null});
    }

    private static boolean alcCloseDevice(long device) {
        return (Boolean) invoke("org.lwjgl.openal.ALC10", "alcCloseDevice", new Class<?>[]{long.class}, device);
    }

    private static long alcCreateContext(long device, IntBuffer attrs) {
        return (Long) invoke("org.lwjgl.openal.ALC10", "alcCreateContext", new Class<?>[]{long.class, IntBuffer.class}, device, attrs);
    }
    
    private static boolean alcIsExtensionPresent(long device, String extName) {
        return (Boolean) invoke("org.lwjgl.openal.ALC10", "alcIsExtensionPresent", new Class<?>[]{long.class, CharSequence.class}, device, extName);
    }

    private static void alcDestroyContext(long context) {
        invoke("org.lwjgl.openal.ALC10", "alcDestroyContext", new Class<?>[]{long.class}, context);
    }

    private static boolean alcMakeContextCurrent(long context) {
        return (Boolean) invoke("org.lwjgl.openal.ALC10", "alcMakeContextCurrent", new Class<?>[]{long.class}, context);
    }

    private static void createCapabilities(long device) {
        try {
            Class<?> alcClass = Class.forName("org.lwjgl.openal.ALC");
            Class<?> alClass = Class.forName("org.lwjgl.openal.AL");
            Class<?> alcCapabilitiesClass = Class.forName("org.lwjgl.openal.ALCCapabilities");
            Object alcCapabilities = alcClass.getMethod("createCapabilities", long.class).invoke(null, device);
            alClass.getMethod("createCapabilities", alcCapabilitiesClass).invoke(null, alcCapabilities);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to initialize OpenAL capabilities", exception);
        }
    }

    private static int alGenSources() {
        return (Integer) invoke("org.lwjgl.openal.AL10", "alGenSources", new Class<?>[]{});
    }

    private static void alDeleteSources(int source) {
        invoke("org.lwjgl.openal.AL10", "alDeleteSources", new Class<?>[]{int.class}, source);
    }

    private static int alGenBuffers() {
        return (Integer) invoke("org.lwjgl.openal.AL10", "alGenBuffers", new Class<?>[]{});
    }

    private static void alDeleteBuffers(int buffer) {
        invoke("org.lwjgl.openal.AL10", "alDeleteBuffers", new Class<?>[]{int.class}, buffer);
    }

    private static void alBufferData(int buffer, int format, ByteBuffer data, int frequency) {
        invoke("org.lwjgl.openal.AL10", "alBufferData", new Class<?>[]{int.class, int.class, ByteBuffer.class, int.class}, buffer, format, data, frequency);
    }

    private static void alSourceQueueBuffers(int source, int buffer) {
        invoke("org.lwjgl.openal.AL10", "alSourceQueueBuffers", new Class<?>[]{int.class, int.class}, source, buffer);
    }

    private static int alSourceUnqueueBuffers(int source) {
        return (Integer) invoke("org.lwjgl.openal.AL10", "alSourceUnqueueBuffers", new Class<?>[]{int.class}, source);
    }

    private static void alSourcePlay(int source) {
        invoke("org.lwjgl.openal.AL10", "alSourcePlay", new Class<?>[]{int.class}, source);
    }

    private static void alSourceStop(int source) {
        invoke("org.lwjgl.openal.AL10", "alSourceStop", new Class<?>[]{int.class}, source);
    }

    private static void alSourcei(int source, int parameter, int value) {
        invoke("org.lwjgl.openal.AL10", "alSourcei", new Class<?>[]{int.class, int.class, int.class}, source, parameter, value);
    }

    private static void alSource3i(int source, int parameter, int value1, int value2, int value3) {
        invoke("org.lwjgl.openal.AL11", "alSource3i", new Class<?>[]{int.class, int.class, int.class, int.class, int.class}, source, parameter, value1, value2, value3);
    }

    private static void alSourcef(int source, int parameter, float value) {
        invoke("org.lwjgl.openal.AL10", "alSourcef", new Class<?>[]{int.class, int.class, float.class}, source, parameter, value);
    }

    private static void alSource3f(int source, int parameter, float x, float y, float z) {
        invoke("org.lwjgl.openal.AL10", "alSource3f", new Class<?>[]{int.class, int.class, float.class, float.class, float.class}, source, parameter, x, y, z);
    }

    private static int alGetSourcei(int source, int parameter) {
        return (Integer) invoke("org.lwjgl.openal.AL10", "alGetSourcei", new Class<?>[]{int.class, int.class}, source, parameter);
    }

    private static void alListener3f(int parameter, float x, float y, float z) {
        invoke("org.lwjgl.openal.AL10", "alListener3f", new Class<?>[]{int.class, float.class, float.class, float.class}, parameter, x, y, z);
    }

    private static void alListenerfv(int parameter, FloatBuffer values) {
        invoke("org.lwjgl.openal.AL10", "alListenerfv", new Class<?>[]{int.class, FloatBuffer.class}, parameter, values);
    }

    private static int alGenFilters() {
        return (Integer) invoke("org.lwjgl.openal.EXTEfx", "alGenFilters", new Class<?>[]{});
    }

    private static void alDeleteFilters(int filter) {
        invoke("org.lwjgl.openal.EXTEfx", "alDeleteFilters", new Class<?>[]{int.class}, filter);
    }

    private static void alFilteri(int filter, int parameter, int value) {
        invoke("org.lwjgl.openal.EXTEfx", "alFilteri", new Class<?>[]{int.class, int.class, int.class}, filter, parameter, value);
    }

    private static void alFilterf(int filter, int parameter, float value) {
        invoke("org.lwjgl.openal.EXTEfx", "alFilterf", new Class<?>[]{int.class, int.class, float.class}, filter, parameter, value);
    }

    private static int alGenAuxiliaryEffectSlots() {
        return (Integer) invoke("org.lwjgl.openal.EXTEfx", "alGenAuxiliaryEffectSlots", new Class<?>[]{});
    }

    private static void alDeleteAuxiliaryEffectSlots(int slot) {
        invoke("org.lwjgl.openal.EXTEfx", "alDeleteAuxiliaryEffectSlots", new Class<?>[]{int.class}, slot);
    }

    private static int alGenEffects() {
        return (Integer) invoke("org.lwjgl.openal.EXTEfx", "alGenEffects", new Class<?>[]{});
    }

    private static void alDeleteEffects(int effect) {
        invoke("org.lwjgl.openal.EXTEfx", "alDeleteEffects", new Class<?>[]{int.class}, effect);
    }

    private static void alEffecti(int effect, int parameter, int value) {
        invoke("org.lwjgl.openal.EXTEfx", "alEffecti", new Class<?>[]{int.class, int.class, int.class}, effect, parameter, value);
    }

    private static void alEffectf(int effect, int parameter, float value) {
        invoke("org.lwjgl.openal.EXTEfx", "alEffectf", new Class<?>[]{int.class, int.class, float.class}, effect, parameter, value);
    }

    private static void alAuxiliaryEffectSloti(int slot, int parameter, int value) {
        invoke("org.lwjgl.openal.EXTEfx", "alAuxiliaryEffectSloti", new Class<?>[]{int.class, int.class, int.class}, slot, parameter, value);
    }

    private static int alGetError() {
        return (Integer) invoke("org.lwjgl.openal.AL10", "alGetError", new Class<?>[]{});
    }

    private static void checkError(String operation) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new IllegalStateException(operation + " failed with OpenAL error " + error);
        }
    }

    private static Object invoke(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName, parameterTypes);
            return method.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to call " + className + "." + methodName, exception);
        }
    }

    private static final class SourceState {
        private final int source;
        private final int directFilter;
        private final int reverbFilter;
        private final ArrayDeque<Integer> queuedBuffers = new ArrayDeque<>();
        private final ArrayDeque<Integer> freeBuffers = new ArrayDeque<>();

        private SourceState(int source, int directFilter, int reverbFilter) {
            this.source = source;
            this.directFilter = directFilter;
            this.reverbFilter = reverbFilter;
        }
    }
}
