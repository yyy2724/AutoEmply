using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
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
        PropertyNameCaseInsensitive = true
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

        var userPrompt = $"formName={formName}. Analyze uploaded image or PDF and return valid LayoutSpec JSON.";
        if (emptyObjectRetryLevel >= 2)
        {
            userPrompt += " Return exactly one JSON object with top-level items array. Never return {}.";
        }

        var systemPrompt = forceNonEmptyItems
            ? $"{preset.SystemPrompt}\n\nHard requirement: items must contain at least one element. Empty array is never allowed. If uncertain, output at least one best-effort Text item."
            : preset.SystemPrompt;
        if (emptyObjectRetryLevel >= 2)
        {
            systemPrompt += "\n\nCritical requirement: never return an empty object {}. Always include items and at least one valid item.";
        }

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

        var payload = new
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
                    description = "Return only LayoutSpec JSON payload for Delphi QuickReport.",
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
                                        orientation = new { type = "string", @enum = new[] { "H", "V" } },
                                        thickness = new { type = "integer" },
                                        stretch = new { type = "boolean" }
                                    },
                                    required = new[] { "type", "left", "top", "width", "height" },
                                    additionalProperties = false
                                }
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
        ClaudeLayoutResult? lastFailure = null;
        for (var attempt = 1; attempt <= maxRetryAttempts; attempt++)
        {
            using var request = new HttpRequestMessage(HttpMethod.Post, endpoint);
            request.Headers.Add("x-api-key", apiKey);
            request.Headers.Add("anthropic-version", "2023-06-01");
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

            using var response = await httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
            var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
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

            var layoutSpec = TryParseLayoutSpec(text);
            if (layoutSpec is null)
            {
                logger.LogError("Claude returned non-JSON content. RequestId={RequestId}, Text={Text}", requestId, text);
                return ClaudeLayoutResult.Fail(400, "JSON \uD30C\uC2F1 \uC2E4\uD328(\uBAA8\uB378 \uC751\uB2F5\uC774 JSON\uC774 \uC544\uB2D8)");
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

                logger.LogError("Claude returned invalid layout. RequestId={RequestId}, Errors={Errors}",
                    requestId, string.Join("; ", validationErrors));
                return ClaudeLayoutResult.Fail(400, "\uB808\uC774\uC544\uC6C3 \uAC80\uC99D \uC2E4\uD328", validationErrors);
            }

            return ClaudeLayoutResult.Ok(layoutSpec);
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

    private static LayoutSpec? TryParseLayoutSpec(string rawText)
    {
        var parsed = Deserialize(rawText);
        if (parsed is not null)
        {
            return parsed;
        }

        var first = rawText.IndexOf('{');
        var last = rawText.LastIndexOf('}');
        if (first < 0 || last <= first)
        {
            return null;
        }

        var jsonOnly = rawText[first..(last + 1)];
        return Deserialize(jsonOnly);
    }

    private static LayoutSpec? Deserialize(string text)
    {
        try
        {
            return JsonSerializer.Deserialize<LayoutSpec>(text, JsonOptions);
        }
        catch
        {
            return null;
        }
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
