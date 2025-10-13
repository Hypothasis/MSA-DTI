document.addEventListener('DOMContentLoaded', function () {
    // ===================================================================
    // INICIALIZAÇÃO DOS GRÁFICOS
    // ===================================================================
    // Inicializamos cada gráfico uma vez com dados vazios. Guardamos as instâncias em um objeto para fácil acesso.
    const charts = {        
        cpu: new ApexCharts(document.querySelector("#cpuChart"), getCpuChartOptions()),
        cpuProcesses: new ApexCharts(document.querySelector("#cpuProcessosChart"), getCpuProcessesChartOptions()),
        
        memory: new ApexCharts(document.querySelector("#memoriaChart"), getMemoryChartOptions()),

        storage: new ApexCharts(document.querySelector("#armazenamentoChart"), getStorageChartOptions()),
        data: new ApexCharts(document.querySelector("#dadosChart"), getDataChartOptions())
    };
    // Renderiza todos os gráficos
    Object.values(charts).forEach(chart => chart.render());


    // ===================================================================
    // FUNÇÃO PRINCIPAL DE BUSCA E ATUALIZAÇÃO DE DADOS
    // ===================================================================

    async function getHostData() {
        console.log("Buscando dados do host no backend...");

        // Pega o publicId da URL da página atual
        const pathParts = window.location.pathname.split('/');
        const publicId = pathParts[pathParts.length - 1];
        if (!publicId) return console.error("ID do host não encontrado na URL.");

        try {
            // Faz a chamada fetch para a API
            const response = await fetch(`/host/api/${publicId}`);
            if (!response.ok) throw new Error(`Erro na API: ${response.status}`);
            
            const data = await response.json();


            if (data.latencyHistory) {
                data.latencyHistory.forEach(point => {
                    point.y = point.y * 1000; // Segundos para Milissegundos
                });
            }

            // --- ATUALIZA A PÁGINA COM OS NOVOS DADOS ---
            
            updateHeader(data);

            if  (data.availabilityHistory) {
                updateAvailabilityGraphic(data.availabilityHistory);
            } else {
                document.getElementById('disponibilidadeGlobal').style.display = 'none';
            }
            
            if  (data.globalAvailability) {
                updateGlobalAvailability(data.globalAvailability);
            } else {
                document.getElementById('disponibilidadeEspecifica').style.display = 'none';
            }

            if (data.cpuUsageHistory) {
                charts.cpu.updateSeries([{ data: data.cpuUsageHistory || [] }]);
            } else {
                document.getElementById('cpuUso').style.display = 'none';
            }

            if (data.processInfoHistory) {
                charts.cpuProcesses.updateSeries([
                    // Objeto para a primeira série (processos máximos)
                    {
                        name: 'Número máximo de processos',
                        data: data.processInfoHistory.max || []
                    },
                    // Objeto para a segunda série (processos atuais)
                    {
                        name: 'Número atual de processos',
                        data: data.processInfoHistory.current || []
                    }
                ]);
            } else {
                document.getElementById('cpuProcessos').style.display = 'none';
            }

            const bitsToMegabits = (bits) => bits / 1_000_000;

            if (data.dataBandwidthInHistory || data.dataBandwidthOutHistory) {

                data.dataBandwidthInHistory.forEach(point => {
                    point.y = bitsToMegabits(point.y);
                });

                data.dataBandwidthOutHistory.forEach(point => {
                    point.y = bitsToMegabits(point.y);
                });


                charts.data.updateSeries([
                    { name: 'Dados Enviados (Upload)', data: data.dataBandwidthOutHistory || [] },
                    { name: 'Dados Recebidos (Download)', data: data.dataBandwidthInHistory || [] }
                ]);
            } else {
                document.getElementById('BandaLargaDados').style.display = 'none';
            }

            if (data.memoryData) {
                const memoriaEmGb = [
                    data.memoryData.total,
                    data.memoryData.free,
                    data.memoryData.used
                ];
                const memoriaPorcents = [
                    100,
                    100 - data.memoryData.percentUsed,
                    data.memoryData.percentUsed
                ];

                charts.memory.updateOptions({
                    series: memoriaPorcents,
                    tooltip: {
                        enabled: true,
                        theme: 'dark',
                        y: {
                            // Novo formatter que usa os dados em GB atuais
                            formatter: function(value, opts) {
                                const gbValue = memoriaEmGb[opts.seriesIndex];
                                return `${value.toFixed(0)}% (${gbValue.toFixed(2)} GB)`;
                            }
                        }
                    },
                    legend: {
                        // Novo formatter da legenda
                        formatter: function(seriesName, opts) {
                            const percentage = opts.w.globals.series[opts.seriesIndex];
                            const gbValue = memoriaEmGb[opts.seriesIndex];
                            return `${seriesName}: ${percentage.toFixed(0)}% (${gbValue.toFixed(2)} GB)`;
                        }
                    }
                });
            } else {
                document.getElementById('memoriaUso').style.display = 'none';
            }


            if (data.storageRootData && data.storageBootData) {
                // Array com os valores em GB na ordem correta para os tooltips
                const armazenamentoEmGb = [
                    data.storageRootData.total,
                    data.storageRootData.used,
                    data.storageBootData.total,
                    data.storageBootData.used
                ];

                // Array com os percentuais para as séries do gráfico
                const storagePorcents = [
                    100, // Total / é sempre 100%
                    data.storageRootData.percentUsed,
                    100, // Total /boot é sempre 100%
                    data.storageBootData.percentUsed
                ];

                // 2. Atualiza o gráfico com as novas séries E os novos formatters
                charts.storage.updateOptions({
                    series: storagePorcents,
                    labels: ['Total ("/")', 'Usado ("/")', 'Total ("/boot")', 'Usado ("/boot")'],
                    tooltip: {
                        y: {
                            formatter: function(value, opts) {
                                // Pega o valor absoluto em GB correspondente ao anel
                                const gbValue = armazenamentoEmGb[opts.seriesIndex];
                                return `${gbValue.toFixed(2)} GB`; // Mostra o valor em GB, não a porcentagem
                            }
                        }
                    },
                    legend: {
                        formatter: function(seriesName, opts) {
                            const percentage = opts.w.globals.series[opts.seriesIndex];
                            const gbValue = armazenamentoEmGb[opts.seriesIndex];
                            // Mostra a porcentagem e o valor em GB na legenda
                            return `${seriesName}: ${percentage.toFixed(1)}% (${gbValue.toFixed(2)} GB)`;
                        }
                    }
                });
            } else {
                document.getElementById('armazenamentoUso').style.display = 'none';
            }
            
            if (data.uptime) {
                updateUptime(data.uptime);
            } else {
                document.getElementById('tempoAtivoServidor').style.display = 'none';
            }
            
            if(data.recentEvents) {
                updateRecentEvents(data.recentEvents);
            } else {
                document.getElementById('eventosRecentes').style.display = 'none';
            }

            console.log("Página atualizada com sucesso!");

        } catch (error) {
            console.error("Falha ao buscar e atualizar os dados do host:", error);
        }
    }

    // ===================================================================
    // FUNÇÕES AUXILIARES PARA ATUALIZAR A UI
    // ===================================================================
    function updateHeader(data) {
        document.getElementById('hostName').textContent = data.name || 'N/A';
        const statusFlag = document.getElementById('hostStatusFlag');
        const statusPoint = document.getElementById('hostStatusPoint');
        
        let flagText = 'Desconhecido';
        let flagColorClass = 'empty'; // Uma cor padrão para status desconhecido

        switch (data.status) {
            case 'OK':
                flagText = 'Funcionando';
                flagColorClass = 'green';
                break;
            case 'ALERT':
                flagText = 'com Alerta!';
                flagColorClass = 'yellow';
                break;
            case 'PROBLEM':
                flagText = 'Parado!';
                flagColorClass = 'red';
                break;
        }

        statusFlag.textContent = flagText;
        // Remove as classes de cor antigas e adiciona a nova
        statusFlag.className = `flag ${flagColorClass}`;
        statusPoint.className = `dot ${flagColorClass}`;
    }

    function updateAvailabilityGraphic(historyData) {
        const container = document.querySelector('.pointGraphic');
        const percentElement = document.querySelector('.porcentPointGraphic p:first-child');
        if (!container || !percentElement) return;

        container.innerHTML = '';

        const intervalMs = 30 * 60 * 1000; // 30 minutos
        const dataMap = new Map();
        let latestTimestamp = 0;

        if (historyData && historyData.length > 0) {
            historyData.forEach(point => {
                // 🔹 Corrige o fuso horário (UTC → Fortaleza UTC-3)
                const offsetMs = 3 * 60 * 60 * 1000; // 3 horas
                const fortalezaTimestamp = point.x - offsetMs;

                // Arredonda para o intervalo de 30 min
                const roundedTimestamp = Math.floor(fortalezaTimestamp / intervalMs) * intervalMs;

                // Armazena no mapa o valor ajustado
                dataMap.set(roundedTimestamp, point.y);

                // Atualiza o timestamp mais recente
                if (fortalezaTimestamp > latestTimestamp) {
                    latestTimestamp = fortalezaTimestamp;
                }
            });

            // Calcula a média geral
            const overallAverage =
                historyData.reduce((acc, point) => acc + point.y, 0) / historyData.length;
            percentElement.textContent = `${overallAverage.toFixed(2)}%`;
        } else {
            percentElement.textContent = `N/A`;
        }

        // Define a classe visual com base na disponibilidade
        function getStatusClass(availability) {
            if (availability == null) return 'empty';
            if (availability >= 99.9) return 'okay';
            if (availability >= 99.0) return 'warning';
            if (availability >= 95.0) return 'avarage';
            if (availability >= 90.0) return 'high';
            return 'disaster';
        }

        const numberOfPoints = 96; // 48h / 0.5h = 96 pontos

        // Define o ponto de referência (último dado ou agora)
        const referenceTime = latestTimestamp > 0 ? latestTimestamp : new Date().getTime();
        const startTimestamp = Math.floor(referenceTime / intervalMs) * intervalMs;

        // Gera os pontos para as últimas 48h
        for (let i = 0; i < numberOfPoints; i++) {
            const pointTimestamp = startTimestamp - i * intervalMs;
            const availability = dataMap.get(pointTimestamp);

            const li = document.createElement('li');
            li.className = `point ${getStatusClass(availability)}`;

            // 🔹 Cria objeto Date com o timestamp ajustado
            const date = new Date(pointTimestamp);

            // 🔹 Exibe a data/hora já no fuso de Fortaleza
            const formattedDateTime = date.toLocaleString('pt-BR', {
                day: '2-digit',
                month: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                timeZone: 'America/Fortaleza'
            });

            const formattedPercent =
                availability != null ? `${availability.toFixed(2)}%` : 'Sem dados';

            li.dataset.line1 = formattedDateTime;
            li.dataset.line2 = formattedPercent;

            container.prepend(li);
        }
    }

    function updateGlobalAvailability(availabilityData) {
        if (!availabilityData) return;
        document.getElementById('availabilityData48h').textContent = `${(availabilityData.last48h || 0).toFixed(2)}%`;
        document.getElementById('availabilityData24h').textContent = `${(availabilityData.last24h || 0).toFixed(2)}%`;
        document.getElementById('availabilityData12h').textContent = `${(availabilityData.last12h || 0).toFixed(2)}%`;
        document.getElementById('availabilityData6h').textContent = `${(availabilityData.last6h || 0).toFixed(2)}%`;
    }

    function updateUptime(uptimeString) {
        if (!uptimeString) return;
        // Ex: "37 dias, 4 horas, 24 minutos"
        const parts = uptimeString.match(/(\d+)\s*dias,\s*(\d+)\s*horas,\s*(\d+)\s*minutos/);
        if (parts) {
            document.getElementById('diasUptime').textContent = parts[1];
            document.getElementById('horasUptime').textContent = parts[2];
            document.getElementById('minutosUptime').textContent = parts[3];
        }
    }

    function updateRecentEvents(eventsData) {
        const container = document.querySelector('.recentEvents');
        if (!container) return;
        container.innerHTML = '';

        if (!eventsData || eventsData.length === 0) {
            container.innerHTML = '<li>Nenhum evento recente.</li>';
            return;
        }

        // Mapeia a severidade para a classe CSS
        const severityMap = {
            '0': 'okay', '1': 'okay', '2': 'important', '3': 'important', '4': 'important', '5': 'error'
        };

        eventsData.forEach(event => {
            const li = document.createElement('li');
            const severityClass = severityMap[event.severity] || 'empty';
            
            li.innerHTML = `
                <div class="imgEvent ${severityClass}"></div>
                <div>
                    <h3 class="titleRecentEvents">${event.name}</h3>
                    <p class="detailsRecentEvents"></p> <p class="dateRecentEvents">${event.timestamp}</p>
                </div>
            `;
            container.appendChild(li);
        });
    }

    function updateSystemOperacionalGraphic(systemOperacionalData) {

    }

    // ===================================================================
    // LÓGICA DO CONTADOR (TIMER)
    // ===================================================================
    const countdownElement = document.getElementById('countDownTime');
    const lastUpdateTimeElement = document.getElementById('lastUpdateTime');

    async function startCountDown() {
        await getHostData(); // Faz a primeira busca imediatamente
        updateLastRequestTime();
        
        const update_trigger = 60;
        let secs_remaining = update_trigger;

        setInterval(async () => {
            secs_remaining--;
            if (countdownElement) {
                countdownElement.textContent = `${secs_remaining}s.`;
            }

            if (secs_remaining <= 0) {
                await getHostData();
                updateLastRequestTime();
                secs_remaining = update_trigger;
            }
        }, 1000);
    }
    
    function updateLastRequestTime() {
        if (lastUpdateTimeElement) {
            lastUpdateTimeElement.textContent = new Date().toLocaleTimeString('pt-BR');
        }
    }
    
    startCountDown(); // Inicia todo o processo
});


