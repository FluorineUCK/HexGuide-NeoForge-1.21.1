$ErrorActionPreference = 'Stop'

$project = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$java = Join-Path $project 'src/main/java'
$resources = Join-Path $project 'src/main/resources'
$failures = [System.Collections.Generic.List[string]]::new()

# A no-argument instance target, MsgNewSpellPatternS2C.handle()V, needs an
# instance injection handler. A static handler with a synthetic "self" argument
# passes javac but is rejected when Mixin applies.
$mixin = Get-Content -LiteralPath (Join-Path $java 'cn/xm1221/HexGuide/mixin/MixinMsgNewSpellPatternS2C.java') -Raw
if ($mixin -match 'private\s+static\s+void\s+routeToEmbedded' -or
    $mixin -match 'routeToEmbedded[^\(]*\(MsgNewSpellPatternS2C\s+self') {
    $failures.Add('MixinMsgNewSpellPatternS2C uses an invalid static/self handler for target handle()V')
}
$marker = Join-Path $java 'cn/xm1221/HexGuide/patchouli/EmbeddedSpellResultAccess.java'
if (-not (Test-Path -LiteralPath $marker) -or
    $mixin -notmatch 'implements\s+EmbeddedSpellResultAccess') {
    $failures.Add('MsgNewSpellPatternS2C mixin has no runtime-verifiable marker interface')
}

# Server gameplay code may use the common codec, but must not load the
# InlineData implementation which contains client resource-manager references.
foreach ($relative in @(
    'cn/xm1221/HexGuide/casting/actions/OpTextCopy.kt',
    'cn/xm1221/HexGuide/demo/DemoGenerator.kt'
)) {
    $file = Join-Path $java $relative
    if ((Get-Content -LiteralPath $file -Raw) -match 'compat\.inline\.IotaInlineData') {
        $failures.Add("Cross-side dependency remains: $relative -> IotaInlineData")
    }
}

$inlineData = Get-Content -LiteralPath (
    Join-Path $java 'cn/xm1221/HexGuide/compat/inline/IotaInlineData.java'
) -Raw
if ($inlineData -match 'net\.minecraft\.client\.' -or
    $inlineData -match 'Minecraft\.getInstance\(') {
    $failures.Add('Common InlineData still links client-only Minecraft classes')
}

$clientProbe = Get-Content -LiteralPath (
    Join-Path $java 'cn/xm1221/HexGuide/neo/HexGuideNeoClient.kt'
) -Raw
foreach ($proof in @(
    'BookSpellcastingAccess::class.java.isAssignableFrom',
    'EmbeddedSpellResultAccess::class.java.isAssignableFrom',
    'BookRegistry.INSTANCE.books',
    'contents.entries'
)) {
    if ($clientProbe -notmatch [regex]::Escape($proof)) {
        $failures.Add("Client runtime probe lacks proof: $proof")
    }
}

# Every declared payload must have a unique ID and be registered exactly once.
$payloadSource = Get-Content -LiteralPath (
    Join-Path $java 'cn/xm1221/HexGuide/networking/msg/HexGuidePayloads.kt'
) -Raw
$networkSource = Get-Content -LiteralPath (
    Join-Path $java 'cn/xm1221/HexGuide/networking/HexGuideNetworking.kt'
) -Raw
$payloadClasses = @([regex]::Matches($payloadSource, '(?:data\s+)?class\s+(Msg[A-Za-z0-9]+)\b') |
    ForEach-Object { $_.Groups[1].Value })
$payloadIds = @([regex]::Matches($payloadSource, 'HexGuide\.id\("([a-z0-9_/]+)"\)') |
    ForEach-Object { $_.Groups[1].Value })
if ($payloadClasses.Count -ne 13) {
    $failures.Add("Expected 13 active payload classes, found $($payloadClasses.Count)")
}
if (@($payloadIds | Sort-Object -Unique).Count -ne $payloadIds.Count) {
    $failures.Add('Network payload IDs are not unique')
}
foreach ($payload in $payloadClasses) {
    $registrations = [regex]::Matches($networkSource, [regex]::Escape($payload + '.TYPE')).Count
    if ($registrations -ne 1) {
        $failures.Add("Payload $payload registration count is $registrations, expected 1")
    }
}

# The port must not execute old loader APIs.
$legacy = Get-ChildItem -LiteralPath $java -Recurse -File |
    Where-Object { $_.Extension -in '.java', '.kt' } |
    Select-String -Pattern '^\s*import\s+(dev\.architectury|net\.fabricmc|net\.minecraftforge)\.'
foreach ($hit in $legacy) {
    $failures.Add("Legacy loader API: $($hit.Path):$($hit.LineNumber)")
}

