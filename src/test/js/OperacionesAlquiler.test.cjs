const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const carpeta = path.join(__dirname, '../../main/resources/static');
const fuente = n => fs.readFileSync(path.join(carpeta, 'js', n), 'utf8');
function entorno() {
    const llamadas=[], avisos=[], respuestas=[], almacenamiento=new Map(), elementos=new Map();
    let post = async () => ({data:{estadoPago:'APROBADO',montoArs:100}});
    let get = async () => ({data:[]});
    let recargas=0;
    const contexto = vm.createContext({
        console, crypto:{randomUUID:()=> '123e4567-e89b-42d3-a456-426614174000'},
        localStorage:{getItem:k=>almacenamiento.get(k)??null,setItem:(k,v)=>almacenamiento.set(k,v),removeItem:k=>almacenamiento.delete(k)},
        document:{addEventListener(){},getElementById:k=>elementos.get(k),querySelectorAll:()=>[]},
        window:{location:{assign:u=>llamadas.push(['ir',u])}},
        axios:{post:async(...args)=>{llamadas.push(['POST',...args]);return post(...args)},
            get:async(...args)=>{llamadas.push(['GET',...args]);return get(...args)}},
        Swal:{fire:async(...args)=>{avisos.push(args);return respuestas.shift()??{isConfirmed:true}}},
        rolesDelUsuario:u=>u.roles, manejarErrorSesion:e=>[401,403].includes(e.response?.status)
    });
    vm.runInContext(fuente('alquileres-vista.js')+'\n'+fuente('alquileres-operaciones.js'),contexto);
    const ops=vm.runInContext('OperacionesAlquiler',contexto);
    ops.configurar({id:1,roles:['CLIENTE']},async()=>{recargas++});
    return {contexto,ops,llamadas,avisos,respuestas,almacenamiento,elementos,
        setPost:f=>{post=f},setGet:f=>{get=f},recargas:()=>recargas};
}
const alquiler={id:7,usuarioId:1,detalles:[{id:9,estado:'PENDIENTE_RETIRO'}]};
test('cliente titular ve retiro y anticipo; cliente ajeno no',()=>{
    const e=entorno();
    assert.match(e.ops.acciones(alquiler),/anticipo\(7\)/);
    assert.match(e.ops.accionesDetalle(alquiler,alquiler.detalles[0]),/retirar\(7,9\)/);
    assert.doesNotMatch(e.ops.acciones({...alquiler,usuarioId:2}),/anticipo/);
});
test('admin incluso con CLIENTE no muestra cobros ni retiros en su panel',()=>{
    const e=entorno();e.ops.configurar({id:1,roles:['ADMIN','CLIENTE']},async()=>{},true);
    assert.doesNotMatch(e.ops.acciones(alquiler,true),/anticipo|retirar/);
    assert.match(e.ops.accionesDetalle(alquiler,alquiler.detalles[0],true),/cancelarAdmin/);
});
test('en uso solo ofrece devolver; terminal no ofrece mutaciones',()=>{
    const e=entorno();
    assert.match(e.ops.accionesDetalle(alquiler,{id:9,estado:'EN_CURSO'}),/devolver/);
    assert.equal(e.ops.accionesDetalle(alquiler,{id:9,estado:'DEVUELTO'}),'');
});
test('anticipo reusa UUID y nunca envía importe elegido por navegador',async()=>{
    const e=entorno();e.setPost(async()=>({data:{estadoPago:'PENDIENTE',montoArs:100}}));
    await e.ops.anticipo(7);await e.ops.anticipo(7);
    assert.equal(new Set(e.llamadas.map(c=>c[3].headers['Idempotency-Key'])).size,1);
    assert.ok(e.llamadas.every(c=>c[2]===null));
    assert.equal(e.llamadas.length,4);
});
test('rechazo confirmado permite nuevo intento explícito, error incierto conserva clave',async()=>{
    const e=entorno();e.setPost(async()=>({data:{estadoPago:'RECHAZADO',montoArs:100}}));
    await e.ops.anticipo(7);assert.equal(e.almacenamiento.size,0);
    e.setPost(async()=>{throw new Error('conexión interrumpida')});
    await e.ops.anticipo(7);assert.equal(e.almacenamiento.size,1);assert.equal(e.llamadas.length,2);
});
test('401 no se anuncia como pago aprobado ni se reenvía',async()=>{
    const e=entorno();e.setPost(async()=>{throw {response:{status:401}}});
    await e.ops.anticipo(7);assert.equal(e.llamadas.length,1);assert.equal(e.recargas(),0);assert.equal(e.avisos.length,0);
});
test('doble clic comparte operación y no duplica POST',async()=>{
    const e=entorno();let liberar;
    e.setPost(()=>new Promise(r=>{liberar=r}));
    const p=e.ops.anticipo(7);await e.ops.anticipo(7);
    assert.equal(e.llamadas.length,1);liberar({data:{estadoPago:'APROBADO',montoArs:100}});await p;
});
test('cancelar diálogo no escribe',async()=>{
    const e=entorno();e.respuestas.push({isConfirmed:false});
    await e.ops.cancelar(7,9);assert.equal(e.llamadas.length,0);
});
test('cancelación usa detalle sin cambiar una fecha o importe desde cliente',async()=>{
    const e=entorno();await e.ops.cancelar(7,9);
    assert.equal(e.llamadas[0][1],'/api/alquileres/7/detalles/9/cancelar');
    assert.equal(e.llamadas[0].length,2);
});
test('devolución filtra internos y sin espacio, envía IDs y falla coherente',async()=>{
    const e=entorno();
    e.setGet(async()=>({data:[{id:1,nombre:'Pública',esPublica:true,espaciosLibres:1},
        {id:2,nombre:'Interna',esPublica:false,espaciosLibres:5},{id:3,nombre:'Llena',espaciosLibres:0}]}));
    e.respuestas.push({isConfirmed:true,value:'1'},{isDenied:true,isConfirmed:false});
    await e.ops.devolver(7,9);
    assert.deepEqual(Object.keys(e.avisos[0][0].inputOptions),['1']);
    const datos=e.llamadas[1][2];
    assert.equal(datos.estacionDevolucionId,1);assert.equal(datos.fallaReportada,false);assert.equal(datos.descripcionFalla,null);
});
test('reintegro pendiente se informa sin afirmar una acreditación',async()=>{
    const e=entorno();e.setPost(async()=>({data:{reembolsos:[{id:3,estado:'EN_PROCESO'}]}}));
    await e.ops.reintegrar(7);assert.match(e.avisos[0][1],/EN_PROCESO/);assert.doesNotMatch(e.avisos[0][1],/CONFIRMADO/);
});
test('moneda restaura expansiones por ID, sin recalcular dinero',()=>{
    const e=entorno();const nodos=[{dataset:{alquilerDetalles:'7'},open:true},{dataset:{alquilerDetalles:'8'},open:false}];
    e.contexto.document.querySelectorAll=s=>s.endsWith('[open]')?nodos.filter(n=>n.open):nodos;
    const abiertos=e.ops.abiertos();nodos.forEach(n=>n.open=false);e.ops.restaurarAbiertos(abiertos);
    assert.equal(nodos[0].open,true);assert.equal(nodos[1].open,false);assert.equal(e.llamadas.length,0);
});
const estacionesReserva=[
    {id:4,nombre:'Plaza Bélgica',direccion:'Av. San Martín 100',esPublica:true},
    {id:5,nombre:'Terminal Sur',direccion:'Calle Sarmiento 200',esPublica:true},
    {id:6,nombre:'Costanera',direccion:'Av. Perito Moreno 300',esPublica:true}
];
const bicicletasReserva=[
    {id:1,estacionId:4,codigoBicicleta:'MB-01',tipo:'MONTANA',precio:10,ubicacion:'Plaza Bélgica'},
    {id:2,estacionId:4,codigoBicicleta:'PA-01',tipo:'PASEO',precio:8,ubicacion:'Plaza Bélgica'},
    {id:3,estacionId:5,codigoBicicleta:'EL-01',tipo:'ELECTRICA',precio:12,ubicacion:'Terminal Sur'}
];
async function entornoReserva({busqueda='',bicis=bicicletasReserva,cotizacion=1530}={}) {
    const e=entorno();
    const html=fs.readFileSync(path.join(carpeta,'alquiler.html'),'utf8');
    const valores={'duracion-reserva':'90','filtro-tipo':'todos'};
    for(const [atributos,id] of html.matchAll(/<[^>]*\bid="([^"]+)"[^>]*>/g)){
        e.elementos.set(id,{
            value:valores[id]??'',checked:false,hidden:/\bhidden\s*>/.test(atributos),
            disabled:/\bdisabled\s*>/.test(atributos),innerHTML:'',textContent:'',events:{},dataset:{},
            classList:{toggle(){}},setAttribute(){},removeAttribute(){},focus(){this.recibioFoco=true;},
            reportValidity(){
                const minutos=Number(this.value);
                return Number.isInteger(minutos) && minutos>=1 && minutos<=35791394;
            },
            scrollIntoView({block}){this.bloqueDesplazado=block;},querySelector:()=>null,querySelectorAll:()=>[],
            addEventListener(nombre,accion){this.events[nombre]=accion}
        });
    }
    const campo=id=>e.elementos.get(id)??null;
    const eventos={};
    Object.assign(e.contexto,{
        URLSearchParams,requerirSesion:async()=>({id:1,roles:['CLIENTE']}),cargarNombreUsuario:async()=>{},
        location:Object.assign(e.contexto.window.location,{search:busqueda})
    });
    Object.assign(e.contexto.document,{
        addEventListener:(nombre,accion)=>{eventos[nombre]=accion},getElementById:campo,querySelector:()=>null
    });
    e.setGet(async ruta=>{
        if(ruta==='/api/estaciones') return {data:estacionesReserva};
        if(ruta==='/api/bicicletas/disponibles') return {data:bicis};
        if(ruta==='/api/cotizacion') {
            if(cotizacion===null) throw new Error('Cotización no disponible');
            return {data:cotizacion};
        }
        throw new Error('Ruta imprevista: '+ruta);
    });
    e.setPost(async()=>({data:{id:17}}));
    vm.runInContext(fuente('alquiler.js'),e.contexto);
    await eventos.DOMContentLoaded();
    return {...e,campo,catalogo:()=>campo('estaciones-container').innerHTML,
        grilla:()=>campo('bicicletas-container').innerHTML,
        solicitud:()=>JSON.parse(JSON.stringify(e.contexto.solicitudReserva())),
        posts:()=>e.llamadas.filter(c=>c[0]==='POST'),
        elegir:id=>e.contexto.elegirEstacion(id),seleccionar:id=>e.contexto.seleccionarBicicleta(id)};
}

