param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Username = ('checkout_demo_' + (Get-Date -Format 'yyyyMMddHHmmss')),
    [string]$Password = 'CheckoutDemo123!',
    [switch]$PlaceOrder
)

# Creates a dedicated demo customer, address and cart through the running backend.
# Use -PlaceOrder to also reserve stock and persist a pending-payment order.
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

function Send-CheckoutRequest([string]$Method, [string]$Path, $Body, $Headers = @{}) {
    $parameters = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $Headers; TimeoutSec = 30 }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10 -Compress))
    }
    $response = Invoke-RestMethod @parameters
    if (-not $response.success) { throw $response.message }
    return $response.data
}

$products = @(Send-CheckoutRequest GET '/api/products' $null | Where-Object { $_.status -eq 'ACTIVE' -and $_.stock -gt 0 })
$availableShops = @{}
foreach ($sellerId in @($products.sellerId | Select-Object -Unique)) {
    $shop = Send-CheckoutRequest GET "/api/seller/shops/$sellerId" $null
    if ($shop.status -eq 'ACTIVE') { $availableShops[[string]$sellerId] = $true }
}
$products = @($products | Where-Object { $availableShops.ContainsKey([string]$_.sellerId) })
if ($products.Count -eq 0) { throw 'No products from ACTIVE shops. Approve a seller before creating checkout demo data.' }
$customer = Send-CheckoutRequest POST '/api/auth/register/customer' @{
    username = $Username; email = "$Username@example.com"; password = $Password; confirmPassword = $Password
    fullName = 'Checkout Demo Customer'; phoneNumber = '0812345678'
}
$login = Send-CheckoutRequest POST '/api/auth/login' @{ usernameOrEmail = $Username; password = $Password }
$authorization = @{ Authorization = "Bearer $($login.token)" }
try {
    # Prefer one product per shop to exercise seller-specific shipment selection.
    $chosen = @($products | Group-Object sellerId | ForEach-Object { $_.Group | Select-Object -First 1 } | Select-Object -First 2)
    $cart = Send-CheckoutRequest POST '/api/checkout/cart' @{
        items = @($chosen | ForEach-Object { @{ productId = $_.productId; quantity = 1 } })
    } $authorization
    $address = Send-CheckoutRequest POST '/api/checkout/addresses' @{
        customerId = $customer.customerId; receiverName = 'Checkout Demo Customer'; phoneNumber = '0812345678'
        addressLine = '123 Demo Road'; district = 'Pathum Wan'; province = 'Bangkok'; postalCode = '10330'; isDefault = $true
    } $authorization
    $methods = @{}
    $quotes = @($chosen | ForEach-Object {
        $method = if ($cart.shippingMethods -contains 'FLASH') { 'FLASH' } else { $cart.shippingMethods[0] }
        $methods[[string]$_.sellerId] = $method
        Send-CheckoutRequest POST '/api/checkout/quote' @{ sellerId = $_.sellerId; shippingMethod = $method; addressId = $address.addressId } $authorization
    })
    $report = [ordered]@{ username = $Username; customerId = $customer.customerId; addressId = $address.addressId; quotes = $quotes; order = $null }
    if ($PlaceOrder) {
        $created = Send-CheckoutRequest POST '/api/checkout/orders' @{
            shippingAddressId = $address.addressId; sellerShippingMethods = $methods; paymentMethod = 'PROMPTPAY'
        } $authorization
        $stored = Send-CheckoutRequest GET "/api/checkout/orders/$($created.orderGroupId)" $null $authorization
        if ($stored.orderGroupId -ne $created.orderGroupId) { throw 'Persisted order could not be read back' }
        foreach ($order in $stored.subOrders) {
            if (-not $order.shippingMethod) { throw 'Shipping method was not persisted' }
        }
        $report.order = $stored
    }
    $report | ConvertTo-Json -Depth 12
} finally {
    $null = Send-CheckoutRequest POST '/api/auth/logout' @{} $authorization
}
