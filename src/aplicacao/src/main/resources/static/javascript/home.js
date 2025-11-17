document.addEventListener('DOMContentLoaded', function () {

    // Variaveis para troca de imagem no header
    const navbarIcon = document.getElementById('navbar-icon');
    const filterIcon = document.getElementById('filter-icon');
    const sectionHeader = document.querySelector('main section header');
    const aside = document.querySelector('main section article aside');

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

    // Checa se o elemento existe
    if (filterIcon && aside) {
        const srcOriginal = filterIcon.src;
        const srcHover = filterIcon.getAttribute('data-hover-src');

        aside.addEventListener('mouseover', () => {
            filterIcon.src = srcHover;
        });

        aside.addEventListener('mouseout', () => {
            filterIcon.src = srcOriginal;
        });
    }

    //#######################################################################
    //###                     FUNÇÕES PARA O FILTROS                      ###
    //#######################################################################

    const categoriaCheckboxes = document.querySelectorAll('li:nth-child(1) article input[type="checkbox"]');
    const estadoCheckboxes = document.querySelectorAll('li:nth-child(2) article input[type="checkbox"]');

    function handleSingleChoice(clickedCheckbox, checkboxGroup) {
        // Se o usuário está desmarcando o checkbox, não fazemos nada.
        if (!clickedCheckbox.checked) {
            return;
        }

        checkboxGroup.forEach(checkbox => {
            // Se o checkbox atual for DIFERENTE do que foi clicado...
            if (checkbox !== clickedCheckbox) {
                // ...desmarca ele.
                checkbox.checked = false;
            }
        });
    }

    categoriaCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            handleSingleChoice(checkbox, categoriaCheckboxes);
        });
    });

    estadoCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', () => {
            handleSingleChoice(checkbox, estadoCheckboxes);
        });
    });
})