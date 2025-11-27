document.addEventListener('DOMContentLoaded', function () {

    const navbarIcon = document.getElementById('navbar-icon');
    const filterIcon = document.getElementById('filter-icon');
    const sectionHeader = document.querySelector('main section header');
    const aside = document.querySelector('main section article aside');

    //#######################################################################
    //###                  FUNÇÕES PARA O IMAGEM HEADER                   ###
    //#######################################################################

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
        if (!clickedCheckbox.checked) {
            return;
        }

        checkboxGroup.forEach(checkbox => {
            if (checkbox !== clickedCheckbox) {
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