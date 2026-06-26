package dev.minevoice.neoforge.client.audio;

import dev.minevoice.common.protocol.VoiceChannel;
import dev.minevoice.neoforge.client.ClientAudioSettings;

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

public final class OpenAlVoicePlaybackBackend implements VoicePlaybackBackend {
    public static final String NAME = "openal";
    private static final int AL_FALSE = 0;
    private static final int AL_TRUE = 1;
    private static final int AL_NO_ERROR = 0;
    private static final int AL_FORMAT_MONO16 = 0x1101;
    private static final int AL_POSITION = 0x1004;
    private static final int AL_GAIN = 0x100A;
    private static final int AL_ORIENTATION = 0x100F;
    private static final int AL_SOURCE_STATE = 0x1010;
    private static final int AL_PLAYING = 0x1012;
    private static final int AL_BUFFERS_QUEUED = 0x1015;
    private static final int AL_BUFFERS_PROCESSED = 0x1016;
    private static final int AL_SOURCE_RELATIVE = 0x202;
    private static final int AL_ROLLOFF_FACTOR = 0x1021;

    private final long device;
    private final long context;
    private final Map<UUID, SourceState> sources = new HashMap<>();
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
            context = alcCreateContext(device);
            if (context == 0L || !alcMakeContextCurrent(context)) {
                throw new IllegalStateException("failed to create OpenAL context");
            }
            createCapabilities(device);
            checkError("OpenAL capabilities");
            return new OpenAlVoicePlaybackBackend(device, context);
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
        return NAME;
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
        float yaw = (float) Math.toRadians(listener.yaw());
        float forwardX = -((float) Math.sin(yaw));
        float forwardZ = (float) Math.cos(yaw);
        FloatBuffer orientation = ByteBuffer.allocateDirect(Float.BYTES * 6)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        orientation.put(forwardX).put(0.0F).put(forwardZ).put(0.0F).put(1.0F).put(0.0F).flip();
        alListenerfv(AL_ORIENTATION, orientation);
    }

    @Override
    public void writeSourceFrame(UUID speakerId, VoiceChannel channel, VoiceSourceSnapshot source, byte[] pcm, int sampleRate, float gain) {
        makeContextCurrent();
        SourceState state = sources.computeIfAbsent(speakerId, ignored -> createSource());
        cleanupProcessedBuffers(state);
        boolean positional = channel == VoiceChannel.PROXIMITY && source.known();
        alSourcei(state.source(), AL_SOURCE_RELATIVE, positional ? AL_FALSE : AL_TRUE);
        if (positional) {
            alSource3f(state.source(), AL_POSITION, (float) source.x(), (float) source.y(), (float) source.z());
        } else {
            alSource3f(state.source(), AL_POSITION, 0.0F, 0.0F, 0.0F);
        }
        alSourcef(state.source(), AL_GAIN, Math.max(0.0F, Math.min(2.0F, gain)));

        int buffer = alGenBuffers();
        ByteBuffer audio = ByteBuffer.allocateDirect(pcm.length).order(ByteOrder.nativeOrder());
        audio.put(pcm).flip();
        alBufferData(buffer, AL_FORMAT_MONO16, audio, sampleRate);
        alSourceQueueBuffers(state.source(), buffer);
        state.queuedBuffers().add(buffer);
        if (alGetSourcei(state.source(), AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(state.source());
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
        } finally {
            alcMakeContextCurrent(0L);
            alcDestroyContext(context);
            alcCloseDevice(device);
        }
    }

    private SourceState createSource() {
        int source = alGenSources();
        alSourcef(source, AL_ROLLOFF_FACTOR, 0.0F);
        return new SourceState(source, new ArrayDeque<>());
    }

    private void cleanupProcessedBuffers(SourceState state) {
        int processed = alGetSourcei(state.source(), AL_BUFFERS_PROCESSED);
        for (int index = 0; index < processed; index++) {
            int buffer = alSourceUnqueueBuffers(state.source());
            state.queuedBuffers().remove(buffer);
            alDeleteBuffers(buffer);
        }
    }

    private void deleteSource(SourceState state) {
        alSourceStop(state.source());
        int queued = alGetSourcei(state.source(), AL_BUFFERS_QUEUED);
        for (int index = 0; index < queued; index++) {
            int buffer = alSourceUnqueueBuffers(state.source());
            alDeleteBuffers(buffer);
        }
        state.queuedBuffers().clear();
        alDeleteSources(state.source());
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

    private static long alcOpenDevice() {
        return (Long) invoke("org.lwjgl.openal.ALC10", "alcOpenDevice", new Class<?>[]{ByteBuffer.class}, new Object[]{null});
    }

    private static boolean alcCloseDevice(long device) {
        return (Boolean) invoke("org.lwjgl.openal.ALC10", "alcCloseDevice", new Class<?>[]{long.class}, device);
    }

    private static long alcCreateContext(long device) {
        return (Long) invoke("org.lwjgl.openal.ALC10", "alcCreateContext", new Class<?>[]{long.class, IntBuffer.class}, device, null);
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

    private record SourceState(int source, ArrayDeque<Integer> queuedBuffers) {
    }
}
