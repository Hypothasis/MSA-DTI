document.addEventListener('DOMContentLoaded', function () {
    /* Filtros na aba Search */
    const filtrosBtn = document.getElementById("filtrosBtn");
    if (filtrosBtn){
        filtrosBtn.addEventListener('click', ()=>{
            document.getElementById('filtrosCats').classList.toggle('actived')
        })
    }    

    // Seleciona todos os modais e botões de fechar
    const readModal = document.getElementById('read-modal');
    const updateModal = document.getElementById('update-modal');
    const deleteModal = document.getElementById('delete-modal');
    const closeButtons = document.querySelectorAll('.close-btn');

    // Lógica para fechar os modais
    function closeModal() {
        readModal.classList.remove('show');
        updateModal.classList.remove('show');
        deleteModal.classList.remove('show');
    }

    closeButtons.forEach(btn => btn.addEventListener('click', closeModal));
    window.addEventListener('click', function(event) {
        if (event.target.classList.contains('modal')) {
            closeModal();
        }
    });


    // Delegação de eventos para os botões do CRUD
    document.querySelector('article ul').addEventListener('click', function(event) {
        const target = event.target.closest('button');
        if (!target) return;

        const listItem = target.closest('li');
        const hostId = listItem.dataset.hostId;
        const hostName = listItem.querySelector('h6').textContent;

        // ---- Lógica para o botão LER ----
        if (target.classList.contains('read')) {
            // 1. Fazer uma chamada fetch para sua API para buscar os detalhes do host
            // fetch(`/api/admin/host/${hostId}`)
            //   .then(response => response.json())
            //   .then(data => {
            //         // 2. Preencher o modal de leitura com os dados
            //         document.getElementById('modal-read-name').textContent = data.name;
            //         document.getElementById('modal-read-description').textContent = data.description;
            //         document.getElementById('modal-read-zabbix-id').textContent = data.zabbixId;
            //         document.getElementById('modal-read-type').textContent = data.type;
            //         // 3. Mostrar o modal
            //         readModal.classList.add('show');
            //   });
            
            // Apenas para exemplo sem API:
            document.getElementById('modal-read-name').textContent = hostName;
            readModal.classList.add('show');
        }

        // ---- Lógica para o botão ATUALIZAR ----
        if (target.classList.contains('update')) {
             // 1. Fazer uma chamada fetch para buscar os dados atuais do host
             // fetch(`/api/admin/host/${hostId}`).then...
             
             // 2. Preencher o FORMULÁRIO no modal de update
             // document.getElementById('modal-update-id').value = hostId;
             // document.getElementById('modal-update-name').value = hostName; // Usar dados da API
             
             // 3. Mostrar o modal
             updateModal.classList.add('show');
        }
        
        // ---- Lógica para o botão DELETAR ----
        if (target.classList.contains('delete')) {
            // 1. Preencher o nome do host no modal de confirmação
            document.getElementById('modal-delete-name').textContent = hostName;
            
            // 2. Guardar o ID no botão de confirmação para uso posterior
            document.getElementById('confirm-delete-btn').dataset.hostId = hostId;

            // 3. Mostrar o modal
            deleteModal.classList.add('show');
        }
    });

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

    // 2. Adiciona um 'ouvinte de evento' para cada radio button
    tabRadios.forEach(radio => {
        radio.addEventListener('change', switchTab);
    });

    // 3. Garante que a aba correta seja exibida (e as outras limpas) ao carregar a página
    switchTab(); 
})
