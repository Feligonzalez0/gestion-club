/**
 * Formulario de nueva cuota individual: precarga datos para evitar errores.
 *  - Al elegir un socio, precarga el importe segun su categoria (valor que
 *    viene de la configuracion en data-importe de cada opcion).
 *  - Al elegir mes/anio, precarga el vencimiento con el dia de vencimiento
 *    configurado (data-dia-vencimiento del formulario), ajustado al ultimo
 *    dia del mes si ese dia no existe (igual que la generacion mensual).
 *
 * Los valores precargados se pueden modificar a mano. Al volver al formulario
 * por un error de validacion no se pisan los valores ya cargados.
 */
document.addEventListener('DOMContentLoaded', function () {
    var socioSelect = document.getElementById('socioId');
    var mesSelect = document.getElementById('mes');
    var anioInput = document.getElementById('anio');
    var importeInput = document.getElementById('importe');
    var vencimientoInput = document.getElementById('fechaVencimiento');

    if (!socioSelect || !mesSelect || !anioInput || !importeInput || !vencimientoInput) {
        return;
    }

    var diaVencimiento = parseInt(socioSelect.form.getAttribute('data-dia-vencimiento'), 10);

    function dosDigitos(numero) {
        return numero < 10 ? '0' + numero : String(numero);
    }

    function sugerirImporte() {
        var opcion = socioSelect.options[socioSelect.selectedIndex];
        var importe = opcion ? opcion.getAttribute('data-importe') : null;

        if (importe) {
            importeInput.value = importe;
        }
    }

    function sugerirVencimiento() {
        var anio = parseInt(anioInput.value, 10);
        var mes = parseInt(mesSelect.value, 10);

        if (isNaN(diaVencimiento) || isNaN(mes) || isNaN(anio) || anio < 2000 || anio > 2100) {
            return;
        }

        var ultimoDiaDelMes = new Date(anio, mes, 0).getDate();
        var dia = Math.min(diaVencimiento, ultimoDiaDelMes);

        vencimientoInput.value = anio + '-' + dosDigitos(mes) + '-' + dosDigitos(dia);
    }

    socioSelect.addEventListener('change', sugerirImporte);
    mesSelect.addEventListener('change', sugerirVencimiento);
    anioInput.addEventListener('input', sugerirVencimiento);

    // Estado inicial: solo se completa lo que todavia esta vacio.
    if (!vencimientoInput.value) {
        sugerirVencimiento();
    }
    if (!importeInput.value) {
        sugerirImporte();
    }
});
