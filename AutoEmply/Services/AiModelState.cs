namespace AutoEmply.Services;

public sealed class AiModelState
{
    public string? LastResponseModel { get; private set; }

    public void Update(string? model)
    {
        if (!string.IsNullOrWhiteSpace(model))
        {
            LastResponseModel = model.Trim();
        }
    }
}
