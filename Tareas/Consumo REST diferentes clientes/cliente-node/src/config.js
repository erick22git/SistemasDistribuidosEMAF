const fs = require('fs');
const path = require('path');

function loadEnvFile() {
  const envPath = path.join(__dirname, '..', '.env');
  if (!fs.existsSync(envPath)) return;

  const contenido = fs.readFileSync(envPath, 'utf-8');
  contenido.split(/\r?\n/).forEach((linea) => {
    const limpia = linea.trim();
    if (!limpia || limpia.startsWith('#')) return;
    const idx = limpia.indexOf('=');
    if (idx === -1) return;
    const clave = limpia.slice(0, idx).trim();
    const valor = limpia.slice(idx + 1).trim();
    if (!(clave in process.env)) {
      process.env[clave] = valor;
    }
  });
}

loadEnvFile();

module.exports = {
  API_BASE_URL: process.env.API_BASE_URL || 'http://127.0.0.1:8000/api',
};
