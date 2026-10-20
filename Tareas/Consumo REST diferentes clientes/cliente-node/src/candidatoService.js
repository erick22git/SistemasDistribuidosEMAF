const api = require('./apiClient');

// CRUD contra el recurso /api/candidatos de elecicion-api.
// En este API el modelo "Persona" que pide la tarea corresponde
// a la entidad Candidato (nombres, apellidos, cargo, ambito).

const listar = () => api.get('/candidatos');

const obtener = (id) => api.get(`/candidatos/${id}`);

const crear = (candidato) => api.post('/candidatos', candidato);

const actualizar = (id, candidato) => api.put(`/candidatos/${id}`, candidato);

const eliminar = (id) => api.del(`/candidatos/${id}`);

module.exports = { listar, obtener, crear, actualizar, eliminar };
