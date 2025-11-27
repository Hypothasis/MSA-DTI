document.addEventListener('DOMContentLoaded', function () {

    //#######################################################################
    //###        CONFIGURAÇÃO DOS GRÁFICOS USANDO APEXCHARTS.JS           ###
    //#######################################################################

    const avgAvailChart = new ApexCharts(document.querySelector("#disponMediaChart"), getAvgAvailabilityChartOptions());
    avgAvailChart.render();

    // ===================================================================
    // FUNÇÃO PRINCIPAL DE BUSCA E ATUALIZAÇÃO DE DADOS
    // ===================================================================
    
    async function getDashboardData() {
        console.log("Buscando dados do dashboard...");
        try {
            const response = await fetch('/admin/api/dashboard-stats');
            if (!response.ok) throw new Error('Falha ao buscar dados do dashboard.');
            
            const data = await response.json();
            
            document.getElementById('kpi-total-hosts').textContent = data.totalHosts;
            document.getElementById('kpi-active-hosts').textContent = data.activeHosts;
            document.getElementById('kpi-problem-hosts').textContent = data.alertHosts;
            document.getElementById('kpi-unreachable-hosts').textContent = data.inactiveHosts;

            const mediaDisponibilidade = data.overallAvailability || 0;
            
            let corGrafico = '#474747ff';

            if (mediaDisponibilidade < 95) {
                corGrafico = '#26E7A6';
            }
            if (mediaDisponibilidade < 90) {
                corGrafico = '#ffc65cff';
            }
            if (mediaDisponibilidade < 80) {
                corGrafico = '#FF4560';
            }

            console.log(`Média de Disponibilidade: ${mediaDisponibilidade}%, Cor do Gráfico: ${corGrafico}`);

            avgAvailChart.updateOptions({
                series: [mediaDisponibilidade],
                fill: {
                    colors: [corGrafico]
                }
            });

            updateAlertFeed(data.latestAlerts);
            
            updateTopHosts(data.topProblemHosts);

        } catch (error) {
            console.error("Erro ao carregar dados do dashboard:", error);
        }
    }
    
    // ===================================================================
    // FUNÇÕES AUXILIARES DE RENDERIZAÇÃO
    // ===================================================================

    function updateAlertFeed(alerts) {
        const container = document.getElementById('alert-feed-container');
        container.innerHTML = '';
        if (!alerts || alerts.length === 0) {
            container.innerHTML = '<p>Nenhum alerta crítico recente. Tudo certo!</p>';
            return;
        }
        
        const severidadeMap = {'5': 'disaster', '4': 'high', '3': 'avarage', '2': 'warning'};
        alerts.forEach(alert => {
            const severityClass = severidadeMap[alert.severity] || 'warning';
            container.innerHTML += `
                <div class="alert-item ${severityClass}">
                    <p class="alert-host">${alert.hostName}</p>
                    <p class="alert-name">${alert.eventName}</p>
                    <p class="alert-time">${alert.timestamp}</p>
                </div>
            `;
        });
    }

    function updateTopHosts(hosts) {
        const container = document.getElementById('top-hosts-container');
        container.innerHTML = '';
        if (!hosts || hosts.length === 0) {
            container.innerHTML = '<p>Nenhum host com problemas no momento.</p>';
            return;
        }

        const statusMap = {
            'ALERT': 'yellow',
            'INACTIVE': 'red'
        };
        hosts.forEach(host => {
            const statusClass = statusMap[host.status] || 'empty';
            container.innerHTML += `
                <div class="problem-host-item">
                    <div class="dot ${statusClass}"></div>
                    <div class="host-info">
                        <a href="/host/${host.publicId}">${host.name}:&nbsp;</a>
                        <p>${host.description}</p>
                    </div>
                </div>
            `;
        });
    }
    
    function getAvgAvailabilityChartOptions() {
        return {
            series: [0],
            chart: { type: 'radialBar', height: 250 },
            plotOptions: {
                radialBar: {
                    hollow: {
                        margin: 0,
                        size: '70%',
                        background: '#293450'
                    },
                    track: {
                        background: '#404F6D',
                    },
                    dataLabels: {
                        name: {
                            show: true,
                            offsetY: -10,
                            color: '#C0C0C0',
                            fontSize: '16px',
                            formatter: function (seriesName) {
                                return 'Disponibilidade'
                            }
                        },
                        value: {
                            offsetY: 5,
                            color: '#FFFFFF',
                            fontSize: '34px',
                            show: true,
                            formatter: function (val) {
                                return val.toFixed(2) + "%";
                            }
                        }
                    }
                }
            },
            tooltip: {
                enabled: true,
                theme: 'dark',
                fillSeriesColor: false,
                y: {
                    formatter: function (val) {
                        return val.toFixed(2) + "%"; 
                    },
                    title: {
                        formatter: function (seriesName) {
                            return 'Disponibilidade:';
                        }
                    }
                },
                marker: {
                    show: false
                }
            },
            fill: {
                colors: ['#26E7A6'],
            },
            stroke: { lineCap: 'round' },
            labels: ['Disponibilidade']
        };
    }
    
    // ===================================================================
    // INICIALIZAÇÃO E TIMER
    // ===================================================================

    function startTimer() {
        const update_trigger = 60; // 60 segundos
        let secs_remaining = update_trigger;

        setInterval(() => {
            secs_remaining--;
            if (secs_remaining <= 0) {
                getDashboardData();
                secs_remaining = update_trigger;
            }
        }, 1000);
    }

    getDashboardData(); 
    startTimer();
});
