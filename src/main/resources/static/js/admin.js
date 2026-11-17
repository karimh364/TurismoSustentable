// Variables globales
let listaEstaciones = []; // Para llenar selects

document.addEventListener("DOMContentLoaded",
    async () => {

        const usuario = await requerirSesion(['ADMIN']);
        if (!usuario) {
            return;
        }
        OperacionesAlquiler.configurar(usuario, async () => {
            await Promise.all([cargarAlquileres(), cargarBicicletas(), cargarEstaciones()]);
        }, true);

        cargarTodo();
    }
);

function cargarTodo() {
    cargarUsuarios();
    cargarEstaciones(); // También llena la lista global para el select de bicis
    cargarBicicletas();
    cargarAlquileres();
}

// ==========================================
// 1. GESTIÓN DE USUARIOS
// ==========================================
async function cargarUsuarios() {
    try {
        const res = await axios.get('/api/usuarios');
        const tbody = document.getElementById('tabla-usuarios-body');
        tbody.innerHTML = '';
        res.data.forEach(u => {
            tbody.innerHTML += `
                <tr>
                    <td>${u.id}</td>
                    <td>${u.nombre} ${u.apellido}</td>
                    <td>${u.email}</td>
                    <td>${u.genero}</td>
                    <td>${u.fechaNacimiento}</td>
                    <td>${u.roles.join(', ')}</td>
                    <td>${u.activo ? '<span class="badge bg-success">Activo</span>' : '<span class="badge bg-danger">Inactivo</span>'}</td>
                    <td>
                        <button class="btn btn-sm btn-warning" onclick="editarUsuario(${u.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-danger" onclick="borrarUsuario(${u.id})"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>`;
        });
        return true;
    } catch (e) {
        if (manejarErrorSesion(e)) return null;
        console.error(e);
        return false;
    }
}

// DELETE Usuario
async function borrarUsuario(id) {
    const decision=await Swal.fire({title:'Inactivar usuario',input:'textarea',
        inputLabel:'Motivo (obligatorio si tiene órdenes abiertas; se pausarán/desasignarán juntas)',
        showCancelButton:true,inputValidator:v=>!v.trim()||v.trim().length>2000?'Ingresá un motivo de hasta 2000 caracteres.':undefined});
    if (decision.isConfirmed) {
        try {
            await axios.delete(`/api/usuarios/${id}`,{data:{motivo:decision.value.trim()}});
            cargarUsuarios();
            Swal.fire('Usuario inactivado', 'Se conservan sus datos y las órdenes cerradas. Las abiertas se desasignan; las que estaban en proceso quedan pausadas.', 'success');
        } catch (e) {
            if (manejarErrorSesion(e)) return;
            Swal.fire('No se pudo inactivar', typeof e.response?.data==='string'?e.response.data:'Intentá nuevamente.', 'error');
        }
    }
}

let cargandoEdicionUsuario = false;
let guardandoEdicionUsuario = false;

function mensajeEdicionUsuario(error, predeterminado) {
    const datos = error.response?.data;
    if (typeof datos === 'string' && datos.trim()) return datos;
    if (Array.isArray(datos?.validationErrors) && datos.validationErrors.length) {
        const etiquetas = {
            nombre: 'Nombre', apellido: 'Apellido', email: 'Email',
            fechaNacimiento: 'Fecha de nacimiento', generoId: 'Género'
        };
        return [...new Set(datos.validationErrors.map(valor => {
            const texto = String(valor);
            const separador = texto.indexOf(':');
            if (separador < 0) return texto;
            const campo = texto.slice(0, separador).trim();
            const mensaje = texto.slice(separador + 1).trim();
            return etiquetas[campo] ? `${etiquetas[campo]}: ${mensaje}` : mensaje;
        }))].join('\n');
    }
    if (!error.response && !error.isAxiosError && !error.request && error.message) {
        return error.message;
    }
    return predeterminado;
}

