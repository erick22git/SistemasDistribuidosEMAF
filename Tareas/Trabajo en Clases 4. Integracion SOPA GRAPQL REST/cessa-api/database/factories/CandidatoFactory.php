<?php

namespace Database\Factories;

use App\Models\Candidato;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends Factory<Candidato>
 */
class CandidatoFactory extends Factory
{
    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        $ambitos=['Rector'=>'Universidad','ViceRector'=>'Universidad', 'Decano'=>'Facultad', 'Director'=>'Carrera'];
        $cargo = $this->faker->randomElement(['Rector', 'ViceRector', 'Decano', 'Director']);
        $ambito = $ambitos[$cargo];
        return [
            'nombres' => $this->faker->firstName(),
            'apellidos' => $this->faker->lastName(),
            'cargo' => $cargo,
            'ambito' => $ambito,
        ];
    }
}
