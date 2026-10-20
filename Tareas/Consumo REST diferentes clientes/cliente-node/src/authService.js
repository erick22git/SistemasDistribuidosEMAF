const api = require('./apiClient');

// Consume POST /api/login de elecicion-api.
// Devuelve { mensaje, token, type, expires, usuario }.
async function login(email, password) {
  return api.post('/login', { email, password });
}

module.exports = { login };