// UsuarioDTO informa el código; el catálogo aporta el ID real para actualizar.
async function editarUsuario(id) {
    if (cargandoEdicionUsuario || guardandoEdicionUsuario) return;
    cargandoEdicionUsuario = true;
    const boton = document.getElementById('user-guardar');
    if (boton) boton.disabled = true;
    document.getElementById('user-id').value = '';
    try {
        const usuarioId = Number(id);
        if (!Number.isSafeInteger(usuarioId) || usuarioId <= 0) {
            throw new Error('Recargá el listado y seleccioná un usuario válido.');
        }
        const [res, catalogo] = await Promise.all([
            axios.get(`/api/usuarios/${usuarioId}`),
            axios.get('/api/catalogos/generos')
        ]);
        const u = res.data;
        const generos = catalogo.data;
        if (!u || u.id !== usuarioId) throw new Error('Los datos del usuario no son válidos. Recargá el listado.');
        if (!Array.isArray(generos) || !generos.length || generos.some(g =>
            !g || !Number.isSafeInteger(g.id) || g.id <= 0 || typeof g.codigo !== 'string' || !g.codigo)) {
            throw new Error('No se pudieron cargar las opciones de género. Intentá nuevamente.');
        }
        const actual = generos.find(g => g.codigo === u.genero);
        if (!actual) throw new Error('El género del usuario no está disponible en el catálogo. Recargá los datos.');

        const selector = document.getElementById('user-genero');
        selector.innerHTML = '';
        const opcionInicial = new Option('Seleccioná un género', '');
        opcionInicial.disabled = true;
        selector.add(opcionInicial);
        generos.forEach(g => selector.add(new Option(g.nombre || g.codigo, String(g.id))));

        document.getElementById('user-id').value = u.id;
        document.getElementById('user-nombre').value = u.nombre;
        document.getElementById('user-apellido').value = u.apellido;
        document.getElementById('user-email').value = u.email;
        selector.value = String(actual.id);
        document.getElementById('user-fecha').value = u.fechaNacimiento || '';

        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
    } catch (error) {
        if (manejarErrorSesion(error)) return;
        await Swal.fire({title: 'No se pudo abrir la edición', icon: 'error',
            text: mensajeEdicionUsuario(error, 'No se pudieron cargar el usuario y sus opciones. Intentá nuevamente.')});
    } finally {
        cargandoEdicionUsuario = false;
        if (boton) boton.disabled = false;
    }
}

async function guardarUsuario() {
    if (guardandoEdicionUsuario || cargandoEdicionUsuario) return;
    guardandoEdicionUsuario = true;
    const boton = document.getElementById('user-guardar');
    if (boton) boton.disabled = true;
    try {
        const id = Number(document.getElementById('user-id').value);
        const data = {
            nombre: document.getElementById('user-nombre').value,
            apellido: document.getElementById('user-apellido').value,
            email: document.getElementById('user-email').value,
            generoId: Number(document.getElementById('user-genero').value),
            fechaNacimiento: document.getElementById('user-fecha').value
        };
        if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Volvé a abrir el usuario antes de guardar.');
        if (!data.nombre.trim()) throw new Error('El nombre es obligatorio.');
        if (!data.apellido.trim()) throw new Error('El apellido es obligatorio.');
        if (!data.email.trim()) throw new Error('El email es obligatorio.');
        if (!Number.isSafeInteger(data.generoId) || data.generoId <= 0) throw new Error('Seleccioná un género válido.');
        if (!data.fechaNacimiento) throw new Error('La fecha de nacimiento es obligatoria.');

        await axios.put(`/api/usuarios/${id}`, data);

        bootstrap.Modal.getInstance(document.getElementById('modalUsuario'))?.hide();
        const actualizado = await cargarUsuarios();
        if (actualizado === null) return; // La sesión ya informó/redirigió el acceso.
        await Swal.fire({title: 'Usuario guardado', icon: actualizado === false ? 'warning' : 'success',
            text: actualizado === false
                ? 'Se guardaron los cambios, pero no se pudo actualizar el listado. Recargá la página para consultarlos.'
                : 'Usuario actualizado con éxito.'});
    } catch (e) {
        if (manejarErrorSesion(e)) return;
        await Swal.fire({title: 'No se pudo guardar el usuario', icon: 'error',
            text: mensajeEdicionUsuario(e, 'No se pudo guardar el usuario. Revisá la conexión e intentá nuevamente.')});
    } finally {
        guardandoEdicionUsuario = false;
        if (boton) boton.disabled = false;
    }
}


