document.addEventListener("DOMContentLoaded", function() {
    checkSession();
});

window.addEventListener('pageshow', evento => {
    if (evento.persisted) checkSession();
});

let comprobandoSesion = false;
let loginEnCurso = false;
const formularioLoginOriginal = document.getElementById('login-form')?.innerHTML;

// Los botones y el formulario reflejan una comprobación real, no una marca local.
async function checkSession() {
    if (comprobandoSesion || loginEnCurso) return;
    comprobandoSesion = true;
    const botonIngreso = document.querySelector('#login-form button[type="submit"]');
    if (botonIngreso) botonIngreso.disabled = true;

    try {
        const usuario = await obtenerUsuarioActual();
        mostrarSesionConfirmada(usuario);
    } catch (error) {
        // No presentar como activa una sesión que no pudimos comprobar.
        mostrarSesionConfirmada(null);
        mostrarAvisoIngreso('No pudimos comprobar tu sesión. Verificá tu conexión e intentá ingresar nuevamente.', 'alert-warning');
    } finally {
        comprobandoSesion = false;
        const boton = document.querySelector('#login-form button[type="submit"]');
        if (boton) boton.disabled = false;
    }
}

function mostrarAvisoIngreso(mensaje, estilo = 'alert-info') {
    const aviso = document.getElementById('status-message');
    if (!aviso) return;
    aviso.classList.remove('d-none', 'alert-info', 'alert-warning', 'alert-danger', 'alert-success');
    aviso.classList.add(estilo);
    aviso.textContent = mensaje;
}

function mostrarSesionConfirmada(usuario) {
    const isLogueado = Boolean(usuario);
    const roles = usuario ? rolesDelUsuario(usuario) : [];
    const esAdmin = roles.includes('ADMIN');
    const puedeAlquilar = esAdmin || roles.includes('CLIENTE');

    document.querySelectorAll('.nav-visitante, .btn-visitante').forEach(
        el => el.classList.toggle('d-none', isLogueado)
    );
    document.querySelectorAll('.nav-usuario').forEach(
        el => el.classList.toggle('d-none', !isLogueado)
    );
    document.querySelectorAll('.btn-usuario').forEach(
        el => el.classList.toggle('d-none', !puedeAlquilar)
    );
    document.querySelectorAll('.nav-admin').forEach(
        el => el.classList.toggle('d-none', !esAdmin)
    );
    document.querySelector('.nav-usuario a[href="mis_alquileres.html"]')
        ?.closest('.nav-item')?.classList.toggle('d-none', !puedeAlquilar);

    if (isLogueado) {
        // Reemplazar el formulario de login en el footer
        const loginFormContainer = document.getElementById('login-form');
        if(loginFormContainer) {
            loginFormContainer.innerHTML = `
                <div class="text-center py-4">
                    <h4 class="text-success mb-3"><i class="fas fa-check-circle"></i> Sesión Activa</h4>
                    <p class="text-muted">Ya has iniciado sesión.</p>
                    ${puedeAlquilar ? '<a href="mis_alquileres.html" class="btn btn-success w-100 mb-2 rounded-pill">Ir a Mi Cuenta</a>' : ''}
                    ${esAdmin ? '<a href="admin.html" class="btn btn-primary w-100 mb-2 rounded-pill">Ir al Panel Admin</a>' : ''}

                    <button type="button" onclick="logout()" class="btn btn-outline-danger w-100 rounded-pill">Cerrar Sesión</button>

                </div>
            `;
        }
    } else {
        const formulario = document.getElementById('login-form');
        if (formulario && !document.getElementById('login-nickname')) {
            formulario.innerHTML = formularioLoginOriginal;
        }
        const motivo = new URLSearchParams(window.location.search);
        if (motivo.get('sesion') === 'finalizada') {
            mostrarAvisoIngreso('Tu sesión finalizó. Iniciá sesión nuevamente para continuar.');
        } else if (motivo.get('sesion') === 'requerida') {
            mostrarAvisoIngreso('Necesitás iniciar sesión para acceder a esa página.');
        }
    }
    if (new URLSearchParams(window.location.search).get('acceso') === 'denegado') {
        Swal.fire('Acceso no permitido', 'Tu cuenta no tiene permiso para acceder a esa página.', 'warning');
    }
}

// Lógica del Formulario de Login
const loginForm = document.getElementById('login-form');
if (loginForm) {
    loginForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        if (comprobandoSesion || loginEnCurso) return;

        const nicknameInput = document.getElementById('login-nickname');
        const passwordInput = document.getElementById('login-password');

        if (!nicknameInput || !passwordInput) return;

        const nickname = nicknameInput.value;
        const password = passwordInput.value;
        const statusDiv = document.getElementById('status-message');
        const botonIngreso = loginForm.querySelector('button[type="submit"]');
        let accesoConcedido = false;
        loginEnCurso = true;
        if (botonIngreso) botonIngreso.disabled = true;

        const params = new URLSearchParams();
        params.append('nickname', nickname);
        params.append('password', password);

        statusDiv.classList.remove('d-none', 'alert-danger', 'alert-success', 'alert-warning');
        statusDiv.classList.add('alert-info');
        statusDiv.textContent = 'Verificando credenciales...';

        try {
            // --- PASO 1: Intentar hacer Login ---
            const response = await axios.post('/api/login', params.toString(), {
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
            });

            if (response.status === 200) {
                // --- PASO 2: Login exitoso, ahora preguntar "¿Quién soy?" ---
                statusDiv.textContent = '¡Bienvenido! Obteniendo datos...';

                const usuario = await obtenerUsuarioActual();
                if (!usuario) {
                    mostrarAvisoIngreso('No pudimos confirmar tu sesión. Intentá ingresar nuevamente.', 'alert-warning');
                    return;
                }
                const roles = rolesDelUsuario(usuario);
                accesoConcedido = true;

                statusDiv.classList.remove('alert-info');
                statusDiv.classList.add('alert-success');
                statusDiv.innerHTML = '<i class="fas fa-check-circle"></i> ¡Acceso concedido!';

                // Redirigir (el checkSession hará el resto)
                setTimeout(() => {
                    // Si es admin, lo mandamos al panel, si es cliente, a alquilar
                    if (roles.includes('ADMIN')) {
                        window.location.href = '/admin.html';
                    } else if (roles.includes('CLIENTE')) {
                        window.location.href = '/alquiler.html';
                    } else {
                        window.location.href = '/index.html';
                    }
                }, 1000);
            }

        } catch (error) {
            // Error de Login (401)
            statusDiv.classList.remove('alert-info');
            statusDiv.classList.add('alert-danger');

            if (error.response && error.response.status === 401) {
                statusDiv.innerHTML = '<i class="fas fa-exclamation-circle"></i> Nombre de usuario o contraseña incorrectos.';
            } else {
                statusDiv.innerHTML = '<i class="fas fa-wifi"></i> Error de conexión.';
            }
            passwordInput.value = '';
        } finally {
            loginEnCurso = accesoConcedido;
            if (botonIngreso) botonIngreso.disabled = accesoConcedido;
        }
    });
}
