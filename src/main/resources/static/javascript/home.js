document.addEventListener('DOMContentLoaded', function () {

    // Variaveis para troca de imagem no header
    const navbarIcon = document.getElementById('navbar-icon');
    const sectionHeader = document.querySelector('main section header');

    //#######################################################################
    //###                  FUNÇÕES PARA O IMAGEM HEADER                   ###
    //#######################################################################

    // Checa se o elemento existe
    if (navbarIcon && sectionHeader) {
        const srcOriginal = navbarIcon.src;
        const srcHover = navbarIcon.getAttribute('data-hover-src');

        sectionHeader.addEventListener('mouseover', () => {
            navbarIcon.src = srcHover;
        });

        sectionHeader.addEventListener('mouseout', () => {
            navbarIcon.src = srcOriginal;
        });
    }
})