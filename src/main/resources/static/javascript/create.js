document.addEventListener('DOMContentLoaded', function () {
    // 1. Seleciona todos os botões de abas e os painéis de conteúdo
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabPanes = document.querySelectorAll('.tab-pane');

    // 2. Adiciona um 'ouvinte de evento' para cada botão
    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            
            // 3. Remove a classe .active de todos os botões e painéis
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabPanes.forEach(pane => pane.classList.remove('active'));

            // 4. Adiciona a classe .active ao botão clicado
            button.classList.add('active');

            // 5. Usa o atributo 'data-tab' do botão para encontrar o painel correspondente
            const targetId = button.getAttribute('data-tab');
            const targetPane = document.getElementById(`tab-${targetId}`);
            
            // 6. Adiciona a classe .active ao painel de conteúdo correspondente
            if (targetPane) {
                targetPane.classList.add('active');
            }
        });
    });
})
