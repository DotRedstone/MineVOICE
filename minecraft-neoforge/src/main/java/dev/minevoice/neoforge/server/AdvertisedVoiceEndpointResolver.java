package dev.minevoice.neoforge.server;

import dev.minevoice.common.util.MineVoiceLogger;
import dev.minevoice.neoforge.config.MineVoiceModConfig;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Enumeration;

public final class AdvertisedVoiceEndpointResolver {
    private final MineVoiceLogger logger;

    public AdvertisedVoiceEndpointResolver(MineVoiceLogger logger) {
        this.logger = logger;
    }

    public String resolveFor(MineVoiceModConfig config, ServerPlayer player) {
        String configured = config.localVoiceAdvertiseHost();
        if (!"auto".equalsIgnoreCase(configured)) {
            return configured;
        }

        InetAddress remoteAddress = remoteAddress(player);
        if (remoteAddress != null && remoteAddress.isLoopbackAddress()) {
            return "127.0.0.1";
        }

        InetAddress siteLocal = firstUsableAddress(true);
        if (siteLocal != null) {
            return siteLocal.getHostAddress();
        }

        InetAddress nonLoopback = firstUsableAddress(false);
        if (nonLoopback != null) {
            logger.warn("MineVOICE could not find a site-local address; advertising " + nonLoopback.getHostAddress());
            return nonLoopback.getHostAddress();
        }

        logger.warn("MineVOICE could not resolve advertiseHost=auto; configure localVoiceAdvertiseHost manually");
        return "127.0.0.1";
    }

    private static InetAddress remoteAddress(ServerPlayer player) {
        SocketAddress socketAddress = remoteSocketAddress(player);
        if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress();
        }
        return null;
    }

    private static SocketAddress remoteSocketAddress(ServerPlayer player) {
        Object packetListener = player.connection;
        Object connection = invokeNoArg(packetListener, "getConnection");
        if (connection == null) {
            connection = fieldValue(packetListener, "connection");
        }
        if (connection == null) {
            return null;
        }
        Object remoteAddress = invokeNoArg(connection, "getRemoteAddress");
        return remoteAddress instanceof SocketAddress socketAddress ? socketAddress : null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Object fieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static InetAddress firstUsableAddress(boolean requireSiteLocal) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                        continue;
                    }
                    if (requireSiteLocal && !address.isSiteLocalAddress()) {
                        continue;
                    }
                    return address;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
