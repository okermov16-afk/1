# Сбор данных ModelPort. Только читает файлы, ничего не меняет.
$ErrorActionPreference = 'Continue'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$game = $null
foreach ($c in @($here, (Split-Path $here -Parent), (Get-Location).Path)) {
    if ($c -and (Test-Path (Join-Path $c 'config\modelport\ported'))) { $game = $c; break }
}
if (-not $game) {
    Write-Host 'Ищу папку игры (config\modelport\ported)...'
    $hit = Get-ChildItem -Path $env:USERPROFILE, 'C:\', 'D:\' -Recurse -Directory -Filter ported -ErrorAction SilentlyContinue -Depth 8 |
        Where-Object { $_.FullName -like '*\config\modelport\ported' } | Select-Object -First 1
    if ($hit) { $game = $hit.FullName -replace '\\config\\modelport\\ported$','' }
}
if (-not $game) { Write-Host 'Папка игры не найдена. Положите этот файл в папку игры (рядом с config) и запустите снова.'; Read-Host 'Enter для выхода'; exit }
Write-Host "Папка игры: $game"
$lib = Join-Path $game 'config\modelport\ported'
$out = New-Object System.Text.StringBuilder
function Add([string]$t) { [void]$out.AppendLine($t) }

Add "##### 0. Папка игры: $game"

Add "`n##### 1. Сводка по модам (блоки / без таблицы размещения / почти постоянная таблица)"
$rows = @()
Get-ChildItem (Join-Path $lib 'blocks') -Recurse -Filter *.json -ErrorAction SilentlyContinue | ForEach-Object {
    try {
        $j = Get-Content $_.FullName -Raw -Encoding UTF8 | ConvertFrom-Json
        $few = ($null -ne $j.placement) -and ((@($j.placement) | Sort-Object -Unique).Count -le 2)
        $rows += [pscustomobject]@{ mod = $j.reg.Split(':')[0]; reg = $j.reg; nul = ($null -eq $j.placement); few = $few; toggle = $j.toggle; rev = $j.rev }
    } catch {}
}
Add (($rows | Group-Object mod | ForEach-Object {
    [pscustomobject]@{ mod = $_.Name; blocks = $_.Count; placementNull = @($_.Group | Where-Object nul).Count; placementAlmostConstant = @($_.Group | Where-Object few).Count }
} | Sort-Object mod | Format-Table -AutoSize | Out-String -Width 200))

Add "`n##### 2. Блоки с bell / switch в имени: placement, toggle, rev"
Add (($rows | Where-Object { $_.reg -match 'bell|switch' } | Sort-Object reg | Format-Table reg, nul, few, toggle, rev -AutoSize | Out-String -Width 200))

Add "`n##### 3. Полные JSON блоков bell / switch (до 12 файлов, до 40000 символов каждый)"
Get-ChildItem (Join-Path $lib 'blocks') -Recurse -Filter *.json -ErrorAction SilentlyContinue |
    Where-Object { $_.BaseName -match 'bell|switch' } | Select-Object -First 12 | ForEach-Object {
        Add "===== $($_.FullName)"
        $t = Get-Content $_.FullName -Raw -Encoding UTF8
        if ($t.Length -gt 40000) { $t = $t.Substring(0, 40000) + "`n...[обрезано]" }
        Add $t
    }

Add "`n##### 4. Журнал игры"
foreach ($lf in @('logs\latest.log', 'logs\debug.log')) {
    $p = Join-Path $game $lf
    if (Test-Path $p) {
        Add "--- $lf"
        Add ((Select-String -Path $p -Pattern 'modelport: таблицы|modelport: блок|снять не удалось|снимок блоков|самопроверк|regfix' -Encoding UTF8 | Select-Object -Last 150 | ForEach-Object { $_.Line }) -join "`n")
    }
}

Add "`n##### 5. capture-report.txt"
$rp = Join-Path $lib 'capture-report.txt'
if (Test-Path $rp) { Add (Get-Content $rp -Raw -Encoding UTF8) }

Add "`n##### 6. capture-exclude.txt"
$ep = Join-Path $lib 'capture-exclude.txt'
if (Test-Path $ep) { Add (Get-Content $ep -Raw -Encoding UTF8) }

$text = $out.ToString()
$file = Join-Path $here 'modelport-result.txt'
[System.IO.File]::WriteAllText($file, $text, (New-Object System.Text.UTF8Encoding($true)))
Set-Clipboard -Value $text
Write-Host ''
Write-Host "Готово. Результат скопирован в буфер обмена ($($text.Length) символов)."
Write-Host "Копия сохранена: $file"
Write-Host 'Вставьте его в чат (Ctrl+V).'
Read-Host 'Enter для выхода'
