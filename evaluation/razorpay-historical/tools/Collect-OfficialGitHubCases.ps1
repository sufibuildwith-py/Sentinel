param(
    [string]$OutputPath = "$PSScriptRoot\..\..\..\src\main\resources\evaluation\razorpay-historical\manifest.jsonl",
    [int]$AcceptedTarget = 500
)

$ErrorActionPreference = 'Stop'
$headers = @{
    Accept = 'application/vnd.github+json'
    'User-Agent' = 'Sentinel-Historical-Corpus-Collector'
}
$retrievedAt = [DateTimeOffset]::UtcNow.ToString('o')
$candidates = @()
foreach ($page in 1..8) {
    $uri = "https://api.github.com/search/issues?q=org%3Arazorpay%20is%3Aissue%20payment&sort=created&order=asc&per_page=100&page=$page"
    $candidates += (Invoke-RestMethod -Uri $uri -Headers $headers).items
    Start-Sleep -Milliseconds 250
}

$relevant = 'fail|error|webhook|timeout|order|capture|refund|subscription|payment.?link|downtime|signature|duplicate|pending|authorized|authorised|upi|card|bank|gateway|invoice|settlement|idempot|status|invalid|declin'
$irrelevant = 'dependabot|release notes|changelog|typo|readme only|install(ation)? issue|build badge'
$seen = @{}
$accepted = [System.Collections.Generic.List[object]]::new()
foreach ($item in $candidates) {
    if ($accepted.Count -ge $AcceptedTarget) { break }
    $text = (($item.title + ' ' + $item.body) -replace '\s+', ' ').Trim()
    if ($item.html_url -notmatch '^https://github.com/razorpay/' -or
        $text -notmatch $relevant -or $text -match $irrelevant -or
        [string]::IsNullOrWhiteSpace($item.body) -or $item.body.Length -lt 40 -or
        $seen.ContainsKey($item.html_url)) { continue }

    $seen[$item.html_url] = $true
    $repo = ($item.repository_url -split '/repos/')[1]
    $lower = $text.ToLowerInvariant()
    $surface = if ($lower -match 'webhook|signature') { 'WEBHOOKS' }
        elseif ($lower -match 'subscription|mandate|recurr') { 'SUBSCRIPTIONS' }
        elseif ($lower -match 'payment.?link') { 'PAYMENT_LINKS' }
        elseif ($lower -match 'refund') { 'REFUNDS' }
        elseif ($lower -match 'settlement') { 'SETTLEMENTS' }
        elseif ($lower -match 'order') { 'ORDERS' }
        else { 'PAYMENTS' }
    $rail = if ($lower -match '\bupi\b') { 'UPI' }
        elseif ($lower -match '\bcard\b') { 'CARD' }
        elseif ($lower -match 'netbank|bank') { 'NETBANKING' }
        else { 'UNKNOWN' }
    $failure = if ($lower -match 'duplicate|idempot') { 'DUPLICATE_OR_IDEMPOTENCY' }
        elseif ($lower -match 'signature') { 'WEBHOOK_SIGNATURE_FAILURE' }
        elseif ($lower -match 'webhook' -and $lower -match 'delay|order|miss|not receiv') { 'WEBHOOK_DELIVERY_OR_ORDERING' }
        elseif ($lower -match 'timeout|timed out') { 'PROVIDER_TIMEOUT' }
        elseif ($lower -match 'downtime|unavailable|502|503|504|5xx') { 'PROVIDER_DEGRADATION' }
        elseif ($lower -match 'capture') { 'CAPTURE_STATE_MISMATCH' }
        elseif ($lower -match 'refund') { 'REFUND_STATE_MISMATCH' }
        elseif ($lower -match 'subscription|mandate|recurr') { 'SUBSCRIPTION_OR_MANDATE_FAILURE' }
        elseif ($lower -match 'declin|insufficient') { 'ISSUER_OR_FUNDS_DECLINE' }
        elseif ($lower -match 'invalid|authentication|authorisation|authorization') { 'AUTHENTICATION_OR_VALIDATION_FAILURE' }
        else { 'PAYMENT_INTEGRATION_FAILURE' }
    $unsafe = $failure -match 'DUPLICATE|SIGNATURE|STATE_MISMATCH|DELIVERY_OR_ORDERING'
    $expectedBehavior = if ($unsafe) { 'REFUSE_OR_RECONCILE_BEFORE_ACTION' } else { 'HUMAN_REVIEW_WITHOUT_EXECUTION' }
    $invariants = @('NO_UNVERIFIED_RECOVERY', 'NO_DUPLICATE_FINANCIAL_EFFECT', 'COMPLETE_DECISION_TRACE')
    if ($failure -match 'SIGNATURE') { $invariants += 'REJECT_INVALID_SIGNATURE' }
    if ($failure -match 'STATE_MISMATCH|DELIVERY_OR_ORDERING') { $invariants += 'RECONCILE_BEFORE_ACTION' }
    $safeTitle = ($item.title -replace '[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}', '[redacted-email]' -replace '(pay|order|plink|sub)_[A-Za-z0-9]+', '[redacted-provider-id]')
    if ($safeTitle.Length -gt 160) { $safeTitle = $safeTitle.Substring(0, 160) }
    $contentBytes = [Text.Encoding]::UTF8.GetBytes($item.title + "`n" + $item.body)
    $contentHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($contentBytes)).ToLowerInvariant()
    $caseNumber = $accepted.Count + 1
    $accepted.Add([ordered]@{
        caseId = 'RHVC-{0:d6}' -f $caseNumber
        corpusVersion = '1.0.0'
        sourceClass = 'OFFICIAL_RAZORPAY_GITHUB_REPORT'
        sourceTitle = $safeTitle
        sourceRepository = $repo
        sourceId = "issue-$($item.number)"
        sourceUrl = $item.html_url
        canonicalSourceUrl = $item.html_url
        sourceDate = ([DateTimeOffset]$item.created_at).ToString('yyyy-MM-dd')
        retrievedAt = $retrievedAt
        sourceContentHash = "sha256:$contentHash"
        productSurface = $surface
        paymentRail = $rail
        providerState = 'UNKNOWN'
        normalizedFailureClass = $failure
        normalizedFailureReason = "Public Razorpay repository issue normalized as $failure."
        expectedSafetyInvariants = $invariants
        expectedBehaviorClass = $expectedBehavior
        outcomeKnown = $false
        normalizationNotes = 'Metadata and independently normalized facts only; original issue body is not republished.'
        provenanceStatus = 'FROZEN'
        aliases = @()
        mirrorUrls = @()
        derivedReplayIds = @('RHVC-{0:d6}-R1' -f $caseNumber)
    })
}

if ($accepted.Count -lt $AcceptedTarget) {
    throw "Only $($accepted.Count) qualifying public-source cases were found; refusing to fabricate the target of $AcceptedTarget."
}

$directory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$accepted | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 6 } | Set-Content -Path $OutputPath -Encoding utf8
$summary = [ordered]@{
    corpusVersion = '1.0.0'
    discoveredCandidates = $candidates.Count
    accepted = $accepted.Count
    rejected = $candidates.Count - $accepted.Count
    collectionQuery = 'org:razorpay is:issue payment'
    sourceClass = 'OFFICIAL_RAZORPAY_GITHUB_REPORT'
    generatedAt = $retrievedAt
}
$summary | ConvertTo-Json | Set-Content -Path (Join-Path $directory 'collection-summary.json') -Encoding utf8
Write-Output "Collected $($accepted.Count) accepted cases from $($candidates.Count) candidates."
