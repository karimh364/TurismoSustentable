const formularioRegistro = document.getElementById('registro-form');
const botonRegistro = document.getElementById('registro-submit');
const estadoCatalogos = document.getElementById('catalogos-status');
const tipoDocumentoRegistro = document.getElementById('reg-tipo-documento');
const numeroDocumentoRegistro = document.getElementById('reg-numero-documento');
const paisEmisorRegistro = document.getElementById('reg-pais-emisor');

let catalogosRegistroListos = false;
let registroEnCurso = false;
let tipoDocumentoAnterior = '';
let ultimoDocumentoValido = '';
let errorDocumentoRegistro = '';

// Cargar las opciones reales del backend.
async function cargarCatalogosRegistro() {
    try {
        const [generos, tiposDocumento, paises] = await Promise.all([
            axios.get('/api/catalogos/generos'),
            axios.get('/api/catalogos/tipos-documento'),
            axios.get('/api/catalogos/paises')
        ]);

        completarCatalogoRegistro('reg-genero', generos.data);
        completarCatalogoRegistro(
            'reg-tipo-documento',
            tiposDocumento.data
        );
        completarCatalogoRegistro('reg-pais-emisor', paises.data);

        [
            'reg-genero',
            'reg-tipo-documento',
            'reg-pais-emisor'
        ].forEach(id => {
            document.getElementById(id).disabled = false;
        });

        catalogosRegistroListos = true;
        estadoCatalogos.classList.add('d-none');
        actualizarTipoDocumentoRegistro();

    } catch {
        catalogosRegistroListos = false;
        botonRegistro.disabled = true;

        estadoCatalogos.textContent =
            'No se pudieron cargar las opciones de registro. ' +
            'Recargá la página para intentarlo nuevamente.';

        estadoCatalogos.classList.remove('text-muted', 'd-none');
        estadoCatalogos.classList.add('text-danger');
    }
}

// Mostrar el nombre de cada opción, conservando su ID real.
function completarCatalogoRegistro(idSelect, opciones) {
    const opcionesInvalidas =
        !Array.isArray(opciones)
        || opciones.length === 0
        || opciones.some(opcion =>
            !Number.isInteger(opcion?.id)
            || opcion.id <= 0
            || typeof opcion.codigo !== 'string'
            || !opcion.codigo.trim()
            || typeof opcion.nombre !== 'string'
            || !opcion.nombre.trim()
        );

    if (opcionesInvalidas) {
        throw new Error('El catálogo no contiene opciones válidas.');
    }

    const select = document.getElementById(idSelect);

    // Conservar solamente la opción inicial "Seleccioná...".
    select.options.length = 1;

    opciones.forEach(opcion => {
        const option = new Option(opcion.nombre, String(opcion.id));
        option.dataset.codigo = opcion.codigo;
        select.add(option);
    });
}

function normalizarNombreRegistro(valor) {
    return valor.normalize('NFC')
        .replace(/[\u2018\u2019\u02BC]/g, "'")
        .replace(/[\u2010\u2011]/g, '-')
        .replace(/\p{White_Space}+/gu, ' ')
        .trim();
}

function codigoTipoDocumentoRegistro() {
    return tipoDocumentoRegistro.selectedOptions[0]?.dataset.codigo || '';
}

function normalizarNumeroDocumentoRegistro(valor, tipo) {
    const numero = valor.replace(/\p{White_Space}+/gu, '').toUpperCase();
    return tipo === 'DNI' ? numero.replace(/\./g, '') : numero;
}

function actualizarBotonRegistro() {
    botonRegistro.disabled = !catalogosRegistroListos
        || registroEnCurso
        || Boolean(errorDocumentoRegistro);
}

