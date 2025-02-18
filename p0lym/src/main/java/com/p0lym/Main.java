package com.p0lym;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese la ruta del archivo con el código del robot: ");

        String filePath = scanner.nextLine(); // Ruta del archivo con el código del robot
        scanner.close();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            
            // Leer todo el archivo línea por línea
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            // Crear el lexer y parser con la entrada del archivo
            RobotLexerParser lexerParser = new RobotLexerParser(new StringReader(sb.toString()));
            ArrayList<RobotLexerParser.Token> tokens = lexerParser.lexer();

            // Ejecutar el parser con los tokens obtenidos
            boolean isValid = lexerParser.parser(tokens, null, null);
            System.out.println("¿Programa válido? " + (isValid ? "Sí" : "No"));

        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        }
    }
}