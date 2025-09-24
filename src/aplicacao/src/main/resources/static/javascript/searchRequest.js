document.addEventListener('DOMContentLoaded', function () {
    // ===================================================================
    // 1. SELEÇÃO DOS ELEMENTOS DO DOM
    // ===================================================================
    const hostListContainer = document.querySelector('#search article ul');
    
    // Seleciona os modais
    const readModal = document.getElementById('read-modal');
    const updateModal = document.getElementById('update-modal');
    const deleteModal = document.getElementById('delete-modal');
    const allModals = document.querySelectorAll('.modal');

    // ===================================================================
    // 2. LÓGICA PARA FECHAR OS MODAIS
    // ===================================================================

    function closeModal() {
        allModals.forEach(modal => modal.classList.remove('show'));
    }

    document.querySelectorAll('.close-btn').forEach(btn => btn.addEventListener('click', closeModal));
    
    window.addEventListener('click', function(event) {
        if (event.target.classList.contains('modal')) {
            closeModal();
        }
    });

    const cancelDeleteBtn = document.getElementById('cancel-delete-btn');
    if (cancelDeleteBtn) {
        cancelDeleteBtn.addEventListener('click', closeModal);
    }


    // ===================================================================
    // 3. FUNÇÕES DINÂMICAS PARA ABRIR E PREENCHER CADA MODAL
    // ===================================================================

    // --- AÇÃO DE LER (READ) ---
    function openReadModal(hostData) {
        // Preenche o modal de leitura com os dados do dataset
        document.getElementById('modal-read-name').textContent = hostData.hostName || 'N/A';
        document.getElementById('modal-read-description').textContent = hostData.hostDescription || 'N/A';
        document.getElementById('modal-read-zabbix-id').textContent = hostData.hostZabbixId || 'N/A';
        document.getElementById('modal-read-public-id').textContent = hostData.hostPublicId || 'N/A';
        document.getElementById('modal-read-db-id').textContent = hostData.hostId || 'N/A';
        document.getElementById('modal-read-type').textContent = hostData.hostType || 'N/A';

        // Preenche a lista de métricas dinamicamente
        const metricsList = readModal.querySelector('ul');
        metricsList.innerHTML = ''; // Limpa a lista antiga
        if (hostData.hostMetrics && hostData.hostMetrics.trim()) {
            const metrics = hostData.hostMetrics.trim().split(', ');
            metrics.forEach(metricName => {
                const li = document.createElement('li');
                li.textContent = metricName;
                metricsList.appendChild(li);
            });
        } else {
            metricsList.innerHTML = '<li>Nenhuma métrica configurada.</li>';
        }
        
        // Mostra o modal
        readModal.classList.add('show');
    }

    // --- AÇÃO DE ATUALIZAR (UPDATE) ---
    async function openUpdateModal(hostData) {
        // Para o update, vamos usar os dados dos atributos data-* para um pré-preenchimento.
        // A lógica mais complexa (com checkboxes) precisaria de uma chamada à API.
        const updateForm = document.getElementById('update-host-form');
        
        updateForm.querySelector('#modal-update-id').value = hostData.hostId;
        updateForm.querySelector('#modal-update-name').value = hostData.hostName;
        updateForm.querySelector('#modal-update-zabbixID').value = hostData.hostZabbixId;
        updateForm.querySelector('#modal-update-description').value = hostData.hostDescription;
        
        // A lógica de carregar os checkboxes marcados idealmente viria de uma chamada à API,
        // pois a string no data-host-metrics não tem os 'names' dos inputs.
        // Mas a parte de preencher os campos básicos está feita.
        
        updateModal.classList.add('show');
    }
    
    // --- AÇÃO DE DELETAR (DELETE) ---
    function openDeleteModal(hostData) {
        // Preenche o nome do host no modal de confirmação
        document.getElementById('modal-delete-name').textContent = hostData.hostName;
        
        // Guarda o ID no botão de confirmação para ser usado depois
        document.getElementById('confirm-delete-btn').dataset.hostId = hostData.hostId;

        // Mostra o modal de confirmação
        deleteModal.classList.add('show');
    }


    // ===================================================================
    // 4. LISTENER PRINCIPAL (DELEGAÇÃO DE EVENTOS)
    // ===================================================================
    if (hostListContainer) {
        hostListContainer.addEventListener('click', function (event) {
            const button = event.target.closest('button');
            if (!button) return; // Se não clicou em um botão, não faz nada

            const hostItem = button.closest('li[data-host-id]');
            if (!hostItem) return; // Garante que temos o item da lista

            // O objeto dataset contém todos os atributos data-* do elemento
            const hostData = hostItem.dataset;

            if (button.classList.contains('read')) {
                openReadModal(hostData);
            }
            
            if (button.classList.contains('update')) {
                openUpdateModal(hostData);
            }
            
            if (button.classList.contains('delete')) {
                openDeleteModal(hostData);
            }
        });
    }
});