// ===================================================================
// FUNÇÕES DE CONFIGURAÇÃO DOS GRÁFICOS (PARA REUTILIZAÇÃO)
// ===================================================================

// Opções para gráficos de linha/área genéricos
function getChartOptions(type, colors, yaxisTitle) {
    return {
        series: [{ data: [] }],
        chart: { type: type, height: 350, zoom: { enabled: true }, toolbar: { autoSelected: 'zoom' }, foreColor: '#555' },
        colors: colors,
        dataLabels: { enabled: false },
        markers: { size: 0 },
        fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.7, opacityTo: 0.1, stops: [0, 90, 100] }},
        yaxis: { title: { text: yaxisTitle } },
        xaxis: { type: 'datetime', labels: { format: 'dd MMM HH:mm' }},
        tooltip: { shared: false, x: { format: 'dd MMM yyyy - HH:mm' }}
    };
}

// Opções para o gráfico de Banda Larga
function getDataChartOptions() {
    const options = getChartOptions('area', ['#008FFB', '#00E396'], 'Tráfego (Mbps)');
    options.title = { text: 'Tráfego de Dados', align: 'left' };
    options.series = [{ name: 'Enviados', data: [] }, { name: 'Recebidos', data: [] }];
    return options;
}

// Opções para gráficos radiais
function getRadialChartOptions(labels) {
    return {
        series: [],
        chart: { height: 390, type: 'radialBar' },
        plotOptions: { radialBar: { offsetY: 0, startAngle: 0, endAngle: 270, hollow: { margin: 5, size: '30%', background: 'transparent' }}},
        colors: ['#092d7aff', 'orange', 'green'],
        labels: labels,
        legend: { show: true, floating: true, fontSize: '16px', position: 'left', offsetX: 50, offsetY: 15, labels: { useSeriesColors: true }}
    };
}