test('el enlace del mapa abre automáticamente solo las bicicletas de su estación',async()=>{
    const e=await entornoReserva({busqueda:'?estacionId=5'});
    assert.match(e.campo('estacion-elegida').textContent,/Terminal Sur/);
    assert.equal(e.campo('seleccion-bicicletas').hidden,false);
    assert.equal(e.campo('titulo-bicicletas').bloqueDesplazado,'start');
    assert.equal(e.campo('titulo-bicicletas').recibioFoco,undefined);
    assert.equal(e.campo('estacion-elegida').recibioFoco,undefined);
    assert.match(e.grilla(),/EL-01/);
    assert.doesNotMatch(e.grilla(),/MB-01|PA-01/);
    assert.equal(e.campo('crear-reserva').disabled,true);
    assert.equal(e.posts().length,0);
});

test('un identificador inexistente o inválido no oculta el listado ni elige otra estación',async()=>{
    for(const busqueda of ['', '?estacionId=999', '?estacionId=4abc']){
        const e=await entornoReserva({busqueda});
        assert.equal(e.campo('seleccion-bicicletas').hidden,true);
        assert.equal(e.campo('titulo-bicicletas').bloqueDesplazado,undefined);
        assert.match(e.catalogo(),/Plaza Bélgica/);
        assert.match(e.catalogo(),/Terminal Sur/);
        assert.equal(e.posts().length,0);
    }
});

