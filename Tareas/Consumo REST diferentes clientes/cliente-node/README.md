# Cliente Node.js — elecicion-api

Cliente de consola (CLI) en **Node.js puro** (sin frameworks, usando el `fetch` nativo de Node 18+)
que consume la API REST `elecicion-api` (Laravel + JWT): login y CRUD del recurso `candidatos`,
que en esta tarea representa el CRUD de **Personas**.

## Requisitos

- Node.js 18 o superior.
- La API `elecicion-api` corriendo (ver README de esa carpeta), por defecto en `http://127.0.0.1:8000`.

## Configuración

1. Copiar `.env.example` a `.env` y ajustar la URL si la API corre en otro host/puerto:

   ```
   API_BASE_URL=http://127.0.0.1:8000/api
   ```

2. Instalar dependencias (no hay dependencias externas, solo Node.js):

   ```
   npm install
   ```

## Ejecución

```
npm start
```

o directamente:

```
node src/index.js
```

Se muestra un menú interactivo:

```
1. Iniciar sesión (login)
2. Listar personas/candidatos
3. Ver una persona/candidato por ID
4. Crear persona/candidato
5. Actualizar persona/candidato
6. Eliminar persona/candidato
7. Cerrar sesión
0. Salir
```

## Usuarios de prueba (seeders de la API)

| email             | password   | role  |
|-------------------|------------|-------|
| test@example.com  | password   | admin |
| test2@example.com | password   | user  |

Solo el usuario con `role = admin` puede crear, actualizar o eliminar
(la API responde `403 Acceso denegado` si el rol no es admin).
Listar y ver por ID funcionan con cualquier usuario autenticado, porque
el middleware JWT protege todo el recurso `candidatos`.

## Estructura

```
cliente-node/
├── package.json
├── .env.example
└── src/
    ├── config.js            # Lee .env (API_BASE_URL)
    ├── sesion.js            # Guarda token/usuario en memoria
    ├── apiClient.js         # Wrapper fetch con header Authorization
    ├── authService.js       # POST /login
    ├── candidatoService.js  # CRUD /candidatos (GET, POST, PUT, DELETE)
    └── index.js             # Menú de consola (readline)
```
