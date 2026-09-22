document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('form[action$="/cuotas"]');

    if (!form) {
        return;
    }

    const mes = form.querySelector('select[name="mes"]');
    const anio = form.querySelector('input[name="anio"]');
    const estado = form.querySelector('select[name="estado"]');

    function aplicarFiltros() {
        form.submit();
    }

    // Selects: filtrar inmediatamente al cambiar.
    mes.addEventListener('change', aplicarFiltros);
    estado.addEventListener('change', aplicarFiltros);

    // Año: filtrar cuando cambia.
    anio.addEventListener('change', aplicarFiltros);

});