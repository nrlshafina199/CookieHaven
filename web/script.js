// script.js

document.addEventListener("DOMContentLoaded", () => {
    loadProducts();
    updateCartCount();
});

async function loadProducts() {
    const container = document.getElementById("product-container");
    if (!container) return;

    try {
        const res = await fetch("/admin/products");
        const products = await res.json();

        container.innerHTML = products.map(p => `
            <div class="product-card" style="border:2px solid #5d4037; padding:20px; margin:10px; background:white; border-radius:10px;">
                <h3>${p.name}</h3>
                <p>RM ${p.price.toFixed(2)}</p>
                <div style="margin-bottom:10px;">
                    <button onclick="changeQty('${p.id}', -1)">-</button>
                    <input type="number" id="qty_${p.id}" value="1" min="1" style="width:40px; text-align:center;" readonly>
                    <button onclick="changeQty('${p.id}', 1)">+</button>
                </div>
                <button onclick="addToCart('${p.id}')" style="background:#5d4037; color:white; border:none; padding:10px; cursor:pointer;">Add to Cart</button>
            </div>
        `).join('');
    } catch (e) { console.error("Menu failed to load", e); }
}

function changeQty(id, delta) {
    const input = document.getElementById('qty_' + id);
    let val = parseInt(input.value) + delta;
    if (val < 1) val = 1;
    input.value = val;
}

function addToCart(productId) {
    const qty = document.getElementById('qty_' + productId).value;
    const params = new URLSearchParams();
    params.append("action", "add");
    params.append("productId", productId);
    params.append("quantity", qty);

    fetch("/api/cart", { method: "POST", body: params })
        .then(res => res.json())
        .then(data => {
            alert("Added!");
            const countEl = document.getElementById("cart-count");
            if (countEl) countEl.textContent = data.cartCount;
        });
}

function updateCartCount() {
    fetch("/api/cart")
        .then(res => res.json())
        .then(data => {
            const countEl = document.getElementById("cart-count");
            if (countEl) countEl.textContent = data.cartCount;
        });
}
async function loadCatalog() {
    const container = document.getElementById("product-grid"); // Check this ID in order.html!
    if (!container) return;

    try {
        const res = await fetch('/admin/products/api');
        const products = await res.json();

        if (products.length === 0) {
            container.innerHTML = "<p>No cookies available yet!</p>";
            return;
        }

        container.innerHTML = products.map(p => `
            <div class="cookie-card">
                <h3>${p.name}</h3>
                <p>RM ${p.price.toFixed(2)}</p>
                <button onclick="addToCart('${p.id}')">Add to Cart</button>
            </div>
        `).join('');
    } catch (err) {
        console.error("Failed to load catalog:", err);
    }
}

// Call it when page loads
document.addEventListener("DOMContentLoaded", loadCatalog);