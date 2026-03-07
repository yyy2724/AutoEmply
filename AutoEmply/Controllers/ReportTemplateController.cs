using System.Text;
using AutoEmply.Data;
using AutoEmply.Entities;
using AutoEmply.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace AutoEmply.Controllers;

/// <summary>
/// 결과지 도서관 API.
/// 보고서 템플릿(DFM+PAS+미리보기)을 업로드, 조회, 다운로드, 삭제한다.
/// </summary>
[ApiController]
[Route("api/report-templates")]
public sealed class ReportTemplateController(AppDbContext dbContext) : ControllerBase
{
    private const long MaxPreviewBytes = 10 * 1024 * 1024;

    /// <summary>카테고리별 그룹핑된 템플릿 목록 반환 (미리보기 데이터 제외).</summary>
    [HttpGet]
    public async Task<IActionResult> GetAll(CancellationToken ct)
    {
        var templates = await dbContext.ReportTemplates
            .AsNoTracking()
            .OrderBy(x => x.Category)
            .ThenBy(x => x.Name)
            .Select(x => new
            {
                x.Id,
                x.Name,
                x.Category,
                x.OriginalFormName,
                HasPreview = x.PreviewData != null,
                x.PreviewContentType,
                x.CreatedAt,
                x.UpdatedAt
            })
            .ToListAsync(ct);

        return Ok(templates);
    }

    /// <summary>단일 템플릿 상세 (미리보기 데이터 제외).</summary>
    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Get(Guid id, CancellationToken ct)
    {
        var template = await dbContext.ReportTemplates
            .AsNoTracking()
            .Where(x => x.Id == id)
            .Select(x => new
            {
                x.Id,
                x.Name,
                x.Category,
                x.OriginalFormName,
                x.DfmContent,
                x.PasContent,
                HasPreview = x.PreviewData != null,
                x.PreviewContentType,
                x.CreatedAt,
                x.UpdatedAt
            })
            .FirstOrDefaultAsync(ct);

        return template is null ? NotFound() : Ok(template);
    }

    /// <summary>미리보기 이미지/PDF를 바이너리로 반환.</summary>
    [HttpGet("{id:guid}/preview")]
    public async Task<IActionResult> GetPreview(Guid id, CancellationToken ct)
    {
        var template = await dbContext.ReportTemplates
            .AsNoTracking()
            .Where(x => x.Id == id)
            .Select(x => new { x.PreviewContentType, x.PreviewData })
            .FirstOrDefaultAsync(ct);

        if (template?.PreviewData is null)
            return NotFound();

        return File(template.PreviewData, template.PreviewContentType ?? "application/octet-stream");
    }

    /// <summary>
    /// 새 템플릿 업로드.
    /// multipart/form-data로 dfm, pas, preview(선택), name, category를 받는다.
    /// </summary>
    [HttpPost]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> Create(
        [FromForm] string name,
        [FromForm] string category,
        [FromForm] IFormFile dfmFile,
        [FromForm] IFormFile pasFile,
        [FromForm] IFormFile? previewFile,
        CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(name) || string.IsNullOrWhiteSpace(category))
            return BadRequest(new { error = "name과 category는 필수입니다." });

        var dfmContent = await ReadTextFileAsync(dfmFile, ct);
        var pasContent = await ReadTextFileAsync(pasFile, ct);

        if (string.IsNullOrWhiteSpace(dfmContent) || string.IsNullOrWhiteSpace(pasContent))
            return BadRequest(new { error = "DFM과 PAS 파일은 필수입니다." });

        var originalFormName = Path.GetFileNameWithoutExtension(dfmFile.FileName);
        if (string.IsNullOrWhiteSpace(originalFormName))
            return BadRequest(new { error = "DFM 파일명에서 폼 이름을 추출할 수 없습니다." });

        byte[]? previewData = null;
        string? previewContentType = null;
        if (previewFile is { Length: > 0 and <= MaxPreviewBytes })
        {
            previewContentType = previewFile.ContentType;
            await using var ms = new MemoryStream();
            await previewFile.CopyToAsync(ms, ct);
            previewData = ms.ToArray();
        }

        var now = DateTimeOffset.UtcNow;
        var entity = new ReportTemplate
        {
            Id = Guid.NewGuid(),
            Name = name.Trim(),
            Category = category.Trim(),
            DfmContent = dfmContent,
            PasContent = pasContent,
            OriginalFormName = originalFormName,
            PreviewContentType = previewContentType,
            PreviewData = previewData,
            CreatedAt = now,
            UpdatedAt = now
        };

        dbContext.ReportTemplates.Add(entity);
        await dbContext.SaveChangesAsync(ct);

        return Ok(new { entity.Id, entity.Name, entity.Category, entity.OriginalFormName });
    }

    /// <summary>
    /// 폼 이름을 치환하여 ZIP 다운로드.
    /// 쿼리: ?formName=Form_QREmply25
    /// </summary>
    [HttpGet("{id:guid}/download")]
    public async Task<IActionResult> Download(Guid id, [FromQuery] string formName, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(formName))
            return BadRequest(new { error = "formName 파라미터는 필수입니다." });

        var template = await dbContext.ReportTemplates
            .AsNoTracking()
            .FirstOrDefaultAsync(x => x.Id == id, ct);

        if (template is null)
            return NotFound();

        var dfmInternalName = DelphiRenamer.ExtractFormNameFromDfm(template.DfmContent)
                              ?? template.OriginalFormName;

        var zipBytes = DelphiRenamer.RenameAndZip(
            template.OriginalFormName,
            dfmInternalName,
            formName.Trim(),
            template.DfmContent,
            template.PasContent);

        return File(zipBytes, "application/zip", $"{formName.Trim()}.zip");
    }

    /// <summary>템플릿 삭제.</summary>
    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id, CancellationToken ct)
    {
        var entity = await dbContext.ReportTemplates.FirstOrDefaultAsync(x => x.Id == id, ct);
        if (entity is null) return NotFound();

        dbContext.ReportTemplates.Remove(entity);
        await dbContext.SaveChangesAsync(ct);
        return NoContent();
    }

    private static readonly Encoding Euckr;

    static ReportTemplateController()
    {
        Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
        Euckr = Encoding.GetEncoding(949);
    }

    private static async Task<string?> ReadTextFileAsync(IFormFile? file, CancellationToken ct)
    {
        if (file is null or { Length: 0 }) return null;
        using var reader = new StreamReader(file.OpenReadStream(), Euckr);
        return await reader.ReadToEndAsync(ct);
    }
}
