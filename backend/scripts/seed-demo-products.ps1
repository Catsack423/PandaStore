param(
    [string]$BaseUrl = 'http://localhost:8080',
    [long]$SellerId = 2,
    [Parameter(Mandatory)][string]$Username,
    [Parameter(Mandatory)][string]$Password
)

# Adds a repeatable demo catalog through the Product API. Existing names are skipped.
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
function Send-DemoProductRequest([string]$Method, [string]$Path, $Body, $Headers = @{}) {
    $request = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $Headers; TimeoutSec = 30 }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
    }
    $response = Invoke-RestMethod @request
    if (-not $response.success) { throw $response.message }
    return $response.data
}

$shop = Send-DemoProductRequest GET "/api/seller/shops/$SellerId" $null
if ($shop.status -ne 'ACTIVE') { throw 'The demo seller must be approved before seeding products.' }
$catalog = @(
    @{ name = 'Demo Havit USB Gamepad'; price = 29; stock = 12; image = 1 },
    @{ name = 'Demo iPhone 14 Plus 128GB'; price = 699; stock = 5; image = 2 },
    @{ name = 'Demo Apple iMac M1 Desktop 24-inch'; price = 1099; stock = 3; image = 3 },
    @{ name = 'Demo MacBook Air M1 Laptop 256GB'; price = 899; stock = 8; image = 4 },
    @{ name = 'Demo Apple Watch Ultra'; price = 499; stock = 1; image = 5 },
    @{ name = 'Demo Logitech MX Master 3 Mouse'; price = 79; stock = 25; image = 6 },
    @{ name = 'Demo Apple iPad Air 64GB'; price = 549; stock = 2; image = 7 },
    @{ name = 'Demo Asus Dual Band Router'; price = 89; stock = 15; image = 8 }
)
$login = Send-DemoProductRequest POST '/api/auth/login' @{ usernameOrEmail = $Username; password = $Password }
$headers = @{ Authorization = "Bearer $($login.token)" }
try {
    $existing = @(Send-DemoProductRequest GET "/api/products/seller/$SellerId" $null $headers)
    $report = foreach ($demo in $catalog) {
        $product = $existing | Where-Object name -EQ $demo.name | Select-Object -First 1
        $created = $false
        if (-not $product) {
            $product = Send-DemoProductRequest POST "/api/products?sellerId=$SellerId" @{
                name = $demo.name
                description = 'Demo catalog product for storefront, cart stock limits and checkout testing.'
                price = $demo.price; stock = $demo.stock; categoryIds = @()
                shippingInfo = 'Choose your delivery service at checkout'
                imageUrls = @("/images/products/product-$($demo.image)-bg-1.png", "/images/products/product-$($demo.image)-bg-2.png")
            } $headers
            $created = $true
        }
        [pscustomobject]@{ productId = $product.productId; name = $product.name; stock = $product.stock; created = $created }
    }
    $report | Format-Table -AutoSize
} finally {
    $null = Send-DemoProductRequest POST '/api/auth/logout' @{} $headers
}
