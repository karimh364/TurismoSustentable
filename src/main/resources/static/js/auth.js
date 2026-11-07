// Estos datos sirven para la presentación; la autenticación depende del servidor.
let consultaSesionPendiente = null;
let redireccionSesionPendiente = false;
let avisoPermisosPendiente = false;

function limpiarDatosSesion() {
    ['usuarioLogueado', 'usuarioRol', 'usuarioNombre'].forEach(
        clave => sessionStorage.removeItem(clave)
    );
}

function rolesDelUsuario(usuario) {
    return Array.isArray(usuario.roles)
        ? usuario.roles
        : (usuario.rol ? [usuario.rol] : []);
}

function guardarDatosSesion(usuario) {
    const roles = rolesDelUsuario(usuario);
    const rolPrincipal = ['ADMIN', 'CLIENTE', 'TECNICO'].find(
        rol => roles.includes(rol)
    );

    sessionStorage.setItem('usuarioLogueado', 'true');
    sessionStorage.setItem('usuarioNombre', usuario.nombre);
    if (rolPrincipal) {
        sessionStorage.setItem('usuarioRol', rolPrincipal);
    } else {
        sessionStorage.removeItem('usuarioRol');
    }
}

// Un 401 significa que no hay sesión; otros errores se propagan sin borrarla.
async function obtenerUsuarioActual() {
    if (!consultaSesionPendiente) {
        consultaSesionPendiente = axios.get('/api/usuarios/actual', {
            timeout: 10000
        }).then(respuesta => {
            guardarDatosSesion(respuesta.data);
            return respuesta.data;
        }).catch(error => {
            if (error.response?.status === 401) {
                limpiarDatosSesion();
                return null;
            }
            throw error;
        }).finally(() => {
            consultaSesionPendiente = null;
        });
    }
    return consultaSesionPendiente;
}

function volverAlIngreso() {
    limpiarDatosSesion();
    if (!redireccionSesionPendiente) {
        redireccionSesionPendiente = true;
        window.location.replace('index.html?sesion=finalizada#login-anchor');
    }
}

// Solo para peticiones protegidas: no usar en login ni al verificar el logout.
function manejarErrorSesion(error) {
    if (error.response?.status === 401) {
        volverAlIngreso();
        return true;
    }
    if (error.response?.status === 403) {
        if (!avisoPermisosPendiente) {
            avisoPermisosPendiente = true;
            Swal.fire({
                icon: 'warning',
                title: 'Acceso no permitido',
                text: 'Tu cuenta no tiene permiso para realizar esta acción.',
                confirmButtonColor: '#198754'
            }).finally(() => { avisoPermisosPendiente = false; });
        }
        return true;
    }
    return false;
}

async function requerirSesion(rolesPermitidos) {
    try {
        const usuario = await obtenerUsuarioActual();
        if (!usuario) {
            volverAlIngreso();
            return null;
        }
        if (!rolesDelUsuario(usuario).some(rol => rolesPermitidos.includes(rol))) {
            await Swal.fire({
                icon: 'warning',
                title: 'Acceso no permitido',
                text: 'Tu cuenta no tiene permiso para acceder a esta página.',
                confirmButtonColor: '#198754'
            });
            window.location.replace('index.html');
            return null;
        }
        return usuario;
    } catch (error) {
        if (!manejarErrorSesion(error)) {
            await Swal.fire({
                icon: 'error',
                title: 'No se pudo comprobar la sesión',
                text: 'Verificá tu conexión y recargá la página para continuar.',
                confirmButtonColor: '#198754'
            });
        }
        return null;
    }
}

// Una página restaurada con Atrás también debe comprobar la sesión real.
window.addEventListener('pageshow', evento => {
    if (!evento.persisted) return;
    const pagina = window.location.pathname.split('/').pop();
    const permisos = {
        'admin.html': ['ADMIN'],
        'alquiler.html': ['ADMIN', 'CLIENTE'],
        'mis_alquileres.html': ['ADMIN', 'CLIENTE']
    };
    if (permisos[pagina]) requerirSesion(permisos[pagina]);
});

// Cierre de la sesión del servidor, con confirmación antes de informar éxito.
async function logout() {

    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 1500,
        timerProgressBar: true
    });

    try {
        /*
         * Cerrar la sesión real del servidor.
         * En estas peticiones del mismo origen el navegador envía JSESSIONID.
         */
        await axios.post(
            '/api/logout',
            null,
            {
                withCredentials: true
            }
        );

        /*
         * Verificar que la sesión realmente haya dejado
         * de ser válida antes de informar éxito.
         */
        try {
            await axios.get(
                '/api/usuarios/actual',
                {
                    withCredentials: true
                }
            );

            /*
             * Si esta petición devuelve 200, la sesión
             * continúa activa y el logout no funcionó.
             */
            throw new Error(
                'La sesión continúa activa después del logout.'
            );

        } catch (verificacionError) {

            const estado =
                verificacionError.response?.status;

            /*
             * Solo 401 confirma falta de autenticación.
             * Un 403 puede corresponder a una sesión sin permisos.
             */
            if (estado !== 401) {
                throw verificacionError;
            }
        }

        /*
         * Limpiar los indicadores de sesión solamente
         * después de confirmar el cierre del backend.
         */
        limpiarDatosSesion();

        await Toast.fire({
            icon: 'success',
            title: '¡Sesión cerrada con éxito!'
        });

        /*
         * No agregar la página de salida al historial. Las páginas privadas
         * igualmente deben volver a comprobar la autenticación.
         */
        window.location.replace(
            'index.html'
        );

    } catch (error) {

        console.error(
            'No se pudo cerrar la sesión correctamente:',
            error
        );

        await Swal.fire({
            icon: 'error',
            title: 'No se pudo cerrar la sesión',
            text: 'No pudimos confirmar el cierre. Verificá tu conexión e intentá nuevamente.',
            confirmButtonColor: '#198754'
        });
    }
}

// --- FUNCIÓN PARA CARGAR EL NOMBRE DEL USUARIO ---
async function cargarNombreUsuario(usuarioConfirmado) {
    const elementoNombre =
        document.getElementById('navbar-username');

    if (!elementoNombre) {
        return;
    }


    // Mostrar inmediatamente el nombre guardado.
    const nombreGuardado =
        sessionStorage.getItem('usuarioNombre');

    if (nombreGuardado) {
        elementoNombre.textContent = nombreGuardado;
    }

    try {
        /*
         * Reutilizar la comprobación de entrada de la página, o consultar
         * al servidor si no se recibió un usuario ya confirmado.
         */
        const usuario = usuarioConfirmado || await obtenerUsuarioActual();
        if (!usuario) {
            volverAlIngreso();
            return;
        }
        const nombreActual = usuario.nombre;

        elementoNombre.textContent = nombreActual;

        sessionStorage.setItem(
            'usuarioNombre',
            nombreActual
        );

    } catch (error) {
        console.error(
            'Error cargando el nombre del usuario',
            error
        );

        manejarErrorSesion(error);
    }
}