// ==========================================
// 2. GESTIÓN DE ESTACIONES
// ==========================================
async function cargarEstaciones() {
    try {
        const res = await axios.get('/api/admin/estaciones');
        listaEstaciones = res.data; // Guardamos para usar en Bicis
        const tbody = document.getElementById('tabla-estaciones-body');
        tbody.innerHTML = '';
        res.data.forEach(e => {
            tbody.innerHTML += `
                <tr>
                    <td>${e.id}</td>
                    <td>${e.nombre}</td>
                    <td>${e.direccion}</td>
                    <td>${e.capacidad}</td>
                    <td>
                        <span class="badge ${e.esPublica ? 'bg-success' : 'bg-secondary'}">
                            ${e.esPublica ? 'Pública' : 'Uso interno'}
                        </span>
                    </td>
                    <td>${e.latitud ? e.latitud : '-'}</td>
                    <td>${e.longitud ? e.longitud : '-'}</td>
                    
                    <td>
                        <button class="btn btn-sm btn-warning" onclick="editarEstacion(${e.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-danger" onclick="borrarEstacion(${e.id})"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>`;
        });
    } catch (e) {
        if (manejarErrorSesion(e)) return;
        console.error(e);
    }
}

function abrirModalEstacion() {
    document.getElementById('estacion-id').value = '';
    document.getElementById('estacion-version').value = '';
    document.getElementById('estacion-nombre').value = '';
    document.getElementById('estacion-direccion').value = '';
    document.getElementById('estacion-capacidad').value = '';
    document.getElementById('estacion-latitud').value = '';
    document.getElementById('estacion-longitud').value = '';
    const selectorUso = document.getElementById('estacion-es-publica');
    selectorUso.value = '';
    selectorUso.disabled = false;
    new bootstrap.Modal(document.getElementById('modalEstacion')).show();
}

async function editarEstacion(id) {
    // Buscamos en la lista local para ahorrar una petición
    const e = listaEstaciones.find(est => est.id === id);
    document.getElementById('estacion-id').value = e.id;
    document.getElementById('estacion-version').value = e.version ?? '';
    document.getElementById('estacion-nombre').value = e.nombre;
    document.getElementById('estacion-direccion').value = e.direccion;
    document.getElementById('estacion-capacidad').value = e.capacidad;
    document.getElementById('estacion-latitud').value = e.latitud ?? '';
    document.getElementById('estacion-longitud').value = e.longitud ?? '';
    const selectorUso = document.getElementById('estacion-es-publica');

    selectorUso.value = typeof e.esPublica === 'boolean'
        ? String(e.esPublica)
        : '';

    selectorUso.disabled = true;
    new bootstrap.Modal(document.getElementById('modalEstacion')).show();
}

async function guardarEstacion() {
    const id = document.getElementById('estacion-id').value;

    const data = {
        nombre: document.getElementById('estacion-nombre').value,
        direccion: document.getElementById('estacion-direccion').value,
        capacidad: parseInt(document.getElementById('estacion-capacidad').value),
        // Convertimos los valores a Float para que Java los reciba correctamente
        latitud: parseFloat(document.getElementById('estacion-latitud').value),
        longitud: parseFloat(document.getElementById('estacion-longitud').value)
    };

    // La clasificación se envía únicamente al crear.
    if (!id) {
        const uso = document.getElementById('estacion-es-publica').value;

        if (uso !== 'true' && uso !== 'false') {
            Swal.fire(
                'Falta seleccionar el uso',
                'Indicá si la estación es pública o de uso interno.',
                'warning'
            );
            return;
        }

        data.esPublica = uso === 'true';
    }

    // La versión se envía únicamente cuando se edita.
    if (id) {
        const versionTexto = document
            .getElementById('estacion-version')
            .value.trim();

        const version = Number(versionTexto);

        if (
            versionTexto === '' ||
            !Number.isInteger(version) ||
            version < 0
        ) {
            Swal.fire(
                'Recargá los datos',
                'Cerrá el formulario y actualizá la página antes de editar.',
                'warning'
            );
            return;
        }

        data.version = version;
    }

    try {
        if (id) await axios.put(`/api/estaciones/${id}`, data);
        else await axios.post('/api/estaciones', data);

        bootstrap.Modal.getInstance(document.getElementById('modalEstacion')).hide();
        cargarEstaciones();
        Swal.fire('Guardado', 'Estación guardada correctamente', 'success');
    } catch (e) {
        if (manejarErrorSesion(e)) return;
        const respuesta = e.response?.data;

        let mensaje =
            'No se pudo guardar la estación. Intentá nuevamente.';

        if (typeof respuesta === 'string' && respuesta.trim()) {
            mensaje = respuesta;
        } else if (Array.isArray(respuesta?.validationErrors)) {
            const mensajes = respuesta.validationErrors.map(
                error => String(error).replace(/^[^:]+:\s*/, '')
            );

            if (mensajes.length > 0) {
                mensaje = [...new Set(mensajes)].join('\n');
            }
        }

        Swal.fire({
            icon: 'error',
            title: 'No se pudo guardar',
            text: mensaje
        });
    }
}

