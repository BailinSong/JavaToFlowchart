package ooo.reindeer.tools.java.flowchart;

import lombok.*;

import java.util.function.Function;

/**
 * @ClassName Node
 * @Author songbailin
 * @Date 2020/6/3 00:35
 * @Version 1.0
 * @Description TODO
 */
@Builder
@Getter
@Setter
@ToString
@With
public class Node {
    String id;
    String text;
    @Builder.Default
    String style = "";
    @Builder.Default
    String type = "";
    @Builder.Default
    String format = "";
    @Builder.Default
    Function<Node, String> convert = Node::toString;

    public String nodeText() {
        return convert.apply(this);
    }
}
