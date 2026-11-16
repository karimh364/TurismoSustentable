$ErrorActionPreference = 'Stop'
$directorioProyectoDemo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $directorioProyectoDemo

Write-Host 'Primero debe estar iniciado MariaDB desde XAMPP.'
Write-Host 'Arranque local persistente: refactor + validate. No recrea ni elimina tablas.'
Write-Host 'Deja esta terminal abierta. Ctrl+C detiene la aplicacion.'

& .\gradlew.bat bootRun --args="--spring.profiles.active=refactor --spring.jpa.hibernate.ddl-auto=validate"
exit $LASTEXITCODE
