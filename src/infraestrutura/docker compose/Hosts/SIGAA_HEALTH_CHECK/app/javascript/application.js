// Configure your import map in config/importmap.rb. Read more: https://github.com/rails/importmap-rails
import "@hotwired/turbo-rails"
import "controllers"

const buttons = document.querySelectorAll("button");

buttons.forEach(button=>{
    button.addEventListener("click", ()=>{
        button.classList.toggle("active");
        button.dataset.status = button.classList.contains("active") ? "UP" : "DOWN";
        changeStatus(button.dataset.type, button.dataset.status);
    })
})

function changeStatus(type, status) {
    const csrfToken = document.querySelector('meta[name="csrf-token"]').content;

    fetch("/home/change", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": csrfToken
        },
        body: JSON.stringify({
            type: type,
            status: status
        })
    })
        .then(response => {
            if (response.ok) {
                console.log(`Sucesso: ${type} alterado para ${status}`);
            } else {
                console.error("Erro ao enviar dados para o servidor.");
            }
        })
        .catch(error => {
            console.error("Falha na requisição:", error);
        });
}
