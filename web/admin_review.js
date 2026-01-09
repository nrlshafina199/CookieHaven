function loadAdminReviews() {
    fetch("/api/reviews?admin=true")
        .then(res => res.text())
        .then(data => {
            const box = document.getElementById("adminReviews");
            box.innerHTML = "";

            if (!data.trim()) {
                box.innerHTML = "<p>No reviews yet.</p>";
                return;
            }

            data.trim().split("\n").forEach(r => {
                const [id, product, text, rating, image] = r.split("|");

                box.innerHTML += `
                  <div class="review-card">
                    <strong>${product}</strong>
                    <div class="stars">${"★".repeat(rating)}${"☆".repeat(5 - rating)}</div>
                    <p>${text}</p>

                    ${
                    image && image !== "none"
                        ? `<img src="${image}" style="max-width:200px;margin-top:10px;">`
                        : ""
                }

                    <br>
                    <button onclick="approve(${id})">Approve</button>
                    <button onclick="removeReview(${id})">Delete</button>
                  </div>
                `;
            });
        })
        .catch(err => {
            console.error("Failed to load admin reviews:", err);
        });
}

function approve(id) {
    fetch("/admin/reviews", {
        method: "POST",
        body: "approve|" + id
    }).then(loadAdminReviews);
}

function removeReview(id) {
    fetch("/admin/reviews", {
        method: "POST",
        body: "delete|" + id
    }).then(loadAdminReviews);
}

loadAdminReviews();
