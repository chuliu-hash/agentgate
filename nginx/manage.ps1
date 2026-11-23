param(
    [ValidateSet('start', 'stop', 'reload', 'test')]
    [string]$Action = 'start',
    [string]$NginxExe = 'nginx.exe'
)
$ErrorActionPreference = 'Stop'
$prefix = $PSScriptRoot.Replace('\', '/') + '/'
foreach ($dir in @('logs', 'temp')) {
    New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot $dir) -Force | Out-Null
}
$executable = (Get-Command $NginxExe -ErrorAction Stop).Source
if ($Action -eq 'stop' -or $Action -eq 'reload') {
    $signal = if ($Action -eq 'stop') { 'quit' } else { 'reload' }
    & $executable -p $prefix -c conf/nginx.conf -s $signal
    if ($LASTEXITCODE -ne 0) { throw 'Nginx control command failed.' }
    exit
}
& $executable -p $prefix -c conf/nginx.conf -t
if ($LASTEXITCODE -ne 0) { throw 'Nginx configuration validation failed.' }
if ($Action -eq 'start') {
    Start-Process -FilePath $executable -ArgumentList @('-p', ('"' + $prefix + '"'), '-c', 'conf/nginx.conf') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden
    Write-Output 'Open http://localhost:8088/ (backend: http://127.0.0.1:8081).'
}
