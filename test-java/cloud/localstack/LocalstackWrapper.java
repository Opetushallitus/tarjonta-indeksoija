package cloud.localstack;

public class LocalstackWrapper {
    private LocalstackWrapper() {}

    public static void start() {
        Localstack.INSTANCE.setupInfrastructure();
    }

    public static void stop() {
        Localstack.teardownInfrastructure();
    }
}