# Registry inventory and localisation coverage.
$itemSource = Get-Content -LiteralPath (Join-Path $java 'cn/xm1221/HexGuide/registry/HexGuideItems.kt') -Raw
$actionSource = Get-Content -LiteralPath (Join-Path $java 'cn/xm1221/HexGuide/registry/HexGuideActions.kt') -Raw
$itemIds = @([regex]::Matches($itemSource, 'register\("([^"]+)"\)') | ForEach-Object { $_.Groups[1].Value })
$actionIds = @([regex]::Matches($actionSource, 'make\("([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
if ($itemIds.Count -ne 2) { $failures.Add("Expected 2 registered items, found $($itemIds.Count)") }
if ($actionIds.Count -ne 6) { $failures.Add("Expected 6 registered actions, found $($actionIds.Count)") }

$languages = @{}
foreach ($locale in 'en_us', 'zh_cn') {
    $object = Get-Content -LiteralPath (Join-Path $resources "assets/hexguide/lang/$locale.json") -Raw |
        ConvertFrom-Json
    $keys = @{}
    foreach ($property in $object.PSObject.Properties) { $keys[$property.Name] = $true }
    $languages[$locale] = $keys
    foreach ($id in $itemIds) {
        if (-not $keys.ContainsKey("item.hexguide.$id")) {
            $failures.Add("$locale missing item.hexguide.$id")
        }
    }
    foreach ($id in $actionIds) {
        if (-not $keys.ContainsKey("hexcasting.action.hexguide:$id")) {
            $failures.Add("$locale missing hexcasting.action.hexguide:$id")
        }
    }
}
$englishKeys = @($languages.en_us.Keys | Sort-Object)
$chineseKeys = @($languages.zh_cn.Keys | Sort-Object)
if (Compare-Object $englishKeys $chineseKeys) {
    $failures.Add('en_us/zh_cn language key sets differ')
}

# Source translatable calls and Patchouli translation-valued fields must exist
# in both locales. Registry IDs/page-type IDs are deliberately excluded.
$translationRefs = [System.Collections.Generic.HashSet[string]]::new()
Get-ChildItem -LiteralPath $java -Recurse -File |
    Where-Object { $_.Extension -in '.java', '.kt' } |
    ForEach-Object {
        $raw = Get-Content -LiteralPath $_.FullName -Raw
        [regex]::Matches($raw, 'Component\.translatable\(\s*"([^"]+)"') | ForEach-Object {
            $key = $_.Groups[1].Value
            if ($key -like 'hexguide.*' -or $key -like 'item.hexguide.*' -or
                $key -like 'tab.hexguide.*' -or $key -like 'key.hexguide.*' -or
                $key -like 'category.hexguide.*' -or $key -like 'hexcasting.action.hexguide:*') {
                [void]$translationRefs.Add($key)
            }
        }
    }

# Patchouli references must resolve to registered content/page types.
$patchouli = @(Get-ChildItem -LiteralPath (
    Join-Path $resources 'assets/hexcasting/patchouli_books/thehexbook/en_us'
) -Recurse -File -Filter '*.json')
$actionReferences = @()
$itemReferences = @()
$customTypes = @()
foreach ($file in $patchouli) {
    $raw = Get-Content -LiteralPath $file.FullName -Raw
    $actionReferences += [regex]::Matches($raw, '"op_id"\s*:\s*"hexguide:([^"]+)"') |
        ForEach-Object { $_.Groups[1].Value }
    $itemReferences += [regex]::Matches($raw, '"(?:item|icon)"\s*:\s*"hexguide:([^"]+)"') |
        ForEach-Object { $_.Groups[1].Value }
    $customTypes += [regex]::Matches($raw, '"type"\s*:\s*"hexguide:([^"]+)"') |
        ForEach-Object { $_.Groups[1].Value }
    [regex]::Matches($raw, '"(?:name|description|text|title|link_text)"\s*:\s*"(hexguide\.[^"]+)"') |
        ForEach-Object { [void]$translationRefs.Add($_.Groups[1].Value) }
}
foreach ($key in $translationRefs) {
    foreach ($locale in 'en_us', 'zh_cn') {
        if (-not $languages[$locale].ContainsKey($key)) {
            $failures.Add("$locale missing referenced translation $key")
        }
    }
}
foreach ($id in @($actionReferences | Sort-Object -Unique)) {
    if ($id -notin $actionIds) { $failures.Add("Patchouli references unregistered action hexguide:$id") }
}
foreach ($id in @($itemReferences | Sort-Object -Unique)) {
    if ($id -notin $itemIds) { $failures.Add("Patchouli references unregistered item hexguide:$id") }
}
$clientSource = Get-Content -LiteralPath (Join-Path $java 'cn/xm1221/HexGuide/HexGuideClient.kt') -Raw
foreach ($type in @($customTypes | Sort-Object -Unique)) {
    if ($clientSource -notmatch [regex]::Escape('"' + $type + '"')) {
        $failures.Add("Patchouli custom page type not registered: hexguide:$type")
    }
}

# Both own items need a normal creative-inventory location. The custom tab is
# intentionally for generated pattern slates/scrolls.
$neoSource = Get-Content -LiteralPath (Join-Path $java 'cn/xm1221/HexGuide/neo/HexGuideNeo.kt') -Raw
foreach ($symbol in 'AMETHYST_PEN', 'NOTE_SCRAP') {
    if ($neoSource -notmatch $symbol) { $failures.Add("Creative inventory missing $symbol") }
}

$legacyRecipe = Join-Path $resources 'data/hexguide/recipes/amethyst_pen.json'
if (Test-Path -LiteralPath $legacyRecipe) {
    $failures.Add('Legacy 1.20 recipes/ directory is still active; 1.21.1 requires recipe/')
}
$recipePath = Join-Path $resources 'data/hexguide/recipe/amethyst_pen.json'
if (-not (Test-Path -LiteralPath $recipePath)) {
    $failures.Add('Missing 1.21.1 recipe/hexguide:amethyst_pen resource')
    $recipe = $null
} else {
    $recipe = Get-Content -LiteralPath $recipePath -Raw |
    ConvertFrom-Json
}
if ($recipe -and (-not $recipe.result.id -or $recipe.result.item)) {
    $failures.Add('amethyst_pen recipe is not in the 1.21 result.id format')
}
if ($recipe -and @($recipe.ingredients | Where-Object { $_ -is [string] }).Count -gt 0) {
    $failures.Add('amethyst_pen recipe still uses legacy bare-string ingredients')
}

# Retired Connector/Architectury tag-fixer assets must not be active.
foreach ($relative in 'architectury.common.json', 'hexguide.accesswidener') {
    if (Test-Path -LiteralPath (Join-Path $resources $relative)) {
        $failures.Add("Obsolete resource active: $relative")
    }
}
if (Test-Path -LiteralPath (Join-Path $java 'cn/xm1221/HexGuide/neo/HexGuideTagFixer.kt')) {
    $failures.Add('Obsolete HexGuideTagFixer is still active')
}
foreach ($pattern in 'HexGuideClientConfig', 'HexGuideServerConfig', 'ConfigHelper', 'MsgSyncConfig',
    'me.shedaniel', 'dummyClientConfigOption', 'dummyServerConfigOption', 'fixTags') {
    $scanFiles = @(
        Get-ChildItem -LiteralPath (Join-Path $project 'src/main') -Recurse -File |
            Where-Object { $_.Name -notlike '*.flatten.json5' }
    ) + @(
        Get-Item -LiteralPath (Join-Path $project 'build.gradle'), (Join-Path $project 'gradle.properties')
    )
    $hits = $scanFiles |
        Select-String -Pattern $pattern -SimpleMatch
    if ($hits) { $failures.Add("Retired config surface remains: $pattern") }
}

# Bundled iota assets are upstream 0.11 typed compounds. Hex 0.12/pre39
# switched the public codec to {type, ...payload fields}; keep an explicit
# legacy adapter so guide Inline references do not silently become red crosses.
$codecSource = Get-Content -LiteralPath (
    Join-Path $java 'cn/xm1221/HexGuide/hexcompat/HexCodecCompat.kt'
) -Raw
foreach ($proof in @(
    'fun deserializeLegacyIota',
    'LEGACY_TYPE_KEY',
    'LEGACY_DATA_KEY',
    'hexcasting:pattern',
    'hexcasting:list',
    'hexcasting:continuation'
)) {
    if ($codecSource -notmatch [regex]::Escape($proof)) {
        $failures.Add("Legacy bundled-iota adapter lacks proof: $proof")
    }
}
$bundledIotas = @(Get-ChildItem -LiteralPath (
    Join-Path $resources 'assets/hexguide/iotas'
) -File -Filter '*.json')
if ($bundledIotas.Count -ne 36) {
    $failures.Add("Expected 36 bundled iota resources, found $($bundledIotas.Count)")
}
foreach ($iotaResource in $bundledIotas) {
    $raw = Get-Content -LiteralPath $iotaResource.FullName -Raw
    if ($raw -notmatch '\\"hexcasting:type\\"' -or $raw -notmatch '\\"hexcasting:data\\"') {
        $failures.Add("Bundled iota is not in the historical format: $($iotaResource.Name)")
    }
}
$clientProbeSource = Get-Content -LiteralPath (
    Join-Path $java 'cn/xm1221/HexGuide/neo/HexGuideNeoClient.kt'
) -Raw
foreach ($proof in @(
    'listResources("iotas")',
    'inlineResourceCount == 36',
    'inlineDecodedCount == if (moreIotasLoaded) 36 else 35'
)) {
    if ($clientProbeSource -notmatch [regex]::Escape($proof)) {
        $failures.Add("Client runtime probe does not cover all bundled iotas: $proof")
    }
}

if ($failures.Count) {
    Write-Output 'HEXGUIDE_STATIC_CONTRACT: FAIL'
    $failures | ForEach-Object { Write-Output " - $_" }
    exit 1
}

$typeCount = @($customTypes | Sort-Object -Unique).Count
Write-Output (
    "HEXGUIDE_STATIC_CONTRACT: PASS items=$($itemIds.Count) actions=$($actionIds.Count) " +
    "payloads=$($payloadClasses.Count) patchouliFiles=$($patchouli.Count) customPageTypes=$typeCount " +
    "bundledIotas=$($bundledIotas.Count) referencedTranslations=$($translationRefs.Count) langKeys=$($englishKeys.Count)"
)