function actualizarTipoDocumentoRegistro() {
    if (!catalogosRegistroListos) {
        return;
    }

    const tipo = codigoTipoDocumentoRegistro();
    const etiqueta = document.getElementById('reg-documento-label');
    const ayuda = document.getElementById('documento-ayuda');

    if (tipo !== tipoDocumentoAnterior) {
        numeroDocumentoRegistro.value = '';
        ultimoDocumentoValido = '';
        paisEmisorRegistro.value = '';
    }
    tipoDocumentoAnterior = tipo;
    errorDocumentoRegistro = '';
    numeroDocumentoRegistro.disabled = !tipo;
    numeroDocumentoRegistro.inputMode = tipo === 'DNI' ? 'numeric' : 'text';
    paisEmisorRegistro.disabled = tipo !== 'PASAPORTE';

    if (tipo === 'DNI') {
        const argentina = Array.from(paisEmisorRegistro.options)
            .find(option => option.dataset.codigo === 'AR');
        etiqueta.textContent = 'Número de DNI';
        ayuda.textContent = 'Solo números; se eliminan puntos y espacios.';
        if (argentina) {
            paisEmisorRegistro.value = argentina.value;
        } else {
            errorDocumentoRegistro = 'No se pudo preparar el registro con DNI. Intentá nuevamente más tarde.';
        }
    } else if (tipo === 'PASAPORTE') {
        etiqueta.textContent = 'Número de pasaporte';
        ayuda.textContent = 'Letras de A a Z y números; las letras se convierten a mayúsculas.';
    } else {
        etiqueta.textContent = 'Número de documento';
        ayuda.textContent = 'Elegí el tipo de documento para ingresar su número.';
        if (tipo) {
            numeroDocumentoRegistro.disabled = true;
            errorDocumentoRegistro = 'No se pudo preparar el tipo de documento seleccionado. Intentá nuevamente más tarde.';
        }
    }

    if (errorDocumentoRegistro) {
        estadoCatalogos.textContent = errorDocumentoRegistro;
        estadoCatalogos.classList.remove('d-none', 'text-muted');
        estadoCatalogos.classList.add('text-danger');
    } else {
        estadoCatalogos.classList.add('d-none');
    }
    actualizarBotonRegistro();
}

function actualizarNumeroDocumentoRegistro(event) {
    if (event?.isComposing) {
        return;
    }

    const tipo = codigoTipoDocumentoRegistro();
    const entrada = numeroDocumentoRegistro.value;
    const cursor = numeroDocumentoRegistro.selectionStart ?? entrada.length;
    const numero = normalizarNumeroDocumentoRegistro(entrada, tipo);
    const formato = tipo === 'DNI' ? /^[0-9]*$/ : /^[A-Z0-9]*$/;
    const longitudMaxima = tipo === 'DNI' ? 8 : 50;

    if (numero.length > longitudMaxima || !formato.test(numero)) {
        numeroDocumentoRegistro.value = ultimoDocumentoValido;
        const posicion = Math.min(cursor, ultimoDocumentoValido.length);
        numeroDocumentoRegistro.setSelectionRange(posicion, posicion);
        return;
    }

    numeroDocumentoRegistro.value = numero;
    const posicion = normalizarNumeroDocumentoRegistro(
        entrada.slice(0, cursor), tipo
    ).length;
    numeroDocumentoRegistro.setSelectionRange(posicion, posicion);
    ultimoDocumentoValido = numero;
}

tipoDocumentoRegistro.addEventListener('change', actualizarTipoDocumentoRegistro);
numeroDocumentoRegistro.addEventListener('input', actualizarNumeroDocumentoRegistro);
numeroDocumentoRegistro.addEventListener('compositionend', actualizarNumeroDocumentoRegistro);
['reg-nombre', 'reg-apellido'].forEach(id => {
    const input = document.getElementById(id);
    input.addEventListener('blur', () => {
        input.value = normalizarNombreRegistro(input.value);
    });
});

// Los IDs se envían como números; una selección vacía, como null.
function obtenerIdRegistro(idSelect) {
    const valor = Number(
        document.getElementById(idSelect).value
    );

    return Number.isInteger(valor) && valor > 0
        ? valor
        : null;
}

