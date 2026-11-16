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
    for(const n of ['navbar-username','crear-orden','configurar-preventivo','volver-admin','alcance-mantenimiento','ordenes-body','mensaje-mantenimiento'])
        campos.set(n,{hidden:false,textContent:'',innerHTML:''});
    let post=async()=>({data:{}});
    const c=vm.createContext({console,Number,Set,
        document:{addEventListener:(n,f)=>eventos[n]=f,getElementById:n=>campos.get(n)},
        requerirSesion:async()=>({id:roles.includes('ADMIN')?1:2,roles}),rolesDelUsuario:u=>u.roles,cargarNombreUsuario:async()=>{},
        manejarErrorSesion:e=>[401,403].includes(e.response?.status),
        axios:{get:async(url)=>{llamadas.push(['GET',url]);return{data:url.endsWith('/tecnicos')?[{id:2,nombre:'Técnico'}]:url==='/api/bicicletas'?[{id:11,estado:'DISPONIBLE',codigoBicicleta:'NUEVA'}]:datos};},
            post:async(...args)=>{llamadas.push(['POST',...args]);return post(...args);},put:async(...args)=>{llamadas.push(['PUT',...args]);return{data:{}};}},
        Swal:{fire:async(...args)=>{avisos.push(args);return respuestas.shift()??{isConfirmed:true,value:'QA'}},showValidationMessage(){}}
    });
    vm.runInContext(fuente,c);await eventos.DOMContentLoaded();
    return{c,m:vm.runInContext('Mantenimiento',c),campos,llamadas,avisos,respuestas,setPost:f=>post=f};
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