async function borrarEstacion(id) {
    if (await confirmar()) {
        try {
            await axios.delete(`/api/estaciones/${id}`);
            cargarEstaciones();
            Swal.fire('Eliminado', '', 'success');
        } catch (e) { 
            if (manejarErrorSesion(e)) return;
            // Aquí capturamos el 409 Conflict (bicis asignadas)
            Swal.fire('No se puede borrar', 'La estación tiene bicicletas asignadas.', 'warning'); 
        }
    }
}

// ==========================================
// 3. GESTIÓN DE BICICLETAS
// ==========================================
let tiposBicicleta = [];
let bicicletaEnEdicion = null;

async function cargarBicicletas() {
    try {
        const incluirEliminadas = document.getElementById('bici-incluir-eliminadas').checked;
        const { data } = await axios.get('/api/bicicletas', { params: { incluirEliminadas } });
        // Un cambio rápido del filtro no debe mostrar la respuesta del filtro anterior.
        if (document.getElementById('bici-incluir-eliminadas').checked !== incluirEliminadas) return;
        const tbody = document.getElementById('tabla-bicicletas-body');
        tbody.replaceChildren();
        data.forEach(b => {
            const fila = tbody.insertRow();
            [b.id, b.codigoBicicleta, b.tipo,
                b.estado + (b.borradoBici ? ' · Eliminada' : ''),
                'USD ' + b.precio,
                b.borradoBici && b.estacionId === null ? 'Fuera del inventario' : b.ubicacion
            ].forEach((valor, indice) => {
                const celda = fila.insertCell();
                if (indice === 3) {
                    const estado = document.createElement('span');
                    estado.className = 'badge bg-' + getColorEstado(b.estado);
                    estado.textContent = valor;
                    celda.append(estado);
                } else celda.textContent = valor;
            });
            const acciones = fila.insertCell();
            function boton(texto, clase, accion) {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn btn-sm me-1 mb-1 ' + clase;
                btn.textContent = texto;
                btn.addEventListener('click', accion);
                acciones.append(btn);
            }
            boton(b.borradoBici ? 'Ver' : 'Ver / editar', 'btn-warning', () => editarBici(b.id));
            if (!b.borradoBici) {
                if (['DISPONIBLE', 'INACTIVA'].includes(b.estado)) {
                    const objetivo = b.estado === 'DISPONIBLE' ? 'INACTIVA' : 'DISPONIBLE';
                    boton(objetivo === 'INACTIVA' ? 'Inactivar' : 'Habilitar', 'btn-outline-secondary',
                        () => operarBicicleta(b.id, objetivo));
                    boton('Dar de baja', 'btn-outline-danger', () => operarBicicleta(b.id, 'DE_BAJA'));
                }
                if (b.estado === 'EXTRAVIADA' || (b.estado === 'DE_BAJA' && b.estacionId === null)) {
                    boton('Gestionar pérdida', 'btn-outline-danger', () => { window.location.href = 'incidencias.html?bicicletaId=' + b.id; });
                } else {
                    boton('Eliminar', 'btn-danger', () => borrarBici(b.id));
                }
            }
        });
    } catch (e) { errorInventario(e); }
}

function errorInventario(e) {
    if (manejarErrorSesion(e)) return;
    const errores = e.response?.data?.validationErrors;
    const mensaje = typeof e.response?.data === 'string' ? e.response.data
        : Array.isArray(errores)
            ? errores.map(m => m.includes(':') ? m.slice(m.indexOf(':') + 1).trim() : m).join('\n')
            : 'No se pudo completar la operación.';
    Swal.fire('No se pudo completar', mensaje, 'error');
}

