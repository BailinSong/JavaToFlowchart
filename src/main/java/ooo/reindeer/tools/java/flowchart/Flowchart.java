package ooo.reindeer.tools.java.flowchart;

import ooo.reindeer.tools.java.flowchart.java8.Java8Lexer;
import ooo.reindeer.tools.java.flowchart.java8.Java8Parser;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @ClassName Flowchart
 * @Author songbailin
 * @Date 2020/5/27 23:49
 * @Version 1.0
 * @Description TODO
 */
public class Flowchart {
    public static TreeNode parse(CharStream stream, String method) {
        Lexer lexer = new Java8Lexer(stream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        Java8Parser parser = new Java8Parser(tokens);

        ParserRuleContext t = parser.compilationUnit();

        TreeBuilder treeBuilder = new TreeBuilder();

        treeBuilder.setMethod(method);


        return treeBuilder.build(t);
    }

    public static TreeNode parseFile(String file, String method) throws IOException {

        return parse(CharStreams.fromFileName(file, StandardCharsets.UTF_8), method);
    }

    public static TreeNode parseString(String string) {
        String cString = "public class Exec{" +
                "   public void exec(){" +
                string +
                "   }" +
                "}";
        return parse(CharStreams.fromString(cString), "exec");
    }

    public static TreeNode parseString(String string, String method) {

        return parse(CharStreams.fromString(string), method);
    }

    public static void main(String[] args) throws IOException {

        List<String> argsList = Arrays.asList(args);
        FlowchartBuilder flowchartBuilder;
        if (argsList.contains("-f")) {
            String filePath = argsList.get(argsList.indexOf("-f") + 1);
            if (argsList.contains("-m")) {
                String method = argsList.get(argsList.indexOf("-m") + 1);

                if (argsList.contains("-dot")) {
                    flowchartBuilder = new DotBuilder();
                } else {
                    flowchartBuilder = new MermaidBuilder();
                }
                System.out.println(flowchartBuilder.builder(parseFile(filePath, method)));
            } else {
                System.err.println("no method");
                Runtime.getRuntime().exit(-1);
            }
        } else {
            Scanner scanner = new Scanner(System.in);
            String line;
            String text = "";
            while ((line = scanner.next()) != null && !line.equalsIgnoreCase("EOF")) {

                text += line + "\n";
            }

            if (argsList.contains("-dot")) {
                flowchartBuilder = new DotBuilder();
            } else {
                flowchartBuilder = new MermaidBuilder();
            }
            System.out.println(flowchartBuilder.builder(parseString(text)));
        }
    }
}
