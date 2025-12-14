// --- Setup ---
const CART_API_URL = '/api/cart';
const PRODUCT_PRICE = 5.00; // Used for safety calculations if price not found in DOM

// --- Existing Code for Old /order Form Submission (kept for safety) ---

const form = document.getElementById("orderForm");

if (form) {
    form.addEventListener("submit", function(e){
        e.preventDefault();
        const formData = new FormData(form);
        const data = {};
        formData.forEach((value,key) => data[key] = value);

        fetch("/order", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams(data)
        })
            .then(res => res.text())
            .then(text => {
                alert(text);
                form.reset();
            })
            .catch(error => console.error('Error submitting old order form:', error));
    });
}


// --------------------------------------------------------------------
// --- MODULE 3: NEW CART & CHECKOUT LOGIC ---
// --------------------------------------------------------------------


// ---------------------------------------------
// 0. INITIAL LOAD (NEWLY ADDED)
// ---------------------------------------------

/**
 * Fetches the current cart count from the server session on page load.
 */
function loadInitialCartCount() {
    // We fetch using a simple GET request
    fetch(CART_API_URL, {
        method: 'GET'
    })
    .then(response => {
        // If the server response is OK, parse the JSON
        if (response.ok) {
            return response.json();
        }
        throw new Error('Failed to fetch initial cart count.');
    })
    .then(result => {
        const cartCounter = document.getElementById('cart-count');
        if (cartCounter && result && typeof result.cartCount !== 'undefined') {
            cartCounter.textContent = result.cartCount;
        }
    })
    .catch(error => {
        console.warn('Initial cart count could not be loaded. Server may be down or endpoint misconfigured.', error);
    });
}

// Call the function when the DOM content is fully loaded
document.addEventListener('DOMContentLoaded', function() {
    // Only call this on pages that have the cart-count element
    if (document.getElementById('cart-count')) {
        loadInitialCartCount();
    }
    // Also run the existing total calculation on the cart page
    updateCartGrandTotal();
});


// ---------------------------------------------
// 1. ADD TO CART (AJAX Requirement: No page reload)
// ---------------------------------------------

/**
 * Sends an AJAX request to the Java backend to add an item to the session cart.
 */
function addToCart(productId, quantity = 1) {
    // Ensure quantity is a valid number
    const qty = parseInt(quantity);
    if (qty <= 0 || isNaN(qty)) {
        alert('Please enter a valid quantity.');
        return;
    }

    const data = new URLSearchParams();
    data.append('action', 'add');
    data.append('productId', productId);
    data.append('quantity', qty);

    fetch(CART_API_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: data
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(result => {
        if (result.success) {
            // Live cart count update
            const cartCounter = document.getElementById('cart-count');
            if (cartCounter) {
                cartCounter.textContent = result.cartCount;
            }
            alert('Item added to cart!');
        }
    })
    .catch(error => {
        console.error('Error adding to cart:', error);
        alert('Failed to add item to cart. See console for details.');
    });
}


// ---------------------------------------------
// 2. UPDATE/DELETE CART (JS Heavy Requirement: Live price calculation)
// ---------------------------------------------

/**
 * Updates the quantity of an item in the cart via AJAX, and performs
 * live client-side price calculation.
 */
function updateCartQuantity(productId, inputElement) {
    const row = inputElement.closest('tr');
    const newQuantity = parseInt(inputElement.value);

    // --- Live Price Calculation ---
    const priceElement = row.querySelector('.item-price');
    const subtotalElement = row.querySelector('.item-subtotal');

    const price = parseFloat(priceElement.textContent) || PRODUCT_PRICE;
    const newSubtotal = (newQuantity * price).toFixed(2);

    subtotalElement.textContent = newSubtotal;
    updateCartGrandTotal(); // Recalculate and update grand total instantly

    // --- Validation and Removal Check ---
    if (newQuantity <= 0) {
        if (confirm("Quantity is zero. Do you want to remove this item?")) {
            removeItemFromCart(productId, row);
        } else {
            // Revert quantity to 1 if user cancels removal
            inputElement.value = 1;
            updateCartQuantity(productId, inputElement);
        }
        return;
    }

    // --- Send update to Java Backend via AJAX ---
    const data = new URLSearchParams();
    data.append('action', 'update');
    data.append('productId', productId);
    data.append('quantity', newQuantity);

    fetch(CART_API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => response.json())
    .then(result => {
        // Update the global cart count element (usually in the header)
        document.getElementById('cart-count').textContent = result.cartCount;
    })
    .catch(error => console.error('Error updating cart:', error));
}


// File: web/script.js (REPLACE existing removeItemFromCart)

/**
 * Removes an item from the cart via AJAX and updates the DOM.
 * @param {string} productId - The ID of the product to delete.
 * @param {HTMLElement} element - The button element that was clicked.
 */
function removeItemFromCart(productId, element) {
    // CRITICAL FIX: Find the parent table row (<tr>) from the button element
    const rowToRemove = element.closest('tr');

    // 1. Visually remove the row instantly
    if (rowToRemove) rowToRemove.remove();
    updateCartGrandTotal();

    // 2. Send deletion request to Java Backend via AJAX
    const data = new URLSearchParams();
    data.append('action', 'delete');
    data.append('productId', productId);

    fetch(CART_API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: data
    })
    .then(response => response.json())
    .then(result => {
        // Update the global cart count element
        document.getElementById('cart-count').textContent = result.cartCount;
    })
    .catch(error => console.error('Error removing item:', error));
}


// ---------------------------------------------
// 3C. GRAND TOTAL CALCULATION (Live Calculation)
// ---------------------------------------------

/**
 * Calculates the total cost and item count of the cart based on the current DOM state.
 * This runs on the cart.html page.
 */
function updateCartGrandTotal() {
    // Only run this function if we are on the cart page (which has the table)
    if (!document.getElementById('cart-table-body')) {
        return;
    }

    let grandTotal = 0;
    let totalItems = 0;

    // Select all rows in the cart table body
    const itemRows = document.querySelectorAll('#cart-table-body tr');

    itemRows.forEach(row => {
        const quantityInput = row.querySelector('.item-quantity-input');
        const priceElement = row.querySelector('.item-price');

        const quantity = parseInt(quantityInput ? quantityInput.value : 0) || 0;
        const price = parseFloat(priceElement ? priceElement.textContent : 0) || 0;

        grandTotal += (quantity * price);
        totalItems += quantity;
    });

    // Update the displayed total and count in the summary section
    const grandTotalElement = document.getElementById('cart-grand-total');
    if (grandTotalElement) {
        grandTotalElement.textContent = grandTotal.toFixed(2);
    }
    // Note: The header count is updated by AJAX response, but this ensures cart.html is consistent
    const cartCountElement = document.getElementById('cart-count');
    if (cartCountElement) {
        cartCountElement.textContent = totalItems;
    }
}