async function prepararCatalogosBicicleta(esEdicion = false, estacionActualId = null) {
    const [tipos, estaciones] = await Promise.all([
        axios.get('/api/admin/tipos-bicicleta'), axios.get('/api/admin/estaciones')
    ]);
    tiposBicicleta = tipos.data;
    listaEstaciones = estaciones.data;
    const tipoSelect = document.getElementById('bici-tipo');
    const estacionSelect = document.getElementById('bici-estacion-select');
    tipoSelect.replaceChildren(new Option('Seleccioná un tipo', ''));
    estacionSelect.replaceChildren();
    if (!esEdicion) {
        const opcionEstacion = new Option('Seleccioná una estación', '', true, true);
        opcionEstacion.disabled = true;
        estacionSelect.add(opcionEstacion);
    }
    tiposBicicleta.forEach(t => tipoSelect.add(new Option(t.codigo, t.id)));
    listaEstaciones.forEach(e => {
        const esActual = esEdicion && e.id === estacionActualId;
        const disponibilidadValida = Number.isInteger(e.espaciosLibres) && e.espaciosLibres >= 0;
        const disponibilidad = disponibilidadValida
            ? e.espaciosLibres + ' espacios libres' : 'Disponibilidad no informada';
        const opcion = new Option(e.nombre + (e.esPublica ? ' · Pública' : ' · Uso interno')
            + ' · ' + disponibilidad + (esActual ? ' · Actual' : ''), e.id);
        opcion.disabled = !esActual && (!disponibilidadValida || e.espaciosLibres === 0);
        estacionSelect.add(opcion);
    });
}

function mostrarTarifaBicicleta() {
    const tipo = tiposBicicleta.find(t => t.id === Number(document.getElementById('bici-tipo').value));
    document.getElementById('bici-precio').value = tipo ? tipo.precioPorHoraUsd : '';
}

function mostrarEstadoBicicleta(codigo) {
    // Solo se muestra el estado vigente; no se ofrecen transiciones manuales.
    document.getElementById('bici-estado').replaceChildren(
        new Option(codigo, codigo, true, true));
}

function actualizarMotivoTraslado() {
    const selector = document.getElementById('bici-estacion-select');
    const motivo = document.getElementById('bici-motivo');
    const destinoId = Number(selector.value) || null;
    const hayTraslado = bicicletaEnEdicion !== null && !selector.disabled
        && destinoId !== null && destinoId !== bicicletaEnEdicion.estacionId;
    document.getElementById('bici-motivo-grupo').classList.toggle('d-none', !hayTraslado);
    motivo.disabled = !hayTraslado;
    motivo.required = hayTraslado;
    if (!hayTraslado) motivo.value = '';
}

async function abrirModalBicicleta() {
    try {
        await prepararCatalogosBicicleta();
        bicicletaEnEdicion = null;
        document.getElementById('bici-id').value = '';
        document.getElementById('bici-codigo').value = '';
        document.getElementById('bici-tipo').value = '';
        mostrarEstadoBicicleta('DISPONIBLE');
        document.getElementById('bici-motivo').value = '';
        ['bici-codigo', 'bici-tipo', 'bici-estacion-select', 'bici-guardar']
            .forEach(id => document.getElementById(id).disabled = false);
        actualizarMotivoTraslado();
        document.getElementById('bici-alquiler-aviso').classList.add('d-none');
        mostrarTarifaBicicleta();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalBicicleta')).show();
    } catch (e) { errorInventario(e); }
}

async function editarBici(id) {
    try {
        const { data: b } = await axios.get('/api/bicicletas/' + id);
        await prepararCatalogosBicicleta(true, b.estacionId);
        bicicletaEnEdicion = b;
        document.getElementById('bici-id').value = b.id;
        document.getElementById('bici-codigo').value = b.codigoBicicleta;
        document.getElementById('bici-tipo').value = b.tipoBicicletaId;
        mostrarEstadoBicicleta(b.estado);
        const estacionSelect = document.getElementById('bici-estacion-select');
        if (b.estacionId == null) {
            const sinEstacion = new Option('Sin estación asignada', '', true, true);
            sinEstacion.disabled = true;
            estacionSelect.add(sinEstacion);
        }
        estacionSelect.value = b.estacionId ?? '';
        document.getElementById('bici-motivo').value = '';
        const editable = !b.borradoBici && ['DISPONIBLE', 'INACTIVA'].includes(b.estado);
        ['bici-codigo', 'bici-tipo', 'bici-estacion-select', 'bici-guardar']
            .forEach(campo => document.getElementById(campo).disabled = !editable);
        actualizarMotivoTraslado();
        document.getElementById('bici-alquiler-aviso').classList.toggle('d-none', editable);
        mostrarTarifaBicicleta();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalBicicleta')).show();
    } catch (e) { errorInventario(e); }
}

