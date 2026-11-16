// Solo presentación: el servidor revalida propiedad, roles, versiones y hechos físicos.
const Incidencias = (() => {
    let perfil=null,lista=[],ocupado=false;
    const t=v=>String(v??'Sin registrar').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const id=v=>{const n=Number(v);if(!Number.isSafeInteger(n)||n<=0)throw new Error('Identificador inválido.');return n;};
    const admin=()=>perfil&&rolesDelUsuario(perfil).includes('ADMIN');
    const nombres={AVERIA:'Avería',ACCIDENTE:'Accidente',PERDIDA:'Pérdida',ROBO:'Robo',PENDIENTE:'Pendiente de revisión',VALIDADA:'Validada',DESCARTADA:'Solicitud descartada',
        EN_CURSO:'En curso',DEVUELTO:'Devuelto',CERRADO_POR_INCIDENCIA:'Cerrado por incidencia',EXTRAVIADA:'Extraviada',MANTENIMIENTO:'En mantenimiento',DE_BAJA:'De baja',
        REPORTAR:'Reporte recibido',RESOLVER:'Revisión administrativa',RECIBIR:'Recepción/recuperación física',RECEPCION_DEVOLUCION:'Recepción por devolución',UBICAR_RECUPERADA:'Ingreso posterior a una estación',BAJA_PERDIDA:'Baja por pérdida definitiva',ELIMINAR_PERDIDA:'Eliminación lógica por pérdida'};
    const nombre=v=>nombres[v]??v??'Sin registrar';
    const fecha=v=>v?new Date(/(Z|[+-]\d\d:\d\d)$/i.test(v)?v:v+'Z').toLocaleString('es-AR',{hour12:false}):'Sin registrar';
    const local=v=>{const d=v?new Date(/(Z|[+-]\d\d:\d\d)$/i.test(v)?v:v+'Z'):new Date();return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,19);};
    function instante(v){const d=new Date(v);if(!v||Number.isNaN(d.getTime()))throw new Error('Ingresá una fecha y hora válida.');return d.toISOString();}
    function dialogo(o){return Swal.fire({confirmButtonText:'Confirmar',cancelButtonText:'Volver',confirmButtonColor:'#198754',cancelButtonColor:'#6c757d',
        customClass:{popup:'maintenance-dialog',confirmButton:'rounded-pill',cancelButton:'rounded-pill'},...o});}
    function estado(s,b=false){document.getElementById('inc-estado').textContent=s;document.getElementById('inc-lista').setAttribute('aria-busy',String(b));}
    function controles(){document.querySelectorAll('[data-inc-busy]').forEach(b=>b.disabled=ocupado);}
    async function error(e){if(manejarErrorSesion(e))return;const d=e.response?.data;await dialogo({title:'No se pudo completar',text:typeof d==='string'?d:Array.isArray(d?.validationErrors)?d.validationErrors.map(v=>String(v).replace(/^[^:]+:\s*/, '')).join(' '):e.message||'Intentá nuevamente.',icon:'error',confirmButtonText:'Entendido'});}
    async function ejecutar(f){if(ocupado)return;const origen=document.activeElement;ocupado=true;controles();
        try{await f();}catch(e){estado('La operación no pudo completarse. Revisá el aviso.');await error(e);}finally{ocupado=false;controles();
            if(document.getElementById('inc-lista').getAttribute('aria-busy')==='true')estado('Sin operaciones en curso.');
            if(origen?.isConnected&&!origen.disabled)origen.focus({preventScroll:true});else document.getElementById('inc-actualizar')?.focus({preventScroll:true});}}
    async function obtener(){estado('Actualizando incidencias…',true);try{const {data}=await axios.get('/api/incidencias');if(!Array.isArray(data))throw new Error('Respuesta de incidencias inválida.');lista=data;renderizar();estado('Incidencias actualizadas.');return true;}finally{document.getElementById('inc-lista').setAttribute('aria-busy','false');}}
    async function cargar(){return ejecutar(obtener);}
    function enlace(i){return `<button type="button" class="btn btn-outline-secondary" data-inc-busy onclick="Incidencias.verAlquiler(${id(i.id)})">Consultar liquidación</button>`;}
    function acciones(i){let h=`<button type="button" class="btn btn-outline-secondary" data-inc-busy onclick="Incidencias.historial(${id(i.id)})">Historial</button>`;
        if(admin()&&!i.borradoBici){
            if(i.estadoRevision==='PENDIENTE')h+=`<button type="button" class="btn btn-success" data-inc-busy onclick="Incidencias.resolver(${id(i.id)})">Revisar solicitud</button>`;
            if(i.estadoDetalle==='CERRADO_POR_INCIDENCIA'&&!i.fechaHoraRecepcionFisica&&['EXTRAVIADA','MANTENIMIENTO'].includes(i.estadoBicicleta))h+=`<button type="button" class="btn btn-outline-success" data-inc-busy onclick="Incidencias.recibir(${id(i.id)})">Recibir / recuperar</button>`;
            if(i.fechaHoraRecepcionFisica&&i.lugarRecepcion&&!i.estacionActualId&&i.estadoBicicleta==='MANTENIMIENTO')h+=`<button type="button" class="btn btn-outline-success" data-inc-busy onclick="Incidencias.recibir(${id(i.id)},true)">Registrar ingreso a estación</button>`;
            if(i.esCorteAplicado&&i.estadoRevision==='VALIDADA'&&['PERDIDA','ROBO'].includes(i.tipo)&&!i.fechaHoraRecepcionFisica){
                if(i.estadoBicicleta==='EXTRAVIADA')h+=`<button type="button" class="btn btn-outline-danger" data-inc-busy onclick="Incidencias.perdida(${id(i.id)},false)">Dar de baja por pérdida definitiva</button>`;
                if(i.estadoBicicleta==='DE_BAJA'&&i.perdidaDefinitiva)h+=`<button type="button" class="btn btn-outline-danger" data-inc-busy onclick="Incidencias.perdida(${id(i.id)},true)">Eliminar del inventario por pérdida</button>`;
            }
            if(i.ordenMantenimientoId)h+='<a class="btn btn-outline-success" href="mantenimiento.html">Ver mantenimiento</a>';
        }return h;}
    function renderizar(){const filtro=document.getElementById('inc-filtro').value;const bici=new URLSearchParams(window.location.search).get('bicicletaId');
        const visibles=lista.filter(i=>(filtro==='TODAS'||i.estadoRevision===filtro)&&(!bici||i.bicicletaId===id(bici)));
        document.getElementById('inc-ver-todas').hidden=!bici;
        document.getElementById('inc-lista').innerHTML=visibles.map(i=>`<div class="col-12 col-xl-6"><article class="card h-100 border-0 shadow-sm" data-incidencia="${id(i.id)}"><div class="card-body">
            <h2 class="h5">Incidencia #${id(i.id)} · ${t(i.codigoBicicleta)}</h2><p><span class="badge ${i.estadoRevision==='PENDIENTE'?'bg-warning text-dark':'bg-secondary'}">${t(nombre(i.estadoRevision))}</span> · ${t(nombre(i.tipo))}</p>
            <p>Alquiler #${id(i.alquilerId)}, detalle #${id(i.detalleId)}. ${t(nombre(i.estadoDetalle))}. Bicicleta: ${t(nombre(i.estadoBicicleta))}${i.borradoBici?' · Eliminada lógicamente':''}.</p>
            <p class="maintenance-work-text">${t(i.descripcion)}</p><p>Lugar informado: ${t(i.lugarReportado)}</p>
            <p>Hecho informado: ${t(fecha(i.fechaHoraHechoInformada))}<br>Reporte recibido: ${t(fecha(i.fechaHoraRegistro))}</p>
            ${i.estadoRevision==='PENDIENTE'?'<p class="alert alert-warning">El corte aún no está autorizado. Si ya devolviste, no se cuenta tiempo posterior a la recepción; el importe sigue pendiente de revisión.</p>':`<p>Decisión: ${t(fecha(i.fechaHoraDecision))}<br>Corte validado: ${t(fecha(i.fechaHoraCorteValidado))}</p><p class="maintenance-work-text">Motivo: ${t(i.motivoDecision)}</p>`}
            <p>Recepción física: ${t(fecha(i.fechaHoraRecepcionFisica))}<br>Ubicación de recepción: ${t(i.estacionRecepcion??i.lugarRecepcion)}</p>
            ${admin()?`<p>Estación actual: ${t(i.ubicacionActual)}</p>`:''}
            <p class="small text-muted">Recuperar no reabre el uso ni habilita la bicicleta. Las fechas se muestran en la hora local del navegador.</p>
            <div class="d-flex flex-wrap gap-2">${acciones(i)}${enlace(i)}</div></div></article></div>`).join('')||'<div class="col-12"><p class="alert alert-light">No hay incidencias para esta consulta.</p></div>';
        controles();}
    async function guardar(url,datos){estado('Guardando la operación…',true);await axios.post(url,datos);let actualizado=true;
        try{await obtener();}catch(e){actualizado=false;if(manejarErrorSesion(e))return;}
        await dialogo({title:'Operación registrada',text:actualizado?'Los datos se actualizaron desde el servidor.':'Se guardó la operación, pero no se pudo actualizar la lista. Usá Actualizar incidencias antes de continuar.',icon:actualizado?'success':'warning',confirmButtonText:'Entendido'});}
    async function nuevo(alquilerId,detalleId){return ejecutar(async()=>{
        estado('Buscando bicicletas en uso…',true);
        const {data}=await axios.get(alquilerId?'/api/alquileres/'+id(alquilerId):admin()?'/api/admin/estadisticas/alquileres-todos':'/api/alquileres');
        const alquileres=alquilerId?[data]:data;if(!Array.isArray(alquileres))throw new Error('Respuesta de alquileres inválida.');
        const opciones={},pares=new Map();alquileres.forEach(a=>(a.detalles??[]).filter(d=>d.estado==='EN_CURSO'&&(!detalleId||d.id===id(detalleId))).forEach(d=>{
            const clave=id(a.id)+':'+id(d.id);opciones[clave]=`Alquiler #${a.id} · ${d.codigoBicicleta}`;pares.set(clave,{a,d});}));
        if(!pares.size)throw new Error('No hay bicicletas en uso para reportar. Una falla que permite devolver puede informarse al registrar la devolución.');
        let par;if(pares.size===1)par=[...pares.values()][0];else{const elegir=await dialogo({title:'Bicicleta afectada',input:'select',inputOptions:opciones,inputPlaceholder:'Elegí una bicicleta en uso',showCancelButton:true,confirmButtonText:'Continuar',inputValidator:v=>!v?'Seleccioná la bicicleta.':undefined});if(!elegir.isConfirmed)return;par=pares.get(elegir.value);}
        estado('Completá el reporte. El corte requiere revisión administrativa.');const {a,d}=par;
        const r=await dialogo({titleText:'Reportar '+d.codigoBicicleta,showCancelButton:true,confirmButtonText:'Enviar reporte',html:`
            <p>Indicá una interrupción que impide la devolución normal. No se aplica una multa ni se detiene automáticamente el cobro.</p>
            <label for="inc-tipo">Tipo</label><select id="inc-tipo" class="form-select"><option value="AVERIA">Avería</option><option value="ACCIDENTE">Accidente</option><option value="PERDIDA">Pérdida</option><option value="ROBO">Robo</option></select>
            <label for="inc-descripcion">Descripción (obligatoria)</label><textarea id="inc-descripcion" class="swal2-textarea" maxlength="10000"></textarea>
            <label for="inc-hecho">Fecha y hora local del hecho (obligatoria)</label><input id="inc-hecho" class="swal2-input" type="datetime-local" step="1" value="${local()}">
            <label for="inc-lugar">Dónde quedó o se vio por última vez (si se conoce)</label><textarea id="inc-lugar" class="swal2-textarea" maxlength="2000"></textarea>
            ${admin()?`<label for="inc-informante">ID del informante identificado (opcional)</label><input id="inc-informante" class="swal2-input" type="number" min="1" value="${id(a.usuarioId)}">`:''}`,
            preConfirm:()=>{try{const descripcion=document.getElementById('inc-descripcion').value.trim();if(!descripcion)throw new Error('Completá la descripción.');
                const datos={alquilerId:id(a.id),detalleId:id(d.id),versionDetalle:d.version,tipo:document.getElementById('inc-tipo').value,descripcion,
                    lugarReportado:document.getElementById('inc-lugar').value.trim()||null,fechaHoraHechoInformada:instante(document.getElementById('inc-hecho').value)};
                if(admin()){const v=document.getElementById('inc-informante').value;datos.informanteId=v?id(v):null;}return datos;
            }catch(e){Swal.showValidationMessage(e.message);return false;}}});
        if(r.isConfirmed)await guardar('/api/incidencias',r.value);
    });}
    function actual(identificador){const i=lista.find(i=>i.id===id(identificador));if(!i)throw new Error('Actualizá la incidencia antes de operar.');return i;}
    function cambiarDecision(){document.getElementById('inc-corte-grupo').hidden=document.getElementById('inc-decision').value!=='VALIDADA';}
    async function resolver(identificador){return ejecutar(async()=>{const i=actual(identificador);const r=await dialogo({title:'Revisar incidencia #'+id(i.id),showCancelButton:true,confirmButtonText:'Guardar decisión',html:`
        <p>Validá el hecho y la interrupción. No confundas el corte de uso con el momento de esta revisión. Si ya volvió, se conserva su devolución real.</p>
        <label for="inc-decision">Decisión</label><select id="inc-decision" class="form-select" onchange="Incidencias.cambiarDecision()"><option value="">Seleccioná</option><option value="VALIDADA">Validar interrupción y corte</option><option value="DESCARTADA">Descartar solicitud de corte</option></select>
        <div id="inc-corte-grupo" hidden><label for="inc-corte">Corte validado (hora local)</label><input id="inc-corte" class="swal2-input" type="datetime-local" step="1" value="${local(i.fechaHoraHechoInformada)}"></div>
        <label for="inc-motivo">Fundamento obligatorio de la revisión</label><textarea id="inc-motivo" class="swal2-textarea" maxlength="2000"></textarea>`,
        preConfirm:()=>{try{const decision=document.getElementById('inc-decision').value,motivo=document.getElementById('inc-motivo').value.trim();if(!decision||!motivo)throw new Error('Seleccioná la decisión y completá su fundamento.');return {version:i.version,versionBicicleta:i.versionBicicleta,decision,motivo,fechaHoraCorteValidado:decision==='VALIDADA'?instante(document.getElementById('inc-corte').value):null};}catch(e){Swal.showValidationMessage(e.message);return false;}}});
        if(r.isConfirmed)await guardar('/api/incidencias/'+id(i.id)+'/resolver',r.value);
    });}
    async function recibir(identificador,ubicar=false){return ejecutar(async()=>{const i=actual(identificador);estado('Consultando ubicaciones…',true);const {data}=await axios.get('/api/admin/estaciones');
        const opciones=data.filter(e=>e.espaciosLibres>0).map(e=>`<option value="${id(e.id)}">${t(e.nombre)} · ${e.espaciosLibres} espacio(s) · ${e.esPublica?'Pública':'Uso interno'}</option>`).join('');
        const r=await dialogo({titleText:(ubicar?'Ingreso a estación de ':'Recepción real de ')+i.codigoBicicleta,showCancelButton:true,confirmButtonText:ubicar?'Registrar ingreso':'Registrar recepción',html:`<p>No reabre el alquiler. La bicicleta queda en mantenimiento, no disponible. ${ubicar?'Se conserva la recepción original fuera de estación.':''}</p>
            <label for="inc-estacion">Ubicación de ingreso</label><select id="inc-estacion" class="form-select"><option value="">${ubicar?'Elegí la estación real':'Fuera de una estación (indicar lugar real)'}</option>${opciones}</select>
            <div ${ubicar?'hidden':''}><label for="inc-recepcion-lugar">Lugar real, solo si no ingresa a una estación</label><textarea id="inc-recepcion-lugar" class="swal2-textarea" maxlength="2000"></textarea></div>
            <label for="inc-motivo">Motivo / contexto (obligatorio)</label><textarea id="inc-motivo" class="swal2-textarea" maxlength="2000"></textarea>
            <label><input type="checkbox" id="inc-confirmar"> Confirmo que recibimos físicamente esta bicicleta.</label>`,preConfirm:()=>{
                const estacion=document.getElementById('inc-estacion').value,lugar=document.getElementById('inc-recepcion-lugar').value.trim(),motivo=document.getElementById('inc-motivo').value.trim(),confirmada=document.getElementById('inc-confirmar').checked;
                if((ubicar&&!estacion)||(!estacion&&!lugar)||(estacion&&lugar)||!motivo||!confirmada){Swal.showValidationMessage('Confirmá la recepción, su motivo y una estación o lugar real, no ambos.');return false;}
                return {version:i.version,versionBicicleta:i.versionBicicleta,estacionId:estacion?id(estacion):null,lugarRecepcion:lugar||null,motivo,recepcionFisicaConfirmada:true};}});
        if(r.isConfirmed)await guardar('/api/incidencias/'+id(i.id)+(ubicar?'/ubicar':'/recibir'),r.value);
    });}
    async function verAlquiler(identificador){return ejecutar(async()=>{const i=actual(identificador);estado('Consultando liquidación…',true);const {data:r}=await axios.get('/api/alquileres/'+id(i.alquilerId)+'/liquidacion');
        const dinero=v=>v==null?'Pendiente de determinar':t(Number(v).toLocaleString('es-AR',{minimumFractionDigits:2,maximumFractionDigits:2}));
        await dialogo({title:'Liquidación del alquiler #'+id(i.alquilerId),confirmButtonText:'Cerrar',html:`<p>Situación: ${t(r.situacionFinanciera.replace(/_/g,' '))}</p><p>Costo final USD: ${dinero(r.costoFinalUsd)}<br>Costo final ARS: ${dinero(r.costoFinalArs)}</p><p>Pagos aprobados ARS: ${dinero(r.pagosAprobadosArs)}<br>Reembolsos confirmados ARS: ${dinero(r.reembolsosConfirmadosArs)}</p><p>Saldo pendiente ARS: ${dinero(r.saldoPendienteArs)}<br>Reembolso pendiente ARS: ${dinero(r.reembolsoPendienteArs)}</p><p class="small">Importes guardados del alquiler; no se usan tarifas o cotizaciones actuales. Los pagos y reintegros se gestionan desde Mis alquileres por su titular.</p>`});estado('Liquidación consultada.');});}
    async function perdida(identificador,eliminar){return ejecutar(async()=>{const i=actual(identificador);const r=await dialogo({title:eliminar?'Eliminar por pérdida definitiva':'Confirmar baja por pérdida definitiva',showCancelButton:true,confirmButtonText:eliminar?'Eliminar lógicamente':'Dar de baja',html:`
        <p>${eliminar?'La bicicleta dejará de aparecer en el inventario habitual, sin borrar su historia. Esta acción no permite restaurarla.':'La bicicleta pasará de extraviada a de baja. Esta es una decisión administrativa explícita, no una consecuencia automática de la demora.'}</p>
        <p>No hubo recepción o retirada física: no se inventará una estación ni se liberarán cupos ficticios. No elimina deudas o movimientos.</p>
        <label for="inc-motivo">Motivo obligatorio</label><textarea id="inc-motivo" class="swal2-textarea" maxlength="2000"></textarea>
        <label><input type="checkbox" id="inc-confirmar"> Confirmo la pérdida definitiva y su respaldo en esta incidencia.</label>`,preConfirm:()=>{const motivo=document.getElementById('inc-motivo').value.trim();if(!motivo||!document.getElementById('inc-confirmar').checked){Swal.showValidationMessage('Ingresá el motivo y confirmá la pérdida definitiva.');return false;}return {version:i.version,versionBicicleta:i.versionBicicleta,motivo,perdidaDefinitivaConfirmada:true};}});
        if(r.isConfirmed)await guardar('/api/incidencias/'+id(i.id)+(eliminar?'/eliminar-perdida':'/baja-perdida'),r.value);
    });}
    async function historial(identificador){return ejecutar(async()=>{estado('Cargando historial…',true);const {data}=await axios.get('/api/incidencias/'+id(identificador)+'/historial');await dialogo({title:'Historial de incidencia #'+id(identificador),confirmButtonText:'Cerrar historial',width:850,
        html:data.map(h=>`<article class="border-bottom pb-2 mb-2"><h3 class="h6">${t(nombre(h.accion))}</h3><p>${t(fecha(h.fechaHoraCambio))} · ${h.origen==='SISTEMA'?'Sistema':'Usuario #'+t(h.actorUsuarioId)}</p>
        <p>${h.estadoNuevo?t(nombre(h.estadoAnterior??'Registro inicial'))+' → '+t(nombre(h.estadoNuevo)):'Actualización de datos, sin cambio de estado.'}</p><p class="maintenance-work-text">${t(h.observaciones)}</p>
        ${h.valoresAnteriores!=null||h.valoresNuevos!=null?`<details><summary>Datos de auditoría (detalle técnico original)</summary><pre class="maintenance-audit">Antes: ${t(h.valoresAnteriores)}\nDespués: ${t(h.valoresNuevos)}</pre></details>`:''}</article>`).join('')});estado('Historial consultado.');});}
    document.addEventListener('DOMContentLoaded',async()=>{perfil=await requerirSesion(['CLIENTE','ADMIN']);if(!perfil)return;await cargarNombreUsuario(perfil);
        document.getElementById('inc-nav-admin').hidden=!admin();document.getElementById('inc-alcance').textContent=admin()?'Revisión administrativa, recepción y seguimiento. El diagnóstico y la aptitud técnica se gestionan en mantenimiento.':'Reportes de tus alquileres y seguimiento de sus decisiones.';
        await cargar();const p=new URLSearchParams(window.location.search);if(p.has('alquilerId')&&p.has('detalleId'))await nuevo(id(p.get('alquilerId')),id(p.get('detalleId')));
    });
    return {cargar,nuevo,renderizar,resolver,recibir,perdida,historial,verAlquiler,cambiarDecision,acciones,t,fecha,instante};
})();
