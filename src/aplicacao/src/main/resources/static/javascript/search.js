document.addEventListener('DOMContentLoaded', function () {
    /* Filtros na aba Search */
    const filtrosBtn = document.getElementById("filtrosBtn");
    if (filtrosBtn){
        filtrosBtn.addEventListener('click', ()=>{
            document.getElementById('filtrosCats').classList.toggle('actived')
        })
    }    

    /* graficos do model Update */
    const tabRadios = document.querySelectorAll('.tab-radio');
    const tabPanes = document.querySelectorAll('.tab-pane');

    function switchTab() {
        const selectedRadio = document.querySelector('.tab-radio:checked');
        if (!selectedRadio) return;

        const targetId = selectedRadio.getAttribute('data-tab');

        // Itera sobre todos os painéis
        tabPanes.forEach(pane => {
            const paneId = pane.id.replace('tab-', ''); // Extrai o 'id' do painel (ex: 'aplicacao')

            if (paneId === targetId) {
                // Se este é o painel que deve ser MOSTRADO, adiciona a classe 'active'
                pane.classList.add('active');
            } else {
                // Se este é um painel que deve ser ESCONDIDO...
                pane.classList.remove('active'); 
                
                // Encontra todos os checkboxes dentro deste painel escondido
                const checkboxes = pane.querySelectorAll('input[type="checkbox"]');
                
                // Itera sobre eles e desmarca cada um
                checkboxes.forEach(checkbox => {
                    checkbox.checked = false;
                });
            }
        });
    }

    tabRadios.forEach(radio => {
        radio.addEventListener('change', switchTab);
    });

    switchTab(); 
})
