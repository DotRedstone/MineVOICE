param(
    [string]$SharedSecret = "minevoice-lan-demo-secret",
    [int]$VoicePort = 24454,
    [switch]$PrepareOnly
)

$ErrorActionPreference = "Stop"
$workspace = Split-Path -Parent $PSScriptRoot
$hostRun = Join-Path $workspace "minecraft-neoforge\run\demo-client-a"
$guestRun = Join-Path $workspace "minecraft-neoforge\run\demo-client-b"

New-Item -ItemType Directory -Force -Path "$hostRun\config", "$guestRun\config" | Out-Null

@"
mode=local
localVoiceBindHost=0.0.0.0
localVoiceBindPort=$VoicePort
localVoiceAdvertiseHost=auto
localVoiceAdvertisePort=$VoicePort
enableLanVoiceServer=true
sharedSecret=$SharedSecret
proximityDistance=48
enableDebugLog=true
"@ | Set-Content -LiteralPath "$hostRun\config\minevoice-server.properties" -Encoding ascii

@"
microphoneDevice=default
outputDevice=default
pushToTalkKey=V
masterVolume=1.0
voiceChatVolume=1.0
microphoneVolume=1.0
activationMode=PUSH_TO_TALK
voiceActivationThreshold=0.35
spatialAudioEnabled=true
voiceCodec=mock
muted=false
deafened=false
showDebugConnectionInfo=true
"@ | Set-Content -LiteralPath "$hostRun\config\minevoice-client.properties" -Encoding ascii

Copy-Item -LiteralPath "$hostRun\config\minevoice-client.properties" -Destination "$guestRun\config\minevoice-client.properties" -Force

Push-Location $workspace
try {
    .\gradlew.bat --console=plain :minecraft-neoforge:build
} finally {
    Pop-Location
}

if ($PrepareOnly) {
    Write-Host "MineVOICE LAN demo configuration prepared."
    exit 0
}

function Start-DemoWindow([string]$Title, [string]$Command) {
    $escapedWorkspace = $workspace.Replace("'", "''")
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-Command",
        "`$Host.UI.RawUI.WindowTitle='$Title'; Set-Location '$escapedWorkspace'; $Command"
    )
}

Start-DemoWindow "MineVOICE LAN Host Client" ".\gradlew.bat :minecraft-neoforge:runDemoClientA"
Start-Sleep -Seconds 4
Start-DemoWindow "MineVOICE LAN Guest Client" ".\gradlew.bat :minecraft-neoforge:runDemoClientB"

Write-Host "MineVOICE LAN demo clients started."
Write-Host "In Client A: Singleplayer -> Open to LAN."
Write-Host "In Client B: Multiplayer -> join the LAN world shown by Minecraft."
Write-Host "If you use ZeroTier/Radmin/Tailscale, set localVoiceAdvertiseHost to that adapter IP."
