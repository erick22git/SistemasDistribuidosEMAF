<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\CandidatoController;
use App\Http\Controllers\LoginController;
use App\Http\Middleware\JwtMiddleware;


Route::apiResource('candidatos', CandidatoController::class)->middleware(JwtMiddleware::class);   
Route::post('/login', [LoginController::class, 'login']);

