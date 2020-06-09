package ooo.reindeer.tools.java.flowchart;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @ClassName MermaidBuilder
 * @Author songbailin
 * @Date 2020/6/3 00:27
 * @Version 1.0
 * @Description TODO
 */
public class DotBuilder extends FlowchartBuilder {

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
                        .replaceAll("\r\n", " ")
                        .replaceAll("\n\r", " ")
                        .replaceAll("\r", " ")
                        .replaceAll("\n", " ")
                        .replaceAll(">", "\\\\>");
            }
        });

        super.setRelationText(new Function<Relation, String>() {
            @Override
            public String apply(Relation relation) {
                if (Objects.isNull(relation.getCondition()) || relation.getCondition().toString().isEmpty()) {
                    return "F" + relation.getFrom().toString().replace('_', 'T') + " -> " + "F" + relation.getTo().toString().replace('_', 'T');
                } else {
                    return "F" + relation.getFrom().toString().replace('_', 'T') + " -> " + "F" + relation.getTo().toString().replace('_', 'T') + "[label=\"" + relation.getCondition() + "\"]";
                }
            }
        });
    }

    @Override
    public StringBuilder builderString(List<Node> nodes, List<Relation> relationList) {
        StringBuilder stringBuilder = new StringBuilder("digraph G{\n");
        nodes.forEach(node -> stringBuilder.append(node.nodeText()).append("\n"));
        relationList.forEach(relation -> stringBuilder.append(relation.relationText()).append("\n"));
        stringBuilder.append("}\n");
        return stringBuilder;
    }
}
