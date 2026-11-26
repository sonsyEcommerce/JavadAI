$ErrorActionPreference = "Stop"

$root = "D:\project\private\javadAI"
$output = Join-Path $root "FULL_PROJECT_SOURCE.txt"

$folders = @("backend", "frontend")

$excludedDirs = @(
    "target", "build", "dist", "out",
    ".git", ".idea", ".vscode", ".gradle",
    ".angular", "node_modules", "coverage",
    ".cache", ".next", "vendor", "bin", "obj",
    "test-results", "playwright-report"
)

$excludedFiles = @(
    "package-lock.json",
    "pnpm-lock.yaml",
    "yarn.lock",
    ".env",
    ".env.local",
    ".env.development",
    ".env.production",
    "secrets.json",
    "credentials.json",
    "id_rsa",
    "id_ed25519"
)

$extensions = @(
    ".java", ".kt", ".xml", ".properties",
    ".yml", ".yaml", ".sql", ".md",
    ".html", ".ts", ".tsx", ".js",
    ".jsx", ".mjs", ".cjs", ".css",
    ".scss", ".sass", ".gradle", ".txt",
    ".sh", ".bat", ".ps1", ".config",
    ".conf", ".json"
)

if (!(Test-Path $root)) {
    Write-Host "[ERROR] Project root not found!" -ForegroundColor Red
    exit 1
}

# Collect source files
$files = @()

foreach ($folder in $folders) {

    $base = Join-Path $root $folder

    if (!(Test-Path $base)) {
        Write-Host "[WARNING] Folder not found: $folder" -ForegroundColor Yellow
        continue
    }

    $items = Get-ChildItem -LiteralPath $base -Recurse -File -ErrorAction Stop

    foreach ($file in $items) {

        $relativePath = $file.FullName.Substring($base.Length).TrimStart('\')
        $parts = $relativePath -split '\\'

        $skip = $false

        foreach ($part in $parts) {
            if ($excludedDirs -contains $part) {
                $skip = $true
                break
            }
        }

        if ($skip) { continue }
        if ($excludedFiles -contains $file.Name) { continue }
        if ($extensions -notcontains $file.Extension.ToLower()) { continue }
        if ($file.Length -gt 2MB) { continue }

        $files += $file
    }
}

$files = $files | Sort-Object FullName -Unique

# UTF-8 without BOM for output
$encoding = New-Object System.Text.UTF8Encoding($false)
$writer = New-Object System.IO.StreamWriter($output, $false, $encoding)

$count = 0
$failed = 0

try {

    $writer.WriteLine("JAVADAI SOURCE EXPORT")
    $writer.WriteLine("Generated: $(Get-Date)")
    $writer.WriteLine("=" * 100)

    foreach ($file in $files) {

        $relativePath = $file.FullName.Substring($root.Length).TrimStart('\')

        try {

            # Read source explicitly as UTF-8
            $content = [System.IO.File]::ReadAllText(
                $file.FullName,
                [System.Text.Encoding]::UTF8
            )

            # Redact sensitive values
            $content = [regex]::Replace(
                $content,
                '(?im)(password|passwd|secret|api[_-]?key|access[_-]?token|client[_-]?secret)(\s*[:=]\s*)([^;\r\n,]+)',
                '$1$2[REDACTED]'
            )

            $content = [regex]::Replace(
                $content,
                '(?im)(Authorization\s*:\s*(Bearer|Basic)\s+)[^\s]+',
                '$1[REDACTED]'
            )

            $content = [regex]::Replace(
                $content,
                '(?i)-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----',
                '[PRIVATE KEY REDACTED]'
            )

            # Write actual new lines
            $writer.WriteLine()
            $writer.WriteLine("=" * 100)
            $writer.WriteLine("FILE: $relativePath")
            $writer.WriteLine("=" * 100)
            $writer.WriteLine($content)

            $count++

            Write-Host "[OK] $relativePath" -ForegroundColor Green
        }
        catch {
            $failed++
            Write-Host "[ERROR] $relativePath" -ForegroundColor Red
            Write-Host $_.Exception.Message -ForegroundColor Yellow
        }
    }
}
finally {
    $writer.Close()
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "TOTAL FILES: $count" -ForegroundColor Cyan
Write-Host "FAILED FILES: $failed" -ForegroundColor Yellow
Write-Host "OUTPUT: $output" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

if ($failed -gt 0) {
    exit 1
}

exit 0