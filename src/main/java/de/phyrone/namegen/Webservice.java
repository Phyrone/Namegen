package de.phyrone.namegen;

import picocli.CommandLine;
import spark.Spark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Webservice implements Runnable {


    private static final File NAMEFILE = new File("names.txt");
    @CommandLine.Option(names = {"-e", "--use-env"})
    boolean useEnv = false;
    List<String> names = new ArrayList<>();
    @CommandLine.Option(names = {"-h", "--host"})
    String host = "0.0.0.0";
    @CommandLine.Option(names = {"-p", "--port"})
    int port = 8080;
    String html = getHtml();
    Random random = new Random();

    public static void main(String[] args) {
        CommandLine.run(new Webservice(), System.out, args);
    }

    private void getNames() {
        System.out.println("Loading Names...");
        List<String> ret = new ArrayList<>();
        if (useEnv) {
            ret = new ArrayList<>(
                    Arrays.asList(System.getenv("RANDOMNAMES")
                            .replace(',', ';')
                            .split(";")));
        } else {
            try {
                if (NAMEFILE.exists()) {
                    Scanner scanner = new Scanner(new FileInputStream(NAMEFILE));
                    while (scanner.hasNextLine()) {
                        ret.addAll(Arrays.asList(scanner.nextLine().split(",")));
                    }
                    scanner.close();
                } else {
                    if (NAMEFILE.createNewFile()) {
                        System.out.println("names.txt Created -> fill and restart it!");
                        System.exit(0);
                    } else {
                        System.err.println("names.txt failed to create -> made it manually");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (ret.isEmpty()) {
            System.err.println("Error: Name's is Empty -> Stopping Server");
            System.exit(1);
        }
        names.clear();
        ret.forEach(s -> {
            if (s != null && !s.isEmpty()) {
                s = s.replace(" ", "");
                names.add(s.substring(0, 1).toUpperCase() + s.substring(1));
            }
        });
    }

    private String getHtml() {
        System.out.println("Loading Html...");
        Scanner scanner = new Scanner(Webservice.class.getResourceAsStream("/files/template.html"));
        StringBuilder ret = new StringBuilder();
        while (scanner.hasNextLine()) {
            ret.append(scanner.nextLine()).append("\n");
        }
        scanner.close();
        return ret.toString();
    }

    public void run() {
        getNames();
        System.out.println("Starting Webservice...");
        Spark.ipAddress(host);
        Spark.port(port);
        Spark.threadPool(10);
        Spark.staticFiles.location("/files/web/");
        Spark.get("/", (req, res) -> {
            res.redirect("/2/");
            return null;
        });

        Spark.get("/:number/", (request, response) -> {
            response.header("Content-Type", "text/html; charset=utf-8");
            return html.replace("EXAMPLENAME", generateName(request.params(":number")));
        });
        Spark.get("/:number/raw", (request, response) -> {
            response.header("Content-Type","text/plain; charset=utf-8");
            return generateName(request.params(":number"));
        });
        Spark.get("/:number/json", (request, response) -> {
            response.header("Content-Type", "application/json; charset=utf-8");
            return "{\n  \"name\": \"" + generateName(request.params(":number")) + "\"\n}";
        });
        Spark.init();
    }

    String generateName(String stringNumber) {
        int number = 2;
        try {
            number = Integer.parseInt(stringNumber);
        } catch (NumberFormatException ignored) {
        }
        StringBuilder ret = new StringBuilder();
        for (int i = 0; number > i; i++) {
            ret.append(names.get(random.nextInt(names.size())));
        }
        return ret.toString();
    }
}
