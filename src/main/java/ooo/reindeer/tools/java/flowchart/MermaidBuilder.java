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
public class MermaidBuilder extends FlowchartBuilder {

    public MermaidBuilder() {
        super();
        super.setNodeText(new Function<Node, String>() {
            @Override
            public String apply(Node node) {
                if (TreeBuilder.RETURN.equalsIgnoreCase(node.getType()) || TreeBuilder.BEGIN.equalsIgnoreCase(node.getType())) {
                    return node.getId() + "((\"" + toSafeText(node.getText()) + "\"))";
                } else if (node.getType().equalsIgnoreCase("if") || node.getType().equalsIgnoreCase("switch")) {
                    return node.getId() + "{\"" + toSafeText(node.getText()) + "\"}";
                } else {
                    return node.getId() + "[\"" + toSafeText(node.getText()) + "\"]";
                }
            }

            String toSafeText(String text) {
                return text
                        .replaceAll("\r\n", " ")
                        .replaceAll("\n\r", " ")
                        .replaceAll("\r", " ")
                        .replaceAll("\n", " ")
                        .replaceAll("\"", "&quot;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;");
            }
        });

        super.setRelationText(new Function<Relation, String>() {
            @Override
            public String apply(Relation relation) {
                if (Objects.isNull(relation.getCondition()) || relation.getCondition().toString().isEmpty()) {
                    return relation.getFrom() + "-->" + relation.getTo();
                } else {
                    return relation.getFrom() + "-->|" + relation.getCondition() + "|" + relation.getTo();
                }
            }
        });
    }

    @Override
    public StringBuilder builderString(List<Node> nodes, List<Relation> relationList) {
        StringBuilder s = new StringBuilder();
        s.append("```mermaid\n" +
                "graph TD;\n");
        nodes.forEach(node -> s.append(node.nodeText()).append("\n"));
        relationList.forEach(relation -> s.append(relation.relationText()).append("\n"));
        s.append("```");
        return s;
    }
}
