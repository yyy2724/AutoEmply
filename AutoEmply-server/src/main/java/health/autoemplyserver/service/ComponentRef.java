package health.autoemplyserver.service;

public record ComponentRef(String name, String type, String onPrintHandler) {

    public ComponentRef(String name, String type) {
        this(name, type, null);
    }
}
