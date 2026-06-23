package dev.minevoice.standalone;

import dev.minevoice.standalone.config.StandaloneConfig;
import dev.minevoice.standalone.config.StandaloneConfigLoader;

import java.nio.file.Path;

public final class StandaloneVoiceServerMain {
    private StandaloneVoiceServerMain() {
    }

    public static void main(String[] args) {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of("minevoice-standalone.properties");
        StandaloneConfig config = new StandaloneConfigLoader().load(configPath);
        StandaloneVoiceServer server = new StandaloneVoiceServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "minevoice-shutdown"));
        server.start();
    }
}
