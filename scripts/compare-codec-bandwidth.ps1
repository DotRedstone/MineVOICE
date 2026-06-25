param(
    [int]$Clients = 5,
    [int]$FramesPerClient = 120,
    [int]$VoicePort = 24454,
    [string]$SharedSecret = "minevoice-bandwidth-test-secret",
    [double]$PlayerSpacing = 3.0,
    [double]$PacketLossRate = 0.0,
    [int]$ReorderWindow = 1,
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"
$workspace = Split-Path -Parent $PSScriptRoot
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) "minevoice-bandwidth"
$configPath = Join-Path $tempRoot "minevoice-standalone.properties"

New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
if ($OutputPath) {
    $outputParent = Split-Path -Parent $OutputPath
    if ($outputParent) {
        New-Item -ItemType Directory -Force -Path $outputParent | Out-Null
    }
    Set-Content -LiteralPath $OutputPath -Value "" -Encoding utf8
}

function Write-Line {
    param([string]$Message = "")

    [Console]::Out.WriteLine($Message)
    if ($script:OutputPath) {
        Add-Content -LiteralPath $script:OutputPath -Value $Message -Encoding utf8
    }
}

@"
bindHost=127.0.0.1
bindPort=$VoicePort
sharedSecret=$SharedSecret
maxPlayers=100
proximityDistance=48
enableBandwidthStats=true
enableDebugLog=false
"@ | Set-Content -LiteralPath $configPath -Encoding ascii

function Start-VoiceServerJob {
    param([string]$Workspace, [string]$ConfigPath)

    Start-Job -Name "MineVOICE bandwidth voice server" -ScriptBlock {
        param($Workspace, $ConfigPath)
        Set-Location $Workspace
        & .\gradlew.bat --console=plain :standalone-server:run --args="$ConfigPath"
    } -ArgumentList $Workspace, $ConfigPath
}

function Wait-VoiceServerReady {
    param([System.Management.Automation.Job]$Job)

    $deadline = (Get-Date).AddSeconds(35)
    $buffer = ""
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 500
        $chunk = Receive-Job -Job $Job -Keep
        if ($chunk) {
            $buffer = ($chunk -join "`n")
            if ($buffer.Contains("UDP voice server listening")) {
                return
            }
        }
        if ($Job.State -notin @("Running", "NotStarted")) {
            throw "voice server job stopped early: $($Job.State)`n$buffer"
        }
    }
    throw "voice server did not become ready within 35 seconds"
}

function Invoke-ClientSim {
    param([string]$Codec)

    Set-Location $workspace
    $simArgs = "$Clients 127.0.0.1 $VoicePort $SharedSecret $FramesPerClient $PlayerSpacing PROXIMITY $Codec $PacketLossRate $ReorderWindow"
    $output = & .\gradlew.bat --console=plain :client-sim:run --args="$simArgs" 2>&1
    [pscustomobject]@{
        Codec = $Codec
        Output = $output
    }
}

function Write-SimSummary {
    param($Result)

    Write-Line ""
    Write-Line "=== $($Result.Codec) ==="
    $Result.Output |
        Where-Object {
            $_ -match "codec=" -or
            $_ -match "udpSentBytes=" -or
            $_ -match "voicePayloadSentBytes=" -or
            $_ -match "voiceFrames="
        } |
        ForEach-Object { Write-Line $_ }
}

$serverJob = $null
try {
    $serverJob = Start-VoiceServerJob -Workspace $workspace -ConfigPath $configPath
    Wait-VoiceServerReady -Job $serverJob

    $opus = Invoke-ClientSim -Codec "opus"
    $mock = Invoke-ClientSim -Codec "mock-pcm"

    Write-Line "MineVOICE codec bandwidth comparison"
    Write-Line "clients=$Clients framesPerClient=$FramesPerClient packetLossRate=$PacketLossRate reorderWindow=$ReorderWindow"
    Write-SimSummary -Result $opus
    Write-SimSummary -Result $mock
} finally {
    if ($serverJob) {
        Stop-Job -Job $serverJob -ErrorAction SilentlyContinue
        Remove-Job -Job $serverJob -Force -ErrorAction SilentlyContinue
    }
}
