// Edición explícita de USUARIO_ROL; no comparte formulario con los datos personales.
let usuarioRolesEnEdicion = null;
let cargandoRolesUsuario = false;
let guardandoRolesUsuario = false;
const codigosRolesUsuario = ['ADMIN', 'CLIENTE', 'TECNICO'];

function controlesRolesUsuario(bloquear) {
    for (const id of ['roles-opciones', 'roles-motivo', 'roles-guardar', 'roles-cancelar', 'roles-cerrar']) {
        const elemento = document.getElementById(id);
        if (elemento) elemento.disabled = bloquear;
    }
    const guardar = document.getElementById('roles-guardar');
    if (guardar) guardar.textContent = guardandoRolesUsuario ? 'Guardando…' : 'Guardar roles';
}

function validarVistaRolesUsuario(datos, id) {
    if (!datos || datos.usuarioId !== id || !Number.isSafeInteger(datos.version) || datos.version < 0
            || typeof datos.nickname !== 'string' || !Array.isArray(datos.roles) || !datos.roles.length
            || datos.roles.some(r => !codigosRolesUsuario.includes(r))
            || new Set(datos.roles).size !== datos.roles.length) {
        throw new Error('Los roles recibidos no son válidos. Cerrá el diálogo y volvé a abrirlo.');
    }
    return datos;
}

async function abrirRolesUsuario(id, boton) {
    if (cargandoRolesUsuario || guardandoRolesUsuario) return;
    const usuarioId = Number(id);
    if (!Number.isSafeInteger(usuarioId) || usuarioId <= 0) return;
    cargandoRolesUsuario = true;
    usuarioRolesEnEdicion = null;
    controlesRolesUsuario(true);
    if (boton) { boton.disabled = true; boton.setAttribute('aria-busy', 'true'); }
    try {
        const {data} = await axios.get(`/api/usuarios/${usuarioId}/roles`);
        const vista = validarVistaRolesUsuario(data, usuarioId);
        usuarioRolesEnEdicion = {usuarioId, version: vista.version};
        document.getElementById('roles-titulo').textContent = `Roles de ${vista.nickname}`;
        document.getElementById('roles-motivo').value = '';
        document.getElementById('roles-estado').textContent = 'Se consultaron los roles vigentes del usuario.';
        for (const codigo of codigosRolesUsuario) {
            document.getElementById(`roles-${codigo.toLowerCase()}`).checked = vista.roles.includes(codigo);
        }
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalRolesUsuario')).show();
    } catch (error) {
        if (manejarErrorSesion(error)) return;
        await Swal.fire({title: 'No se pudieron consultar los roles', icon: 'error',
            text: mensajeRolesUsuario(error, 'Verificá la conexión e intentá nuevamente.')});
    } finally {
        cargandoRolesUsuario = false;
        controlesRolesUsuario(false);
        if (boton) { boton.disabled = false; boton.removeAttribute('aria-busy'); }
    }
}

function mensajeRolesUsuario(error, alternativa) {
    const datos = error.response?.data;
    if (typeof datos === 'string' && datos.trim()) return datos;
    if (Array.isArray(datos?.validationErrors) && datos.validationErrors.length) {
        return datos.validationErrors.map(String).join('\n');
    }
    return !error.response && !error.request && !error.isAxiosError && error.message ? error.message : alternativa;
}

async function guardarRolesUsuario() {
    if (guardandoRolesUsuario || cargandoRolesUsuario || !usuarioRolesEnEdicion) return;
    const edicion = {...usuarioRolesEnEdicion};
    const roles = codigosRolesUsuario.filter(c => document.getElementById(`roles-${c.toLowerCase()}`).checked);
    const motivo = document.getElementById('roles-motivo').value.trim();
    if (!roles.length || !motivo || motivo.length > 2000) {
        await Swal.fire({title: 'Revisá los datos', icon: 'warning',
            text: 'Seleccioná al menos un rol e indicá un motivo de hasta 2000 caracteres.'});
        return;
    }
    guardandoRolesUsuario = true;
    controlesRolesUsuario(true);
    document.getElementById('roles-estado').textContent = 'Guardando cambios…';
    let escrituraConfirmada = false;
    try {
        const {data} = await axios.put(`/api/usuarios/${edicion.usuarioId}/roles`, {
            version: edicion.version, roles, motivo
        });
        escrituraConfirmada = true;
        validarVistaRolesUsuario(data, edicion.usuarioId);
        const huboCambios = data.version !== edicion.version;
        usuarioRolesEnEdicion = null;
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalRolesUsuario')).hide();

        // Incluye el caso de quitarse ADMIN: confirmar perfil antes de consultar otra ruta ADMIN.
        const perfil = await obtenerUsuarioActual();
        if (!perfil) { volverAlIngreso(); return; }
        if (!rolesDelUsuario(perfil).includes('ADMIN')) {
            await Swal.fire({title: 'Roles actualizados', icon: 'success',
                text: 'Tu cuenta ya no tiene ADMIN. Tu sesión sigue activa; volverás al inicio.'});
            window.location.replace('index.html');
            return;
        }
        const actualizado = await cargarUsuarios();
        if (actualizado === null) return;
        await Swal.fire({title: huboCambios ? 'Roles actualizados' : 'Sin cambios en los roles', icon: actualizado === false ? 'warning' : 'success',
            text: actualizado === false
                ? 'Los cambios se guardaron, pero no se pudo actualizar el listado. Recargá la página.'
                : huboCambios ? 'Los cambios quedaron guardados y se aplican en las siguientes peticiones protegidas.'
                    : 'La cuenta ya tenía esa composición de roles. No se generó un cambio adicional.'});
    } catch (error) {
        if (manejarErrorSesion(error)) return;
        if (escrituraConfirmada) {
            usuarioRolesEnEdicion = null;
            bootstrap.Modal.getOrCreateInstance(document.getElementById('modalRolesUsuario')).hide();
            await Swal.fire({title: 'Respuesta recibida del servidor', icon: 'warning',
                text: 'El servidor aceptó los cambios, pero no se pudo completar la actualización de la pantalla. Recargá para consultar el estado; no repitas el guardado.'});
        } else {
            document.getElementById('roles-estado').textContent = 'No se pudo confirmar el guardado. Revisá el aviso y recargá los roles antes de reintentar.';
            await Swal.fire({title: 'No se pudieron cambiar los roles', icon: 'error',
                text: mensajeRolesUsuario(error, 'No se pudo confirmar el guardado. Revisá la conexión y recargá los roles antes de reintentar.')});
        }
    } finally {
        guardandoRolesUsuario = false;
        controlesRolesUsuario(false);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('modalRolesUsuario');
    if (!modal) return;
    modal.addEventListener('hide.bs.modal', evento => {
        if (guardandoRolesUsuario && usuarioRolesEnEdicion) evento.preventDefault();
        else usuarioRolesEnEdicion = null;
    });
    modal.addEventListener('shown.bs.modal', () => document.getElementById('roles-motivo').focus());
});
