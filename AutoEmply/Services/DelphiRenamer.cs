using System.IO.Compression;
using System.Text;
using System.Text.RegularExpressions;

namespace AutoEmply.Services;

/// <summary>
/// 저장된 DFM/PAS 파일의 폼 이름과 클래스 이름을 일괄 치환하여 ZIP으로 묶어주는 서비스.
///
/// 치환 대상 (3가지):
///   1. 파일/unit 이름  : Form_QRChart02  → Form_QRChart03  (언더스코어 포함)
///   2. DFM 내부 폼 이름: FormQRChart02   → FormQRChart03   (언더스코어 제거)
///   3. 클래스 이름      : TFormQRChart02  → TFormQRChart03  (T 접두사)
/// </summary>
public static class DelphiRenamer
{
    private static readonly Encoding Euckr;

    static DelphiRenamer()
    {
        Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
        Euckr = Encoding.GetEncoding(949);
    }

    /// <summary>
    /// DFM/PAS 내용에서 원본 이름들을 새 이름으로 모두 치환한 뒤 ZIP 바이트로 반환.
    /// </summary>
    /// <param name="originalFileName">DB에 저장된 원본 파일명 (예: Form_QRChart02)</param>
    /// <param name="dfmInternalName">DFM 첫 줄에서 추출한 내부 이름 (예: FormQRChart02)</param>
    /// <param name="newFormName">사용자가 입력한 새 이름 (예: Form_QRChart03)</param>
    /// <param name="dfmContent">원본 DFM 내용</param>
    /// <param name="pasContent">원본 PAS 내용</param>
    public static byte[] RenameAndZip(
        string originalFileName,
        string dfmInternalName,
        string newFormName,
        string dfmContent,
        string pasContent)
    {
        var newDfmName = RemoveUnderscores(newFormName);
        var oldClassName = "T" + RemoveUnderscores(dfmInternalName);
        var newClassName = "T" + newDfmName;

        string Rename(string content)
        {
            // 클래스명 치환 (가장 긴 패턴부터)
            content = ReplaceExact(content, oldClassName, newClassName);

            if (!string.Equals(originalFileName, dfmInternalName, StringComparison.Ordinal))
            {
                // 파일명과 DFM 내부명이 다른 경우 (예: Form_QRChart02 vs FormQRChart02)
                content = ReplaceExact(content, originalFileName, newFormName);
                content = ReplaceExact(content, dfmInternalName, newDfmName);
            }
            else
            {
                // 같은 경우 한 번만 치환
                content = ReplaceExact(content, originalFileName, newFormName);
            }

            return content;
        }

        using var stream = new MemoryStream();
        using (var archive = new ZipArchive(stream, ZipArchiveMode.Create, leaveOpen: true))
        {
            WriteEntry(archive, $"{newFormName}.dfm", Rename(dfmContent));
            WriteEntry(archive, $"{newFormName}.pas", Rename(pasContent));
        }
        return stream.ToArray();
    }

    /// <summary>
    /// DFM 첫 줄에서 내부 폼 이름을 추출한다.
    /// 예: "object FormQRChart02: TFormQRChart02" → "FormQRChart02"
    /// </summary>
    public static string? ExtractFormNameFromDfm(string dfmContent)
    {
        if (string.IsNullOrWhiteSpace(dfmContent)) return null;

        var firstLine = dfmContent.Split('\n', 2)[0].Trim();
        var match = Regex.Match(firstLine, @"^object\s+(\w+)\s*:", RegexOptions.IgnoreCase);
        return match.Success ? match.Groups[1].Value : null;
    }

    private static string RemoveUnderscores(string name) =>
        name.Replace("_", string.Empty, StringComparison.Ordinal);

    /// <summary>단어 경계를 고려한 정확한 치환.</summary>
    private static string ReplaceExact(string input, string oldValue, string newValue)
    {
        if (string.IsNullOrEmpty(oldValue) || oldValue == newValue) return input;

        var pattern = @"\b" + Regex.Escape(oldValue) + @"\b";
        return Regex.Replace(input, pattern, newValue);
    }

    private static void WriteEntry(ZipArchive archive, string entryName, string content)
    {
        var entry = archive.CreateEntry(entryName);
        using var writer = new StreamWriter(entry.Open(), Euckr);
        writer.Write(content);
    }
}