// ===================================================================
// FUNÇÕES DE CONFIGURAÇÃO DOS GRÁFICOS ESPECÍFICOS
// ===================================================================

function getCpuChartOptions() {
    return {
        series: [{
            name: 'Uso de CPU',
            data: [] // Começa com dados vazios
        }],
        chart: {
            type: 'area',
            stacked: false,
            height: 350,
            zoom: {
                type: 'x',
                enabled: true,
                autoScaleYaxis: true
            },
            toolbar: {
                autoSelected: 'zoom'
            },
            foreColor: '#555'
        },
        colors: ['#ad3d24ff'],
        dataLabels: {
            enabled: false
        },
        markers: {
            size: 0,
        },
        title: {
            text: 'Uso de CPU da Aplicação',
            align: 'left',
            offsetX: 20
        },
        fill: {
            type: 'gradient',
            gradient: {
                shade: 'light',
                shadeIntensity: 1,
                inverseColors: false,
                opacityFrom: 0.7,
                opacityTo: 0.1,
                stops: [0, 90, 100]
            },
        },
        yaxis: {
            labels: {
                formatter: function (val) {
                    return val.toFixed(2) + " %";
                },
            },
            title: {
                text: 'CPU (%)'
            },
        },
        xaxis: {
            type: 'datetime',
            labels: {
                formatter: function (value, timestamp) {
                    const date = new Date(timestamp);
                    // Formata a data para pt-BR FORÇANDO o fuso de Fortaleza
                    return date.toLocaleString('pt-BR', {
                        day: '2-digit', month: 'short', 
                        hour: '2-digit', minute: '2-digit',
                        timeZone: 'America/Fortaleza'
                    });
                }
            }
        },
        tooltip: {
            x: {
                formatter: function (value) {
                    const date = new Date(value);
                    // Formata o tooltip para pt-BR FORÇANDO o fuso de Fortaleza
                    return date.toLocaleString('pt-BR', {
                        day: '2-digit', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                        timeZone: 'America/Fortaleza'
                    });
                }
            },
            y: {
                formatter: function (val) {
                    return val.toFixed(2) + " %";
                }
            }
        }
    };
}