test('elegir estación baja al paso 2 sin enfocar su nombre y conserva la navegación por teclado',async()=>{
    for(const detalleClick of [1,0]){
        const e=await entornoReserva();
        await e.campo('estaciones-container').events.click({
            detail:detalleClick,target:{closest:()=>({dataset:{estacionId:'5'}})}
        });
        assert.equal(e.campo('seleccion-bicicletas').hidden,false);
        assert.equal(e.campo('titulo-bicicletas').bloqueDesplazado,'start');
        assert.equal(Boolean(e.campo('titulo-bicicletas').recibioFoco),detalleClick===0);
        assert.equal(e.campo('estacion-elegida').recibioFoco,undefined);
    }
});

test('la búsqueda de estaciones acepta nombre o dirección sin acentos y muestra disponibilidad',async()=>{
    const e=await entornoReserva();
    const buscar=e.campo('buscar-estacion');
    const buscarPor=valor=>{buscar.value=valor;buscar.events.input();};
    buscarPor('belgica');
    assert.match(e.catalogo(),/Plaza Bélgica/);
    assert.match(e.catalogo(),/2\s+bici/i);
    assert.doesNotMatch(e.catalogo(),/Terminal Sur/);
    buscarPor('san martin');
    assert.match(e.catalogo(),/San Martín 100/);
    assert.doesNotMatch(e.catalogo(),/Calle Sarmiento/);
    buscarPor('sarmiento');
    assert.match(e.catalogo(),/Terminal Sur/);
    assert.match(e.catalogo(),/1\s+bici/i);
    assert.doesNotMatch(e.catalogo(),/Plaza Bélgica/);
    buscarPor('lugar inexistente');
    assert.doesNotMatch(e.catalogo(),/Plaza Bélgica|Terminal Sur|Costanera/);
    assert.equal(e.llamadas.filter(c=>c[0]==='GET').length,3);
});

