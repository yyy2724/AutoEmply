using AutoEmply.Data;
using AutoEmply.Services;
using AutoEmply.Services.Prompts;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddSingleton<DelphiGenerator>();
builder.Services.AddSingleton<StructureToLayoutConverter>();
builder.Services.AddSingleton<LayoutPostProcessor>();
builder.Services.AddSingleton<AiModelState>();
builder.Services.AddHttpClient<ClaudeClient>(client =>
{
    var timeoutSeconds = builder.Configuration.GetValue<int?>("Anthropic:RequestTimeoutSeconds") ?? 240;
    client.Timeout = TimeSpan.FromSeconds(Math.Max(30, timeoutSeconds));
});
builder.Services.AddScoped<PromptPresetService>();
builder.Services.AddScoped<PromptPresetSeeder>();

var connectionString = builder.Configuration.GetConnectionString("Default");
if (string.IsNullOrWhiteSpace(connectionString))
{
    throw new InvalidOperationException("ConnectionStrings:Default is required (appsettings.Development.json or ConnectionStrings__Default).");
}

builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));
builder.Services.AddCors(options =>
{
    options.AddPolicy("DevClient", policy =>
    {
        var origins = builder.Configuration.GetSection("Cors:AllowedOrigins").Get<string[]>()
            ?? ["http://localhost:5239", "https://localhost:7267"];

        policy.WithOrigins(origins)
            .AllowAnyHeader()
            .AllowAnyMethod();
    });
});
var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseCors("DevClient");

app.UseAuthorization();

app.MapControllers();

using (var scope = app.Services.CreateScope())
{
    var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    await dbContext.Database.MigrateAsync();
    var seeder = scope.ServiceProvider.GetRequiredService<PromptPresetSeeder>();
    await seeder.SeedAsync(CancellationToken.None);
}

app.Run();
