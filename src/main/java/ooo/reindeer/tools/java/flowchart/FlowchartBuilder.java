package ooo.reindeer.tools.java.flowchart;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static ooo.reindeer.tools.java.flowchart.TreeBuilder.*;

/**
 * @ClassName FlowchartBuilder
 * @Author songbailin
 * @Date 2020/6/4 11:39
 * @Version 1.0
 * @Description TODO
 */
public abstract class FlowchartBuilder {

    @Setter
    @Getter
    boolean debug = false;

    @Setter
    @Getter
    boolean autoEndNode=false;

    List<Node> nodes = new ArrayList<>();
    List<Relation> relationList = new ArrayList<>();

    @Getter
    @Setter
    Function<Relation, String> relationText = null;

    @Getter
    @Setter
    Function<Node, String> nodeText = null;


    List<String> ignore = Arrays.asList(CONDITION, WHILE, SWITCH_END, CASE);//,"while"

    String appendDebugTag(String tag) {
        return (debug) ? "_" + tag : "";
    }

    //块结尾关系补偿
    private void buildBlockTailRelations(TreeNode root) {
        root.walk(tn -> {
            if (tn.getPrevious() != null && (IF.equalsIgnoreCase(tn.getPrevious().getType()))) {
                tn.getPrevious().walk(treeNode1 -> {
                    if (treeNode1.lastOfBlock()) {
                        if (!(treeNode1.getType().equals(IF)
                                || treeNode1.getType().equals(RETURN)
                                || treeNode1.getType().equals(CONDITION)
                                || treeNode1.getType().equals(WHILE)
                                || treeNode1.getType().equals(SWITCH_END)
                        )) {
                            for (Relation relation : relationList) {
                                if (relation.getFrom().equals(treeNode1.getId())) {
                                    return;
                                }
                            }
                            TreeNode pn = tn;
                            while (pn.getType().equals(CASE) || pn.getType().equals(SWITCH_END)) {
                                pn = pn.findeNearNext();
                            }

                            relationList.add(Relation
                                    .builder()
                                    .from(treeNode1.getId())
                                    .condition(appendDebugTag("BBTR"))
                                    .to(pn.getId())
                                    .converter(relationText)
                                    .build());
                        }
                    }
                });
            }
        });


    }

    private void buildBreakRelations(TreeNode treeNode) {
        treeNode.walk(treeNode1 -> {
            if (treeNode1.getType().equals(BREAK)) {
                TreeNode pn = treeNode1.findeNearNext();
                while (!(pn.getType().equals(WHILE) || pn.getType().equals(FOR_UPDATER) || pn.getType().equals(SWITCH_END))) {
                    pn = pn.findeNearNext();
                }


                switch (pn.getType()) {
                    case FOR_UPDATER:
                    case SWITCH_END:
                        pn = pn.findeNearNext();
                        if (pn.getType().equals(WHILE)) {
                            pn = getRealNode(pn.getParent());
                        }

                        break;
                    case WHILE:
                        pn = getRealNode(pn.parent);
                        pn = pn.findeNearNext();
                        break;
                }

                if (pn != null) {
                    Relation merge = Relation.builder().from(treeNode1.getId()).to(pn.getId()).condition(appendDebugTag("BBR")).converter(relationText
                    ).build();
                    relationList.add(merge);
                }
            }
        });
    }

    private void buildContinueRelations(TreeNode treeNode) {
        treeNode.walk(treeNode1 -> {
            if (treeNode1.getType().equals(CONTINUE)) {
                TreeNode pn = treeNode1.findeNearNext();
                while (!(pn.getType().equals(WHILE) || pn.getType().equals(FOR_UPDATER))) {
                    pn = pn.findeNearNext();
                }

                switch (pn.getType()) {
                    case FOR_UPDATER:
                        break;
                    case WHILE:
                        pn = getRealNode(pn.parent);
                        break;
                }

                Relation merge = Relation.builder().from(treeNode1.getId()).to(pn.getId()).condition(appendDebugTag("BCR")).converter(relationText
                ).build();
                relationList.add(merge);
            }
        });
    }

    //创建直接关系
    private void buildDirectRelations(TreeNode root) {
        AtomicReference<TreeNode> ptn = new AtomicReference<>(root);

        root.walk(treeNode -> {
            Relation relation;
            if ((Objects.isNull(ptn.get().getType())
                    || ptn.get().getType().isEmpty()
                    || ptn.get().getType().equals(BEGIN)
//                    || ptn.get().getType().equals(RETURN)
//                    || ptn.get().getType().equals(BREAK)
//                    || ptn.get().getType().equals(CONTINUE)
            )
                    && !(
                    treeNode.getType().equals(CONDITION)
                            || treeNode.getType().equals(CASE)
                            || treeNode.getType().equals(SWITCH_END)
                            || treeNode.getType().equals(WHILE)
            )
            ) {
                relation = Relation
                        .builder()
                        .from(ptn.get().getId())
                        .condition(appendDebugTag("BDR"))
                        .to(treeNode.getId())
                        .converter(relationText)
                        .build();
                relationList.add(relation);
            }

            ptn.set(treeNode);
        });
    }

