param(
    [string]$SharedSecret = "minevoice-local-demo-secret",
    [int]$VoicePort = 24454,
    [int]$MinecraftPort = 25565,
    [ValidateSet("survival", "creative", "adventure", "spectator")]
    [string]$GameMode = "creative",
    [ValidateSet("auto", "java-sound", "openal")]
    [string]$AudioPlaybackBackend = "openal",
    [switch]$PrepareOnly
)

$ErrorActionPreference = "Stop"
$workspace = Split-Path -Parent $PSScriptRoot
$serverRun = Join-Path $workspace "minecraft-neoforge\run\demo-server"
$clientARun = Join-Path $workspace "minecraft-neoforge\run\demo-client-a"
$clientBRun = Join-Path $workspace "minecraft-neoforge\run\demo-client-b"
$demoPlayers = @("MineVoiceA", "MineVoiceB")

New-Item -ItemType Directory -Force -Path "$serverRun\config", "$clientARun\config", "$clientBRun\config" | Out-Null

function Get-OfflinePlayerUuid([string]$PlayerName) {
    $md5 = [System.Security.Cryptography.MD5]::Create()
    try {
        $hash = $md5.ComputeHash([System.Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$PlayerName"))
    } finally {
        $md5.Dispose()
    }

    $hash[6] = ($hash[6] -band 0x0F) -bor 0x30
    $hash[8] = ($hash[8] -band 0x3F) -bor 0x80
    $hex = [System.BitConverter]::ToString($hash).Replace("-", "").ToLowerInvariant()
    return "{0}-{1}-{2}-{3}-{4}" -f $hex.Substring(0, 8), $hex.Substring(8, 4), $hex.Substring(12, 4), $hex.Substring(16, 4), $hex.Substring(20, 12)
}

$demoOperators = @($demoPlayers | ForEach-Object {
    [PSCustomObject]@{
        uuid = Get-OfflinePlayerUuid $_
        name = $_
        level = 4
        bypassesPlayerLimit = $false
    }
})
$demoOperators | ConvertTo-Json | Set-Content -LiteralPath "$serverRun\ops.json" -Encoding ascii

@"
mode=local
localVoiceBindHost=0.0.0.0
localVoiceBindPort=$VoicePort
localVoiceAdvertiseHost=auto
localVoiceAdvertisePort=$VoicePort
enableLanVoiceServer=true
sharedSecret=$SharedSecret
proximityDistance=48
voiceCodec=opus
audioPlaybackBackend=auto
spatialBackend=auto
enableOcclusion=true
enableSpatialDebug=true
enableSoundPhysicsCompat=true
jitterBufferMs=60
enableDebugLog=true
"@ | Set-Content -LiteralPath "$serverRun\config\minevoice-server.properties" -Encoding ascii

@"
eula=true
"@ | Set-Content -LiteralPath "$serverRun\eula.txt" -Encoding ascii

@"
online-mode=false
server-port=$MinecraftPort
enforce-secure-profile=false
gamemode=$GameMode
force-gamemode=true
op-permission-level=4
motd=MineVOICE Local Embedded Demo
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
voiceCodec=opus
audioPlaybackBackend=$AudioPlaybackBackend
muted=false
deafened=false
showDebugConnectionInfo=true
"@ | Set-Content -LiteralPath "$clientARun\config\minevoice-client.properties" -Encoding ascii

Copy-Item -LiteralPath "$clientARun\config\minevoice-client.properties" -Destination "$clientBRun\config\minevoice-client.properties" -Force

Push-Location $workspace
try {
    .\gradlew.bat --console=plain :minecraft-neoforge:build
} finally {
    Pop-Location
}

if ($PrepareOnly) {
    Write-Host "MineVOICE Local embedded demo configuration prepared."
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

Start-DemoWindow "MineVOICE Minecraft Server (Local Voice)" ".\gradlew.bat :minecraft-neoforge:runDemoServer"
Start-Sleep -Seconds 8
Start-DemoWindow "MineVOICE Client A" ".\gradlew.bat :minecraft-neoforge:runDemoClientA"
Start-Sleep -Seconds 3
Start-DemoWindow "MineVOICE Client B" ".\gradlew.bat :minecraft-neoforge:runDemoClientB"

Write-Host "MineVOICE Local embedded demo started."
Write-Host "No standalone voice server window is needed in Local mode."
Write-Host "In both clients, join Multiplayer -> Direct Connection -> 127.0.0.1:$MinecraftPort"
Write-Host "MineVoiceA and MineVoiceB are forced to $GameMode mode and receive OP level 4."
Write-Host "Press O in either client to open MineVOICE, then test V proximity voice and G group voice."
