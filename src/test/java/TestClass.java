import ooo.reindeer.tools.java.flowchart.DotBuilder;
import ooo.reindeer.tools.java.flowchart.FlowchartBuilder;
import ooo.reindeer.tools.java.flowchart.TreeBuilder;
import ooo.reindeer.tools.java.flowchart.TreeNode;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @ClassName TestClass
 * @Author songbailin
 * @Date 2020/6/4 09:04
 * @Version 1.0
 * @Description TODO
 */

public class TestClass {

    boolean reBuildSample = true;
    boolean force = false;

    private String load(String name) {
        try {
//            URL url = TestClass.class.getClassLoader().getResource(name);
            return new String(

                    Files.readAllBytes(Paths.get("src/test/resources/" + name)), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void saveDot(String name, String dot) {
        if (!reBuildSample) return;
        try {
            if (!force && Paths.get("src/test/resources/" + name + ".dot").toFile().exists()) return;
            Files.write(Paths.get("src/test/resources/" + name + ".dot"), dot.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() throws IOException {

//        Paths.get("src/test/resources/");
        Stream<Path> files = Files.walk(Paths.get("src/test/resources/"), 1, FileVisitOption.FOLLOW_LINKS);

        files
                .filter(path -> path.toString().endsWith(".cf"))
                .forEach(path -> {
                    try {
                        System.out.print("path = " + path);
                        System.out.flush();
                        TreeNode tree = TreeBuilder.parseString(load(path.toFile().getName()));
                        FlowchartBuilder flowchartBuilder = new DotBuilder();
                        String r = flowchartBuilder.builder(tree);
                        saveDot(path.toFile().getName(), r);
                        String sample = load(path.toFile().getName() + ".dot");
                        System.out.println(" parse ok");
                        assert r.trim().equals(sample.trim());
                    } catch (Exception e) {
                        System.out.println(" parse error");
                        e.printStackTrace(System.out);
                    }
                });

    }

    @Test
    public void testDebug() {

        force = true;
        reBuildSample=true;
        String cfString = "anonymous.cf";
        TreeNode tree = TreeBuilder.parseString(load(cfString));
        System.out.println("tree = " + tree);
        FlowchartBuilder flowchartBuilder = new DotBuilder();
        flowchartBuilder.setDebug(true);
        String r = flowchartBuilder.builder(tree);
//        saveDot(cfString, r);

        System.out.println(r);
    }

    @Test
    public void testFile() throws IOException {

//        force = true;
        reBuildSample=true;
        String cfString = "/Users/songbailin/IdeaProjects/queue/component-queue-priority/src/main/java/com/wisdom/ucp/component/queue/impl/priority/MessageQueue.java";
        TreeNode tree = TreeBuilder.parseFile(cfString,"poll");
        FlowchartBuilder flowchartBuilder = new DotBuilder();
//        flowchartBuilder.setDebug(true);
        String r = flowchartBuilder.builder(tree);
//        saveDot(cfString, r)
        System.out.println(r);
    }

    @Test
    public void testPart() throws IOException {

//        force = true;
        reBuildSample=true;
        String cfString =
                "server.createContext(\"/\", exchange -> {\n" +
                        "            System.out.println(\"HttpPath = \" + exchange.getRequestURI().getPath());\n" +
                        "            if (exchange.getRequestURI().getPath().isEmpty() || exchange.getRequestURI().getPath().equals(\"/\")) {\n" +
                        "                sendData(staticPath + \"/index.html\", exchange);\n" +
                        "            } else if (exchange.getRequestURI().getPath().equals(\"/api/parse/part/\")) {\n" +
                        "                if (exchange.getRequestMethod().equalsIgnoreCase(\"POST\")) {\n" +
                        "\n" +
                        "                    exchange.getResponseHeaders().set(\"Content-Type\", \"text/plain;charset=utf-8\");\n" +
                        "\n" +
                        "                    String postString = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);\n" +
                        "\n" +
                        "                    TreeNode tree = TreeBuilder.parseString(postString);\n" +
                        "                    FlowchartBuilder flowchartBuilder = new DotBuilder();\n" +
                        "                    String r = flowchartBuilder.builder(tree);\n" +
                        "\n" +
                        "                    System.out.println(\"code = \" + postString + \"\\nr = [\" + r + \"]\");\n" +
                        "\n" +
                        "                    exchange.sendResponseHeaders(200, r.getBytes(StandardCharsets.UTF_8).length);\n" +
                        "                    exchange.getResponseBody().write(r.getBytes(StandardCharsets.UTF_8));\n" +
                        "                    exchange.getResponseBody().flush();\n" +
                        "                } else {\n" +
                        "                    exchange.sendResponseHeaders(204, 0);\n" +
                        "                }\n" +
                        "            } else {\n" +
                        "                sendData(staticPath + exchange.getRequestURI().getPath(), exchange);\n" +
                        "\n" +
                        "            }\n" +
                        "            exchange.close();\n" +
                        "\n" +
                        "        });";
        TreeNode tree = TreeBuilder.parseString(cfString);
        FlowchartBuilder flowchartBuilder = new DotBuilder();
//        flowchartBuilder.setDebug(true);
        String r = flowchartBuilder.builder(tree);
//        saveDot(cfString, r)
        System.out.println(r);
    }


}
