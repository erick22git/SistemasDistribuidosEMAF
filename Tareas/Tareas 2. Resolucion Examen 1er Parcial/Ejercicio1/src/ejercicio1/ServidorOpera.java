/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ejercicio1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author USUARIO
 */
public class ServidorOpera {

    public static void main(String[] args) throws Exception {
        Registry registro = LocateRegistry.createRegistry(1098);
        registro.rebind("Operador", new Operador());
        System.out.println("Servidor Operador listo en el puerto 1098");
    }

}
