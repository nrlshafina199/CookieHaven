function loadFeedback() {
    fetch("/api/reviews")
        .then(res => res.json())
        .then(data => {
            const box = document.getElementById("feedbackList");
            box.innerHTML = "";

            if (data.length === 0) {
                box.innerHTML = "<p>No feedback yet.</p>";
                return;
            }

            data.forEach(r => {
                box.innerHTML += `
                    <div class="review-card">
                        <strong>🍪 ${r.cookie}</strong>
                        <div>${"★".repeat(r.rating)}</div>
                        <p>${r.comment}</p>
                        ${r.image !== "none" ? `<img src="${r.image}" style="max-width:100%">` : ""}
                        <br><br>
                        <button onclick="deleteFeedback(${r.id})">🗑 Delete</button>
                    </div>
                `;
            });
        });
}

function deleteFeedback(id) {
    if (!confirm("Delete this feedback?")) return;

    fetch("/admin/reviews", {
        method: "POST",
        body: "delete|" + id
    }).then(loadFeedback);
}

loadFeedback();
