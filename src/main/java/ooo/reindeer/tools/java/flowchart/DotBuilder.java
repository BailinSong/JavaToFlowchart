package ooo.reindeer.tools.java.flowchart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 点构造器
 *
 * @ClassName MermaidBuilder
 * @Author songbailin
 * @Date 2020/6/3 00:27
 * @Version 1.0
 * @Description TODO
 */
public class DotBuilder extends FlowchartBuilder {

    /**
     * 点构造器
     */
    public DotBuilder() {
        super();
        super.setNodeText(new Function<Node, String>() {
            @Override
            public String apply(Node node) {
                if (TreeBuilder.RETURN.equalsIgnoreCase(node.getType()) || TreeBuilder.BEGIN.equalsIgnoreCase(node.getType())|| TreeBuilder.END.equalsIgnoreCase(node.getType())) {
                    return "F" + node.getId().replace('_', 'T') + "[ shape=ellipse, label=\"" + toSafeText(node.getText()) + "\", style=\"filled\", fillcolor=\"white\"]";
                } else if (node.getType().equalsIgnoreCase("if") || node.getType().equalsIgnoreCase("switch")) {
                    return "F" + node.getId().replace('_', 'T') + "[ shape=diamond, label=\"" + toSafeText(node.getText()) + "\", style=\"filled\", fillcolor=\"white\"]";
                } else {
                    return "F" + node.getId().replace('_', 'T') + "[ shape=box, label=\"" + toSafeText(node.getText()) + "\", style=\"filled\", fillcolor=\"white\"]";
                }
            }

            String toSafeText(String text) {
                return text
                        .replaceAll("\"", "\\\\\"")
                        .replaceAll("<", "\\\\<")
                        .replaceAll("\r", " \\\\r")
                        .replaceAll("\n", " \\\\n")
                        .replaceAll(">", "\\\\>");
            }
        });

        super.setRelationText(new Function<Relation, String>() {

            Map<String,Integer> minlenMap=new HashMap<>();
            int minle=1;
            @Override
            public String apply(Relation relation) {
                if (Objects.isNull(relation.getCondition()) || relation.getCondition().toString().isEmpty()) {
                    return "F" + relation.getFrom().toString().replace('_', 'T') + " -> " + "F" + relation.getTo().toString().replace('_', 'T');
                } else {
                    return "F" + relation.getFrom().toString().replace('_', 'T') + " -> " + "F" + relation.getTo().toString().replace('_', 'T') + "[label=\"" + relation.getCondition() + "\", minlen="+minlenMap.computeIfAbsent(relation.getCondition().toString(),s -> minle++)+"]";
                }
            }
        });
    }

    /**
     * builder字符串
     *
     * @param nodes
     *         节点
     * @param relationList
     *         关系列表
     *
     * @return {@link StringBuilder}
     */
    @Override
    public StringBuilder builderString(List<Node> nodes, List<Relation> relationList) {
        StringBuilder stringBuilder = new StringBuilder("digraph G{\nranksep = 0.1;\nnodesep = 0.1;\n");
        nodes.forEach(node -> stringBuilder.append(node.nodeText()).append("\n"));
        relationList.forEach(relation -> stringBuilder.append(relation.relationText()).append("\n"));
        stringBuilder.append("}\n");
        return stringBuilder;
    }
}
