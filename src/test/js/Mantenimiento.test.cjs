const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const fuente=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/mantenimiento.js'),'utf8');
const base={id:7,version:3,bicicletaId:10,versionBicicleta:2,codigoBicicleta:'MANT-01',estadoBicicleta:'MANTENIMIENTO',
    tipo:'CORRECTIVO',estado:'CREADA',tecnicoId:2,tecnicoNombre:'Técnico',ubicacion:'Taller interno',estacionId:1,estacionPublica:false};
async function entorno(roles=['ADMIN'],datos=[base]){
    const eventos={},campos=new Map(),llamadas=[],avisos=[],respuestas=[];
    for(const n of ['navbar-username','crear-orden','configurar-preventivo','volver-admin','alcance-mantenimiento','ordenes-body','mensaje-mantenimiento','actualizar-ordenes','rol-mantenimiento','panel-ordenes','carga-mantenimiento'])
        campos.set(n,{hidden:false,disabled:false,textContent:'',innerHTML:'',atributos:{},
            setAttribute(k,v){this.atributos[k]=v;},getAttribute(k){return this.atributos[k];}});
    let post=async()=>({data:{}});
    let get=async(url)=>({data:url.endsWith('/tecnicos')?[{id:2,nombre:'Técnico'}]:url==='/api/bicicletas'?[{id:11,estado:'DISPONIBLE',codigoBicicleta:'NUEVA'}]:datos});
    const desplegables=[];
    const c=vm.createContext({console,Number,Set,
        document:{addEventListener:(n,f)=>eventos[n]=f,getElementById:n=>campos.get(n),activeElement:null,body:{},
            querySelector:()=>null,querySelectorAll:s=>s==='[data-mant-busy]'?['actualizar-ordenes','crear-orden','configurar-preventivo'].map(n=>campos.get(n))
                :s==='details[data-mant-detalle][open]'?desplegables.filter(d=>d.open):s==='details[data-mant-detalle]'?desplegables:[]},
        requerirSesion:async()=>({id:roles.includes('ADMIN')?1:2,roles}),rolesDelUsuario:u=>u.roles,cargarNombreUsuario:async()=>{},
        manejarErrorSesion:e=>[401,403].includes(e.response?.status),
        axios:{get:async(url)=>{llamadas.push(['GET',url]);return get(url);},
            post:async(...args)=>{llamadas.push(['POST',...args]);return post(...args);},put:async(...args)=>{llamadas.push(['PUT',...args]);return{data:{}};}},
        Swal:{fire:async(...args)=>{const o=args[0];avisos.push(typeof o==='object'?[o.title,o.text,o.icon,o]:args);return respuestas.shift()??{isConfirmed:true,value:'QA'}},showValidationMessage(){}}
    });
    vm.runInContext(fuente,c);await eventos.DOMContentLoaded();
    return{c,m:vm.runInContext('Mantenimiento',c),campos,llamadas,avisos,respuestas,desplegables,setPost:f=>post=f,setGet:f=>get=f};
}
test('ADMIN gestiona sin botones de trabajo técnico',async()=>{
    const e=await entorno();const html=e.campos.get('ordenes-body').innerHTML;
    assert.match(html,/Asignar técnico/);assert.match(html,/Cancelar orden/);assert.doesNotMatch(html,/data-accion="iniciar"/);
    assert.equal(e.campos.get('crear-orden').hidden,false);
});
test('TECNICO asignado ve iniciar y ubicación interna sin gestión administrativa',async()=>{
    const e=await entorno(['TECNICO']);const html=e.campos.get('ordenes-body').innerHTML;
    assert.match(html,/data-accion="iniciar"/);assert.match(html,/Taller interno/);
    assert.doesNotMatch(html,/Asignar técnico|Cancelar orden/);assert.equal(e.campos.get('crear-orden').hidden,true);
});
test('TECNICO no recibe botones para una orden ajena',async()=>{
    const e=await entorno(['TECNICO'],[{...base,tecnicoId:3}]);assert.doesNotMatch(e.campos.get('ordenes-body').innerHTML,/data-accion="iniciar"/);
});
test('finalización técnica no ofrece habilitación; ADMIN solo con APTA',async()=>{
    const o={...base,estado:'FINALIZADA',resultadoTecnico:'APTA'};
    const tec=await entorno(['TECNICO'],[o]);assert.doesNotMatch(tec.m.acciones(o),/habilitar/);
    const admin=await entorno(['ADMIN'],[o]);assert.match(admin.m.acciones(o),/habilitar/);
    assert.doesNotMatch(admin.m.acciones({...o,resultadoTecnico:'NO_APTA'}),/habilitar/);
});
test('cancelar diálogo no escribe',async()=>{
    const e=await entorno();e.respuestas.push({isConfirmed:false});await e.m.operar(7,'cancelar');
    assert.equal(e.llamadas.filter(c=>c[0]==='POST').length,0);
});
test('comando incluye versión y motivo, sin estado ni actor elegidos por JS',async()=>{
    const e=await entorno();e.respuestas.push({isConfirmed:true,value:'  Revisar  '});await e.m.operar(7,'cancelar');
    const p=e.llamadas.find(c=>c[0]==='POST');
    assert.equal(p[1],'/api/mantenimiento/ordenes/7/cancelar');
    assert.deepEqual(JSON.parse(JSON.stringify(p[2])),{version:3,motivo:'Revisar'});
});
test('habilitar incluye también versión de la bicicleta',async()=>{
    const e=await entorno(['ADMIN'],[{...base,estado:'FINALIZADA',resultadoTecnico:'APTA'}]);e.respuestas.push({isConfirmed:true,value:'Verificada'});
    await e.m.operar(7,'habilitar');assert.equal(e.llamadas.find(c=>c[0]==='POST')[2].versionBicicleta,2);
});
test('doble clic no repite escritura',async()=>{
    const e=await entorno(['TECNICO']);let terminar;
    e.setPost(()=>new Promise(r=>terminar=r));const p=e.m.operar(7,'iniciar');await new Promise(r=>setImmediate(r));
    await e.m.operar(7,'iniciar');assert.equal(e.llamadas.filter(c=>c[0]==='POST').length,1);terminar({data:{}});await p;
});
test('409 conserva el error y no anuncia éxito ni reintenta',async()=>{
    const e=await entorno(['TECNICO']);e.setPost(async()=>{throw{response:{status:409,data:'La orden fue modificada.'}}});
    await e.m.operar(7,'iniciar');
    assert.equal(e.llamadas.filter(c=>c[0]==='POST').length,1);
    assert.ok(e.avisos.some(a=>a[0]==='No se pudo completar'&&a[1].includes('modificada')));
    assert.ok(!e.avisos.some(a=>a[0]==='Operación registrada'));
});
test('401 se delega al control de sesión sin éxito falso',async()=>{
    const e=await entorno(['TECNICO']);e.setPost(async()=>{throw{response:{status:401}}});await e.m.operar(7,'iniciar');
    assert.ok(!e.avisos.some(a=>a[0]==='Operación registrada'));
});
test('textos recibidos se escapan y costos desconocidos no son cero',async()=>{
    const e=await entorno(['ADMIN'],[{...base,codigoBicicleta:'<script>alert(1)</script>',diagnostico:'<img onerror=alert(1)>',costoRepuestos:null}]);
    const h=e.campos.get('ordenes-body').innerHTML;assert.doesNotMatch(h,/<script>|<img/);assert.match(h,/&lt;script&gt;/);assert.match(h,/Sin informar/);
});
test('crear envía bicicleta, tipo y motivo sin fabricar asignación o resultado',async()=>{
    const e=await entorno();e.respuestas.push({isConfirmed:true,value:'11'},{isConfirmed:true,value:'CORRECTIVO'},{isConfirmed:true,value:'Freno'});
    await e.m.crear();const p=e.llamadas.find(c=>c[0]==='POST');
    assert.deepEqual(JSON.parse(JSON.stringify(p[2])),{bicicletaId:11,tipo:'CORRECTIVO',motivo:'Freno'});
});
test('recarga simultánea comparte GET y anuncia carga sin duplicar solicitudes',async()=>{
    const e=await entorno();let terminar;
    e.setGet(()=>new Promise(r=>terminar=r));const antes=e.llamadas.length;
    const p=e.m.cargar(),q=e.m.cargar();assert.equal(p,q);assert.equal(e.llamadas.length,antes+1);
    assert.equal(e.campos.get('actualizar-ordenes').disabled,true);
    assert.equal(e.campos.get('carga-mantenimiento').hidden,false);
    assert.equal(e.campos.get('panel-ordenes').getAttribute('aria-busy'),'true');
    terminar({data:[base]});await p;
    assert.equal(e.campos.get('actualizar-ordenes').disabled,false);
    assert.equal(e.campos.get('carga-mantenimiento').hidden,true);
    assert.equal(e.campos.get('panel-ordenes').getAttribute('aria-busy'),'false');
});
test('POST pendiente muestra Guardando y mantiene controles deshabilitados',async()=>{
    const e=await entorno(['TECNICO']);let terminar;
    e.setPost(()=>new Promise(r=>terminar=r));const p=e.m.operar(7,'iniciar');await new Promise(r=>setImmediate(r));
    assert.match(e.campos.get('mensaje-mantenimiento').textContent,/Guardando/);
    assert.equal(e.campos.get('actualizar-ordenes').disabled,true);
    terminar({data:{}});await p;assert.equal(e.campos.get('actualizar-ordenes').disabled,false);
});
test('error al recargar después de guardar distingue escritura confirmada de consulta fallida',async()=>{
    const e=await entorno(['TECNICO']);e.setGet(async()=>{throw new Error('Sin conexión');});
    await e.m.operar(7,'iniciar');
    const a=e.avisos.find(a=>a[0]==='Operación registrada');assert.equal(a[2],'warning');assert.match(a[1],/no pudimos actualizar/);
    assert.equal(e.llamadas.filter(c=>c[0]==='POST').length,1);
    assert.equal(e.campos.get('actualizar-ordenes').disabled,false);
});
test('etiquetas humanas no modifican códigos enviados ni el modelo recibido',async()=>{
    const datos={...base,estado:'EN_PROCESO',resultadoTecnico:'NO_APTA'};const antes=JSON.stringify(datos);
    const e=await entorno(['TECNICO'],[datos]);const html=e.campos.get('ordenes-body').innerHTML;
    assert.match(html,/En proceso/);assert.match(html,/No apta/);assert.doesNotMatch(html,/EN_PROCESO|NO_APTA/);assert.equal(JSON.stringify(datos),antes);
});
test('fechas UTC sin offset se convierten para mostrar, sin reinterpretar offsets existentes',async()=>{
    const e=await entorno();const entradas=[];const DateReal=Date;
    e.c.Date=class extends DateReal{constructor(v){super(v);entradas.push(v);}toLocaleString(loc){assert.equal(loc,'es-AR');return'16/11/2026, 12:00';}};
    assert.equal(e.m.fecha('2026-11-16T15:00:00'),'16/11/2026, 12:00');
    assert.equal(entradas[0],'2026-11-16T15:00:00Z');
    e.m.fecha('2026-11-16T12:00:00-03:00');assert.equal(entradas[1],'2026-11-16T12:00:00-03:00');
    assert.equal(e.m.fecha(null),'Sin registrar');assert.equal(e.m.fecha('fecha-inválida'),'Fecha no disponible');
});
test('diálogos usan textos en español y acciones explícitas',async()=>{
    const e=await entorno();e.respuestas.push({isConfirmed:false});await e.m.operar(7,'cancelar');
    const opciones=e.avisos[0][3];assert.equal(opciones.confirmButtonText,'Cancelar orden');assert.equal(opciones.cancelButtonText,'Volver');
    assert.equal(opciones.customClass.popup,'maintenance-dialog');
});
test('historial describe eventos sin inventar transiciones ni exponer JSON como información principal',async()=>{
    const e=await entorno();e.setGet(async()=>({data:[{accion:'ASIGNAR',origen:'USUARIO',actorUsuarioId:1,fechaHoraCambio:'2026-11-16T15:00:00',
        estadoAnterior:null,estadoNuevo:null,observaciones:'<script>no</script>',valoresNuevos:'{"tecnicoId":2}'}]}));
    await e.m.historial(7);const o=e.avisos[0][3];
    assert.match(o.html,/Asignación de técnico/);assert.match(o.html,/sin cambio de estado/);assert.match(o.html,/detalle técnico original/);
    assert.doesNotMatch(o.html,/<script>/);assert.equal(o.confirmButtonText,'Cerrar historial');
});
test('recargar conserva los detalles abiertos por identificador',async()=>{
    const e=await entorno();e.desplegables.push({open:true,dataset:{mantDetalle:'7'}},{open:false,dataset:{mantDetalle:'8'}});
    await e.m.cargar();assert.equal(e.desplegables[0].open,true);assert.equal(e.desplegables[1].open,false);
});
test('HTML dispone de menú plegable, encabezados accesibles y CSS acotado',()=>{
    const html=fs.readFileSync(path.join(__dirname,'../../main/resources/static/mantenimiento.html'),'utf8');
    const css=fs.readFileSync(path.join(__dirname,'../../main/resources/static/css/estilos.css'),'utf8');
    assert.match(html,/data-bs-toggle="collapse"/);assert.match(html,/aria-current="page"/);assert.match(html,/scope="col"/);
    assert.match(html,/bootstrap.bundle.min.js/);assert.match(html,/Saltar al contenido/);
    assert.match(css,/\.maintenance-page \.maintenance-table td\[data-label\]/);assert.match(css,/max-width: 767.98px/);
});
test('TECNICO ve el historial legible sin mostrar snapshots internos', async () => {
    const e = await entorno(['TECNICO']);

    e.setGet(async () => ({
        data: [{
            accion: 'TRABAJO',
            origen: 'USUARIO',
            actorUsuarioId: 2,
            observaciones: 'Freno revisado',
            valoresAnteriores: 'INTERNO_ANTES',
            valoresNuevos: 'INTERNO_DESPUES'
        }]
    }));

    await e.m.historial(7);

    const html = e.avisos[0][3].html;

    assert.match(html, /Freno revisado/);
    assert.doesNotMatch(
        html,
        /Datos de auditoría|INTERNO_ANTES|INTERNO_DESPUES|maintenance-audit/
    );
});
