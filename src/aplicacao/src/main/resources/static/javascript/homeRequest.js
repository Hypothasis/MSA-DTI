document.addEventListener('DOMContentLoaded', function () {

    const servicesContainer = document.getElementById('services-list-container');

    // Variavel para o tempo de requisição Assícrono
    const countdownElement = document.getElementById('countDownTime');
    const lastUpdateTimeElement = document.getElementById('lastUpdateTime');
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    // --- Elementos do Filtro ---
    const filterBtn = document.getElementById('filtros-btn');
    const resetBtn = document.getElementById('reset-btn');
    const availabilityRange = document.getElementById('disponibilidade-range');

    // Elementos do Alerta Sonoro
    const alertButton = document.getElementById('alert-btn');
    const alertAudio = document.getElementById('alert-sound');
    const ALERT_STORAGE_KEY = 'msaSoundAlertsEnabled'; // Chave para o localStorage

    let allHostsData = [];
    let isFilterActive = false;

    //#######################################################################
    //###                 LÓGICA DO ALERTA SONORO                       ###
    //#######################################################################

    let isAlertEnabled = false; // Estado inicial

    // Função para tocar o som de forma segura
    async function playAlertSound() {
        if (!alertAudio) return;
        try {
            alertAudio.currentTime = 0; // Reinicia o som
            await alertAudio.play();
        } catch (err) {
            console.warn("Não foi possível tocar o som de alerta (o usuário pode precisar interagir com a página primeiro).", err);
        }
    }

    // Função para atualizar a aparência do botão de alerta
    function updateAlertButtonUI() {
        if (!alertButton) return;
        const img = alertButton.querySelector('img');
        const text = alertButton.querySelector('p');

        if (isAlertEnabled) {
            img.src = '/image/icons/sound_black.png';
            text.textContent = 'Alert Sound On';
        } else {
            img.src = '/image/icons/mute_black.png';
            text.textContent = 'Alert Sound Off';
        }
    }

    // Inicializa o botão de alerta
    if (alertButton && alertAudio) {
        // Carrega o estado salvo do localStorage
        isAlertEnabled = localStorage.getItem(ALERT_STORAGE_KEY) === 'true';

        updateAlertButtonUI();

        // 3. Adiciona o listener de clique
        alertButton.addEventListener('click', () => {
            isAlertEnabled = !isAlertEnabled; // Inverte o estado
            localStorage.setItem(ALERT_STORAGE_KEY, isAlertEnabled); // Salva o novo estado
            updateAlertButtonUI(); // Atualiza a UI
        });
    }

    //#######################################################################
    //###                    FUNÇÕES DE FILTRAGEM                       ###
    //#######################################################################

    /**
     * Função principal que aplica os filtros e re-renderiza os cards.
     */
    function applyFiltersAndRender() {
        const filters = getSelectedFilters();
        let filteredHosts = allHostsData;

        // --- LÓGICA DE FILTRAGEM ---
        
        if (isFilterActive) {
            // Filtro de Categoria
            if (filters.categories.length > 0) {

                filteredHosts = filteredHosts.filter(host => filters.categories.includes(host.type));
            }
            // Filtro de Estado
            if (filters.states.length > 0) {
                filteredHosts = filteredHosts.filter(host => filters.states.includes(host.status));
            }
        } else {
            filteredHosts = filteredHosts.filter(host => host.status === 'ALERT' || host.status === 'INACTIVE');
        }

        // O slider funciona como "mostrar hosts com disponibilidade ATÉ este valor"
        filteredHosts = filteredHosts.filter(host => {
            const hostAvailability = (host.globalAvailability ? host.globalAvailability.last48h : 0);
            return hostAvailability <= filters.availability; // <-- Lógica corrigida para "menor ou igual"
        });

        // 3. Renderiza os cards com a lista final filtrada
        renderHostStatusCards(filteredHosts, isFilterActive);
    }

    /**
     * Lê os checkboxes e o slider e retorna um objeto com os filtros selecionados.
     */
    function getSelectedFilters() {
        const categories = [];
        if (document.getElementById('cat-servicos').checked) categories.push('APPLICATION');
        if (document.getElementById('cat-servidores').checked) categories.push('SERVER');
        if (document.getElementById('cat-DB').checked) categories.push('DATABASE');
        
        const states = [];
        if (document.getElementById('funcional').checked) states.push('ACTIVE');
        if (document.getElementById('alerta').checked) states.push('ALERT');
        if (document.getElementById('parado').checked) states.push('INACTIVE');

        const availability = availabilityRange ? parseFloat(availabilityRange.value) : 100;

        return { categories, states, availability };
    }

    // Adiciona o listener ao botão "Aplicar"
    if (filterBtn) {
        filterBtn.addEventListener('click', () => {
            console.log("Aplicando filtros... " + allHostsData.length + " hosts disponíveis.");
            isFilterActive = true; // ATIVA O FILTRO MANUAL
            applyFiltersAndRender();
        });
    }

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            console.log("Resetando filtros...");
            isFilterActive = false; // DESATIVA O FILTRO MANUAL
            applyFiltersAndRender();
        })
    }

    //#######################################################################
    //###         FUNÇÕES PARA REQUISIÇÃO DE BUSCA DE SERVIÇOS            ###
    //#######################################################################

    const searchInput = document.getElementById('home-search-input');
    const resultsList = document.getElementById('search-results-list');
    const resultsContainer = document.getElementById('outdoor-search');
    let debounceTimer;

    searchInput.addEventListener('input', function(event) {
        const searchTerm = event.target.value;

        // Cancela o timer anterior para não fazer múltiplas requisições
        clearTimeout(debounceTimer);

        if (searchTerm.length < 2) {
            resultsList.innerHTML = '';
            resultsContainer.classList.remove('expanded');
            return;
        }

        // Cria um novo timer. A busca só acontece após 300ms de inatividade
        debounceTimer = setTimeout(() => {
            fetchHosts(searchTerm);
        }, 300);
    });

    async function fetchHosts(term) {
        try {
            const response = await fetch(`/host/api/search?term=${encodeURIComponent(term)}`);
            if (!response.ok) throw new Error('Erro na busca.');
            
            const hosts = await response.json();
            renderResults(hosts);

        } catch (error) {
            console.error("Falha ao buscar hosts:", error);
            resultsList.innerHTML = '<li>Erro ao buscar. Tente novamente.</li>';
            resultsContainer.classList.add('expanded');
        }
    }

    function renderResults(hosts) {
        resultsList.innerHTML = ''; // Limpa resultados antigos

        if (hosts.length === 0) {
            resultsList.innerHTML = '<li>Nenhum serviço encontrado.</li>';
        } else {
            hosts.forEach(host => {
                const li = document.createElement('li');
                const p = document.createElement('p');
                const a = document.createElement('a');

                p.textContent = host.name;
                a.textContent = 'Acessar';
                a.href = `/host/${host.publicId}`;
                a.target = '_blank';

                li.appendChild(p);
                li.appendChild(a);
                resultsList.appendChild(li);
            });
        }
        // Mostra ou esconde o container de resultados
        resultsContainer.classList.toggle('expanded', hosts.length > 0);
    }

    //#######################################################################
    //###                    FUNÇÕES PARA CONFIGURAÇÃO                    ###
    //#######################################################################

    // Função que constrói o HTML dos cards
    function renderHostStatusCards(hosts) {
        if (!servicesContainer) return;
        servicesContainer.innerHTML = ''; 

        if (!hosts || hosts.length === 0) {
            const messageElement = document.createElement('div');
            messageElement.className = 'no-problems-message';
            messageElement.textContent = 'Nenhum host com problema';
            servicesContainer.appendChild(messageElement);
            return;
        }

        const statusMap = {
            'ACTIVE': { text: 'Funcionando', color: 'green' },
            'ALERT': { text: 'com Alerta!', color: 'yellow' },
            'INACTIVE': { text: 'Parado!', color: 'red' }
        };

        hosts.forEach(host => {
            const li = document.createElement('li');
            const statusInfo = statusMap[host.status] || { text: 'Desconhecido', color: 'empty' };
            const availability48h = (host.globalAvailability ? host.globalAvailability.last48h : 0); // Ajuste aqui se o DTO mudou

            li.innerHTML = `
                <header>
                    <div class="app-info">
                        <a href="/host/${host.publicId}" target="_blank">${host.name}</a>
                        <p>|</p>
                        <p class="porcent ${statusInfo.color}">${availability48h.toFixed(1)}%</p>
                    </div>
                    <div class="app-status">
                        <div class="dot ${statusInfo.color}"></div>
                        <p class="flag ${statusInfo.color}">${statusInfo.text}</p>
                    </div>
                </header>
                <div>
                    <ol class="pointGraphic">
                        ${generatePointGraphicHTML(host.availabilityHistory)}
                    </ol>
                </div>
            `;
            servicesContainer.appendChild(li);
        });
    }

    /**
     * Gera a string HTML para os 96 pontos do gráfico de disponibilidade,
     * tratando fuso horário e dias vazios.
     */
    function generatePointGraphicHTML(historyData) {
        const intervalMs = 30 * 60 * 1000; // 30 minutos
        const dataMap = new Map();
        let latestTimestamp = 0;

        // Seleciona o elemento de porcentagem (precisa estar acessível no escopo)
        const percentElement = document.querySelector('.porcentPointGraphic p:first-child'); 

        if (historyData && historyData.length > 0) {
            let availabilitySum = 0; // Para calcular a média

            historyData.forEach(point => {
                // 🔹 Corrige o fuso horário (UTC → Fortaleza UTC-3)
                const offsetMs = 3 * 60 * 60 * 1000; // 3 horas
                const fortalezaTimestamp = point.x - offsetMs;
                const roundedTimestamp = Math.floor(fortalezaTimestamp / intervalMs) * intervalMs;

                // ==========================================================
                // CORREÇÃO: Converte o valor (ex: 1.0) para porcentagem (ex: 100.0)
                // ==========================================================
                const percentValue = point.y * 100.0;
                dataMap.set(roundedTimestamp, percentValue);
                availabilitySum += percentValue; // Soma a porcentagem
                
                if (fortalezaTimestamp > latestTimestamp) {
                    latestTimestamp = fortalezaTimestamp;
                }
            });

            // Calcula a média geral usando a soma das porcentagens
            const overallAverage = availabilitySum / historyData.length;
            if (percentElement) {
                percentElement.textContent = `${overallAverage.toFixed(2)}%`;
            }
        } else {
            if (percentElement) {
                percentElement.textContent = `N/A`;
            }
        }

        // Função auxiliar para determinar a cor (agora recebe 0-100)
        function getStatusClass(availability) {
            if (availability == null) return 'empty';
            if (availability >= 99.9) return 'okay';
            if (availability >= 99.0) return 'warning';
            if (availability >= 95.0) return 'avarage';
            if (availability >= 90.0) return 'high';
            return 'disaster'; // Agora só retorna 'disaster' se a média for < 90
        }

        const numberOfPoints = 96; // 96 pontos de 30 min = 48 horas
        const referenceTime = latestTimestamp > 0 ? latestTimestamp : new Date().getTime();
        const startTimestamp = Math.floor(referenceTime / intervalMs) * intervalMs;

        let pointsHTML = ''; 
        const offsetMs = 3 * 60 * 60 * 1000; // Define o fuso

        for (let i = numberOfPoints - 1; i >= 0; i--) {
            const pointTimestamp = startTimestamp - (i * intervalMs);
            
            // 'availability' agora é 100.0, 0.0, ou undefined
            const availability = dataMap.get(pointTimestamp); 

            const date = new Date(pointTimestamp);
            
            // Lê a data/hora como se estivesse em UTC (que é o UTC-3 que calculamos)
            const formattedDate = `${String(date.getUTCDate()).padStart(2, '0')}/${String(date.getUTCMonth() + 1).padStart(2, '0')}`;
            const formattedTime = `${String(date.getUTCHours()).padStart(2, '0')}:${String(date.getUTCMinutes()).padStart(2, '0')}`;
            
            // 'availability' já está em porcentagem, então .toFixed(2) funciona
            const formattedPercent = availability != null ? `${availability.toFixed(2)}%` : 'Sem dados';
            
            // 'getStatusClass' recebe a porcentagem (100.0) e retorna 'okay'
            const statusClass = getStatusClass(availability);

            pointsHTML += `<li class="point ${statusClass}" data-line1="${formattedDate}, ${formattedTime}" data-line2="${formattedPercent}"></li>`;
        }

        return pointsHTML;
    }

    //#######################################################################
    //###        FUNÇÕES PARA REQUISIÇÃO ASSÍCRONA PARA BACKEND           ###
    //#######################################################################

    function updateLastRequestTime() {
        // Obtém dados no tempo real
        const now = new Date();

        // Parse para Horas, Minutos e Segundos
        const hour = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const formattedTime = `${hour}:${minutes}:${seconds}`;

        if (lastUpdateTimeElement) {
            // Modifica o valor no html
            lastUpdateTimeElement.textContent = formattedTime;
        }
    }

    // função assícrona principal para chamar requisição ao backend
    async function startCountDown() {
        const update_trigger = 60; 
        let secs_remaining = update_trigger;

        // Laço de repetição continua
        while (true) {
            try {
                // Modifica a tag para horarío real no html
                if (countdownElement) {
                    countdownElement.textContent = `${secs_remaining}s.`;
                }

                // Se a contagem chega a zero
                // zera o tempo e chama função de requisição ao backend
                if (secs_remaining <= 0) {

                    // Registra no elemento do <html> o tempo da última atualização
                    updateLastRequestTime();

                    // Chama função assícrona para backend
                    await getHomeData();

                    // Atualiza para 60s 
                    secs_remaining = update_trigger;

                    // Atualiza elemento para 60s
                    if (countdownElement) {
                        countdownElement.textContent = `${secs_remaining}s.`;
                    }
                }
                
                secs_remaining--;
                
                await delay(1000);

            } catch (error) {
                console.error("Erro no contador:", error);
                await delay(5000);
            }
        }
    }

    // Função que busca e renderiza os cards de status dos hosts
    async function getHomeData() {
        console.log("Buscando status dos hosts...");
        try {
            const response = await fetch('/api/public/hosts/status');
            if (!response.ok) throw new Error('Falha ao buscar status dos hosts.');

            allHostsData = await response.json(); // Salva os dados na variável global
            
            // Aplica os filtros e renderiza a lista
            applyFiltersAndRender(); 

            // A lógica de alerta sonoro NÃO DEPENDE do filtro, mas dos dados brutos
            const problemHosts = allHostsData.filter(host => host.status !== 'ACTIVE');
            if (isAlertEnabled && problemHosts.length > 0) {
                playAlertSound();
            }

        } catch (error) {
            console.error(error);
            if (servicesContainer) {
                servicesContainer.innerHTML = '<li>Erro ao carregar status dos serviços.</li>';
            }
        }
    }

    getHomeData();
    updateLastRequestTime();
    startCountDown();

})