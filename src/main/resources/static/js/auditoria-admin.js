// Vista de solo lectura. El panel y los endpoints comprueban ADMIN en el servidor.
document.addEventListener('DOMContentLoaded', () => {
    const buscar = id => document.getElementById(`auditoria-${id}`);
    const tab = buscar('tab');
    if (!tab) return;

    const ruta = '/api/admin/auditoria';
    const tamano = 20;
    const entidades = new Map(Object.entries({
        USUARIO: 'Usuario', ESTACION: 'Estación', BICICLETA: 'Bicicleta', TIPO_BICICLETA: 'Tipo de bicicleta',
        ORDEN_MANTENIMIENTO: 'Orden de mantenimiento', INCIDENCIA: 'Incidencia',
        ALQUILER_DETALLE: 'Detalle de alquiler'
    }));
    const acciones = new Map(Object.entries({
        CAMBIAR_ROLES: 'Cambiar roles', INACTIVAR: 'Inactivar', ACTUALIZAR: 'Actualizar',
        CREAR: 'Crear', ELIMINAR: 'Eliminar', CAMBIAR_ESTADO: 'Cambiar estado',
        ELIMINAR_SALIDA_DEFINITIVA: 'Retirar del inventario', ACTUALIZAR_TARIFA: 'Actualizar tarifa',
        DERIVAR_MANTENIMIENTO: 'Derivar a mantenimiento', HABILITAR_MANTENIMIENTO: 'Habilitar tras mantenimiento',
        ASIGNAR: 'Asignar', HABILITAR: 'Habilitar', REPORTAR: 'Reportar', RECIBIR: 'Registrar recepción',
        RESOLVER: 'Resolver', ESTADO_POR_INCIDENCIA: 'Cambiar estado por incidencia',
        RECUPERAR_INCIDENCIA: 'Registrar recuperación',
        CANCELAR: 'Cancelar',
        ACTUALIZAR_PREVENTIVO: 'Actualizar mantenimiento preventivo',
        DESASIGNAR_POR_INACTIVACION: 'Desasignar por inactivación',
        UBICAR_RECUPERADA: 'Ubicar bicicleta recuperada',
        BAJA_PERDIDA: 'Dar de baja por pérdida',
        ELIMINAR_PERDIDA: 'Eliminar del inventario por pérdida',
        RECEPCION_DEVOLUCION: 'Registrar recepción por devolución'
    }));
    const campos = new Map(Object.entries({
        roles: 'Roles', activo: 'Estado de la cuenta', email: 'Correo electrónico', nombre: 'Nombre',
        apellido: 'Apellido', fechaNacimiento: 'Fecha de nacimiento', generoId: 'Género',
        capacidad: 'Capacidad', esPublica: 'Uso de la estación', borradoEstacion: 'Estado de la estación',
        direccion: 'Dirección', latitud: 'Latitud', longitud: 'Longitud'
    }));
    const roles = new Map([['ADMIN', 'Administrador'], ['CLIENTE', 'Cliente'], ['TECNICO', 'Técnico']]);

    const entidad = buscar('entidad');
    const accion = buscar('accion');
    const administrador = buscar('administrador');
    const estadoAdministradores = buscar('administradores-estado');
    const estado = buscar('estado');
    const resultados = buscar('resultados');
    const tabla = buscar('tabla');
    const eventos = buscar('eventos');
    const paginacion = buscar('paginacion');
    const anterior = buscar('anterior');
    const siguiente = buscar('siguiente');
    const detalle = buscar('detalle');
    const tituloDetalle = buscar('detalle-titulo');
    const estadoDetalle = buscar('detalle-estado');
    const eventoDetalle = buscar('detalle-evento');
    const motivoDetalle = buscar('detalle-motivo');
    const tablaCambios = buscar('cambios-tabla');
    const cambios = buscar('cambios');
    let filtros = {};
    let pagina = 0;
    let solicitudLista = 0;
    let solicitudDetalle = 0;
    let botonDetalle = null;
    let administradoresCargados = false;
    let cargaAdministradores = null;

    function elemento(etiqueta, texto) {
        const nodo = document.createElement(etiqueta);
        nodo.textContent = texto;
        return nodo;
    }

    function cargarSugerencias(lista, etiquetas) {
        for (const [codigo, etiqueta] of etiquetas) {
            const opcion = elemento('option', etiqueta);
            opcion.value = codigo;
            lista.append(opcion);
        }
    }

    function enteroDesde(valor, minimo) {
        return Number.isSafeInteger(valor) && valor >= minimo;
    }

    function eventoValido(evento) {
        return evento && enteroDesde(evento.id, 1) && enteroDesde(evento.administradorId, 1)
            && enteroDesde(evento.entidadId, 1) && typeof evento.administradorNickname === 'string'
            && typeof evento.entidadAfectada === 'string' && typeof evento.accion === 'string'
            && typeof evento.fechaRegistrada === 'string';
    }

    function fechaRegistrada(fecha) {
        // ISO local sin offset: mostrar hasta los segundos, sin pasar por Date ni redondear.
        const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?)$/.exec(fecha);
        return partes ? `${partes[3]}/${partes[2]}/${partes[1]}, ${partes[4].split('.')[0]}` : 'Fecha no disponible';
    }

    function actor(evento) {
        return `${evento.administradorNickname} (#${evento.administradorId})`;
    }

    function cargarAdministradores() {
        if (administradoresCargados) return;
        if (cargaAdministradores) return cargaAdministradores;

        estadoAdministradores.textContent = 'Cargando administradores…';
        cargaAdministradores = axios.get(`${ruta}/administradores`, {timeout: 10000}).then(({data}) => {
            if (!Array.isArray(data) || !data.every(a => a && enteroDesde(a.administradorId, 1)
                    && typeof a.administradorNickname === 'string')) throw new Error();
            for (const a of data) {
                const opcion = elemento('option', actor(a));
                opcion.value = String(a.administradorId);
                administrador.append(opcion);
            }
            administradoresCargados = true;
            administrador.disabled = !data.length;
            estadoAdministradores.textContent = data.length ? '' : 'Todavía no hay administradores con eventos registrados.';
        }).catch(error => {
            estadoAdministradores.textContent = manejarErrorSesion(error) ? ''
                : 'No se pudo cargar el filtro por administrador. Volvé a abrir Auditoría para reintentar.';
        }).finally(() => { cargaAdministradores = null; });
        return cargaAdministradores;
    }

    function objetivo(evento) {
        const tipoEId = `${entidades.get(evento.entidadAfectada) ?? evento.entidadAfectada} #${evento.entidadId}`;
        const referencia = typeof evento.referenciaEntidad === 'string' ? evento.referenciaEntidad.trim() : '';
        return referencia ? `${referencia} · ${tipoEId}` : tipoEId;
    }

    function accionDe(evento) {
        return acciones.get(evento.accion) ?? evento.accion;
    }

    function limpiarListado() {
        eventos.replaceChildren();
        tabla.hidden = true;
        paginacion.hidden = true;
        anterior.disabled = true;
        siguiente.disabled = true;
    }

    function dibujarEvento(evento) {
        const fila = document.createElement('tr');
        const datos = [`#${evento.id}`, actor(evento), objetivo(evento), accionDe(evento),
            fechaRegistrada(evento.fechaRegistrada)];
        for (const dato of datos) {
            const celda = elemento('td', dato);
            celda.className = 'text-break';
            fila.append(celda);
        }
        const celdaDetalle = document.createElement('td');
        const boton = elemento('button', 'Ver detalle');
        boton.type = 'button';
        boton.className = 'btn btn-sm btn-outline-success';
        boton.setAttribute('aria-label', `Ver detalle del evento #${evento.id}`);
        boton.setAttribute('aria-controls', 'auditoria-detalle');
        boton.setAttribute('aria-expanded', 'false');
        boton.addEventListener('click', () => botonDetalle === boton
            ? tituloDetalle.focus() : cargarDetalle(evento.id, boton));
        celdaDetalle.append(boton);
        fila.append(celdaDetalle);
        eventos.append(fila);
    }

    async function cargarListado(numeroPagina) {
        const actual = ++solicitudLista;
        pagina = numeroPagina;
        cerrarDetalle();
        limpiarListado();
        estado.textContent = 'Cargando eventos de auditoría…';
        resultados.setAttribute('aria-busy', 'true');
        try {
            const {data} = await axios.get(ruta, {params: {...filtros, pagina: numeroPagina, tamano}, timeout: 10000});
            if (actual !== solicitudLista) return;
            if (!data || data.pagina !== numeroPagina || !enteroDesde(data.totalPaginas, 0)
                    || !enteroDesde(data.totalEventos, 0) || !Array.isArray(data.eventos)
                    || !data.eventos.every(eventoValido)) throw new Error();

            if (!data.eventos.length) {
                estado.textContent = 'No hay eventos de auditoría para estos filtros.';
            } else {
                data.eventos.forEach(dibujarEvento);
                tabla.hidden = false;
                const cantidad = data.totalEventos;
                estado.textContent = `${cantidad} ${cantidad === 1 ? 'evento encontrado' : 'eventos encontrados'}.`;
            }
            if (data.totalPaginas > 0) {
                buscar('pagina').textContent = `Página ${pagina + 1} de ${data.totalPaginas}`;
                paginacion.hidden = false;
                anterior.disabled = pagina === 0;
                siguiente.disabled = pagina + 1 >= data.totalPaginas;
            }
        } catch (error) {
            if (actual !== solicitudLista) return;
            estado.textContent = manejarErrorSesion(error)
                ? 'No se puede consultar la auditoría con la sesión actual.'
                : 'No se pudieron cargar los eventos. Volvé a aplicar los filtros para reintentar.';
        } finally {
            if (actual === solicitudLista) resultados.setAttribute('aria-busy', 'false');
        }
    }

    function cerrarDetalle(devolverFoco = false) {
        solicitudDetalle++;
        if (botonDetalle) {
            botonDetalle.setAttribute('aria-expanded', 'false');
            if (devolverFoco) botonDetalle.focus();
            botonDetalle = null;
        }
        detalle.hidden = true;
        estadoDetalle.textContent = '';
        eventoDetalle.hidden = true;
        eventoDetalle.textContent = '';
        motivoDetalle.hidden = true;
        motivoDetalle.textContent = '';
        tablaCambios.hidden = true;
        cambios.replaceChildren();
    }

    function valorDe(campo, valor) {
        if (valor == null) return '—';
        if (campo === 'roles' && Array.isArray(valor) && valor.every(rol => roles.has(rol))) {
            return valor.map(rol => roles.get(rol)).join(', ') || 'Sin roles';
        }
        if (campo === 'capacidad' && enteroDesde(valor, 0)) return String(valor);
        if (typeof valor === 'boolean') {
            if (campo === 'activo') return valor ? 'Activa' : 'Inactiva';
            if (campo === 'esPublica') return valor ? 'Pública' : 'Uso interno';
            if (campo === 'borradoEstacion') return valor ? 'Eliminada' : 'Activa';
        }
        return 'No disponible';
    }

    function dibujarCambio(cambio) {
        const fila = document.createElement('tr');
        const encabezado = elemento('th', campos.get(cambio.campo) ?? 'Campo no reconocido');
        encabezado.scope = 'row';
        fila.append(encabezado);
        if (cambio.valoresReservados) {
            const celda = elemento('td', 'Campo modificado · valores reservados');
            celda.colSpan = 2;
            fila.append(celda);
        } else {
            fila.append(elemento('td', valorDe(cambio.campo, cambio.antes)),
                elemento('td', valorDe(cambio.campo, cambio.despues)));
        }
        cambios.append(fila);
    }

    async function cargarDetalle(id, boton) {
        cerrarDetalle();
        const actual = solicitudDetalle;
        botonDetalle = boton;
        boton.setAttribute('aria-expanded', 'true');
        tituloDetalle.textContent = `Detalle del evento #${id}`;
        estadoDetalle.textContent = 'Cargando el detalle…';
        detalle.hidden = false;
        tituloDetalle.focus();
        try {
            const {data} = await axios.get(`${ruta}/${id}`, {timeout: 10000});
            if (actual !== solicitudDetalle) return;
            if (!data || !eventoValido(data.evento) || data.evento.id !== id
                    || typeof data.motivoRegistrado !== 'boolean' || typeof data.detalleDisponible !== 'boolean'
                    || (data.detalleDisponible && (!Array.isArray(data.cambios) || !data.cambios.length
                        || !data.cambios.every(c => c && typeof c.campo === 'string'
                            && typeof c.valoresReservados === 'boolean')))) throw new Error();

            const evento = data.evento;
            eventoDetalle.textContent = `${actor(evento)} · ${objetivo(evento)} · ${accionDe(evento)} · ${fechaRegistrada(evento.fechaRegistrada)}`;
            eventoDetalle.hidden = false;
            motivoDetalle.textContent = data.motivoRegistrado ? 'Motivo registrado (texto reservado).' : 'Sin motivo registrado.';
            motivoDetalle.hidden = false;
            if (data.detalleDisponible) {
                data.cambios.forEach(dibujarCambio);
                tablaCambios.hidden = false;
                estadoDetalle.textContent = 'Cambios registrados.';
            } else {
                estadoDetalle.textContent = 'Detalle de cambios no disponible para este evento.';
            }
        } catch (error) {
            if (actual !== solicitudDetalle) return;
            if (manejarErrorSesion(error)) {
                solicitudLista++;
                cerrarDetalle();
                limpiarListado();
                resultados.setAttribute('aria-busy', 'false');
                estado.textContent = 'No se puede consultar la auditoría con la sesión actual.';
                return;
            }
            estadoDetalle.textContent = error.response?.status === 404
                ? 'El evento ya no está disponible. Volvé a aplicar los filtros para actualizar el listado.'
                : 'No se pudo cargar el detalle. Cerralo y volvé a abrirlo para reintentar.';
        }
    }

    function aplicarFiltros(evento) {
        evento.preventDefault();
        const normalizar = valor => valor.trim().toUpperCase().replace(/\s+/g, '_');
        const tipo = normalizar(entidad.value);
        const operacion = normalizar(accion.value);
        const administradorId = administrador.value === '' ? null : Number(administrador.value);
        const esCodigo = (valor, maximo) => !valor || (valor.length <= maximo && /^[A-Z][A-Z0-9_]*$/.test(valor));
        if (!esCodigo(tipo, 80) || !esCodigo(operacion, 50)) {
            solicitudLista++;
            cerrarDetalle();
            limpiarListado();
            resultados.setAttribute('aria-busy', 'false');
            estado.textContent = 'Revisá los filtros: usá letras, números y guion bajo (ejemplo: CAMBIAR_ROLES).';
            (!esCodigo(tipo, 80) ? entidad : accion).focus();
            return;
        }
        if (administradorId !== null && !enteroDesde(administradorId, 1)) {
            estado.textContent = 'Seleccioná un administrador válido.';
            administrador.focus();
            return;
        }
        filtros = {};
        if (tipo) filtros.entidad = tipo;
        if (operacion) filtros.accion = operacion;
        if (administradorId !== null) filtros.administradorId = administradorId;
        return cargarListado(0);
    }

    cargarSugerencias(buscar('entidades'), entidades);
    cargarSugerencias(buscar('acciones'), acciones);
    tab.addEventListener('shown.bs.tab', () => Promise.all([cargarListado(pagina), cargarAdministradores()]));
    tab.addEventListener('hide.bs.tab', () => {
        solicitudLista++;
        cerrarDetalle();
        limpiarListado();
        resultados.setAttribute('aria-busy', 'false');
    });
    buscar('filtros').addEventListener('submit', aplicarFiltros);
    buscar('limpiar').addEventListener('click', () => {
        entidad.value = '';
        accion.value = '';
        administrador.value = '';
        filtros = {};
        return cargarListado(0);
    });
    anterior.addEventListener('click', () => { if (!anterior.disabled) return cargarListado(pagina - 1); });
    siguiente.addEventListener('click', () => { if (!siguiente.disabled) return cargarListado(pagina + 1); });
    buscar('detalle-cerrar').addEventListener('click', () => cerrarDetalle(true));
});
