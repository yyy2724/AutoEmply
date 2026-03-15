package health.autoemplyserver.service;

public class AiModelState {

    private volatile String lastResponseModel;

    public String getLastResponseModel() {
        return lastResponseModel;
    }

    public void update(String model) {
        if (model != null && !model.isBlank()) {
            this.lastResponseModel = model;
        }
    }
}
