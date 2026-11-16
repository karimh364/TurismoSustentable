const Mantenimiento = (() => {
    let perfil = null, ordenes = [];
    const pendientes = new Set();
    let cargaPendiente = null, cargando = false;
    const texto = v => String(v ?? '—').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const id = v => { const n=Number(v); if(!Number.isSafeInteger(n)||n<=0) throw new Error('Identificador inválido.'); return n; };
    const admin = () => perfil && rolesDelUsuario(perfil).includes('ADMIN');
    const tecnico = o => perfil && rolesDelUsuario(perfil).includes('TECNICO') && Number(perfil.id)===o.tecnicoId;
    const etiquetas = {
        CREADA:'Creada', EN_PROCESO:'En proceso', PAUSADA:'Pausada', FINALIZADA:'Finalizada', CANCELADA:'Cancelada',
        PREVENTIVO:'Preventivo', CORRECTIVO:'Correctivo', APTA:'Apta', NO_APTA:'No apta',
        MANTENIMIENTO:'En mantenimiento', DISPONIBLE:'Disponible', INACTIVA:'Inactiva', DE_BAJA:'De baja',
        RESERVADA:'Reservada', EN_USO:'En uso', EXTRAVIADA:'Extraviada',
        CREAR:'Orden creada', INICIAR:'Inicio del trabajo', REANUDAR:'Reanudación del trabajo',
        PAUSAR:'Pausa del trabajo', TRABAJO:'Registro de trabajo', FINALIZAR:'Finalización técnica',
        ASIGNAR:'Asignación de técnico', CANCELAR:'Cancelación de la orden', HABILITAR:'Habilitación de la bicicleta',
        DESASIGNAR_POR_INACTIVACION:'Desasignación por inactivación del técnico'
    };
    const etiqueta = v => v == null ? 'Sin informar' : etiquetas[v] ?? String(v).replace(/_/g,' ').toLowerCase();
    function fecha(valor) {
        if(!valor) return 'Sin registrar';
        const cadena=String(valor);
        const d=new Date(/(Z|[+-][0-9]{2}:[0-9]{2})$/i.test(cadena)?cadena:cadena+'Z');
        return Number.isNaN(d.getTime())?'Fecha no disponible':d.toLocaleString('es-AR',{
            day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit',hour12:false
        });
    }
    function dialogo(opciones) {
        return Swal.fire({
            confirmButtonText:'Confirmar',cancelButtonText:'Cancelar',
            confirmButtonColor:'#198754',cancelButtonColor:'#6c757d',
            customClass:{popup:'maintenance-dialog',confirmButton:'rounded-pill',cancelButton:'rounded-pill'},
            ...opciones
        });
    }
    function aviso(title,text,icon) {return dialogo({title,text,icon,confirmButtonText:'Entendido'});}
    function estado(mensaje,ocupado=false) {
        const e=document.getElementById('mensaje-mantenimiento');if(e)e.textContent=mensaje;
        const spinner=document.getElementById('carga-mantenimiento');if(spinner)spinner.hidden=!ocupado;
        document.getElementById('panel-ordenes')?.setAttribute('aria-busy',String(ocupado));
    }
    function controles() {
        document.querySelectorAll('[data-mant-busy]').forEach(b=>{b.disabled=cargando||pendientes.size>0;});
    }
    async function solicitar(accion,mensaje) {
        estado(mensaje,true);
        try {return await accion();} finally {estado('');}
    }
    function recuperarFoco(origen) {
        if(!origen || (document.activeElement!==document.body && document.activeElement!==origen
                && !document.activeElement?.closest?.('.maintenance-dialog')))return;
        if(origen.isConnected && !origen.disabled) {origen.focus({preventScroll:true});return;}
        const numero=Number(origen.dataset?.ordenId);
        const fila=Number.isSafeInteger(numero)?document.querySelector('[data-orden-mantenimiento="'+numero+'"]'):null;
        (fila??document.getElementById('actualizar-ordenes'))?.focus({preventScroll:true});
    }
    function boton(o,accion,etiqueta) {
        return `<button type="button" class="btn btn-sm btn-outline-success" data-mant-busy data-orden-id="${id(o.id)}" data-accion="${accion}" onclick="Mantenimiento.operar(${id(o.id)},'${accion}')">${etiqueta}</button>`;
    }
    function acciones(o) {
        let html = `<button type="button" class="btn btn-sm btn-outline-secondary" data-mant-busy data-orden-id="${id(o.id)}" onclick="Mantenimiento.historial(${id(o.id)})">Historial</button>`;
        if(admin()) {
            if(['CREADA','PAUSADA'].includes(o.estado)) html+=boton(o,'asignar','Asignar técnico');
            if(['CREADA','EN_PROCESO','PAUSADA'].includes(o.estado)) html+=boton(o,'cancelar','Cancelar orden');
            if(o.estado==='FINALIZADA' && o.resultadoTecnico==='APTA' && o.estadoBicicleta==='MANTENIMIENTO')
                html+=boton(o,'habilitar','Habilitar bicicleta');
        }
        if(tecnico(o)) {
            if(o.estado==='CREADA') html+=boton(o,'iniciar','Iniciar');
            if(o.estado==='PAUSADA') html+=boton(o,'reanudar','Reanudar');
            if(o.estado==='EN_PROCESO') html+=boton(o,'pausar','Pausar')+boton(o,'trabajo','Registrar trabajo')+boton(o,'finalizar','Finalizar');
        }
        return html;
    }
    function renderizar() {
        const abiertos=Array.from(document.querySelectorAll('details[data-mant-detalle][open]')).map(e=>e.dataset.mantDetalle);
        document.getElementById('ordenes-body').innerHTML=ordenes.map(o => `<tr role="row" tabindex="-1" data-orden-mantenimiento="${id(o.id)}">
            <td role="cell" data-label="Orden / bicicleta"><div class="maintenance-cell">#${id(o.id)}<br><strong>${texto(o.codigoBicicleta)}</strong><br>${texto(etiqueta(o.estadoBicicleta))}</div></td>
            <td role="cell" data-label="Tipo / estado"><div class="maintenance-cell">${texto(etiqueta(o.tipo))}<br><span class="badge ${o.estado==='FINALIZADA'?'bg-success':o.estado==='EN_PROCESO'?'bg-warning text-dark':'bg-secondary'}">${texto(etiqueta(o.estado))}</span></div></td>
            <td role="cell" data-label="Técnico"><div class="maintenance-cell">${texto(o.tecnicoNombre || 'Sin asignación')}</div></td>
            <td role="cell" data-label="Ubicación física"><div class="maintenance-cell">${texto(o.ubicacion || 'Sin presencia física registrada')}${o.estacionId ? '<br><small>'+ (o.estacionPublica?'Pública':'Estación/taller interno')+'</small>' : ''}</div></td>
            <td role="cell" data-label="Trabajo y resultado"><div class="maintenance-cell"><details data-mant-detalle="${id(o.id)}"><summary>Ver trabajo</summary>
                <p class="mt-2 maintenance-work-text">Diagnóstico: ${texto(o.diagnostico)}</p><p class="maintenance-work-text">Trabajo: ${texto(o.descripcionTrabajo)}</p>
                <p class="maintenance-work-text">Observaciones: ${texto(o.observaciones)}</p><p>Resultado: ${texto(etiqueta(o.resultadoTecnico))}</p>
                <p>Creación: ${texto(fecha(o.fechaCreacion))}<br>Primer inicio: ${texto(fecha(o.fechaInicio))}<br>Cierre: ${texto(fecha(o.fechaFin))}</p>
                <p>Repuestos: ${o.costoRepuestos==null?'Sin informar': 'AR$ '+Number(o.costoRepuestos).toLocaleString('es-AR',{minimumFractionDigits:2,maximumFractionDigits:2})}</p>
                <small>Costo interno; no es un cargo automático al cliente.</small></details></div></td>
            <td role="cell" data-label="Acciones"><div class="maintenance-cell maintenance-actions">${acciones(o)}</div></td></tr>`).join('') || '<tr><td colspan="6">No hay órdenes para tu consulta.</td></tr>';
        document.querySelectorAll('details[data-mant-detalle]').forEach(e=>{e.open=abiertos.includes(e.dataset.mantDetalle);});
        controles();
    }
    function cargar() {
        if(cargaPendiente)return cargaPendiente;
        cargaPendiente=(async()=>{
            cargando=true;controles();estado('Actualizando las órdenes…',true);
            try {
                const {data}=await axios.get('/api/mantenimiento/ordenes');
                if(!Array.isArray(data)) throw new Error('Respuesta de órdenes inválida.');
                ordenes=data;renderizar();estado(ordenes.length?'Lista de órdenes actualizada.':'No hay órdenes para tu consulta.');
                return true;
            } catch(e) {
                await error(e);estado('No se pudieron actualizar las órdenes. Intentá nuevamente.');
                if(!ordenes.length)document.getElementById('ordenes-body').innerHTML='<tr><td colspan="6">La consulta no está disponible. Usá Actualizar órdenes para reintentar.</td></tr>';
                return false;
            } finally {cargando=false;cargaPendiente=null;controles();}
        })();
        return cargaPendiente;
    }
    async function error(e) {
        if(manejarErrorSesion(e)) return;
        const d=e.response?.data;
        const mensaje=typeof d==='string'?d:Array.isArray(d?.validationErrors)?d.validationErrors.map(v=>String(v).replace(/^[^:]+:\s*/,'')).join(' '):e.message||'Intentá nuevamente.';
        await aviso('No se pudo completar',mensaje,'error');
    }
    async function ejecutar(clave,accion) {
        if(pendientes.has(clave)) return;
        const origen=document.activeElement;
        pendientes.add(clave);controles();
        try {await accion();} catch(e) {estado('No se pudo completar la operación. Revisá el aviso antes de continuar.');await error(e);}
        finally {pendientes.delete(clave);controles();recuperarFoco(origen);}
    }
    async function motivo(titulo,confirmar='Confirmar') {
        const r=await dialogo({title:titulo,input:'textarea',inputLabel:'Motivo obligatorio',showCancelButton:true,confirmButtonText:confirmar,cancelButtonText:'Volver',
            inputValidator:v=>!v.trim()||v.trim().length>2000?'Ingresá un motivo de hasta 2000 caracteres.':undefined});
        return r.isConfirmed?r.value.trim():null;
    }
    async function elegirTecnico() {
        const {data}=await solicitar(()=>axios.get('/api/mantenimiento/tecnicos'),'Buscando técnicos activos…');
        const opciones={}; data.forEach(t=>{opciones[id(t.id)]=t.nombre;});
        if(!data.length) throw new Error('No hay técnicos activos disponibles para asignar.');
        const r=await dialogo({title:'Asignar técnico',confirmButtonText:'Asignar',input:'select',inputOptions:opciones,inputPlaceholder:'Elegí un técnico activo',
            showCancelButton:true,inputValidator:v=>!v?'Seleccioná un técnico.':undefined});
        return r.isConfirmed?id(r.value):null;
    }
    async function trabajo(o,finalizar) {
        const r=await dialogo({title:finalizar?'Finalizar orden':'Registrar trabajo',
            confirmButtonText:finalizar?'Finalizar orden':'Guardar trabajo',cancelButtonText:'Volver sin guardar',html:`
            <p class="small">Para finalizar, completá diagnóstico, trabajo y resultado. La bicicleta seguirá fuera de alquiler hasta la autorización administrativa.</p>
            <label for="mant-diagnostico">Diagnóstico${finalizar?' (obligatorio)':''}</label><textarea id="mant-diagnostico" class="swal2-textarea" rows="3" aria-required="${finalizar}" maxlength="10000">${texto(o.diagnostico??'')}</textarea>
            <label for="mant-trabajo">Trabajo realizado${finalizar?' (obligatorio)':''}</label><textarea id="mant-trabajo" class="swal2-textarea" rows="3" aria-required="${finalizar}" maxlength="10000">${texto(o.descripcionTrabajo??'')}</textarea>
            <label for="mant-observaciones">Observaciones (opcional)</label><textarea id="mant-observaciones" class="swal2-textarea" rows="3" maxlength="10000">${texto(o.observaciones??'')}</textarea>
            <label for="mant-costo">Repuestos ARS (opcional; vacío conserva el valor registrado)</label>
            <input id="mant-costo" class="swal2-input" type="number" min="0" step="0.01" value="${o.costoRepuestos==null?'':texto(o.costoRepuestos)}">
            ${finalizar?'<label for="mant-resultado">Resultado técnico (obligatorio)</label><select id="mant-resultado" class="form-select" aria-required="true"><option value="">Seleccioná</option><option value="APTA">Apta</option><option value="NO_APTA">No apta</option></select>':''}`,
            showCancelButton:true,preConfirm:()=>{
                const d={diagnostico:document.getElementById('mant-diagnostico').value.trim(),
                    descripcionTrabajo:document.getElementById('mant-trabajo').value.trim(),
                    observaciones:document.getElementById('mant-observaciones').value.trim()};
                const costo=document.getElementById('mant-costo').value;
                if(costo!=='') {
                    if(!/^(0|[1-9][0-9]{0,9})([.][0-9]{1,2})?$/.test(costo)) {Swal.showValidationMessage('El costo admite hasta 10 enteros y 2 decimales, sin negativos.'); return false;}
                    d.costoRepuestos=Number(costo);
                }
                if(finalizar) {
                    d.resultadoTecnico=document.getElementById('mant-resultado').value;
                    if(!d.diagnostico||!d.descripcionTrabajo||!d.resultadoTecnico) {Swal.showValidationMessage('Completá diagnóstico, trabajo y resultado.');return false;}
                }
                return d;
            }});
        return r.isConfirmed?r.value:null;
    }
    async function operar(ordenId,accion) {
        return ejecutar('orden:'+id(ordenId),async()=>{
            const o=ordenes.find(v=>v.id===ordenId);
            if(!o) throw new Error('Recargá la orden antes de operar.');
            let datos={version:o.version};
            if(accion==='asignar') {const t=await elegirTecnico();if(t==null)return;datos.tecnicoId=t;}
            else if(['cancelar','pausar','habilitar'].includes(accion)) {
                const m=await motivo(accion==='habilitar'?'Autorizar vuelta al servicio':accion==='pausar'?'Pausar trabajo':'Cancelar orden',
                    accion==='habilitar'?'Solicitar habilitación':accion==='pausar'?'Pausar':'Cancelar orden');
                if(m==null) return; datos.motivo=m;
                if(accion==='habilitar') datos.versionBicicleta=o.versionBicicleta;
            } else if(['trabajo','finalizar'].includes(accion)) {
                const t=await trabajo(o,accion==='finalizar'); if(t==null)return; Object.assign(datos,t);
            } else if(! (await dialogo({title:accion==='iniciar'?'Iniciar trabajo':'Reanudar trabajo',
                confirmButtonText:accion==='iniciar'?'Iniciar trabajo':'Reanudar trabajo',icon:'question',showCancelButton:true})).isConfirmed) return;
            await solicitar(()=>axios.post('/api/mantenimiento/ordenes/'+id(ordenId)+'/'+accion,datos),'Guardando la operación…');
            const actualizado=await cargar();
            await aviso('Operación registrada',!actualizado?'Se registró la operación, pero no pudimos actualizar la lista. Usá Actualizar órdenes antes de continuar.'
                :accion==='finalizar'?'La bicicleta continúa fuera de alquiler hasta la habilitación de administración.':'Los datos fueron actualizados.',
                actualizado?'success':'warning');
        });
    }
    async function crear() {
        return ejecutar('crear',async()=>{
            const {data}=await solicitar(()=>axios.get('/api/bicicletas'),'Buscando bicicletas para mantenimiento…');
            const ocupadas=new Set(ordenes.filter(o=>['CREADA','EN_PROCESO','PAUSADA'].includes(o.estado)).map(o=>o.bicicletaId));
            const opciones={};
            data.filter(b=>!b.borradoBici&&['DISPONIBLE','INACTIVA','MANTENIMIENTO'].includes(b.estado)&&!ocupadas.has(b.id))
                .forEach(b=>{opciones[id(b.id)]=b.codigoBicicleta+' · '+etiqueta(b.estado);});
            if(!Object.keys(opciones).length) throw new Error('No hay bicicletas sin una orden abierta que admitan mantenimiento.');
            const bici=await dialogo({title:'Crear orden',confirmButtonText:'Continuar',input:'select',inputOptions:opciones,inputPlaceholder:'Elegí una bicicleta',
                showCancelButton:true,inputValidator:v=>!v?'Seleccioná una bicicleta.':undefined});
            if(!bici.isConfirmed)return;
            const tipo=await dialogo({title:'Tipo de mantenimiento',confirmButtonText:'Continuar',input:'select',inputOptions:{CORRECTIVO:'Correctivo',PREVENTIVO:'Preventivo'},showCancelButton:true});
            if(!tipo.isConfirmed)return;
            const razon=await motivo('Motivo de la orden','Crear orden'); if(razon==null)return;
            await solicitar(()=>axios.post('/api/mantenimiento/ordenes',{bicicletaId:id(bici.value),tipo:tipo.value,motivo:razon}),'Creando la orden…');
            const actualizado=await cargar();
            await aviso('Orden creada',actualizado?'Puede quedar sin técnico hasta que administración lo asigne.':'La orden se creó, pero falta actualizar la lista. Usá Actualizar órdenes.',actualizado?'success':'warning');
        });
    }
    async function historial(ordenId) {
        return ejecutar('historia:'+id(ordenId),async()=>{
            const {data}=await solicitar(()=>axios.get('/api/mantenimiento/ordenes/'+id(ordenId)+'/historial'),'Cargando el historial…');
            await dialogo({title:'Historial de orden #'+id(ordenId),confirmButtonText:'Cerrar historial',width:900,
                html:'<p class="small text-muted">Fechas en hora local. Se conserva quién realizó cada operación.</p>'+data.map(h=>`<article class="border-bottom mb-3 pb-3">
                <h3 class="h6">${texto(etiqueta(h.accion))}</h3><p class="small">${texto(fecha(h.fechaHoraCambio))}</p>
                <p>${h.estadoAnterior!=null && h.estadoNuevo!=null
                    ?texto(etiqueta(h.estadoAnterior))+' → '+texto(etiqueta(h.estadoNuevo))
                    :h.estadoNuevo!=null?'Estado inicial: '+texto(etiqueta(h.estadoNuevo)):'Actualización de datos, sin cambio de estado.'}</p>
                <p class="small">${h.origen==='SISTEMA'?'Generado automáticamente por el sistema.':'Registrado por el usuario #'+texto(h.actorUsuarioId)+'.'}</p>
                <p class="maintenance-work-text">${texto(h.observaciones)}</p><details><summary>Datos de auditoría (detalle técnico original)</summary>
                <h4 class="h6 mt-2">Antes</h4><pre class="maintenance-audit">${texto(h.valoresAnteriores)}</pre>
                <h4 class="h6">Después</h4><pre class="maintenance-audit">${texto(h.valoresNuevos)}</pre></details></article>`).join('')});
        });
    }
    async function intervalos() {
        return ejecutar('intervalos',async()=>{
            const {data}=await solicitar(()=>axios.get('/api/admin/tipos-bicicleta'),'Cargando intervalos preventivos…');
            const opciones={}; data.forEach(t=>{opciones[id(t.id)]=t.codigo+' · '+t.intervaloPreventivoMeses+' mes(es)';});
            const t=await dialogo({title:'Intervalo preventivo por tipo',confirmButtonText:'Continuar',input:'select',inputOptions:opciones,showCancelButton:true});
            if(!t.isConfirmed)return;
            const tipo=data.find(v=>v.id===id(t.value));
            const m=await dialogo({title:'Meses calendario',confirmButtonText:'Continuar',input:'number',inputValue:tipo.intervaloPreventivoMeses,inputAttributes:{min:1,step:1},
                showCancelButton:true,inputValidator:v=>!Number.isSafeInteger(Number(v))||Number(v)<=0||Number(v)>2147483647?'Ingresá un entero positivo válido.':undefined});
            if(!m.isConfirmed)return;
            const razon=await motivo('Motivo del cambio de intervalo','Guardar intervalo');if(razon==null)return;
            await solicitar(()=>axios.put('/api/mantenimiento/tipos-bicicleta/'+id(tipo.id)+'/intervalo',
                {mesesAnteriores:tipo.intervaloPreventivoMeses,meses:Number(m.value),motivo:razon}),'Guardando el intervalo…');
            await aviso('Intervalo actualizado','No borra revisiones ni libera bicicletas automáticamente. Las nuevas reservas comprueban su vigencia.','success');
        });
    }
    async function iniciar() {
        perfil=await requerirSesion(['ADMIN','TECNICO']);if(!perfil)return;
        await cargarNombreUsuario(perfil);
        ['crear-orden','configurar-preventivo','volver-admin'].forEach(n=>{document.getElementById(n).hidden=!admin();});
        document.getElementById('rol-mantenimiento').textContent=admin()?'Modo administrador':'Modo técnico';
        document.getElementById('alcance-mantenimiento').textContent=admin()?'Gestioná órdenes y autorizá la vuelta al servicio. Las tareas técnicas requieren ese rol y una asignación vigente.':'Consultá y trabajá exclusivamente sobre tus órdenes asignadas.';
        await cargar();
    }
    document.addEventListener('DOMContentLoaded',iniciar);
    return {cargar,crear,operar,historial,intervalos,acciones,texto,fecha,etiqueta};
})();