function getMemoryChartOptions() {
    return {
        series: [100, 0, 100], // Começa com 0% de uso
        chart: {
            height: 390,
            type: 'radialBar',
        },
        plotOptions: {
            radialBar: {
                offsetY: 0,
                startAngle: 0,
                endAngle: 270,
                hollow: {
                    margin: 5,
                    size: '30%',
                    background: 'transparent',
                }
            }
        },
        colors: ['#092d7aff', 'green', 'orange'],
        labels: ['Memória Total', 'Memória Livre', 'Memória em Uso'],

        tooltip: {
            enabled: true,
            theme: 'dark',
            y: {
                // Formatter simples para o estado inicial, mostrando apenas a porcentagem
                formatter: function(value) {
                    return `${value.toFixed(0)}%`;
                }
            }
        },

        legend: {
            show: true,
            floating: true,
            fontSize: '16px',
            position: 'left',
            offsetX: 50,
            offsetY: 15,
            labels: {
                useSeriesColors: true,
            },
            // Formatter simples para o estado inicial
            formatter: function(seriesName, opts) {
                const percentage = opts.w.globals.series[opts.seriesIndex];
                return `${seriesName}: ${percentage.toFixed(0)}%`;
            }
        },

        responsive: [{
            breakpoint: 480,
            options: {
                legend: {
                    show: false
                }
            }
        }]
    };
}

