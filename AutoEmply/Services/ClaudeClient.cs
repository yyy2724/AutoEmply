using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using AutoEmply.Models;
using AutoEmply.Services.Prompts;

namespace AutoEmply.Services;

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

    public async Task<ClaudeLayoutResult> GenerateLayoutSpecAsync(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset,
        CancellationToken cancellationToken,
        bool forceNonEmptyItems = false,
        int emptyObjectRetryLevel = 0)
    {
        var apiKey = Environment.GetEnvironmentVariable("ANTHROPIC_API_KEY");
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            return ClaudeLayoutResult.Fail(400, "API \uD0A4 \uC5C6\uC74C");
        }

        var endpoint = configuration["Anthropic:ApiUrl"] ?? "https://api.anthropic.com/v1/messages";
        var model = preset.Model;
        var maxRetryAttempts = Math.Max(1, configuration.GetValue<int?>("Anthropic:MaxRetryAttempts") ?? 3);
        logger.LogInformation("Claude request configuration. Model={Model}, Endpoint={Endpoint}, MaxRetryAttempts={MaxRetryAttempts}", model, endpoint, maxRetryAttempts);

        _ = forceNonEmptyItems;
        _ = emptyObjectRetryLevel;
        var systemPrompt = preset.SystemPrompt;
        var userPrompt = RenderUserPromptTemplate(preset.UserPromptTemplate, formName);

        object visualBlock = mediaType.Equals("application/pdf", StringComparison.OrdinalIgnoreCase)
            ? new
            {
                type = "document",
                source = new
                {
                    type = "base64",
                    media_type = mediaType,
                    data = fileBase64
                }
            }
            : new
            {
                type = "image",
                source = new
                {
                    type = "base64",
                    media_type = mediaType,
                    data = fileBase64
                }
            };

        static string BuildValidationGuidance(IReadOnlyCollection<string> errors)
        {
            return string.Join(" | ", errors.Take(5));
        }

        object BuildPayload(string qualityGuidance)
        {
            _ = qualityGuidance;

            return new
            {
                model,
                max_tokens = preset.MaxTokens,
                temperature = preset.Temperature,
                system = systemPrompt,
                tools = new object[]
                {
                    new
                    {
                        name = "emit_layout_spec",
                        description = "Return only LayoutSpec JSON payload for Delphi QuickReport, preserving source layout detail, proportions, and colors.",
                        input_schema = new
                        {
                            type = "object",
                            properties = new
                            {
                                items = new
                                {
                                    type = "array",
                                    items = new
                                    {
                                        type = "object",
                                        properties = new
                                        {
                                            name = new { type = "string" },
                                            type = new { type = "string", @enum = new[] { "Text", "Line", "Rect", "Image" } },
                                            left = new { type = "integer" },
                                            top = new { type = "integer" },
                                            width = new { type = "integer" },
                                            height = new { type = "integer" },
                                            caption = new { type = "string" },
                                            align = new { type = "string", @enum = new[] { "Left", "Center", "Right" } },
                                            fontSize = new { type = "integer" },
                                            bold = new { type = "boolean" },
                                            transparent = new { type = "boolean" },
                                            textColor = new { type = "string" },
                                            orientation = new { type = "string", @enum = new[] { "H", "V" } },
                                            thickness = new { type = "integer" },
                                            strokeColor = new { type = "string" },
                                            fillColor = new { type = "string" },
                                            filled = new { type = "boolean" },
                                            stretch = new { type = "boolean" }
                                        },
                                        required = new[] { "type", "left", "top", "width", "height" },
                                        additionalProperties = false
                                    }
                                },
                                pas = new
                                {
                                    type = "object",
                                    properties = new
                                    {
                                        uses = new
                                        {
                                            type = "array",
                                            items = new { type = "string" }
                                        },
                                        methods = new
                                        {
                                            type = "array",
                                            items = new
                                            {
                                                type = "object",
                                                properties = new
                                                {
                                                    declaration = new { type = "string" },
                                                    body = new
                                                    {
                                                        type = "array",
                                                        items = new { type = "string" }
                                                    }
                                                },
                                                required = new[] { "declaration", "body" },
                                                additionalProperties = false
                                            }
                                        }
                                    },
                                    additionalProperties = false
                                }
                            },
                            required = new[] { "items" },
                            additionalProperties = false
                        }
                    }
                },
                tool_choice = new
                {
                    type = "tool",
                    name = "emit_layout_spec"
                },
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
        }

        ClaudeLayoutResult? lastFailure = null;
        var retryQualityGuidance = string.Empty;
        for (var attempt = 1; attempt <= maxRetryAttempts; attempt++)
        {
            var payload = BuildPayload(retryQualityGuidance);
            using var request = new HttpRequestMessage(HttpMethod.Post, endpoint);
            request.Headers.Add("x-api-key", apiKey);
            request.Headers.Add("anthropic-version", "2023-06-01");
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

            HttpResponseMessage response;
            string body;
            try
            {
                response = await httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
                body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
            }
            catch (OperationCanceledException ex) when (!cancellationToken.IsCancellationRequested)
            {
                logger.LogError(ex, "Claude request timed out. Attempt={Attempt}/{MaxRetryAttempts}", attempt, maxRetryAttempts);
                return ClaudeLayoutResult.Fail(504, "Claude 요청 시간 초과", ["Anthropic API timeout. Increase Anthropic:RequestTimeoutSeconds or lower MaxTokens."]);
            }
            catch (OperationCanceledException ex)
            {
                logger.LogWarning(ex, "Claude request canceled by timeout token. Attempt={Attempt}/{MaxRetryAttempts}", attempt, maxRetryAttempts);
                return ClaudeLayoutResult.Fail(504, "Claude 요청 시간 초과", ["Anthropic API request was canceled by server timeout token."]);
            }
            catch (Exception ex) when (IsTransientNetworkException(ex))
            {
                logger.LogWarning(ex, "Transient Claude network failure. Attempt={Attempt}/{MaxRetryAttempts}", attempt, maxRetryAttempts);
                if (attempt < maxRetryAttempts)
                {
                    var delayMs = (int)Math.Pow(2, attempt - 1) * 1000;
                    await Task.Delay(delayMs, cancellationToken).ConfigureAwait(false);
                    continue;
                }

                return ClaudeLayoutResult.Fail(503, "Claude 네트워크 오류", ["Transient network error while calling Anthropic API."]);
            }

            using (response)
            {
            using var responseDoc = TryParseJson(body);
            var requestId = GetRequestId(response, responseDoc);
            aiModelState.Update(GetResponseModel(responseDoc));

            if (!response.IsSuccessStatusCode)
            {
                logger.LogError("Claude API failed. Attempt={Attempt}/{MaxAttempts}, Status={StatusCode}, RequestId={RequestId}, Body={Body}",
                    attempt, maxRetryAttempts, (int)response.StatusCode, requestId, body);

                var externalMessage = TryExtractClaudeErrorMessage(responseDoc) ?? "Claude API request failed.";
                var details = new List<string>
                {
                    $"ClaudeStatus={(int)response.StatusCode}",
                    externalMessage
                };
                if (!string.IsNullOrWhiteSpace(requestId))
                {
                    details.Add($"RequestId={requestId}");
                }

                lastFailure = ClaudeLayoutResult.Fail((int)response.StatusCode, "\uB808\uC774\uC544\uC6C3 \uC0DD\uC131 \uC2E4\uD328", details);

                if (attempt < maxRetryAttempts && IsTransientStatusCode((int)response.StatusCode))
                {
                    var delayMs = (int)Math.Pow(2, attempt - 1) * 1000;
                    logger.LogWarning("Transient Claude failure. Retrying after {DelayMs}ms. Attempt={Attempt}/{MaxAttempts}", delayMs, attempt, maxRetryAttempts);
                    await Task.Delay(delayMs, cancellationToken).ConfigureAwait(false);
                    continue;
                }

                return lastFailure;
            }

            if (!TryReadModelText(responseDoc, out var text))
            {
                logger.LogError("Claude response parsing failed. RequestId={RequestId}, Body={Body}", requestId, body);
                return ClaudeLayoutResult.Fail(400, "JSON \uD30C\uC2F1 \uC2E4\uD328(\uBAA8\uB378 \uC751\uB2F5\uC774 JSON\uC774 \uC544\uB2D8)");
            }

            logger.LogInformation("Claude model raw output preview (max 2000 chars). RequestId={RequestId}, Preview={Preview}",
                requestId, TruncateForLog(text, 2000));

            if (IsEmptyObjectPayload(text) && emptyObjectRetryLevel < 2)
            {
                var nextLevel = emptyObjectRetryLevel + 1;
                logger.LogWarning("Claude returned empty object payload. Retrying with stronger constraints. Level={NextLevel}", nextLevel);
                return await GenerateLayoutSpecAsync(
                    formName,
                    mediaType,
                    fileBase64,
                    preset,
                    cancellationToken,
                    forceNonEmptyItems: true,
                    emptyObjectRetryLevel: nextLevel);
            }

            var layoutSpec = TryParseLayoutSpec(text, out var parseError);
            if (layoutSpec is null)
            {
                logger.LogError("Claude layout parse failed. RequestId={RequestId}, ParseError={ParseError}, Text={Text}", requestId, parseError, text);
                if (attempt < maxRetryAttempts)
                {
                    retryQualityGuidance = parseError ?? "Return strict JSON only with top-level items array.";
                    continue;
                }

                return ClaudeLayoutResult.Fail(400, "JSON 파싱 실패", [parseError ?? "Invalid layout JSON payload."]);
            }

            var validationErrors = LayoutSpecValidator.Validate(formName, layoutSpec);
            if (validationErrors.Count > 0)
            {
                if (!forceNonEmptyItems && IsItemsEmptyValidationFailure(validationErrors))
                {
                    logger.LogWarning("Claude returned empty items. Retrying once with stronger prompt constraint.");
                    return await GenerateLayoutSpecAsync(
                        formName,
                        mediaType,
                        fileBase64,
                        preset,
                        cancellationToken,
                        forceNonEmptyItems: true,
                        emptyObjectRetryLevel: emptyObjectRetryLevel);
                }

                if (attempt < maxRetryAttempts)
                {
                    retryQualityGuidance = BuildValidationGuidance(validationErrors);
                    logger.LogWarning("Claude output failed validation. Retrying with guidance. Attempt={Attempt}/{MaxAttempts}, Guidance={Guidance}",
                        attempt, maxRetryAttempts, retryQualityGuidance);
                    continue;
                }

                logger.LogError("Claude returned invalid layout. RequestId={RequestId}, Errors={Errors}",
                    requestId, string.Join("; ", validationErrors));
                return ClaudeLayoutResult.Fail(400, "\uB808\uC774\uC544\uC6C3 \uAC80\uC99D \uC2E4\uD328", validationErrors);
            }

            return ClaudeLayoutResult.Ok(layoutSpec);
            }
        }

        return lastFailure ?? ClaudeLayoutResult.Fail(500, "\uB808\uC774\uC544\uC6C3 \uC0DD\uC131 \uC2E4\uD328");
    }

    private JsonDocument? TryParseJson(string body)
    {
        try
        {
            return JsonDocument.Parse(body);
        }
        catch (JsonException ex)
        {
            logger.LogWarning(ex, "Failed to parse response body as JSON");
            return null;
        }
    }

    private static string? GetRequestId(HttpResponseMessage response, JsonDocument? doc)
    {
        if (response.Headers.TryGetValues("request-id", out var reqIds))
        {
            return reqIds.FirstOrDefault();
        }

        if (response.Headers.TryGetValues("x-request-id", out var xReqIds))
        {
            return xReqIds.FirstOrDefault();
        }

        if (doc?.RootElement.TryGetProperty("request_id", out var requestId) == true)
        {
            return requestId.GetString();
        }

        return null;
    }

    private static string? GetResponseModel(JsonDocument? doc)
    {
        if (doc?.RootElement.TryGetProperty("model", out var model) == true)
        {
            return model.GetString();
        }

        return null;
    }

    private static string? TryExtractClaudeErrorMessage(JsonDocument? doc)
    {
        if (doc?.RootElement.ValueKind != JsonValueKind.Object)
        {
            return null;
        }

        if (doc.RootElement.TryGetProperty("error", out var errorElement) &&
            errorElement.ValueKind == JsonValueKind.Object &&
            errorElement.TryGetProperty("message", out var messageElement))
        {
            return messageElement.GetString();
        }

        return null;
    }

    private static bool IsTransientStatusCode(int statusCode) =>
        statusCode is 429 or 500 or 502 or 503 or 504 or 529;

    private static bool IsTransientNetworkException(Exception ex) =>
        ex is HttpRequestException or IOException;

    private static bool IsItemsEmptyValidationFailure(IReadOnlyCollection<string> errors) =>
        errors.Any(x => x.Contains("layoutSpec.items must contain at least one item.", StringComparison.OrdinalIgnoreCase));

    private static string TruncateForLog(string value, int maxLength)
    {
        if (string.IsNullOrEmpty(value) || value.Length <= maxLength)
        {
            return value;
        }

        return value[..maxLength];
    }

    private static bool IsEmptyObjectPayload(string text) =>
        string.Equals(text.Trim(), "{}", StringComparison.Ordinal);


    private static bool TryReadModelText(JsonDocument? doc, out string text)
    {
        text = string.Empty;
        
        if (doc is null)
        {
            return false;
        }

        if (!doc.RootElement.TryGetProperty("content", out var content) || content.ValueKind != JsonValueKind.Array)
        {
            return false;
        }

        foreach (var block in content.EnumerateArray())
        {
            if (block.TryGetProperty("type", out var blockType) &&
                string.Equals(blockType.GetString(), "tool_use", StringComparison.OrdinalIgnoreCase) &&
                block.TryGetProperty("input", out var inputElement) &&
                inputElement.ValueKind == JsonValueKind.Object)
            {
                text = inputElement.GetRawText();
                return !string.IsNullOrWhiteSpace(text);
            }

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

    private static LayoutSpec? TryParseLayoutSpec(string rawText, out string? parseError)
    {
        parseError = null;
        var parsed = Deserialize(rawText, out parseError);
        if (parsed is not null)
        {
            return parsed;
        }

        var first = rawText.IndexOf('{');
        var last = rawText.LastIndexOf('}');
        if (first < 0 || last <= first)
        {
            parseError ??= "No JSON object boundaries found in model output.";
            return null;
        }

        var jsonOnly = rawText[first..(last + 1)];
        return Deserialize(jsonOnly, out parseError);
    }

    private static LayoutSpec? Deserialize(string text, out string? parseError)
    {
        parseError = null;
        try
        {
            return JsonSerializer.Deserialize<LayoutSpec>(text, JsonOptions);
        }
        catch (JsonException ex)
        {
            parseError = $"JSON schema/type mismatch: {ex.Message}";
            return null;
        }
        catch
        {
            parseError = "Unknown JSON deserialization error.";
            return null;
        }
    }

    // ===== Phase 1: Form Structure Extraction =====

    public async Task<ClaudeFormStructureResult> GenerateFormStructureAsync(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset,
        CancellationToken cancellationToken)
    {
        var apiKey = Environment.GetEnvironmentVariable("ANTHROPIC_API_KEY");
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            return ClaudeFormStructureResult.Fail(400, "API 키 없음");
        }

        var endpoint = configuration["Anthropic:ApiUrl"] ?? "https://api.anthropic.com/v1/messages";
        var model = preset.Model;
        var maxRetryAttempts = Math.Max(1, configuration.GetValue<int?>("Anthropic:MaxRetryAttempts") ?? 3);
        logger.LogInformation("Claude Phase1 request. Model={Model}, Endpoint={Endpoint}", model, endpoint);

        var systemPrompt = preset.SystemPrompt;
        var userPrompt = RenderUserPromptTemplate(preset.UserPromptTemplate, formName);

        object visualBlock = mediaType.Equals("application/pdf", StringComparison.OrdinalIgnoreCase)
            ? new { type = "document", source = new { type = "base64", media_type = mediaType, data = fileBase64 } }
            : (object)new { type = "image", source = new { type = "base64", media_type = mediaType, data = fileBase64 } };

        static string BuildValidationGuidance(IReadOnlyCollection<string> errors)
        {
            return string.Join(" | ", errors.Take(5));
        }

        object BuildPayload(string qualityGuidance)
        {
            _ = qualityGuidance;

            return new
            {
                model,
                max_tokens = preset.MaxTokens,
                temperature = preset.Temperature,
                system = systemPrompt,
                tools = new object[] { BuildFormStructureTool() },
                tool_choice = new { type = "tool", name = "emit_form_structure" },
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
        }

        ClaudeFormStructureResult? lastFailure = null;
        var retryGuidance = string.Empty;

        for (var attempt = 1; attempt <= maxRetryAttempts; attempt++)
        {
            var payload = BuildPayload(retryGuidance);
            using var request = new HttpRequestMessage(HttpMethod.Post, endpoint);
            request.Headers.Add("x-api-key", apiKey);
            request.Headers.Add("anthropic-version", "2023-06-01");
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

            HttpResponseMessage response;
            string body;
            try
            {
                response = await httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
                body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
            }
            catch (OperationCanceledException ex) when (!cancellationToken.IsCancellationRequested)
            {
                logger.LogError(ex, "Claude Phase1 timed out. Attempt={Attempt}/{Max}", attempt, maxRetryAttempts);
                return ClaudeFormStructureResult.Fail(504, "Claude 요청 시간 초과");
            }
            catch (OperationCanceledException)
            {
                return ClaudeFormStructureResult.Fail(504, "Claude 요청 취소됨");
            }
            catch (Exception ex) when (IsTransientNetworkException(ex))
            {
                logger.LogWarning(ex, "Claude Phase1 network failure. Attempt={Attempt}/{Max}", attempt, maxRetryAttempts);
                if (attempt < maxRetryAttempts)
                {
                    await Task.Delay((int)Math.Pow(2, attempt - 1) * 1000, cancellationToken).ConfigureAwait(false);
                    continue;
                }

                return ClaudeFormStructureResult.Fail(503, "Claude 네트워크 오류");
            }

            using (response)
            {
                using var responseDoc = TryParseJson(body);
                var requestId = GetRequestId(response, responseDoc);
                aiModelState.Update(GetResponseModel(responseDoc));

                if (!response.IsSuccessStatusCode)
                {
                    logger.LogError("Claude Phase1 API failed. Status={Status}, RequestId={RequestId}",
                        (int)response.StatusCode, requestId);

                    var msg = TryExtractClaudeErrorMessage(responseDoc) ?? "Claude API request failed.";
                    lastFailure = ClaudeFormStructureResult.Fail((int)response.StatusCode, "구조 추출 실패", [msg]);

                    if (attempt < maxRetryAttempts && IsTransientStatusCode((int)response.StatusCode))
                    {
                        await Task.Delay((int)Math.Pow(2, attempt - 1) * 1000, cancellationToken).ConfigureAwait(false);
                        continue;
                    }

                    return lastFailure;
                }

                if (!TryReadModelText(responseDoc, out var text))
                {
                    logger.LogError("Claude Phase1 response parsing failed. RequestId={RequestId}", requestId);
                    return ClaudeFormStructureResult.Fail(400, "JSON 파싱 실패(모델 응답이 JSON이 아님)");
                }

                logger.LogInformation("Claude Phase1 output preview. RequestId={RequestId}, Preview={Preview}",
                    requestId, TruncateForLog(text, 2000));

                var structure = TryParseFormStructure(text, out var parseError);
                if (structure is null)
                {
                    logger.LogError("Claude Phase1 parse failed. ParseError={Error}", parseError);
                    if (attempt < maxRetryAttempts)
                    {
                        retryGuidance = parseError ?? "Return valid FormStructure JSON.";
                        continue;
                    }

                    return ClaudeFormStructureResult.Fail(400, "구조 JSON 파싱 실패", [parseError ?? "Invalid structure JSON."]);
                }

                var validationErrors = FormStructureValidator.Validate(structure);
                if (validationErrors.Count > 0)
                {
                    if (attempt < maxRetryAttempts)
                    {
                        retryGuidance = BuildValidationGuidance(validationErrors);
                        logger.LogWarning("Claude Phase1 validation failed. Retrying. Guidance={Guidance}", retryGuidance);
                        continue;
                    }

                    return ClaudeFormStructureResult.Fail(400, "구조 검증 실패", validationErrors);
                }

                return ClaudeFormStructureResult.Ok(structure);
            }
        }

        return lastFailure ?? ClaudeFormStructureResult.Fail(500, "구조 추출 실패");
    }

    private static FormStructure? TryParseFormStructure(string rawText, out string? parseError)
    {
        parseError = null;
        try
        {
            var result = JsonSerializer.Deserialize<FormStructure>(rawText, JsonOptions);
            if (result is not null) return result;
        }
        catch (JsonException ex)
        {
            parseError = $"FormStructure JSON parse error: {ex.Message}";
        }
        catch
        {
            parseError = "Unknown FormStructure deserialization error.";
        }

        // Fallback: try to extract JSON from surrounding text
        var first = rawText.IndexOf('{');
        var last = rawText.LastIndexOf('}');
        if (first < 0 || last <= first) return null;

        try
        {
            return JsonSerializer.Deserialize<FormStructure>(rawText[first..(last + 1)], JsonOptions);
        }
        catch (JsonException ex)
        {
            parseError ??= $"FormStructure JSON parse error: {ex.Message}";
            return null;
        }
    }

    private static string RenderUserPromptTemplate(string template, string formName)
    {
        var rendered = (template ?? string.Empty)
            .Replace("{{formName}}", formName, StringComparison.Ordinal)
            .Replace("{formName}", formName, StringComparison.Ordinal);

        return rendered.Trim();
    }

    private static object BuildFormStructureTool()
    {
        return new
        {
            name = "emit_form_structure",
            description = "Extract the logical structure of a Korean form from the image. Output sections, tables, rows, cells, and freeform elements using fractions (0.0-1.0) instead of pixel coordinates.",
            input_schema = new
            {
                type = "object",
                properties = new
                {
                    title = new { type = "string", description = "Main title text of the form" },
                    titleFontSize = new { type = "integer", description = "Font size for title. Default 20." },
                    sections = new
                    {
                        type = "array",
                        items = new
                        {
                            type = "object",
                            properties = new
                            {
                                sectionType = new { type = "string", @enum = new[] { "table", "freeform", "keyvalue" } },
                                label = new { type = "string", description = "Optional section label" },
                                hasHeaderBackground = new { type = "boolean" },
                                table = new
                                {
                                    type = "object",
                                    properties = new
                                    {
                                        columns = new
                                        {
                                            type = "array",
                                            items = new
                                            {
                                                type = "object",
                                                properties = new
                                                {
                                                    widthFraction = new { type = "number", description = "Fraction of table width (0.0-1.0). All columns must sum to 1.0." },
                                                    header = new { type = "string" },
                                                    isHeaderColumn = new { type = "boolean", description = "True if this column has colored background" }
                                                },
                                                required = new[] { "widthFraction" }
                                            }
                                        },
                                        rows = new
                                        {
                                            type = "array",
                                            items = new
                                            {
                                                type = "object",
                                                properties = new
                                                {
                                                    heightHint = new { type = "string", description = "standard(20px), compact(14px), tall(30px), or integer" },
                                                    isHeaderRow = new { type = "boolean" },
                                                    cells = new
                                                    {
                                                        type = "array",
                                                        items = new
                                                        {
                                                            type = "object",
                                                            properties = new
                                                            {
                                                                text = new { type = "string" },
                                                                align = new { type = "string", @enum = new[] { "Left", "Center", "Right" } },
                                                                bold = new { type = "boolean" },
                                                                fontSize = new { type = "integer" },
                                                                colSpan = new { type = "integer", description = "Number of columns this cell spans. Default 1." },
                                                                rowSpan = new { type = "integer" },
                                                                hasBackground = new { type = "boolean", description = "True if cell has colored background" },
                                                                fieldName = new { type = "string", description = "Variable/placeholder name if present" },
                                                                textColor = new { type = "string" }
                                                            },
                                                            required = new[] { "text" }
                                                        }
                                                    }
                                                },
                                                required = new[] { "cells" }
                                            }
                                        },
                                        fullWidth = new { type = "boolean" },
                                        leftFraction = new { type = "number" },
                                        widthFraction = new { type = "number" }
                                    },
                                    required = new[] { "columns", "rows" }
                                },
                                elements = new
                                {
                                    type = "array",
                                    items = new
                                    {
                                        type = "object",
                                        properties = new
                                        {
                                            elementType = new { type = "string", @enum = new[] { "text", "checkbox", "image", "line" } },
                                            text = new { type = "string" },
                                            xFraction = new { type = "number" },
                                            yFraction = new { type = "number" },
                                            widthFraction = new { type = "number" },
                                            heightFraction = new { type = "number" },
                                            align = new { type = "string" },
                                            bold = new { type = "boolean" },
                                            fontSize = new { type = "integer" }
                                        },
                                        required = new[] { "elementType", "text", "xFraction", "yFraction", "widthFraction" }
                                    }
                                }
                            },
                            required = new[] { "sectionType" }
                        }
                    },
                    footer = new
                    {
                        type = "array",
                        items = new
                        {
                            type = "object",
                            properties = new
                            {
                                elementType = new { type = "string", @enum = new[] { "text", "image", "signature_line" } },
                                text = new { type = "string" },
                                align = new { type = "string" },
                                bold = new { type = "boolean" },
                                fontSize = new { type = "integer" },
                                xFraction = new { type = "number" },
                                widthFraction = new { type = "number" }
                            },
                            required = new[] { "elementType" }
                        }
                    }
                },
                required = new[] { "sections" }
            }
        };
    }
}

public sealed record ClaudeLayoutResult(
    bool Success,
    int StatusCode,
    string? Error,
    IReadOnlyCollection<string>? Details,
    LayoutSpec? LayoutSpec)
{
    public static ClaudeLayoutResult Ok(LayoutSpec layoutSpec) =>
        new(true, 200, null, null, layoutSpec);

    public static ClaudeLayoutResult Fail(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
        new(false, statusCode, error, details, null);
}

public sealed record ClaudeFormStructureResult(
    bool Success,
    int StatusCode,
    string? Error,
    IReadOnlyCollection<string>? Details,
    FormStructure? FormStructure)
{
    public static ClaudeFormStructureResult Ok(FormStructure structure) =>
        new(true, 200, null, null, structure);

    public static ClaudeFormStructureResult Fail(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
        new(false, statusCode, error, details, null);
}


