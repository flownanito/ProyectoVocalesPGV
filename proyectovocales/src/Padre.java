import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Padre {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java Padre <fichero> [numHijos]");
            System.exit(1);
        }

        Paths.get(".");
        List<Process> hijos = new ArrayList<>();
        List<String> nombresResultados = new ArrayList<>();

        // Lanzar un hijo por cada fichero
        for (int i = 0; i < args.length; i++) {
            String fichero = args[i];
            String id = String.valueOf(i + 1);
            Path pathFichero = Paths.get(fichero);

            // Determinar número de líneas para calcular fin
            int totalLineas = contarLineas(pathFichero);
            int inicio = 0;
            int fin = totalLineas - 1;
            String ficheroSalida = "resultado-" + id + ".res";
            nombresResultados.add(ficheroSalida);

            List<String> comando = new ArrayList<>();
            comando.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
            comando.add("-cp");
            comando.add(System.getProperty("java.class.path"));
            comando.add("Hijo");
            comando.add(fichero);
            comando.add(String.valueOf(inicio));
            comando.add(String.valueOf(fin));
            comando.add(ficheroSalida);

            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);


            try {
                Process p = pb.start();
                hijos.add(p);
                System.out.printf("Padre: hijo %s lanzado para procesar %s%n", id, fichero);
            } catch (IOException e) {
                System.err.printf("Padre: error iniciando hijo para %s -> %s%n", fichero, e.getMessage());
            }
        }

        // Esperar a que todos los hijos terminen
        for (int i = 0; i < hijos.size(); i++) {
            try {
                int exit = hijos.get(i).waitFor();
                System.out.printf("Padre: hijo %d terminó con código %d%n", i + 1, exit);
            } catch (InterruptedException e) {
                System.err.println("Padre: espera interrumpida.");
                Thread.currentThread().interrupt();
            }
        }

        // Leer resultados y mostrar informe final
        int totalPalabras = 0;
        int totalVocales = 0;

        System.out.println("\n--- Resultados Parciales ---");
        for (int i = 0; i < nombresResultados.size(); i++) {
            String fichero = args[i];
            String resultado = nombresResultados.get(i);
            Path pathRes = Paths.get(resultado);

            if (!Files.exists(pathRes)) {
                System.err.printf("Padre: no existe el resultado del hijo %d (%s)%n", i + 1, resultado);
                continue;
            }

            int palabras = 0;
            int vocales = 0;
            try (BufferedReader br = Files.newBufferedReader(pathRes, StandardCharsets.UTF_8)) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.startsWith("palabras=")) {
                        palabras = Integer.parseInt(linea.substring("palabras=".length()).trim());
                    } else if (linea.startsWith("vocales=")) {
                        vocales = Integer.parseInt(linea.substring("vocales=".length()).trim());
                    }
                }
            } catch (IOException e) {
                System.err.printf("Padre: error leyendo %s -> %s%n", pathRes, e.getMessage());
            }

            System.out.printf("Archivo %s -> Palabras: %d | Vocales: %d%n", fichero, palabras, vocales);
            totalPalabras += palabras;
            totalVocales += vocales;
        }

        System.out.println("\n--- Informe Final ---");
        System.out.printf("Total palabras procesadas: %d%n", totalPalabras);
        System.out.printf("Total vocales encontradas: %d%n", totalVocales);

        if (totalPalabras > 0) {
            double promedio = (double) totalVocales / totalPalabras;
            System.out.printf("Promedio de vocales por palabra: %.3f%n", promedio);
        } else {
            System.out.println("Promedio de vocales por palabra: N/A (no se procesaron palabras)");
        }
    }

    /**
     * Cuenta cuántas líneas tiene un fichero (para definir el índice final).
     */
    private static int contarLineas(Path fichero) {
        try (BufferedReader br = Files.newBufferedReader(fichero, StandardCharsets.UTF_8)) {
            int count = 0;
            while (br.readLine() != null) count++;
            return count;
        } catch (IOException e) {
            System.err.printf("Error contando líneas en %s -> %s%n", fichero, e.getMessage());
            return 0;
        }
    }
}