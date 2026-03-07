namespace AutoEmply.Entities;

/// <summary>
/// 결과지 도서관에 저장되는 하나의 보고서 템플릿.
/// DFM + PAS 원본 + 미리보기 이미지를 보관한다.
/// </summary>
public sealed class ReportTemplate
{
    public Guid Id { get; set; }

    /// <summary>도서관에서 표시되는 이름 (예: "국가검진 기록지")</summary>
    public string Name { get; set; } = string.Empty;

    /// <summary>카테고리 (왼쪽 탭 분류용, 예: "건강검진", "혈액검사")</summary>
    public string Category { get; set; } = string.Empty;

    /// <summary>원본 DFM 파일 내용 (텍스트)</summary>
    public string DfmContent { get; set; } = string.Empty;

    /// <summary>원본 PAS 파일 내용 (텍스트)</summary>
    public string PasContent { get; set; } = string.Empty;

    /// <summary>원본 DFM/PAS에 사용된 폼 이름 (예: "Form_QROriginal")</summary>
    public string OriginalFormName { get; set; } = string.Empty;

    /// <summary>미리보기 파일의 MIME 타입 (image/png, application/pdf 등)</summary>
    public string? PreviewContentType { get; set; }

    /// <summary>미리보기 파일 데이터 (바이너리)</summary>
    public byte[]? PreviewData { get; set; }

    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
}
