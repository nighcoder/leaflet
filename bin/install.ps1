
########################################################################################
#  ARGUMENTS
########################################################################################

Param (
    [Alias('i')][string] $identity,
    [Parameter(Mandatory=$true, Position=0)][string] $kernel
)

########################################################################################
#  DEFAULTS
########################################################################################

if (!$identity) {
    $identity = "leaflet-$(bin\version)"
}

$UserLibPath = "$env:LocalAppData\Programs"
$SystemLibPath = "$env:ProgramFiles"

if (Test-Path -PathType Container "$SystemLibPath\$kernel") {
    $LibDir = "$SystemLibPath\$kernel"
}

if (Test-Path -PathType Container "$UserLibPath\$kernel") {
    $LibDir = "$UserLibPath\$kernel"
}

########################################################################################
#  FAIL CONDITIONS
########################################################################################

if (!$LibDir) {
    Write-Error "Can't find kernel $kernel"
    exit 10
}

if (Test-Path -PathType Leaf "$LibDir\plugins\$idenity.jar") {
    Write-Error "Plugin $identity is already installed under $kernel"
    exit 20
}

if (! $(Test-Path -PathType Leaf "target\plugins\$identity.jar")) {
    Write-Error "Can't find jarfile $identity"
    exit 30
}

########################################################################################
#  INSTALLING
########################################################################################

Write-Host "Installing plugin $identity under $kernel"
Copy-Item "target\plugins\$identity.jar" "$LibDir\plugins"

foreach ($lib in @(Get-ChildItem "target\lib")) {
    if (! (Test-Path "$LibDir\lib\$lib")) {
        Copy-Item $lib.FullName "$LibDir\lib"
    }
}

Push-Location "$LibDir\lib"
New-Item -ItemType SymbolicLink -Path "$identity.jar" -Target "..\plugins\$identity.jar" | Out-Null
Pop-Location
