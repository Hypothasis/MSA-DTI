document.addEventListener('DOMContentLoaded', function () {

    // Variavel para Botão de Alert
    let alert_flag = false
    let alert_btn = document.getElementById('alert-btn')

    // Variavel para Botão de Fullscreen
    let fullscreen_btn = document.getElementById('fullscreen-btn')
    const fullscreenElement = document.documentElement;

    //#######################################################################
    //###         FUNÇÕES PARA O BOTÃO DE FULLSCREEN E ALERTSOUND         ###
    //#######################################################################

    // Checa se o elemento existe
    if (fullscreen_btn){
        fullscreen_btn.addEventListener('click', () => {
            // Serve como um chave para a classe css
            fullscreen_btn.classList.toggle('actived') 

            if (!document.fullscreenElement) {
                // Chama a função para tornar a tag <html> fullscreen
                startFullscreen(fullscreenElement);
            } else {
                stopFullscreen();
            }
        })
    }

    // Checa se o elemento existe
    if (alert_btn){
        alert_btn.addEventListener('click', () => {
            alert_btn.classList.toggle('actived') 
        
            const alert_icon = alert_btn.querySelector('img');

            // Além de checar se o icone existe
            // verifica se o botao foi clicado
            // Se sim muda o icone
            if (alert_icon) {
                if (!alert_flag) {
                    alert_icon.src = '../img/icons/sound_black.png'
                    alert_flag = true
                } else {
                    alert_icon.src = '../img/icons/mute_black.png'
                    alert_flag = false
                }
            }
        })
    }

    function startFullscreen(elemento) {
        if (elemento.requestFullscreen) {
            elemento.requestFullscreen();
        } else if (elemento.webkitRequestFullscreen) { // Safari
            elemento.webkitRequestFullscreen();
        } else if (elemento.mozRequestFullScreen) { // Firefox
            elemento.mozRequestFullScreen();
        } else if (elemento.msRequestFullscreen) { // IE/Edge
            elemento.msRequestFullscreen();
        }
    }

    function stopFullscreen() {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.webkitExitFullscreen) { // Safari
            document.webkitExitFullscreen();
        } else if (document.mozCancelFullScreen) { // Firefox
            document.mozCancelFullScreen();
        } else if (document.msExitFullscreen) { // IE/Edge
            document.msExitFullscreen();
        }
    }

})