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
public class ServidorBanco {

    public static void main(String[] args) throws Exception {
        Registry registro = LocateRegistry.createRegistry(1099);
        registro.rebind("Banco", new Banco());
        System.out.println("Servidor Banco listo en el puerto 1099");
    }

}
