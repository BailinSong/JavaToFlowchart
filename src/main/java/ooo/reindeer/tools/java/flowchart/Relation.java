package ooo.reindeer.tools.java.flowchart;

import lombok.*;

import java.util.function.Function;

/**
 * @ClassName Relation
 * @Author songbailin
 * @Date 2020/5/28 10:19
 * @Version 1.0
 * @Description TODO
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Relation implements Cloneable {
    Object from;
    @Builder.Default
    Object condition = null;
    Object to;
    @Builder.Default
    Function<Relation, String> converter = Relation::toString;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Relation temp = new Relation();
        temp.setTo(getTo());
        temp.setFrom(getFrom());
        temp.setCondition(getCondition());
        return temp;
    }

    public String relationText() {
        return converter.apply(this);
    }


}
