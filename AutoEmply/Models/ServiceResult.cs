namespace AutoEmply.Models;

/// <summary>
/// 서비스 계층의 범용 결과 래퍼.
/// 성공 시 Value를, 실패 시 StatusCode + Error + Details를 담는다.
/// Controller에서 HTTP 응답으로 직접 매핑된다.
/// </summary>
public sealed record ServiceResult<T>(
    bool Success,
    int StatusCode,
    string? Error,
    IReadOnlyCollection<string>? Details,
    T? Value)
{
    public static ServiceResult<T> Ok(T value) =>
        new(true, 200, null, null, value);

    public static ServiceResult<T> Fail(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
        new(false, statusCode, error, details, default);
}

/// <summary>ZIP 내보내기 결과물.</summary>
public sealed record ExportArtifact(byte[] Bytes, string FileName);
