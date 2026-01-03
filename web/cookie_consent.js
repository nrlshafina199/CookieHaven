document.addEventListener("DOMContentLoaded", () => {
    const banner = document.createElement('div');
    banner.id = 'cookie-banner';
    banner.style = `
        display: none;
        position: fixed;
        bottom: 20px;
        left: 20px;
        right: 20px;
        background: #5d4037;
        color: white;
        padding: 20px;
        border-radius: 10px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.3);
        z-index: 10000;
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-family: sans-serif;
        gap: 20px;
    `;

    banner.innerHTML = `
        <div style="max-width: 70%;">
            <p style="margin: 0;">
                🍪 <strong>Cookie Consent:</strong>
                We use cookies to keep you logged in and improve your experience.
                Read our
                <a href="privacy.html" style="color: #ffccbc;">Privacy Policy</a> and
                <a href="terms.html" style="color: #ffccbc;">Terms of Service</a>.
            </p>
        </div>

        <div style="display: flex; gap: 10px;">
            <button id="reject-cookies"
                style="background: transparent; color: white; border: 1px solid white;
                       padding: 8px 16px; border-radius: 5px; cursor: pointer;">
                Reject
            </button>

            <button id="accept-cookies"
                style="background: white; color: #5d4037; border: none;
                       padding: 8px 16px; border-radius: 5px; cursor: pointer; font-weight: bold;">
                Accept
            </button>
        </div>
    `;

    document.body.appendChild(banner);

    if (!localStorage.getItem("cookiesAccepted")) {
        banner.style.display = "flex";
    }

    document.getElementById("accept-cookies").onclick = () => {
        localStorage.setItem("cookiesAccepted", "true");
        banner.style.display = "none";
    };

    document.getElementById("reject-cookies").onclick = () => {
        localStorage.setItem("cookiesAccepted", "false");
        banner.style.display = "none";
    };
});
