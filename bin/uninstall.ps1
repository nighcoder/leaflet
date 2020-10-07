
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
    $identity = "leaflet-$(bin/version)"
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

if (! $(Test-Path -PathType Leaf "$LibDir\plugins\$identity.jar")) {
    Write-Error "Can't find $identity installed under $kernel"
    exit 30
}

########################################################################################
#  DEPENDENCIES
########################################################################################

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-Dependencies {
    Param (
        $jar
    )
    $jarfile = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    $manifest = $jarfile |
                Select-Object -ExpandProperty Entries |
                Where-Object {$_.Name -eq "MANIFEST.MF"}
    $file = New-TemporaryFile
    $file.Delete()
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($manifest, $file.FullName)
    $jarfile.Dispose()
    $CP_key = $false
    $deps = foreach ($line in $(Get-Content $file)) {
                Switch -Regex ($line) {
                    "^Class-Path: (.+)$" {
                        $CP_key = $true
                        Write-Output $matches[1]
                    }
                    "^[^:]+$" {
                        if ($CP_key) {
                                Write-Output $line.Substring(1)
                            }
                    }
                    default {
                        $CP_key = $false
                    }
                }
            }
    if ($deps) {
        foreach ($dep in $($deps -join '').split(" ")) {
            $file = Get-ChildItem "$($jar.Directory)\$dep"
            if ($file.target) {
                Get-ChildItem $file.target
            }
            else {
                $file
            }
        }
    }
}

$deps = Get-Dependencies $(Get-ChildItem "$LibDir\plugins\$identity.jar") |
        Where-Object {! $_.Directory.Name -eq "plugins" }

$other_deps = @()
foreach ($jar in $(Get-ChildItem -Exclude "$identity.jar" "$LibDir\$kernel.jar", "$LibDir\plugins\*")) {
    $dep = Get-Dependencies $jar
    if ($dep.Basename -Contains $identity.jar) {
        Write-Error "Can't uninstall $identity bacause $($jar.Basename) depends on it."
        exit 20
    }
    $other_deps += $dep
}

########################################################################################
#  UNINSTALLING
########################################################################################

Remove-Item "$LibDir\plugins\$identity.jar"
Remove-Item -ErrorAction SilentlyContinue "$LibDir\plugins\enabled\$identity.jar"
Remove-Item "$LibDir\lib\$identity.jar"

foreach ($dep in $deps) {
    if (! $other_deps -Contains $dep) {
        Write-Ouput "Removing dependency $($dep.Name)"
        Remove-Item $dep
    }
}
exit