function getStorageChartOptions() {
    return {
        // Começa com 4 séries zeradas
        series: [0, 0, 0, 0], 
        chart: {
            height: 390,
            type: 'radialBar',
        },
        plotOptions: {
            radialBar: {
                offsetY: 0,
                startAngle: 0,
                endAngle: 270,
                hollow: { margin: 5, size: '30%', background: 'transparent' }
            }
        },
        // Cores para: Total /, Usado /, Total /boot, Usado /boot
        colors: ['#4d097aff', '#ff8c00', '#2e8b57', '#ffa500'],
        labels: ['Armazenamento Total ("/")', 'Armazenamento Usado ("/")', 'Armazenamento Total ("/boot")', 'Armazenamento Usado ("/boot")'],
        legend: {
            show: true,
            floating: true,
            fontSize: '14px',
            position: 'left',
            offsetX: 10,
            offsetY: 15,
            labels: { useSeriesColors: true },
            // Formatter inicial simples
            formatter: (seriesName, opts) => `${seriesName}: ${opts.w.globals.series[opts.seriesIndex].toFixed(0)}%`
        }
    };
}

function getDataChartOptions() {
    return {
        series: [{
            name: 'Dados Enviados (Upload)',
            data: []
        }, {
            name: 'Dados Recebidos (Download)',
            data: []
        }],
        chart: {
            height: 350,
            type: 'area',
            zoom: {
                enabled: true
            }
        },
        colors: ['#008FFB', '#00E396'],
        dataLabels: {
            enabled: false
        },
        stroke: {
            curve: 'smooth',
            width: 3
        },
        title: {
            text: 'Tráfego de Dados da Aplicação',
            align: 'left'
        },
        yaxis: {
            title: {
                text: 'Tráfego (Mbps)'
            },
            labels: {
                formatter: function(val) {
                    return val.toFixed(2) + " Mbps";
                }
            }
        },
        xaxis: {
            type: 'datetime',
            labels: {
                formatter: function (value, timestamp) {
                    const date = new Date(timestamp);
                    // Formata a data para pt-BR FORÇANDO o fuso de Fortaleza
                    return date.toLocaleString('pt-BR', {
                        day: '2-digit', month: 'short', 
                        hour: '2-digit', minute: '2-digit',
                        timeZone: 'America/Fortaleza'
                    });
                }
            }
        },
        tooltip: {
            x: {
                formatter: function (value) {
                    const date = new Date(value);
                    // Formata o tooltip para pt-BR FORÇANDO o fuso de Fortaleza
                    return date.toLocaleString('pt-BR', {
                        day: '2-digit', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                        timeZone: 'America/Fortaleza'
                    });
                }
            }
        },
        legend: {
            position: 'top'
        }
    };
}

