using AutoEmply_Client;
using Microsoft.AspNetCore.Components.Web;
using Microsoft.AspNetCore.Components.WebAssembly.Hosting;
using Toolbelt.Blazor.Extensions.DependencyInjection;
using MudBlazor.Services;

var builder = WebAssemblyHostBuilder.CreateDefault(args);
builder.RootComponents.Add<App>("#app");
builder.RootComponents.Add<HeadOutlet>("head::after");

var apiBaseUrl = builder.Configuration["ApiBaseUrl"] ?? builder.HostEnvironment.BaseAddress;
var clientTimeoutSeconds = builder.Configuration.GetValue<int?>("ClientRequestTimeoutSeconds") ?? 300;
builder.Services.AddScoped(_ => new HttpClient
{
    BaseAddress = new Uri(apiBaseUrl),
    Timeout = TimeSpan.FromSeconds(Math.Max(30, clientTimeoutSeconds))
});
builder.Services.AddMudServices();
builder.Services.AddHotKeys2();

await builder.Build().RunAsync();