formularioRegistro.addEventListener('submit', async function(event) {
    event.preventDefault();

    if (registroEnCurso) {
        return;
    }

    if (!catalogosRegistroListos) {
        mostrarErrores([
            'Esperá a que se carguen las opciones de registro.'
        ]);
        return;
    }

    if (errorDocumentoRegistro) {
        mostrarErrores([errorDocumentoRegistro]);
        return;
    }

    const payload = {
        nombre: normalizarNombreRegistro(document.getElementById('reg-nombre').value),
        apellido: normalizarNombreRegistro(document.getElementById('reg-apellido').value),
        nickname: document.getElementById('reg-nickname').value,
        email: document.getElementById('reg-email').value,

        fechaNacimiento:
            document.getElementById('reg-nacimiento').value || null,

        generoId: obtenerIdRegistro('reg-genero'),
        tipoDocumentoId: obtenerIdRegistro('reg-tipo-documento'),

        numeroDocumento:
            normalizarNumeroDocumentoRegistro(
                numeroDocumentoRegistro.value, codigoTipoDocumentoRegistro()
            ),

        paisEmisorId: obtenerIdRegistro('reg-pais-emisor'),
        password: document.getElementById('reg-password').value
    };

    registroEnCurso = true;
    botonRegistro.disabled = true;
    botonRegistro.textContent = 'Registrando...';

    try {
        await axios.post('/api/usuarios', payload);

        await Swal.fire({
            icon: 'success',
            title: '¡Registro exitoso!',
            text: 'Tu cuenta ha sido creada. Ahora podés iniciar sesión.',
            confirmButtonColor: '#198754'
        });

        window.location.href = 'index.html#login-anchor';

    } catch (error) {

        const estado = error.response?.status;
        const datos = error.response?.data;

        if (estado === 400
            && Array.isArray(datos?.validationErrors)
            && datos.validationErrors.length > 0) {

            mostrarErrores(datos.validationErrors);

        } else if ((estado === 400 || estado === 409)
            && typeof datos === 'string'
            && datos.trim()) {

            mostrarErrores([datos]);

        } else if (estado === 400) {
            mostrarErrores([
                'Revisá el formato de los datos ingresados.'
            ]);

        } else if (estado === 409) {
            mostrarErrores([
                'Los datos entran en conflicto con otro registro. ' +
                'Revisá el email, el nombre de usuario y el documento.'
            ]);

        } else if (estado === 401 || estado === 403) {
            mostrarErrores([
                'El servidor no autorizó el registro. ' +
                'Recargá la página e intentá nuevamente.'
            ]);

        } else if (estado >= 500) {
            mostrarErrores([
                'Ocurrió un error en el servidor. Intentá nuevamente.'
            ]);

        } else {
            mostrarErrores([
                'No se pudo confirmar el registro. Revisá la conexión. ' +
                'Antes de reintentar, comprobá si podés iniciar sesión.'
            ]);
        }

        registroEnCurso = false;
        actualizarBotonRegistro();
        botonRegistro.textContent = 'Registrarme';
    }
});

// Mostrar los mensajes como texto, sin interpretarlos como HTML.
function mostrarErrores(mensajes) {
    const statusDiv = document.getElementById('status-message');
    statusDiv.replaceChildren();

    const mensajesLegibles = mensajes.map(mensaje => {
        if (mensaje.startsWith('nickname:')
            && !document.getElementById('reg-nickname').value.trim()) {
            return 'El nombre de usuario es obligatorio.';
        }

        if (mensaje.startsWith('password:')
            && !document.getElementById('reg-password').value.trim()) {
            return 'La contraseña es obligatoria.';
        }

        return mensaje
            .replace(
                /^(nombre|apellido|nickname|email|fechaNacimiento|generoId|tipoDocumentoId|numeroDocumento|paisEmisorId|password):\s*/,
                ''
            )
            .replace(/\bnickname\b/gi, 'nombre de usuario')
            .replace(/\bclave\b/gi, 'contraseña');
    });

    const titulo = document.createElement('strong');
    titulo.textContent = 'Por favor, revisá los siguientes errores:';

    const lista = document.createElement('ul');

    [...new Set(mensajesLegibles)].sort().forEach(mensaje => {
        const elemento = document.createElement('li');
        elemento.textContent = mensaje;
        lista.appendChild(elemento);
    });

    statusDiv.append(titulo, lista);
    statusDiv.classList.remove('d-none');

    statusDiv.tabIndex = -1;
    statusDiv.focus({ preventScroll: true });
    statusDiv.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    });
}

cargarCatalogosRegistro();
