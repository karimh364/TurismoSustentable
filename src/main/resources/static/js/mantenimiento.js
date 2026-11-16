const Mantenimiento = (() => {
    let perfil = null, ordenes = [];
    const pendientes = new Set();
    const texto = v => String(v ?? '—').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const id = v => { const n=Number(v); if(!Number.isSafeInteger(n)||n<=0) throw new Error('Identificador inválido.'); return n; };
    const admin = () => perfil && rolesDelUsuario(perfil).includes('ADMIN');
    const tecnico = o => perfil && rolesDelUsuario(perfil).includes('TECNICO') && Number(perfil.id)===o.tecnicoId;
    function boton(o,accion,etiqueta) {
        return `<button class="btn btn-sm btn-outline-success m-1" data-accion="${accion}" onclick="Mantenimiento.operar(${id(o.id)},'${accion}')">${etiqueta}</button>`;
    }
    function acciones(o) {
        let html = `<button class="btn btn-sm btn-outline-secondary m-1" onclick="Mantenimiento.historial(${id(o.id)})">Historial</button>`;
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
        document.getElementById('ordenes-body').innerHTML=ordenes.map(o => `<tr data-orden-mantenimiento="${id(o.id)}">
            <td>#${id(o.id)}<br><strong>${texto(o.codigoBicicleta)}</strong><br>${texto(o.estadoBicicleta)}</td>
            <td>${texto(o.tipo)}<br><span class="badge bg-secondary">${texto(o.estado)}</span></td>
            <td>${texto(o.tecnicoNombre || 'Sin asignación')}</td>
            <td>${texto(o.ubicacion || 'Sin presencia física registrada')}${o.estacionId ? '<br><small>'+ (o.estacionPublica?'Pública':'Estación/taller interno')+'</small>' : ''}</td>
            <td><details><summary>Ver trabajo</summary>
                <p>Diagnóstico: ${texto(o.diagnostico)}</p><p>Trabajo: ${texto(o.descripcionTrabajo)}</p>
                <p>Observaciones: ${texto(o.observaciones)}</p><p>Resultado: ${texto(o.resultadoTecnico)}</p>
                <p>Creación: ${texto(o.fechaCreacion)} UTC<br>Primer inicio: ${texto(o.fechaInicio)} UTC<br>Cierre: ${texto(o.fechaFin)} UTC</p>
                <p>Repuestos: ${o.costoRepuestos==null?'Sin informar': 'AR$ '+Number(o.costoRepuestos).toLocaleString('es-AR',{minimumFractionDigits:2,maximumFractionDigits:2})}</p>
                <small>Costo interno; no es un cargo automático al cliente.</small></details></td>
            <td>${acciones(o)}</td></tr>`).join('') || '<tr><td colspan="6">No hay órdenes para tu consulta.</td></tr>';
    }
    async function cargar() {
        try {
            const {data}=await axios.get('/api/mantenimiento/ordenes');
            if(!Array.isArray(data)) throw new Error('Respuesta de órdenes inválida.');
            ordenes=data; renderizar(); document.getElementById('mensaje-mantenimiento').textContent='';
        } catch(e) {await error(e); document.getElementById('mensaje-mantenimiento').textContent='No se pudieron actualizar las órdenes.';}
    }
    async function error(e) {
        if(manejarErrorSesion(e)) return;
        const d=e.response?.data;
        const mensaje=typeof d==='string'?d:Array.isArray(d?.validationErrors)?d.validationErrors.map(v=>String(v).replace(/^[^:]+:\s*/,'')).join(' '):e.message||'Intentá nuevamente.';
        await Swal.fire('No se pudo completar',mensaje,'error');
    }
    async function ejecutar(clave,accion) {
        if(pendientes.has(clave)) return;
        pendientes.add(clave);
        try {await accion();} catch(e) {await error(e);} finally {pendientes.delete(clave);}
    }
    async function motivo(titulo) {
        const r=await Swal.fire({title:titulo,input:'textarea',inputLabel:'Motivo obligatorio',showCancelButton:true,
            inputValidator:v=>!v.trim()||v.trim().length>2000?'Ingresá un motivo de hasta 2000 caracteres.':undefined});
        return r.isConfirmed?r.value.trim():null;
    }
    async function elegirTecnico() {
        const {data}=await axios.get('/api/mantenimiento/tecnicos');
        const opciones={}; data.forEach(t=>{opciones[id(t.id)]=t.nombre;});
        if(!data.length) throw new Error('No hay técnicos activos disponibles para asignar.');
        const r=await Swal.fire({title:'Asignar técnico',input:'select',inputOptions:opciones,inputPlaceholder:'Elegí un técnico activo',
            showCancelButton:true,inputValidator:v=>!v?'Seleccioná un técnico.':undefined});
        return r.isConfirmed?id(r.value):null;
    }
    async function trabajo(o,finalizar) {
        const r=await Swal.fire({title:finalizar?'Finalizar orden':'Registrar trabajo',html:`
            <label for="mant-diagnostico">Diagnóstico</label><textarea id="mant-diagnostico" class="swal2-textarea" maxlength="10000">${texto(o.diagnostico??'')}</textarea>
            <label for="mant-trabajo">Trabajo realizado</label><textarea id="mant-trabajo" class="swal2-textarea" maxlength="10000">${texto(o.descripcionTrabajo??'')}</textarea>
            <label for="mant-observaciones">Observaciones</label><textarea id="mant-observaciones" class="swal2-textarea" maxlength="10000">${texto(o.observaciones??'')}</textarea>
            <label for="mant-costo">Repuestos ARS (opcional; vacío conserva el valor registrado)</label>
            <input id="mant-costo" class="swal2-input" type="number" min="0" step="0.01" value="${o.costoRepuestos==null?'':texto(o.costoRepuestos)}">
            ${finalizar?'<label for="mant-resultado">Resultado técnico</label><select id="mant-resultado" class="swal2-select" style="display:block"><option value="">Seleccioná</option><option value="APTA">APTA</option><option value="NO_APTA">NO APTA</option></select>':''}`,
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
                const m=await motivo(accion==='habilitar'?'Autorizar vuelta al servicio':accion==='pausar'?'Pausar trabajo':'Cancelar orden');
                if(m==null) return; datos.motivo=m;
                if(accion==='habilitar') datos.versionBicicleta=o.versionBicicleta;
            } else if(['trabajo','finalizar'].includes(accion)) {
                const t=await trabajo(o,accion==='finalizar'); if(t==null)return; Object.assign(datos,t);
            } else if(! (await Swal.fire({title:accion==='iniciar'?'Iniciar trabajo':'Reanudar trabajo',icon:'question',showCancelButton:true})).isConfirmed) return;
            await axios.post('/api/mantenimiento/ordenes/'+id(ordenId)+'/'+accion,datos);
            await cargar();
            await Swal.fire('Operación registrada',accion==='finalizar'?'La bicicleta continúa fuera de alquiler hasta la habilitación ADMIN.':'Los datos fueron actualizados.','success');
        });
    }
    async function crear() {
        return ejecutar('crear',async()=>{
            const {data}=await axios.get('/api/bicicletas');
            const ocupadas=new Set(ordenes.filter(o=>['CREADA','EN_PROCESO','PAUSADA'].includes(o.estado)).map(o=>o.bicicletaId));
            const opciones={};
            data.filter(b=>!b.borradoBici&&['DISPONIBLE','INACTIVA','MANTENIMIENTO'].includes(b.estado)&&!ocupadas.has(b.id))
                .forEach(b=>{opciones[id(b.id)]=b.codigoBicicleta+' · '+b.estado;});
            if(!Object.keys(opciones).length) throw new Error('No hay bicicletas sin una orden abierta que admitan mantenimiento.');
            const bici=await Swal.fire({title:'Crear orden',input:'select',inputOptions:opciones,inputPlaceholder:'Elegí una bicicleta',
                showCancelButton:true,inputValidator:v=>!v?'Seleccioná una bicicleta.':undefined});
            if(!bici.isConfirmed)return;
            const tipo=await Swal.fire({title:'Tipo de mantenimiento',input:'select',inputOptions:{CORRECTIVO:'Correctivo',PREVENTIVO:'Preventivo'},showCancelButton:true});
            if(!tipo.isConfirmed)return;
            const razon=await motivo('Motivo de la orden'); if(razon==null)return;
            await axios.post('/api/mantenimiento/ordenes',{bicicletaId:id(bici.value),tipo:tipo.value,motivo:razon});
            await cargar(); await Swal.fire('Orden creada','Puede quedar sin técnico hasta la asignación ADMIN.','success');
        });
    }
    async function historial(ordenId) {
        return ejecutar('historia:'+id(ordenId),async()=>{
            const {data}=await axios.get('/api/mantenimiento/ordenes/'+id(ordenId)+'/historial');
            await Swal.fire({title:'Historial de orden #'+id(ordenId),width:900,html:data.map(h=>`<article class="text-start border-bottom mb-3">
                <strong>${texto(h.accion)}</strong> · ${texto(h.fechaHoraCambio)} UTC<br>
                ${texto(h.estadoAnterior)} → ${texto(h.estadoNuevo)}<br>${texto(h.origen)} · actor ${texto(h.actorUsuarioId)}
                <p>${texto(h.observaciones)}</p><details><summary>Datos anteriores / nuevos</summary>
                <pre style="white-space:pre-wrap;overflow-wrap:anywhere">${texto(h.valoresAnteriores)}<br>${texto(h.valoresNuevos)}</pre></details></article>`).join('')});
        });
    }
    async function intervalos() {
        return ejecutar('intervalos',async()=>{
            const {data}=await axios.get('/api/admin/tipos-bicicleta');
            const opciones={}; data.forEach(t=>{opciones[id(t.id)]=t.codigo+' · '+t.intervaloPreventivoMeses+' mes(es)';});
            const t=await Swal.fire({title:'Intervalo preventivo por tipo',input:'select',inputOptions:opciones,showCancelButton:true});
            if(!t.isConfirmed)return;
            const tipo=data.find(v=>v.id===id(t.value));
            const m=await Swal.fire({title:'Meses calendario',input:'number',inputValue:tipo.intervaloPreventivoMeses,inputAttributes:{min:1,step:1},
                showCancelButton:true,inputValidator:v=>!Number.isSafeInteger(Number(v))||Number(v)<=0||Number(v)>2147483647?'Ingresá un entero positivo válido.':undefined});
            if(!m.isConfirmed)return;
            const razon=await motivo('Motivo del cambio de intervalo');if(razon==null)return;
            await axios.put('/api/mantenimiento/tipos-bicicleta/'+id(tipo.id)+'/intervalo',
                {mesesAnteriores:tipo.intervaloPreventivoMeses,meses:Number(m.value),motivo:razon});
            await Swal.fire('Intervalo actualizado','No borra revisiones ni libera bicicletas automáticamente. Las nuevas reservas comprueban su vigencia.','success');
        });
    }
    async function iniciar() {
        perfil=await requerirSesion(['ADMIN','TECNICO']);if(!perfil)return;
        await cargarNombreUsuario(perfil);
        ['crear-orden','configurar-preventivo','volver-admin'].forEach(n=>{document.getElementById(n).hidden=!admin();});
        document.getElementById('alcance-mantenimiento').textContent=admin()?'ADMIN: gestión de órdenes y habilitación. El trabajo técnico requiere rol TECNICO y asignación.':'TECNICO: consulta y trabajo exclusivamente sobre tus órdenes asignadas.';
        await cargar();
    }
    document.addEventListener('DOMContentLoaded',iniciar);
    return {cargar,crear,operar,historial,intervalos,acciones,texto};
})();
