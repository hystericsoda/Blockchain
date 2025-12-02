package org.example;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException, ParseException, NoSuchAlgorithmException, InterruptedException {

        while (true) {

            int val = 0;
            System.out.println("\n######### BLOCKCHAIN #########");
            System.out.println("1) Agregar registro");
            System.out.println("2) Mostrar registros");
            System.out.println("3) Verificar registros");
            System.out.println("4) Terminar");
            System.out.print("\nOpción: ");

            try {
                val = scanner.nextInt();
            } catch (InputMismatchException _) { }
            scanner.nextLine();

            switch (val) {

                case 1: {
                    if (!verifyData()) {
                        addData();
                    }
                    Thread.sleep(2000);
                    continue;
                }
                case 2: {
                    showData();
                    Thread.sleep(2000);
                    continue;
                }
                case 3: {
                    verifyData();
                    Thread.sleep(2000);
                    continue;
                }
                case 4: {
                    System.exit(0);
                }
                default: {
                    System.out.println("Elige una opción válida!");
                    Thread.sleep(2000);
                }

            }

        }

    }

    private static void addData() throws IOException, ParseException, NoSuchAlgorithmException {

        System.out.println("\n--------- agregar datos nuevos ---------\n");

        double value;
        String msg;
        System.out.println("Ingresa nuevos datos");
        try {
            System.out.print("Monto: ");
            value = scanner.nextDouble();
            scanner.nextLine();
            System.out.print("Descripción: ");
            msg = scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Valores inválidos!");
            return;
        }

        // Tomar ID y HASH del último registro en Base de Datos
        JSONParser parser = new JSONParser();
        Reader reader = new FileReader("test.json");
        Object object = parser.parse(reader);

        JSONArray jsonArr = (JSONArray) object;
        JSONObject jsonObj = (JSONObject) jsonArr.getLast();

        long prevId = (long) jsonObj.get("id");
        String prevHash = jsonObj.get("hash").toString();

        // Asignar nuevos datos
        long id = prevId + 1;
        // long timestamp = System.currentTimeMillis();
        String timestamp = LocalDateTime.now().toString();

        String textToHash = (id + timestamp + value + msg + prevHash);

        System.out.println("TextToHash: " + textToHash);

        // Calcular HASH
        String hash = calculateHash(textToHash);

        // Añadir nuevos datos
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("timestamp", timestamp);
        jsonObject.put("prev_hash", prevHash);
        jsonObject.put("hash", hash);

        JSONArray data = new JSONArray();
        JSONObject objArr = new JSONObject();
        objArr.put("value", value);
        objArr.put("msg", msg);
        data.add(objArr);
        jsonObject.put("data", data);

        jsonArr.addLast(jsonObject);

        // Escribir nuevo objeto JSON en archivo de BD
        try (FileWriter fileWriter = new FileWriter("test.json")) {
            fileWriter.write(jsonArr.toJSONString());
            fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Value added: " + jsonArr.getLast());

    }

    private static void showData() throws IOException, ParseException {

        System.out.println("\n--------- mostrar registros ---------\n");

        // Tomar registros en archivo de BD
        JSONParser parser = new JSONParser();
        Reader reader = new FileReader("test.json");
        Object object = parser.parse(reader);

        JSONArray jsonArr = (JSONArray) object;
        System.out.println("Tamaño de consulta: " + jsonArr.size() + " registro/s");

        for (int i = jsonArr.size()-1; i >= 0; i--) {
            // Obtener índice/fila completa para valores independientes
            JSONObject jsonObj = (JSONObject) jsonArr.get(i);
            // Obtener valores de matrices dentro de índice
            JSONArray objArr = (JSONArray) jsonObj.get("data");
            JSONObject arrVal = (JSONObject) objArr.getFirst();

            System.out.print("ID: " + jsonObj.get("id").toString());
            System.out.print(", Monto: " + arrVal.get("value"));
            System.out.print(", Mensaje: " + arrVal.get("msg"));
            System.out.print(", Fecha: " + jsonObj.get("timestamp"));
            System.out.print(", PrevHash: " + jsonObj.get("prev_hash"));
            System.out.println(", Hash: " + jsonObj.get("hash"));
        }

    }

    private static boolean verifyData() throws IOException, ParseException, NoSuchAlgorithmException {

        System.out.println("\n--------- verificar base de datos ---------\n");

        // Tomar registros en archivo de BD
        JSONParser parser = new JSONParser();
        Reader reader = new FileReader("test.json");
        Object object = parser.parse(reader);

        JSONArray jsonArr = (JSONArray) object;

        System.out.println("Tamaño de consulta: " + jsonArr.size() + " registro/s");

        boolean altered = false;
        String prevHash = "";
        for (int i = (jsonArr.size()-1); i > 0; i--) {
            // Obtener índice/fila completa para valores independientes
            JSONObject jsonObj = (JSONObject) jsonArr.get(i);
            // Obtener valores de matrices dentro de índice
            JSONArray objArr = (JSONArray) jsonObj.get("data");
            JSONObject arrVal = (JSONObject) objArr.getFirst();

            // Almacenar Hash de registro posterior
            String nextPrevHash = prevHash;

            String id = jsonObj.get("id").toString();
            String timestamp = jsonObj.get("timestamp").toString();
            String value = arrVal.get("value").toString();
            String msg = arrVal.get("msg").toString();
            String currHash = jsonObj.get("hash").toString();
            prevHash = jsonObj.get("prev_hash").toString();

            String textToHash = (id + timestamp + value + msg + prevHash);
            String hash = calculateHash(textToHash);

            if (i != (jsonArr.size()-1)) {
                /*
                  Verificar el hash del registro actual con
                  el del registro posterior. Si equivale, continúa;
                  si no, retorna.
                 */
                if (!nextPrevHash.equals(hash)) {
                    System.out.print(ANSI_RED + "Altered! - ");
                    altered = true;
                } else {
                    System.out.print(ANSI_GREEN + "Passed!  - ");
                }
            } else {
                /* Omitir verificación de hash posterior
                   en primer registro. Verificar con hash
                   propio en su lugar **/
                if (!currHash.equals(hash)) {
                    System.out.print(ANSI_RED + "Altered! - ");
                    altered = true;
                } else {
                    System.out.print(ANSI_GREEN + "Passed!  - ");
                }
            }

            System.out.print(ANSI_RESET + "ID: " + jsonObj.get("id").toString());
            System.out.print(", Monto: " + arrVal.get("value"));
            System.out.print(", Mensaje: " + arrVal.get("msg"));
            System.out.print(", Timestamp: " + jsonObj.get("timestamp"));
            System.out.print(", PrevHash: " + jsonObj.get("prev_hash"));
            System.out.println(", Hash: " + jsonObj.get("hash"));

            // Retornar si se encuentran alteraciones
            if (altered) {
                System.out.println("Los datos han sido alterados en índice [" + i + "]!");
                return true;
            }

        }

        return false;

    }

    private static String calculateHash(String textToHash) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(textToHash.getBytes(StandardCharsets.UTF_8));

        return HexFormat.of().formatHex(hashBytes);
    }

}
