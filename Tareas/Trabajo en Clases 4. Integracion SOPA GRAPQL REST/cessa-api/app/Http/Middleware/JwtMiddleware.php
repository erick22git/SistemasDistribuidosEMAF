<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;


class JwtMiddleware
{
    /**
     * Handle an incoming request.
     *
     * @param  Closure(Request): (Response)  $next
     */
    public function handle(Request $request, Closure $next)
    {
        try {

            $autorizacion = $request->header('Authorization');
            $jwt = substr($autorizacion, 7);
            $key = env('JWT_SECRET');
            $algoritmo = env('JWT_ALGORITHM');

            $datos = JWT::decode($jwt,  new Key($key, $algoritmo));

            $request->attributes->add(['user' => $datos->data]);

        } catch (\Exception $e) {

            return response()->json(['status' => 'No autorizado ' . $e->getMessage()], 401);
        }
          return $next($request);
        
    }
}
