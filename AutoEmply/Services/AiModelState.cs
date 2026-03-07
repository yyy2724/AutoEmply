namespace AutoEmply.Services;

/// <summary>
/// Claude API 응답에서 실제 사용된 모델명을 추적하는 싱글톤 상태.
/// 설정 파일의 모델명과 실제 런타임 모델명이 다를 수 있기 때문에
/// (예: 별칭, 라우팅), 마지막 응답의 모델을 저장해 둔다.
/// </summary>
public sealed class AiModelState
{
    public string? LastResponseModel { get; private set; }

    public void Update(string? model)
    {
        if (!string.IsNullOrWhiteSpace(model))
            LastResponseModel = model.Trim();
    }
}
