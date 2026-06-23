$ErrorActionPreference = "Stop"

foreach ($port in 24454, 25565) {
    $processIds = @()
    $processIds += Get-NetUDPEndpoint -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess
    $processIds += Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess
    foreach ($processId in $processIds | Select-Object -Unique) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine -like '*minevoice*') {
            Stop-Process -Id $processId -Force
            Write-Host "Stopped MineVOICE process $processId on port $port"
        }
    }
}