// ===================================================================
// FUNÇÕES DE CONFIGURAÇÃO DOS GRÁFICOS ESPECÍFICOS DATABASE
// ===================================================================

function getCpuProcessesChartOptions() {
    return {
        series: [{
            name: 'Número máximo de processos',
            data: []
        }, {
            name: 'Número atual de processos',
            data: []
        }],
        chart: {
            height: 350,
            type: 'area',
            zoom: {
                enabled: true
            }
        },
        colors: ['#fb7900ff', '#00e3b2ff'],
        dataLabels: {
            enabled: false
        },
        stroke: {
            curve: 'smooth',
            width: 3
        },
        title: {
            text: 'Uso de Processos ao Longo do Tempo',
            align: 'left'
        },
        yaxis: {
            title: {
                text: 'Contagem' // Adicionei um título para clareza
            }
        },
        xaxis: {
            type: 'datetime',
            labels: {
                formatter: function (value, timestamp) {
                    const date = new Date(timestamp);
                    // Formata a data para pt-BR FORÇANDO o fuso de Fortaleza
                    return date.toLocaleString('pt-BR', {
                        day: '2-digit', month: 'short', 
                        hour: '2-digit', minute: '2-digit',
                        timeZone: 'America/Fortaleza'
                    });
                }
            }
        },
        tooltip: {
            x: {
                formatter: function (value) {
                    const date = new Date(value);
                    // Formata o tooltip para pt-BR FORÇANDO o fuso de Fortaleza
                    return date.toLocaleString('pt-BR', {
                        day: '2-digit', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                        timeZone: 'America/Fortaleza'
                    });
                }
            },
        },
        legend: {
            position: 'top'
        }
    };
}

