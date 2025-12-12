const form = document.getElementById("orderForm");

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
        });
});
