// Guarda en memoria el token JWT y los datos del usuario autenticado
// durante la ejecución del cliente de consola.

let token = null;
let usuario = null;

function iniciarSesion(nuevoToken, nuevoUsuario) {
  token = nuevoToken;
  usuario = nuevoUsuario;
}

function cerrarSesion() {
  token = null;
  usuario = null;
}

function getToken() {
  return token;
}

function getUsuario() {
  return usuario;
}

function estaAutenticado() {
  return Boolean(token);
}

module.exports = {
  iniciarSesion,
  cerrarSesion,
  getToken,
  getUsuario,
  estaAutenticado,
};
