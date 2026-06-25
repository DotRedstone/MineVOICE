param(
    [string]$SharedSecret = "minevoice-remote-demo-secret",
    [int]$VoicePort = 24454,
    [int]$MinecraftPort = 25565,
    [switch]$PrepareOnly
)

$ErrorActionPreference = "Stop"
$workspace = Split-Path -Parent $PSScriptRoot
$demoDirectory = Join-Path $PSScriptRoot "remote-demo"
$serverRun = Join-Path $workspace "minecraft-neoforge\run\demo-server"
$clientARun = Join-Path $workspace "minecraft-neoforge\run\demo-client-a"
$clientBRun = Join-Path $workspace "minecraft-neoforge\run\demo-client-b"

New-Item -ItemType Directory -Force -Path $demoDirectory, "$serverRun\config", "$clientARun\config", "$clientBRun\config" | Out-Null

@"
bindHost=0.0.0.0
bindPort=$VoicePort
sharedSecret=$SharedSecret
maxPlayers=20
proximityDistance=48
enableBandwidthStats=true
enableDebugLog=true
"@ | Set-Content -LiteralPath (Join-Path $demoDirectory "minevoice-standalone.properties") -Encoding ascii

@"
mode=remote
remoteVoiceHost=127.0.0.1
remoteVoicePort=$VoicePort
sharedSecret=$SharedSecret
enableDebugLog=true
"@ | Set-Content -LiteralPath "$serverRun\config\minevoice-server.properties" -Encoding ascii

@"
eula=true
"@ | Set-Content -LiteralPath "$serverRun\eula.txt" -Encoding ascii

@"
online-mode=false
server-port=$MinecraftPort
enforce-secure-profile=false
motd=MineVOICE Remote Demo
"@ | Set-Content -LiteralPath "$serverRun\server.properties" -Encoding ascii

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
"@ | Set-Content -LiteralPath "$clientARun\config\minevoice-client.properties" -Encoding ascii

Copy-Item -LiteralPath "$clientARun\config\minevoice-client.properties" -Destination "$clientBRun\config\minevoice-client.properties" -Force

Push-Location $workspace
try {
    .\gradlew.bat --console=plain :standalone-server:installDist :minecraft-neoforge:build
} finally {
    Pop-Location
}

if ($PrepareOnly) {
    Write-Host "MineVOICE Remote demo configuration prepared."
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

Start-DemoWindow "MineVOICE Voice Server (Remote)" ".\gradlew.bat :standalone-server:run --args='../scripts/remote-demo/minevoice-standalone.properties'"
Start-Sleep -Seconds 3
Start-DemoWindow "MineVOICE Minecraft Server" ".\gradlew.bat :minecraft-neoforge:runDemoServer"
Start-Sleep -Seconds 8
Start-DemoWindow "MineVOICE Client A" ".\gradlew.bat :minecraft-neoforge:runDemoClientA"
Start-Sleep -Seconds 3
Start-DemoWindow "MineVOICE Client B" ".\gradlew.bat :minecraft-neoforge:runDemoClientB"

Write-Host "MineVOICE Remote demo started."
Write-Host "In both clients, join Multiplayer -> Direct Connection -> 127.0.0.1:$MinecraftPort"
