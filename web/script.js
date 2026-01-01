document.addEventListener("DOMContentLoaded", () => {
    // Try to load the admin view (if we are on the admin page)
    if (document.getElementById("product-container")) {
        loadAdminProducts();
    }

    // Try to load the customer view (if we are on the shop page)
    if (document.getElementById("product-grid")) {
        loadCatalog();
    }

    updateCartCount();

    // Fix for the Accept Button
    const acceptBtn = document.querySelector(".cookie-consent button");
    if(acceptBtn) {
        acceptBtn.addEventListener("click", () => {
            document.querySelector(".cookie-consent").style.display = "none";
        });
    }
});

// --- ADMIN PAGE FUNCTION ---
async function loadAdminProducts() {
    const container = document.getElementById("product-container");
    if (!container) return;

    try {
        const res = await fetch("/admin/products/api");
        if (!res.ok) throw new Error("Failed to load");
        const products = await res.json();

        container.innerHTML = products.map(p => `
            <div class="product-card" style="border:2px solid #5d4037; padding:20px; margin:10px; background:white; border-radius:10px;">
                <h3>${p.name}</h3>
                <p>RM ${p.price.toFixed(2)}</p>
                <p>Stock: ${p.stock}</p>
            </div>
        `).join('');
    } catch (e) { console.error("Admin menu failed", e); }
}

// --- CUSTOMER SHOP FUNCTION ---
async function loadCatalog() {
    const container = document.getElementById("product-grid");
    if (!container) return;

    try {
        const res = await fetch('/admin/products/api');

        // Error handling for visual feedback
        if (!res.ok) {
            container.innerHTML = "<p style='color:red'>Error loading catalog. Is the server running?</p>";
            return;
        }

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

function addToCart(productId) {
    const params = new URLSearchParams();
    params.append("action", "add");
    params.append("productId", productId);
    params.append("quantity", 1);

    fetch("/api/cart", { method: "POST", body: params })
        .then(res => res.json())
        .then(data => {
            alert("Added to cart!");
            updateCartCount();
        });
}

function updateCartCount() {
    fetch("/api/cart")
        .then(res => res.json())
        .then(data => {
            const countEl = document.getElementById("cart-count");
            if (countEl) countEl.textContent = data.cartCount || 0;
        })
        .catch(e => console.log("Cart not loaded yet"));
}