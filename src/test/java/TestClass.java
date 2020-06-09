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
        String cfString = "paper-5-5";
        TreeNode tree = TreeBuilder.parseString(load(cfString));
        System.out.println("tree = " + tree);
        FlowchartBuilder flowchartBuilder = new DotBuilder();
        flowchartBuilder.setDebug(true);
        String r = flowchartBuilder.builder(tree);
        saveDot(cfString, r);

        System.out.println(r);
    }



}
