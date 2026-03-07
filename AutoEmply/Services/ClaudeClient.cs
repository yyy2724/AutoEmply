using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using AutoEmply.Models;
using AutoEmply.Services.Prompts;

namespace AutoEmply.Services;

/// <summary>
/// Anthropic Claude API와 통신하는 HTTP 클라이언트.
///
/// 두 가지 모드를 지원한다:
///   1. GenerateLayoutSpecAsync  - 이미지 → LayoutSpec (픽셀 좌표 기반)
///   2. GenerateFormStructureAsync - 이미지 → FormStructure (비율 기반 논리 구조)
///
/// 공통 흐름: API 호출 → 응답 파싱 → 유효성 검증 → 재시도(최대 N회)
/// </summary>
public sealed class ClaudeClient(
    HttpClient httpClient,
    IConfiguration configuration,
    ILogger<ClaudeClient> logger,
    AiModelState aiModelState)
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        NumberHandling = JsonNumberHandling.AllowReadingFromString
    };

    // ═══════════════════════════════════════════
    //  공개 API
    // ═══════════════════════════════════════════

    /// <summary>이미지를 분석하여 Delphi QuickReport LayoutSpec을 생성한다.</summary>
    public async Task<ClaudeLayoutResult> GenerateLayoutSpecAsync(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset,
        CancellationToken ct,
        bool forceNonEmptyItems = false,
        int emptyObjectRetryLevel = 0)
    {
        var toolSchema = ClaudeToolSchemas.BuildLayoutSpecTool();
        var toolName = "emit_layout_spec";

        var parseResult = await CallClaudeWithRetriesAsync(
            formName, mediaType, fileBase64, preset, toolSchema, toolName, ct,
            // 파싱: 원시 JSON → LayoutSpec
            (rawText, attempt) =>
            {
                if (IsEmptyObjectPayload(rawText) && emptyObjectRetryLevel < 2)
                    return ParseOutcome<LayoutSpec>.RecursiveRetry;

                var layoutSpec = TryParseJson<LayoutSpec>(rawText, out var parseError);
                if (layoutSpec is null)
                    return ParseOutcome<LayoutSpec>.RetryWith(parseError ?? "Return strict JSON only with top-level items array.");

                var validationErrors = LayoutSpecValidator.Validate(formName, layoutSpec);
                if (validationErrors.Count > 0)
                {
                    if (!forceNonEmptyItems && IsItemsEmptyValidation(validationErrors))
                        return ParseOutcome<LayoutSpec>.RecursiveRetry;

                    return ParseOutcome<LayoutSpec>.RetryWith(string.Join(" | ", validationErrors.Take(5)));
                }

                return ParseOutcome<LayoutSpec>.Succeed(layoutSpec);
            });

        // 빈 응답 재시도가 필요한 경우 → 강화된 제약으로 재귀 호출
        if (parseResult.NeedsRecursiveRetry)
        {
            logger.LogWarning("Claude returned empty/items-empty payload. Retrying with stronger constraints. Level={Level}", emptyObjectRetryLevel + 1);
            return await GenerateLayoutSpecAsync(
                formName, mediaType, fileBase64, preset, ct,
                forceNonEmptyItems: true,
                emptyObjectRetryLevel: emptyObjectRetryLevel + 1);
        }

        if (!parseResult.Success)
            return ClaudeLayoutResult.Fail(parseResult.StatusCode, parseResult.Error!, parseResult.Details);

        return ClaudeLayoutResult.Ok(parseResult.Value!);
    }

    /// <summary>이미지를 분석하여 논리적 FormStructure를 추출한다 (Phase 1).</summary>
    public async Task<ClaudeFormStructureResult> GenerateFormStructureAsync(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset,
        CancellationToken ct)
    {
        var toolSchema = ClaudeToolSchemas.BuildFormStructureTool();
        var toolName = "emit_form_structure";

        var parseResult = await CallClaudeWithRetriesAsync(
            formName, mediaType, fileBase64, preset, toolSchema, toolName, ct,
            (rawText, _) =>
            {
                var structure = TryParseJson<FormStructure>(rawText, out var parseError);
                if (structure is null)
                    return ParseOutcome<FormStructure>.RetryWith(parseError ?? "Return valid FormStructure JSON.");

                var validationErrors = FormStructureValidator.Validate(structure);
                if (validationErrors.Count > 0)
                    return ParseOutcome<FormStructure>.RetryWith(string.Join(" | ", validationErrors.Take(5)));

                return ParseOutcome<FormStructure>.Succeed(structure);
            });

        if (!parseResult.Success)
            return ClaudeFormStructureResult.Fail(parseResult.StatusCode, parseResult.Error!, parseResult.Details);

        return ClaudeFormStructureResult.Ok(parseResult.Value!);
    }

    // ═══════════════════════════════════════════
    //  핵심 재시도 루프 (두 모드가 공유)
    // ═══════════════════════════════════════════

    /// <summary>
    /// Claude API 호출 → 응답 파싱 → 실패 시 재시도를 관리하는 공통 루프.
    /// parseAndValidate 콜백으로 모드별 파싱/검증 로직을 주입받는다.
    /// </summary>
    private async Task<ParseOutcome<T>> CallClaudeWithRetriesAsync<T>(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset,
        object toolSchema,
        string toolName,
        CancellationToken ct,
        Func<string, int, ParseOutcome<T>> parseAndValidate)
    {
        var apiKey = Environment.GetEnvironmentVariable("ANTHROPIC_API_KEY");
        if (string.IsNullOrWhiteSpace(apiKey))
            return ParseOutcome<T>.FailWith(400, "API 키 없음");

        var endpoint = configuration["Anthropic:ApiUrl"] ?? "https://api.anthropic.com/v1/messages";
        var maxRetries = Math.Max(1, configuration.GetValue<int?>("Anthropic:MaxRetryAttempts") ?? 3);

        logger.LogInformation("Claude request. Model={Model}, Endpoint={Endpoint}, MaxRetries={Max}",
            preset.Model, endpoint, maxRetries);

        var userPrompt = RenderUserPromptTemplate(preset.UserPromptTemplate, formName);
        var visualBlock = BuildVisualBlock(mediaType, fileBase64);

        ParseOutcome<T>? lastFailure = null;

        for (var attempt = 1; attempt <= maxRetries; attempt++)
        {
            // ── 1. HTTP 요청 ──
            var payload = BuildPayload(preset, toolSchema, toolName, userPrompt, visualBlock);

            using var request = new HttpRequestMessage(HttpMethod.Post, endpoint);
            request.Headers.Add("x-api-key", apiKey);
            request.Headers.Add("anthropic-version", "2023-06-01");
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

            HttpResponseMessage response;
            string body;
            try
            {
                response = await httpClient.SendAsync(request, ct).ConfigureAwait(false);
                body = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            }
            catch (OperationCanceledException ex) when (!ct.IsCancellationRequested)
            {
                logger.LogError(ex, "Claude timeout. Attempt={Attempt}/{Max}", attempt, maxRetries);
                return ParseOutcome<T>.FailWith(504, "Claude 요청 시간 초과",
                    ["Anthropic API timeout. Increase Anthropic:RequestTimeoutSeconds or lower MaxTokens."]);
            }
            catch (OperationCanceledException)
            {
                return ParseOutcome<T>.FailWith(504, "Claude 요청 취소됨");
            }
            catch (Exception ex) when (ex is HttpRequestException or IOException)
            {
                logger.LogWarning(ex, "Claude network failure. Attempt={Attempt}/{Max}", attempt, maxRetries);
                if (attempt < maxRetries)
                {
                    await ExponentialBackoffAsync(attempt, ct);
                    continue;
                }
                return ParseOutcome<T>.FailWith(503, "Claude 네트워크 오류");
            }

            // ── 2. 응답 처리 ──
            using (response)
            {
                using var responseDoc = TryParseJsonDocument(body);
                var requestId = ExtractRequestId(response, responseDoc);
                aiModelState.Update(ExtractResponseModel(responseDoc));

                // 2a. HTTP 실패
                if (!response.IsSuccessStatusCode)
                {
                    var statusCode = (int)response.StatusCode;
                    logger.LogError("Claude API failed. Attempt={Attempt}/{Max}, Status={Status}, RequestId={Id}",
                        attempt, maxRetries, statusCode, requestId);

                    var msg = ExtractClaudeErrorMessage(responseDoc) ?? "Claude API request failed.";
                    lastFailure = ParseOutcome<T>.FailWith(statusCode, "레이아웃 생성 실패", [$"ClaudeStatus={statusCode}", msg]);

                    if (attempt < maxRetries && IsTransientStatusCode(statusCode))
                    {
                        await ExponentialBackoffAsync(attempt, ct);
                        continue;
                    }
                    return lastFailure;
                }

                // 2b. 모델 텍스트 추출
                if (!TryExtractModelOutput(responseDoc, out var rawText))
                {
                    logger.LogError("Claude response parsing failed. RequestId={Id}, Body={Body}", requestId, body);
                    return ParseOutcome<T>.FailWith(400, "JSON 파싱 실패(모델 응답이 JSON이 아님)");
                }

                logger.LogInformation("Claude output preview. RequestId={Id}, Preview={Preview}",
                    requestId, Truncate(rawText, 2000));

                // 2c. 모드별 파싱 + 검증
                var outcome = parseAndValidate(rawText, attempt);

                if (outcome.NeedsRecursiveRetry)
                    return outcome;

                if (outcome.Success)
                    return outcome;

                // 파싱 실패 → 재시도
                if (attempt < maxRetries)
                {
                    logger.LogWarning("Claude parse/validation failed. Retrying. Attempt={Attempt}/{Max}, Guidance={G}",
                        attempt, maxRetries, outcome.RetryGuidance);
                    continue;
                }

                return ParseOutcome<T>.FailWith(400, outcome.Error ?? "파싱 실패", outcome.Details);
            }
        }

        return lastFailure ?? ParseOutcome<T>.FailWith(500, "레이아웃 생성 실패");
    }

    // ═══════════════════════════════════════════
    //  Payload 빌더
    // ═══════════════════════════════════════════

    private static object BuildPayload(
        ResolvedPromptPreset preset, object toolSchema, string toolName,
        string userPrompt, object visualBlock) => new
    {
        model = preset.Model,
        max_tokens = preset.MaxTokens,
        temperature = preset.Temperature,
        system = preset.SystemPrompt,
        tools = new[] { toolSchema },
        tool_choice = new { type = "tool", name = toolName },
        messages = new object[]
        {
            new
            {
                role = "user",
                content = new object[]
                {
                    new { type = "text", text = userPrompt },
                    visualBlock
                }
            }
        }
    };

    private static object BuildVisualBlock(string mediaType, string fileBase64)
    {
        var blockType = mediaType.Equals("application/pdf", StringComparison.OrdinalIgnoreCase)
            ? "document"
            : "image";

        return new
        {
            type = blockType,
            source = new { type = "base64", media_type = mediaType, data = fileBase64 }
        };
    }

    // ═══════════════════════════════════════════
    //  응답 파싱 헬퍼
    // ═══════════════════════════════════════════

    private JsonDocument? TryParseJsonDocument(string body)
    {
        try { return JsonDocument.Parse(body); }
        catch (JsonException ex)
        {
            logger.LogWarning(ex, "Failed to parse response body as JSON");
            return null;
        }
    }

    /// <summary>Claude 응답에서 tool_use.input 또는 text 블록의 내용을 추출한다.</summary>
    private static bool TryExtractModelOutput(JsonDocument? doc, out string text)
    {
        text = string.Empty;
        if (doc is null) return false;
        if (!doc.RootElement.TryGetProperty("content", out var content) || content.ValueKind != JsonValueKind.Array)
            return false;

        foreach (var block in content.EnumerateArray())
        {
            // tool_use 블록 우선
            if (block.TryGetProperty("type", out var blockType) &&
                string.Equals(blockType.GetString(), "tool_use", StringComparison.OrdinalIgnoreCase) &&
                block.TryGetProperty("input", out var input) &&
                input.ValueKind == JsonValueKind.Object)
            {
                text = input.GetRawText();
                return !string.IsNullOrWhiteSpace(text);
            }

            // text 블록 폴백
            if (block.TryGetProperty("type", out var type) &&
                string.Equals(type.GetString(), "text", StringComparison.OrdinalIgnoreCase) &&
                block.TryGetProperty("text", out var value))
            {
                text = value.GetString() ?? string.Empty;
                return !string.IsNullOrWhiteSpace(text);
            }
        }

        return false;
    }

    private static string? ExtractRequestId(HttpResponseMessage response, JsonDocument? doc)
    {
        if (response.Headers.TryGetValues("request-id", out var ids)) return ids.FirstOrDefault();
        if (response.Headers.TryGetValues("x-request-id", out var xIds)) return xIds.FirstOrDefault();
        if (doc?.RootElement.TryGetProperty("request_id", out var rid) == true) return rid.GetString();
        return null;
    }

    private static string? ExtractResponseModel(JsonDocument? doc) =>
        doc?.RootElement.TryGetProperty("model", out var m) == true ? m.GetString() : null;

    private static string? ExtractClaudeErrorMessage(JsonDocument? doc)
    {
        if (doc?.RootElement.ValueKind != JsonValueKind.Object) return null;
        if (doc.RootElement.TryGetProperty("error", out var err) &&
            err.ValueKind == JsonValueKind.Object &&
            err.TryGetProperty("message", out var msg))
            return msg.GetString();
        return null;
    }

    // ═══════════════════════════════════════════
    //  JSON 역직렬화
    // ═══════════════════════════════════════════

    /// <summary>
    /// 원시 텍스트를 T로 역직렬화한다.
    /// 첫 시도 실패 시, 텍스트에서 JSON 객체 경계({...})를 추출하여 재시도한다.
    /// </summary>
    private static T? TryParseJson<T>(string rawText, out string? parseError) where T : class
    {
        parseError = null;

        var result = Deserialize<T>(rawText, out parseError);
        if (result is not null) return result;

        // 주변 텍스트 제거 후 재시도
        var first = rawText.IndexOf('{');
        var last = rawText.LastIndexOf('}');
        if (first < 0 || last <= first)
        {
            parseError ??= "No JSON object boundaries found in model output.";
            return null;
        }

        return Deserialize<T>(rawText[first..(last + 1)], out parseError);
    }

    private static T? Deserialize<T>(string text, out string? parseError) where T : class
    {
        parseError = null;
        try { return JsonSerializer.Deserialize<T>(text, JsonOptions); }
        catch (JsonException ex) { parseError = $"JSON schema/type mismatch: {ex.Message}"; return null; }
        catch { parseError = "Unknown JSON deserialization error."; return null; }
    }

    // ═══════════════════════════════════════════
    //  유틸리티
    // ═══════════════════════════════════════════

    private static string RenderUserPromptTemplate(string template, string formName) =>
        (template ?? string.Empty)
            .Replace("{{formName}}", formName, StringComparison.Ordinal)
            .Replace("{formName}", formName, StringComparison.Ordinal)
            .Trim();

    private static bool IsTransientStatusCode(int code) =>
        code is 429 or 500 or 502 or 503 or 504 or 529;

    private static bool IsEmptyObjectPayload(string text) =>
        string.Equals(text.Trim(), "{}", StringComparison.Ordinal);

    private static bool IsItemsEmptyValidation(IReadOnlyCollection<string> errors) =>
        errors.Any(e => e.Contains("layoutSpec.items must contain at least one item.", StringComparison.OrdinalIgnoreCase));

    private static string Truncate(string value, int max) =>
        string.IsNullOrEmpty(value) || value.Length <= max ? value : value[..max];

    private static async Task ExponentialBackoffAsync(int attempt, CancellationToken ct) =>
        await Task.Delay((int)Math.Pow(2, attempt - 1) * 1000, ct).ConfigureAwait(false);

    // ═══════════════════════════════════════════
    //  ParseOutcome - 파싱 결과 래퍼
    // ═══════════════════════════════════════════

    /// <summary>
    /// CallClaudeWithRetriesAsync의 parseAndValidate 콜백이 반환하는 결과.
    /// 성공 / 재시도 필요 / 재귀 재시도 필요 / 최종 실패 네 가지 상태를 표현한다.
    /// </summary>
    private sealed record ParseOutcome<T>(
        bool Success,
        int StatusCode,
        string? Error,
        IReadOnlyCollection<string>? Details,
        T? Value,
        string? RetryGuidance = null,
        bool NeedsRecursiveRetry = false)
    {
        public static ParseOutcome<T> Succeed(T value) =>
            new(true, 200, null, null, value);

        public static ParseOutcome<T> FailWith(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
            new(false, statusCode, error, details, default);

        public static ParseOutcome<T> RetryWith(string guidance) =>
            new(false, 0, guidance, null, default, RetryGuidance: guidance);

        public static ParseOutcome<T> RecursiveRetry =>
            new(false, 0, null, null, default, NeedsRecursiveRetry: true);
    }
}
