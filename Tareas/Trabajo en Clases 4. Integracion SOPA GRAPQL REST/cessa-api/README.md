## habilitar apis 
php artisan install:api

## hacer correr las migraciones

php artisan migrate

## Crear modelo y migracion, seeder, factory y controller para api

php artisan make:model Candidato -msfc --api

## Luego de crear el modelo

### en la migracion colocar los campos

## CRear los seeders 
no olivdar enlazar los seeders al databaseseeder

## hacer correr las migraciones y los seeders

php artisan migrate:fresh --seed

## hacer correr la api
php artisan serve

## Poner la rua en api.php
Route::apiResource('candidatos', CandidatoController::class);   

## Instalar libreria
composer require firebase/php-jwt

# Escribir codigo en el archivo .env
JWT_SECRET=secret
JWT_ALGORITHM=HS256

# Crear el midleware 
php artisan make:middleware JwtMiddleware

# Escribir codigo del midleware
 escribir elcodigo para leer el token  
# Proteger las rutasincluyendo a las que sean necesarias
->middleware(JwtMiddleware::class)
# crear un Controlador para el login
php artisan make:controller LoginController