    //创建For块关系
    private void buildForRelations(TreeNode root) {

        AtomicReference<TreeNode> ptn = new AtomicReference<>(root);

        root.walk(treeNode -> {

            if (ptn.get().getType().equals(FOR_INIT)) {

                relationList.add(Relation
                        .builder()
                        .from(ptn.get().getId())
                        .condition(appendDebugTag("BFR_INIT"))
                        .to(treeNode.getId())
                        .converter(relationText)
                        .build());
            }

            if (treeNode.getType().equals(FOR_UPDATER)) {

                relationList.add(Relation
                        .builder()
                        .from(treeNode.getId())
                        .condition(appendDebugTag("BFR_UPDATER"))
                        .to(getRealNode(treeNode.getParent()).getId())
                        .converter(relationText)
                        .build());
            }

            ptn.set(treeNode);
        });

    }

    //if块默认关系补偿
    private void buildIfDefaultConditionalRelations(TreeNode root) {
        root.walk(treeNode -> {


            //if块为流程中最后一个步骤特殊处理
            if (treeNode.getType().equals(IF) && treeNode.lastOfBlock()&&
                    (treeNode.getPrevious()==null||(treeNode.getPrevious()!=null&&!treeNode.getPrevious().getType().equals(IF)))) {
                boolean hasFalse = ifBlockHasElse(treeNode);
                if (hasFalse) {
                    return;
                }
                TreeNode pn = treeNode.getParent();
                while (pn.getType().equals(CONDITION) || pn.lastOfBlock()) {
                    pn = pn.getParent();
                    if (pn == null) {
                        return;
                    }
                }
                try {
                    pn = pn.findeNearNext();
                    while (pn.getType().equals(CASE) || pn.getType().equals(SWITCH_END)) {
                        pn = pn.findeNearNext();
                    }
                    relationList.add(Relation.builder().from(treeNode.getId()).to(pn.getId()).condition("false" + appendDebugTag("BIDCR1")).converter(relationText
                    ).build());
                    return;

//                    for (int index = pn.getParent().getSub().size() - 1; index >= 0; index--) {
//                        if (pn.getParent().getSub().get(index).getPrevious().equals(pn)) {
//
//                        }
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {

                TreeNode rpn = treeNode.getPrevious();

                if (rpn == null) {
                    return;
                }


                if (rpn.getType().equals(IF)) {

                    boolean hasFalse = ifBlockHasElse(rpn);
                    boolean hasTrue = ifBlockHasTrue(rpn);
                    //如果没有else 分支则泽东生成else分支
                    if (!hasFalse) {
                        TreeNode pn = treeNode;
                        if (treeNode.getType().equalsIgnoreCase(WHILE)) {
                            pn = getRealNode(treeNode.getParent());

                        }
                        relationList.add(Relation.builder().from(rpn.getId()).to(pn.id).condition(((hasTrue) ? "false" : "") + appendDebugTag("BIDCR2")).converter(relationText
                        ).build());

                    }
                }
            }
        });
    }

    //创建IF块关系
    private void buildIfRelations(TreeNode root) {

        root.walk(treeNode -> {

            if (treeNode.getType().equals(IF)) {

                treeNode.getSub().forEach(treeNode1 -> {
                    if (treeNode1.getType().equals(CONDITION)) {

                        if (treeNode1.getSub().size() < 1) {

//                            TreeNode pn = treeNode1.findeNearNext();
//                            while(){
//
//                            }

                        } else {

                            relationList.add(Relation
                                    .builder()
                                    .from(treeNode.getId())
                                    .condition(treeNode1.getText() + appendDebugTag("BIR"))
                                    .to(treeNode1.getSub().get(0).getId())
                                    .converter(relationText)
                                    .build());
                        }
                    }
                });
            }
        });
    }

    protected void buildSCaseRelations(TreeNode treeNode) {

        List<TreeNode> caseList = new ArrayList<>();

        treeNode.walk(treeNode1 -> {
            if (treeNode1.getType().equals(SWITCH)) {
                boolean hasDefault = false;
                for (int i = 0; i < treeNode1.getSub().size(); i++) {
                    if (treeNode1.getSub().get(i).getType().equals(CASE)) {
                        caseList.add(treeNode1.getSub().get(i));
                        if (treeNode1.getSub().get(i).getText().equalsIgnoreCase("default")) {
                            hasDefault = true;
                        }
                    } else {
                        if (caseList.size() > 0) {
                            while (caseList.size() > 0) {
                                TreeNode pn = caseList.remove(0);
                                Relation merge = Relation.builder().from(treeNode1.getId()).to(treeNode1.getSub().get(i).getId()).condition(pn.getText() + appendDebugTag("BSCR.1")).converter(relationText
                                ).build();
                                relationList.add(merge);
                            }
                        }
                        if (!(treeNode1.getSub().get(i).getType().equals(BREAK) || treeNode1.getSub().get(i).getType().equals(IF))) {
                            TreeNode fnn = treeNode1.getSub().get(i).findeNearNext();
                            while ((fnn.getType().equals(CASE) || fnn.getType().equals(SWITCH_END))) {
                                fnn = fnn.findeNearNext();

                                if (fnn.getType().equals(WHILE)) {
                                    fnn = getRealNode(fnn.getParent());
                                }
                                if(!(fnn.getType().equals(CASE) || fnn.getType().equals(SWITCH_END))) {
                                    Relation merge = Relation.builder().from(treeNode1.getSub().get(i).getId()).to(fnn.getId()).condition(appendDebugTag("BSCR.2")).converter(relationText
                                    ).build();
                                    relationList.add(merge);
                                }
                            }
                        }
                    }
                }
                if (!hasDefault) {
                    TreeNode fnn = treeNode1.findeNearNext();
                    if (fnn.getType().equals(WHILE)) {
                        fnn = getRealNode(fnn.getParent());
                    }
                    Relation merge = Relation.builder().from(treeNode1.getId()).to(fnn.getId()).condition("default" + appendDebugTag("BSCR.3")).converter(relationText
                    ).build();
                    relationList.add(merge);
                }
            }
        });

    }

    private void buildWhileRelations(TreeNode treeNode) {

        treeNode.walk(tn -> {

            if (tn.getType().equals(IF)
                    || tn.getType().equals(CONDITION)
                    || tn.getType().equals(BREAK)
                    || tn.getType().equals(CONTINUE)
                    || tn.getType().equals(RETURN)
                    || tn.getType().equals(SWITCH_END)
                    || tn.getType().equals(SWITCH)) {
                return;
            }

            TreeNode fnn = tn.findeNearNext();
//            while (fnn!=null&&(fnn.getType().equals(CONDITION)||fnn.getType().equals(IF))){
            while (fnn != null && (fnn.getType().equals(CONDITION))) {
                fnn = fnn.findeNearNext();
            }
            if (fnn != null && fnn.getType().equals(WHILE)) {

                Relation merge = Relation.builder().from(tn.getId()).to(getRealNode(fnn.parent).getId()).condition(appendDebugTag("BWR")).converter(relationText
                ).build();
                relationList.add(merge);
            }

        });
    }

    public String builder(TreeNode treeNode) {
        initNodes(treeNode);
        initRelation(treeNode);

        StringBuilder s = builderString(nodes, relationList);

        return s.toString();
    }

    public abstract StringBuilder builderString(List<Node> nodes, List<Relation> relationList);

    private void
    createNode(TreeNode tn) {


        if (!ignore.contains(tn.getType())) {

            nodes.add(Node.builder().id(tn.getId().toString()).type(tn.getType()).text(tn.getText()).convert(nodeText).build());
        }
    }

    TreeNode getRealNode(TreeNode tn) {
        if (tn.getType().equals(CONDITION)) {
            return tn.getParent();
        } else {
            return tn;
        }
    }

    private boolean ifBlockHasElse(TreeNode rpn) {
        boolean hasFalse = false;

        for (TreeNode node : rpn.getSub()) {
            hasFalse |= "false".equalsIgnoreCase(node.getText());
        }
        return hasFalse;
    }

    private boolean ifBlockHasTrue(TreeNode rpn) {
        boolean hasFalse = false;

        for (TreeNode node : rpn.getSub()) {
            hasFalse |= "true".equalsIgnoreCase(node.getText());
        }
        return hasFalse;
    }

    private void initNodes(TreeNode treeNode) {

        TreeNode.walk(treeNode, this::createNode);
    }

    private void initRelation(TreeNode treeNode) {
        buildDirectRelations(treeNode);
        buildIfRelations(treeNode);
        buildForRelations(treeNode);
        buildSCaseRelations(treeNode);
//        buildDoRelations(treeNode);
        buildWhileRelations(treeNode);
        buildBreakRelations(treeNode);
        buildContinueRelations(treeNode);
        buildIfDefaultConditionalRelations(treeNode);
        buildBlockTailRelations(treeNode);
        if(isAutoEndNode()) {
            buildEndRelations(treeNode);
        }
    }

    private void buildEndRelations(TreeNode treeNode) {
        boolean needEndNode=false;
        TreeNode endNode = TreeNode.builder().id(END).text(END).type(END).build();
        List<Relation> endRelationList =new ArrayList<>();
        for (Relation relation : relationList) {

            boolean isReturn=false;
            for (Node node : nodes) {
                if(relation.getTo().equals(node.getId())&&node.getType().equals(RETURN)) {
                    isReturn=true;
                    break;
                }
            }

            if(isReturn) {
                continue;
            }

            boolean isEnd=true;
            for (Relation relation1 : relationList) {
                if(relation.getTo().equals(relation1.getFrom())){
                    isEnd=false;
                    break;
                }
            }

            if(isEnd){
                endRelationList.add(Relation.builder().from(relation.getTo()).to(endNode.getId()).converter(relationText).condition(appendDebugTag("BER")).build());
            }
        }

        if(endRelationList.size()>0){
            createNode(endNode);
            relationList.addAll(endRelationList);
        }
    }


}