async function guardarBicicleta() {
    const id = document.getElementById('bici-id').value;
    const tipoId = Number(document.getElementById('bici-tipo').value);
    const estacionId = Number(document.getElementById('bici-estacion-select').value) || null;
    const codigo = document.getElementById('bici-codigo').value.trim();
    const motivo = document.getElementById('bici-motivo').value.trim();
    if (!codigo || !tipoId || (!id && !estacionId)) {
        Swal.fire('Faltan datos', 'Completá el código, el tipo y la estación para el alta.', 'warning');
        return;
    }
    if (id && estacionId && estacionId !== bicicletaEnEdicion.estacionId && !motivo) {
        Swal.fire('Falta el motivo', 'Indicá por qué se traslada la bicicleta.', 'warning');
        return;
    }
    const data = { codigoBicicleta: codigo, tipoBicicletaId: tipoId, estacionId };
    if (id) Object.assign(data, { version: bicicletaEnEdicion.version, motivo });
    const boton = document.getElementById('bici-guardar');
    boton.disabled = true;
    try {
        if (id) await axios.put('/api/bicicletas/' + id, data);
        else await axios.post('/api/bicicletas', data);
        bootstrap.Modal.getInstance(document.getElementById('modalBicicleta')).hide();
        await Promise.all([cargarBicicletas(), cargarEstaciones()]);
        Swal.fire('Guardado', '', 'success');
    } catch (e) { errorInventario(e); }
    finally { boton.disabled = false; }
}

async function operarBicicleta(id, estado = null) {
    try {
        const { data: b } = await axios.get('/api/bicicletas/' + id);
        const resultado = await Swal.fire({
            titleText: estado ? b.codigoBicicleta + ' → ' + estado : 'Eliminar ' + b.codigoBicicleta,
            text: !estado
                ? 'Confirmá que la bicicleta ya fue retirada físicamente del inventario desde su ubicación registrada. '
                    + 'Quedará DE_BAJA y eliminada del listado habitual; se liberará su espacio '
                    + 'y se conservará el registro histórico. Esta acción no permite restaurarla.'
                : estado === 'DE_BAJA'
                    ? 'Debe estar en una estación/taller interno habilitado. Dar de baja no libera su espacio físico.'
                    : 'Se registrará el cambio y el administrador que lo realiza.',
            input: 'text', inputLabel: 'Motivo', inputAttributes: { maxlength: 1000 },
            showCancelButton: true, cancelButtonText: 'Cancelar',
            confirmButtonText: estado ? 'Confirmar cambio' : 'Confirmar retiro y eliminar',
            inputValidator: valor => !valor?.trim() ? 'Indicá el motivo.' : undefined
        });
        if (!resultado.isConfirmed) return;
        const data = { version: b.version, motivo: resultado.value.trim() };
        if (estado) await axios.patch('/api/admin/bicicletas/' + id + '/estado/' + estado, data);
        else await axios.delete('/api/bicicletas/' + id, {
            data: { ...data, salidaFisicaConfirmada: true }
        });
        await Promise.all([cargarBicicletas(), cargarEstaciones()]);
        Swal.fire('Operación registrada', '', 'success');
    } catch (e) { errorInventario(e); }
}

async function borrarBici(id) { await operarBicicleta(id); }

