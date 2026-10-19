<?php

namespace App\Http\Controllers;

use App\Models\Candidato;
use Illuminate\Http\Request;

class CandidatoController extends Controller
{
    /**
     * Display a listing of the resource.
     */
    public function index()
    {
        $candidatos = Candidato::all();
        return response()->json($candidatos, 200);
    }

    /**
     * Store a newly created resource in storage.
     */
    public function store(Request $request)
    {
       if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato = Candidato::create($request->all());
        return response()->json($candidato, 201);
    }

    /**
     * Display the specified resource.
     */
    public function show(Candidato $candidato)
    {
        return response()->json($candidato, 200);
    }

    /**
     * Update the specified resource in storage.
     */
    public function update(Request $request, Candidato $candidato)
    {
         if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato->update($request->all());
        return response()->json($candidato, 200);
    }

    /**
     * Remove the specified resource from storage.
     */
    public function destroy(Request $request, Candidato $candidato)
    {
        if ($request->attributes->get('user')->role !== 'admin') {
            return response()->json(['message' => 'Acceso denegado'], 403);
        }
        $candidato->delete();
        return response()->json($candidato, 204);
    }
}
