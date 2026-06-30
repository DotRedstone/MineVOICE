package dev.minevoice.standalone;

import dev.minevoice.server.config.CoreVoiceServerConfig;
import dev.minevoice.server.config.CoreVoiceServerConfigLoader;
import dev.minevoice.server.CoreVoiceServer;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class StandaloneVoiceServerMain {
    private StandaloneVoiceServerMain() {}

    public static void main(String[] args) {
        Path configPath;
        if (args.length > 0) {
            configPath = Paths.get(args[0]);
        } else {
            configPath = Paths.get("config", "minevoice-server.properties");
        }
        CoreVoiceServerConfig config = new CoreVoiceServerConfigLoader().load(configPath);
        CoreVoiceServer server = new CoreVoiceServer(config);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
