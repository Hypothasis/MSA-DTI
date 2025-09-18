document.addEventListener('DOMContentLoaded', function () {

    //#######################################################################
    //###        CONFIGURAÇÃO DOS GRÁFICOS USANDO APEXCHARTS.JS           ###
    //#######################################################################

    // Suponha que sua API retornou este valor
    const mediaDisponibilidade = 100;

    // Lógica para definir a cor baseada no valor
    let corGrafico = '#26E7A6'; // Verde (bom)

    if (mediaDisponibilidade < 90) {
        corGrafico = '#ffc65cff'; // Amarelo (ok)
    }

    if (mediaDisponibilidade < 80) {
        corGrafico = '#FF4560'; // Vermelho (ruim)
    }

    var options = {
        series: [mediaDisponibilidade],
        chart: {
            height: 350,
            type: 'radialBar',
        },
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
            colors: [corGrafico]
        },
        stroke: {
            lineCap: 'round'
        },
        labels: ['Disponibilidade'],
    };

    var chart = new ApexCharts(document.querySelector("#disponMediaChart"), options);
    chart.render();
      

})