test('selección no mezcla estaciones, respeta hasta diez unidades y conserva el contrato v5',async()=>{
    const bicis=[...bicicletasReserva,...Array.from({length:9},(_,i)=>({
        ...bicicletasReserva[0],id:i+10,codigoBicicleta:'MB-'+(i+10)
    }))];
    const e=await entornoReserva({busqueda:'?estacionId=4',bicis});
    e.seleccionar(1);e.seleccionar(2);e.seleccionar(3);
    assert.deepEqual(e.solicitud(),{estacionRetiroId:4,bicicletaIds:[1,2],duracionEstimadaSegundos:5400});
    for(let id=10;id<=18;id++)e.seleccionar(id);
    const unidades=e.solicitud().bicicletaIds;
    assert.equal(unidades.length,10);
    assert.equal(unidades.includes(3),false);
    e.campo('duracion-reserva').value='0';
    assert.throws(()=>e.solicitud(),/duración/i);
});

test('al cambiar de estación, cancelar conserva las bicicletas y confirmar limpia la selección',async()=>{
    const e=await entornoReserva({busqueda:'?estacionId=4'});
    e.seleccionar(1);
    e.respuestas.push({isConfirmed:false});
    await e.elegir(5);
    assert.equal(e.avisos.at(-1)[0].cancelButtonText,'Conservar selección');
    assert.match(e.campo('estacion-elegida').textContent,/Plaza Bélgica/);
    assert.deepEqual(e.solicitud().bicicletaIds,[1]);
    e.respuestas.push({isConfirmed:true});
    await e.elegir(5);
    assert.match(e.campo('estacion-elegida').textContent,/Terminal Sur/);
    assert.equal(e.campo('crear-reserva').disabled,true);
    assert.throws(()=>e.solicitud(),/bicicleta/i);
    assert.equal(e.posts().length,0);
});

test('el resumen muestra estación, códigos, duración y total antes del POST; volver no escribe',async()=>{
    const e=await entornoReserva({busqueda:'?estacionId=4'});
    e.seleccionar(1);e.seleccionar(2);
    e.campo('switch-moneda').checked=true;
    e.respuestas.push({isConfirmed:false});
    await e.contexto.confirmarReserva();
    const modal=e.avisos.at(-1)[0];
    assert.equal(modal.title,'Confirmar reserva');
    assert.equal(modal.cancelButtonText,'Volver a editar');
    for(const contenido of [/Plaza Bélgica/,/MB-01/,/PA-01/,/90\s*(?:min|minutos)/,/41\.310,00/])
        assert.match(modal.html,contenido);
    assert.equal(e.posts().length,0);
    assert.equal(e.campo('crear-reserva').disabled,false);
    await e.contexto.confirmarReserva();
    assert.equal(e.posts().length,1);
    assert.equal(e.posts()[0][1],'/api/alquileres');
    assert.deepEqual(JSON.parse(JSON.stringify(e.posts()[0][2])),{
        estacionRetiroId:4,bicicletaIds:[1,2],duracionEstimadaSegundos:5400
    });
});

test('una duración inválida no abre el resumen ni envía la reserva',async()=>{
    const e=await entornoReserva({busqueda:'?estacionId=4'});
    e.seleccionar(1);
    e.campo('duracion-reserva').value='1.5';
    await e.contexto.confirmarReserva();
    assert.equal(e.avisos.length,0);
    assert.equal(e.posts().length,0);
    assert.equal(e.campo('crear-reserva').disabled,false);
});

