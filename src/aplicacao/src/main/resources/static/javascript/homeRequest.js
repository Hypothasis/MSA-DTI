document.addEventListener('DOMContentLoaded', function () {

    const servicesContainer = document.getElementById('services-list-container');

    const countdownElement = document.getElementById('countDownTime');
    const lastUpdateTimeElement = document.getElementById('lastUpdateTime');
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    const filterBtn = document.getElementById('filtros-btn');
    const resetBtn = document.getElementById('reset-btn');
    const availabilityRange = document.getElementById('disponibilidade-range');

    const alertButton = document.getElementById('alert-btn');
    const alertAudio = document.getElementById('alert-sound');
    const ALERT_STORAGE_KEY = 'msaSoundAlertsEnabled'; // Chave para o localStorage

    let allHostsData = [];
    let isFilterActive = false;

    //#######################################################################
    //###                 LÓGICA DO ALERTA SONORO                       ###
    //#######################################################################

    let isAlertEnabled = false;

    async function playAlertSound() {
        if (!alertAudio) return;
        try {
            alertAudio.currentTime = 0;
            await alertAudio.play();
        } catch (err) {
            console.warn("Não foi possível tocar o som de alerta (o usuário pode precisar interagir com a página primeiro).", err);
        }
    }

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

    if (alertButton && alertAudio) {
        isAlertEnabled = localStorage.getItem(ALERT_STORAGE_KEY) === 'true';

        updateAlertButtonUI();

        alertButton.addEventListener('click', () => {
            isAlertEnabled = !isAlertEnabled; 
            localStorage.setItem(ALERT_STORAGE_KEY, isAlertEnabled); 
            updateAlertButtonUI(); 
        });
    }

    //#######################################################################
    //###                    FUNÇÕES DE FILTRAGEM                       ###
    //#######################################################################


    function applyFiltersAndRender() {
        const filters = getSelectedFilters();
        let filteredHosts = allHostsData;

        
        if (isFilterActive) {
            if (filters.categories.length > 0) {
                filteredHosts = filteredHosts.filter(host => filters.categories.includes(host.type));
            }
            if (filters.states.length > 0) {
                filteredHosts = filteredHosts.filter(host => filters.states.includes(host.status));
            }
        } else {
            filteredHosts = filteredHosts.filter(host => host.status === 'ALERT' || host.status === 'INACTIVE');
        }

        filteredHosts = filteredHosts.filter(host => {
            const hostAvailability = (host.globalAvailability ? host.globalAvailability.last48h : 0);
            return hostAvailability <= filters.availability;
        });

        renderHostStatusCards(filteredHosts, isFilterActive);
    }

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

    if (filterBtn) {
        filterBtn.addEventListener('click', () => {
            console.log("Aplicando filtros... " + allHostsData.length + " hosts disponíveis.");
            isFilterActive = true; 
            applyFiltersAndRender();
        });
    }

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            console.log("Resetando filtros...");
            isFilterActive = false;
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

        clearTimeout(debounceTimer);

        if (searchTerm.length < 2) {
            resultsList.innerHTML = '';
            resultsContainer.classList.remove('expanded');
            return;
        }

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
        resultsList.innerHTML = ''; 

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
        resultsContainer.classList.toggle('expanded', hosts.length > 0);
    }

    //#######################################################################
    //###                    FUNÇÕES PARA CONFIGURAÇÃO                    ###
    //#######################################################################

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
            const availability48h = (host.globalAvailability ? host.globalAvailability.last48h : 0);

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

    function generatePointGraphicHTML(historyData) {
        const intervalMs = 30 * 60 * 1000;
        const dataMap = new Map();
        let latestTimestamp = 0;

        const percentElement = document.querySelector('.porcentPointGraphic p:first-child'); 

        if (historyData && historyData.length > 0) {
            let availabilitySum = 0;

            historyData.forEach(point => {
                const offsetMs = 3 * 60 * 60 * 1000;
                const fortalezaTimestamp = point.x - offsetMs;
                const roundedTimestamp = Math.floor(fortalezaTimestamp / intervalMs) * intervalMs;
                const percentValue = point.y * 100.0;
                dataMap.set(roundedTimestamp, percentValue);
                availabilitySum += percentValue; 

                if (fortalezaTimestamp > latestTimestamp) {
                    latestTimestamp = fortalezaTimestamp;
                }
            });

            const overallAverage = availabilitySum / historyData.length;
            if (percentElement) {
                percentElement.textContent = `${overallAverage.toFixed(2)}%`;
            }
        } else {
            if (percentElement) {
                percentElement.textContent = `N/A`;
            }
        }

        function getStatusClass(availability) {
            if (availability == null) return 'empty';
            if (availability >= 99.9) return 'okay';
            if (availability >= 99.0) return 'warning';
            if (availability >= 95.0) return 'avarage';
            if (availability >= 90.0) return 'high';
            return 'disaster';
        }

        const numberOfPoints = 96; 
        const referenceTime = latestTimestamp > 0 ? latestTimestamp : new Date().getTime();
        const startTimestamp = Math.floor(referenceTime / intervalMs) * intervalMs;

        let pointsHTML = ''; 
        const offsetMs = 3 * 60 * 60 * 1000; 

        for (let i = numberOfPoints - 1; i >= 0; i--) {
            const pointTimestamp = startTimestamp - (i * intervalMs);
            
            const availability = dataMap.get(pointTimestamp); 

            const date = new Date(pointTimestamp);
            
            const formattedDate = `${String(date.getUTCDate()).padStart(2, '0')}/${String(date.getUTCMonth() + 1).padStart(2, '0')}`;
            const formattedTime = `${String(date.getUTCHours()).padStart(2, '0')}:${String(date.getUTCMinutes()).padStart(2, '0')}`;
            
            const formattedPercent = availability != null ? `${availability.toFixed(2)}%` : 'Sem dados';
            
            const statusClass = getStatusClass(availability);

            pointsHTML += `<li class="point ${statusClass}" data-line1="${formattedDate}, ${formattedTime}" data-line2="${formattedPercent}"></li>`;
        }

        return pointsHTML;
    }

    //#######################################################################
    //###        FUNÇÕES PARA REQUISIÇÃO ASSÍCRONA PARA BACKEND           ###
    //#######################################################################

    function updateLastRequestTime() {
        const now = new Date();

        const hour = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const formattedTime = `${hour}:${minutes}:${seconds}`;

        if (lastUpdateTimeElement) {
            lastUpdateTimeElement.textContent = formattedTime;
        }
    }

    async function startCountDown() {
        const update_trigger = 60; 
        let secs_remaining = update_trigger;

        while (true) {
            try {
                if (countdownElement) {
                    countdownElement.textContent = `${secs_remaining}s.`;
                }

                if (secs_remaining <= 0) {

                    updateLastRequestTime();

                    await getHomeData();

                    secs_remaining = update_trigger;

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

    async function getHomeData() {
        console.log("Buscando status dos hosts...");
        try {
            const response = await fetch('/api/public/hosts/status');
            if (!response.ok) throw new Error('Falha ao buscar status dos hosts.');

            allHostsData = await response.json(); 
            
            applyFiltersAndRender(); 

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