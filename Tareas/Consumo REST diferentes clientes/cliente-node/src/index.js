const readline = require('readline');
const authService = require('./authService');
const candidatoService = require('./candidatoService');
const sesion = require('./sesion');
const { API_BASE_URL } = require('./config');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

// Cola de líneas propia (en vez de encadenar rl.question): si se usa
// rl.question() de forma secuencial con await de por medio (ej. llamadas
// a la API entre una pregunta y la siguiente), las líneas que llegan
// mientras no hay una pregunta pendiente se pierden al venir la entrada
// por pipe/redirección. Con un listener fijo sobre 'line' no se pierde
// ninguna línea, sea entrada interactiva o redirigida.
const colaLineas = [];
let resolverPendiente = null;

rl.on('line', (linea) => {
  const limpia = linea.trim();
  if (resolverPendiente) {
    const resolver = resolverPendiente;
    resolverPendiente = null;
    resolver(limpia);
  } else {
    colaLineas.push(limpia);
  }
});

function preguntar(texto) {
  process.stdout.write(texto);
  if (colaLineas.length > 0) {
    return Promise.resolve(colaLineas.shift());
  }
  return new Promise((resolve) => {
    resolverPendiente = resolve;
  });
}

async function pantallaLogin() {
  console.log('\n=== Iniciar sesión ===');
  const email = await preguntar('Email: ');
  const password = await preguntar('Password: ');

  try {
    const respuesta = await authService.login(email, password);
    sesion.iniciarSesion(respuesta.token, respuesta.usuario);
    console.log(`\nBienvenido/a, ${respuesta.usuario.name} (rol: ${respuesta.usuario.role}).`);
    console.log(`Token válido hasta: ${new Date(respuesta.expires * 1000).toLocaleString()}`);
  } catch (err) {
    console.log(`\nNo se pudo iniciar sesión: ${err.message}`);
  }
}

function mostrarCandidato(c) {
  console.log(
    `  #${c.id} | ${c.nombres} ${c.apellidos} | Cargo: ${c.cargo} | Ámbito: ${c.ambito}`
  );
}

async function listarCandidatos() {
  console.log('\n=== Listado de Personas (Candidatos) ===');
  try {
    const candidatos = await candidatoService.listar();
    if (!candidatos.length) {
      console.log('  (sin registros)');
    } else {
      candidatos.forEach(mostrarCandidato);
    }
  } catch (err) {
    console.log(`Error al listar: ${err.message}`);
  }
}

async function verCandidato() {
  const id = await preguntar('ID de la persona/candidato: ');
  try {
    const candidato = await candidatoService.obtener(id);
    mostrarCandidato(candidato);
  } catch (err) {
    console.log(`Error al obtener: ${err.message}`);
  }
}

async function pedirDatosCandidato() {
  const nombres = await preguntar('Nombres: ');
  const apellidos = await preguntar('Apellidos: ');
  const cargo = await preguntar('Cargo al que postula: ');
  const ambito = await preguntar('Ámbito (nacional/departamental/municipal): ');
  return { nombres, apellidos, cargo, ambito };
}

async function crearCandidato() {
  console.log('\n=== Registrar nueva persona/candidato ===');
  const datos = await pedirDatosCandidato();
  try {
    const creado = await candidatoService.crear(datos);
    console.log('Creado correctamente:');
    mostrarCandidato(creado);
  } catch (err) {
    console.log(`Error al crear: ${err.message}`);
  }
}

async function actualizarCandidato() {
  console.log('\n=== Actualizar persona/candidato ===');
  const id = await preguntar('ID a actualizar: ');
  const datos = await pedirDatosCandidato();
  try {
    const actualizado = await candidatoService.actualizar(id, datos);
    console.log('Actualizado correctamente:');
    mostrarCandidato(actualizado);
  } catch (err) {
    console.log(`Error al actualizar: ${err.message}`);
  }
}

async function eliminarCandidato() {
  console.log('\n=== Eliminar persona/candidato ===');
  const id = await preguntar('ID a eliminar: ');
  const confirmar = await preguntar(`¿Confirma eliminar el registro #${id}? (s/n): `);
  if (confirmar.toLowerCase() !== 's') {
    console.log('Operación cancelada.');
    return;
  }
  try {
    await candidatoService.eliminar(id);
    console.log('Eliminado correctamente.');
  } catch (err) {
    console.log(`Error al eliminar: ${err.message}`);
  }
}

function mostrarMenu() {
  console.log('\n==============================================');
  console.log(' Cliente Node.js - elecicion-api (REST + JWT)');
  console.log(` API: ${API_BASE_URL}`);
  console.log(` Sesión: ${sesion.estaAutenticado() ? sesion.getUsuario().email : 'sin iniciar'}`);
  console.log('==============================================');
  console.log('1. Iniciar sesión (login)');
  console.log('2. Listar personas/candidatos');
  console.log('3. Ver una persona/candidato por ID');
  console.log('4. Crear persona/candidato');
  console.log('5. Actualizar persona/candidato');
  console.log('6. Eliminar persona/candidato');
  console.log('7. Cerrar sesión');
  console.log('0. Salir');
}

async function main() {
  console.log('Cliente de consola en Node.js para consumir elecicion-api.');
  let salir = false;

  while (!salir) {
    mostrarMenu();
    const opcion = await preguntar('\nSeleccione una opción: ');

    switch (opcion) {
      case '1':
        await pantallaLogin();
        break;
      case '2':
        if (!sesion.estaAutenticado()) {
          console.log('\nDebe iniciar sesión primero (la API protege /candidatos con JWT).');
        } else {
          await listarCandidatos();
        }
        break;
      case '3':
        if (!sesion.estaAutenticado()) {
          console.log('\nDebe iniciar sesión primero.');
        } else {
          await verCandidato();
        }
        break;
      case '4':
        if (!sesion.estaAutenticado()) {
          console.log('\nDebe iniciar sesión primero.');
        } else {
          await crearCandidato();
        }
        break;
      case '5':
        if (!sesion.estaAutenticado()) {
          console.log('\nDebe iniciar sesión primero.');
        } else {
          await actualizarCandidato();
        }
        break;
      case '6':
        if (!sesion.estaAutenticado()) {
          console.log('\nDebe iniciar sesión primero.');
        } else {
          await eliminarCandidato();
        }
        break;
      case '7':
        sesion.cerrarSesion();
        console.log('\nSesión cerrada.');
        break;
      case '0':
        salir = true;
        break;
      default:
        console.log('\nOpción no válida.');
    }
  }

  console.log('\nHasta luego.');
  rl.close();
}

main();
