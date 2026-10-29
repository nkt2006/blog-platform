$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Установите Maven 3.9 или новее и добавьте mvn в PATH."
}
Write-Host "Для запуска требуется Java 21. Проверьте JAVA_HOME и mvn -version."
& mvn javafx:run @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
