const { API_BASE_URL } = require('./config');
const sesion = require('./sesion');

// Cliente HTTP genérico basado en fetch (nativo desde Node 18+).
// Centraliza la URL base, el header Authorization y el manejo de errores
// para que los servicios (auth, candidatos) no repitan ese código.
async function peticion(metodo, ruta, cuerpo) {
  const opciones = {
    method: metodo,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
  };

  const token = sesion.getToken();
  if (token) {
    opciones.headers.Authorization = `Bearer ${token}`;
  }

  if (cuerpo !== undefined) {
    opciones.body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(`${API_BASE_URL}${ruta}`, opciones);
  } catch (err) {
    throw new Error(
      `No se pudo conectar con la API en ${API_BASE_URL}. ¿Está corriendo "php artisan serve"? (${err.message})`
    );
  }

  const texto = await respuesta.text();
  let datos = null;
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      datos = texto;
    }
  }

  if (!respuesta.ok) {
    const mensaje =
      (datos && (datos.mensaje || datos.message)) ||
      `Error HTTP ${respuesta.status}`;
    const error = new Error(mensaje);
    error.status = respuesta.status;
    error.datos = datos;
    throw error;
  }

  return datos;
}

module.exports = {
  get: (ruta) => peticion('GET', ruta),
  post: (ruta, cuerpo) => peticion('POST', ruta, cuerpo),
  put: (ruta, cuerpo) => peticion('PUT', ruta, cuerpo),
  del: (ruta) => peticion('DELETE', ruta),
};