test('sin cotización el resumen no inventa importes en pesos y conserva el estimado en dólares',async()=>{
    const e=await entornoReserva({busqueda:'?estacionId=4',cotizacion:null});
    e.seleccionar(1);e.seleccionar(2);
    e.respuestas.push({isConfirmed:false});
    await e.contexto.confirmarReserva();
    const resumen=e.avisos.at(-1)[0].html;
    assert.match(resumen,/US\$\s*27,00/);
    assert.match(resumen,/Referencia en ARS no disponible/i);
    assert.doesNotMatch(resumen,/AR\$\s*[\d.]+,\d{2}/);
    assert.equal(e.posts().length,0);
});
test('ambas páginas cargan operaciones después de vista y antes del consumidor',()=>{
    for (const [html,js] of [['mis_alquileres.html','mis_alquileres.js'],['admin.html','admin.js']]) {
        const texto=fs.readFileSync(path.join(carpeta,html),'utf8');
        assert.ok(texto.indexOf('js/alquileres-vista.js')<texto.indexOf('js/alquileres-operaciones.js'));
        assert.ok(texto.indexOf('js/alquileres-operaciones.js')<texto.indexOf('js/'+js));
    }
});

test('historial se ofrece a cliente y admin sin consultas anticipadas',()=>{
    const e=entorno();
    assert.match(e.ops.acciones(alquiler),/historial\(7\)/);
    e.ops.configurar({id:2,roles:['ADMIN']},async()=>{},true);
    assert.match(e.ops.acciones(alquiler,true),/historial\(7\)/);
    assert.equal(e.llamadas.length,0);
});
test('historial consulta una vez y presenta creación, transiciones y actor sin HTML recibido',async()=>{
    const e=entorno();
    e.setGet(async()=>({data:[{id:1,alquilerId:7,estadoAnterior:null,estadoNuevo:'RESERVADO',
        actorUsuarioId:1,actorNickname:'<img src=x onerror=alert(1)>',origen:'USUARIO',
        observaciones:'<script>dato</script>',fechaHoraCambio:'2026-11-18T12:00:00'},
        {id:2,alquilerId:7,estadoAnterior:'RESERVADO',estadoNuevo:'VENCIDO',origen:'SISTEMA',
            actorUsuarioId:null,actorNickname:null,fechaHoraCambio:'2026-11-18T12:15:00'}]}));
    await e.ops.historial(7);
    assert.deepEqual(e.llamadas,[['GET','/api/alquileres/7/historial']]);
    const modal=e.avisos[0][0];
    assert.match(modal.html,/Creación/);assert.match(modal.html,/Vencido/);
    assert.match(modal.html,/Proceso automático del sistema/);
    assert.match(modal.html,/&lt;script&gt;/);assert.doesNotMatch(modal.html,/<img|<script>/);
    assert.equal(e.recargas(),0);
});
test('historial vacío no fabrica movimientos',async()=>{
    const e=entorno();await e.ops.historial(7);
    assert.match(e.avisos[0][0].html,/No hay movimientos registrados/);
    assert.doesNotMatch(e.avisos[0][0].html,/<li/);
});
test('historial inválido o de otro alquiler no se presenta como éxito',async()=>{
    for(const data of [{eventos:[]},[null],[{alquilerId:8,estadoNuevo:'RESERVADO',origen:'USUARIO'}],
            [{alquilerId:7,estadoNuevo:'SALDADO',origen:'SISTEMA'}]]) {
        const e=entorno();e.setGet(async()=>({data}));await e.ops.historial(7);
        assert.equal(e.avisos[0][0],'No se pudo completar');assert.equal(e.avisos[0][2],'error');
    }
});
test('historial respeta 401/403 y no reintenta ni escribe',async()=>{
    for(const status of [401,403]) {
        const e=entorno();e.setGet(async()=>{throw {response:{status}}});await e.ops.historial(7);
        assert.equal(e.llamadas.length,1);assert.equal(e.avisos.length,0);assert.equal(e.recargas(),0);
    }
});
test('doble clic en historial no duplica GET; una apertura posterior consulta datos frescos',async()=>{
    const e=entorno();let resolver;
    e.setGet(()=>new Promise(r=>{resolver=r}));
    const consulta=e.ops.historial(7);await e.ops.historial(7);assert.equal(e.llamadas.length,1);
    resolver({data:[]});await consulta;
    e.setGet(async()=>({data:[]}));await e.ops.historial(7);assert.equal(e.llamadas.length,2);
    assert.equal(e.recargas(),0);
});