async function editarTarifaTipo() {
    try {
        const { data: tipos } = await axios.get('/api/admin/tipos-bicicleta');
        const opciones = Object.fromEntries(tipos.map(t => [t.id, t.codigo + ' · USD ' + t.precioPorHoraUsd + '/h']));
        const seleccion = await Swal.fire({
            title: 'Tarifas por tipo', input: 'select', inputOptions: opciones,
            inputPlaceholder: 'Seleccioná un tipo', showCancelButton: true, cancelButtonText: 'Cancelar',
            inputValidator: valor => !valor ? 'Seleccioná un tipo.' : undefined
        });
        if (!seleccion.isConfirmed) return;
        const tipo = tipos.find(t => t.id === Number(seleccion.value));
        const resultado = await Swal.fire({
            titleText: 'Tarifa de ' + tipo.codigo,
            text: 'Se aplica a todas las bicicletas de este tipo en próximos alquileres. No cambia los viajes iniciados.',
            input: 'text', inputValue: String(tipo.precioPorHoraUsd),
            inputLabel: 'USD por hora', showCancelButton: true, cancelButtonText: 'Cancelar',
            inputValidator: valor => {
                const precio = valor.trim().replace(',', '.');
                return !/^\d{1,8}(\.\d{1,2})?$/.test(precio) || Number(precio) <= 0
                    ? 'Ingresá un precio mayor que cero, con hasta 2 decimales.' : undefined;
            }
        });
        if (!resultado.isConfirmed) return;
        await axios.put('/api/admin/tipos-bicicleta/' + tipo.id + '/tarifa', {
            precioPorHoraUsd: resultado.value.trim().replace(',', '.'),
            precioAnterior: tipo.precioPorHoraUsd
        });
        await cargarBicicletas();
        Swal.fire('Tarifa actualizada', 'Los alquileres iniciados conservan su tarifa.', 'success');
    } catch (e) { errorInventario(e); }
}

// ==========================================
// 4. GESTIÓN DE ALQUILERES
// ==========================================
async function cargarAlquileres() {
    const abiertos = typeof OperacionesAlquiler === 'undefined' ? [] : OperacionesAlquiler.abiertos();
    const tbody = document.getElementById('tabla-alquileres-body');
    if (!tbody) return;
    // Solo esta tabla: desplegar unidades no cambia el ancho de las columnas.
    if (typeof tbody.closest === 'function') tbody.closest('table').style.tableLayout = 'fixed';

    try {
        const res = await axios.get(
            '/api/admin/estadisticas/alquileres-todos'
        );

        if (!Array.isArray(res.data)) {
            throw new Error('Respuesta de alquileres inválida.');
        }

        const filas = res.data.map(alquiler => {
            VistaAlquileres.validar(alquiler);
            const id = VistaAlquileres.id(alquiler.id);

            return `
                <tr>
                    <td>${id}</td>
                    <td>ID: ${VistaAlquileres.texto(alquiler.usuarioId)}</td>
                    <td>${VistaAlquileres.bicicletas(alquiler, 'USD', d => typeof OperacionesAlquiler === 'undefined' ? '' : OperacionesAlquiler.accionesDetalle(alquiler, d, true))}</td>
                    <td>
                        <span class="badge bg-secondary">
                            ${VistaAlquileres.texto(alquiler.estado)}
                        </span>
                    </td>
                    <td>${VistaAlquileres.fecha(alquiler.fechaHoraCreacion)}</td>
                    <td>${VistaAlquileres.fecha(alquiler.fechaHoraCierre)}</td>
                    <td>
                        ${VistaAlquileres.importes(alquiler, 'USD')}
                        <hr class="my-1">
                        ${VistaAlquileres.importes(alquiler, 'ARS')}
                    </td>
                    <td>
                        ${typeof OperacionesAlquiler === 'undefined' ? '<span class="text-muted">Solo consulta</span>' : OperacionesAlquiler.acciones(alquiler, true)}
                    </td>
                </tr>`;
        });

        tbody.innerHTML = filas.join('')
            || '<tr><td colspan="8">No hay alquileres visibles.</td></tr>';
        if (typeof OperacionesAlquiler !== 'undefined') OperacionesAlquiler.restaurarAbiertos(abiertos);

    } catch (error) {
        if (manejarErrorSesion(error)) return;

        console.error(error);
        tbody.innerHTML = `
            <tr>
                <td colspan="8">
                    No se pudieron cargar los alquileres.
                    Intentá nuevamente.
                </td>
            </tr>
        `;
    }
}

// UTILIDADES
function getColorEstado(estado) {
    if (estado === 'DISPONIBLE') return 'success';
    if (estado === 'EN_USO' || estado === 'RESERVADA') return 'warning';
    if (estado === 'DE_BAJA' || estado === 'EXTRAVIADA') return 'danger';
    if (estado === 'MANTENIMIENTO') return 'info';
    return 'secondary'; // Mantenimiento/Inactiva
}

async function confirmar(msg = "¿Estás seguro?") {
    const res = await Swal.fire({
        title: msg, icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33'
    });
    return res.isConfirmed;
}
