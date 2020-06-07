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

    boolean debug = false;

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
                                || treeNode1.getType().equals(SWITCH_END))) {
                            for (Relation relation : relationList) {
                                if (relation.getFrom().equals(treeNode1.getId())) {
                                    return;
                                }
                            }
                            relationList.add(Relation
                                    .builder()
                                    .from(treeNode1.getId())
                                    .condition(appendDebugTag("BBTR"))
                                    .to(tn.getId())
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
            if (treeNode.getType().equals(IF) && treeNode.lastOfBlock()) {
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
                    for (int index = pn.getParent().getSub().size() - 1; index >= 0; index--) {
                        if (pn.getParent().getSub().get(index).getPrevious().equals(pn)) {
                            relationList.add(Relation.builder().from(treeNode.getId()).to(pn.getParent().getSub().get(index).getId()).condition("false" + appendDebugTag("BIDCR")).converter(relationText
                            ).build());
                            return;
                        }
                    }
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

                    //如果没有else 分支则泽东生成else分支
                    if (!hasFalse) {

                        relationList.add(Relation.builder().from(rpn.getId()).to(treeNode.id).condition("false" + appendDebugTag("BIDCR")).converter(relationText
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
                        relationList.add(Relation
                                .builder()
                                .from(treeNode.getId())
                                .condition(treeNode1.getText() + appendDebugTag("BIR"))
                                .to(treeNode1.getSub().get(0).getId())
                                .converter(relationText)
                                .build());
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
                                Relation merge = Relation.builder().from(treeNode1.getId()).to(treeNode1.getSub().get(i).getId()).condition(pn.getText() + appendDebugTag("BSCR")).converter(relationText
                                ).build();
                                relationList.add(merge);
                            }
                        }
                        if (!treeNode1.getSub().get(i).getType().equals(BREAK)) {
                            TreeNode fnn = treeNode1.getSub().get(i).findeNearNext();
                            while ((fnn.getType().equals(CASE) || fnn.getType().equals(SWITCH_END))) {
                                fnn = fnn.findeNearNext();
                                if (fnn.getType().equals(WHILE)) {
                                    fnn = getRealNode(fnn.getParent());
                                }
                                Relation merge = Relation.builder().from(treeNode1.getSub().get(i).getId()).to(fnn.getId()).condition(appendDebugTag("BSCR")).converter(relationText
                                ).build();
                                relationList.add(merge);
                            }
                        }
                    }
                }
                if (!hasDefault) {
                    TreeNode fnn = treeNode1.findeNearNext();
                    if (fnn.getType().equals(WHILE)) {
                        fnn = getRealNode(fnn.getParent());
                    }
                    Relation merge = Relation.builder().from(treeNode1.getId()).to(fnn.getId()).condition("default" + appendDebugTag("BSCR")).converter(relationText
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

    /*
        private void buildRelations(TreeNode tn) {
            TreeNode pNode = tn.findPrevious();
            Relation relation;

    //        if (tn.getType().equalsIgnoreCase(TreeBuilder.FOR_UPDATER)) {
    //            TreeNode ppNode = tn;
    //            do {
    //                ppNode = ppNode.getParent();
    //            } while (!ppNode.getType().equalsIgnoreCase(TreeBuilder.IF));
    //            Relation merge = Relation.builder().from(tn.getId()).to(ppNode.getId()).converter(relationText
    //            ).build();
    //            relationList.add(merge);
    //
    //        }

    //        if(tn.getType().equalsIgnoreCase("while")){
    //            TreeNode ppNode=tn;
    //            do{
    //                ppNode=ppNode.getParent();
    //            }while (!ppNode.getType().equalsIgnoreCase("if"));
    //            Relation merge = Relation.builder().from(tn.getId()).to(ppNode.getId()).convert(MermaidBuilder::FT
    //            ).build();
    //        }

    //        if(tn.findPrevious().getType().equalsIgnoreCase("forInit")){
    //            TreeNode ppNode=tn;
    //
    //            Relation merge = Relation.builder().from(tn.findPrevious().getSub().get(0).getId()).to(tn.getId()).convert(MermaidBuilder::FT
    //            ).build();
    //            relationList.add(merge);
    //
    //        }

            if (tn.getPrevious() != null && ("if".equalsIgnoreCase(tn.getPrevious().type))) {

                boolean hasFalse = false;

                //循环所有分支
                for (TreeNode node : tn.getPrevious().getSub()) {

                    //默认没有 else分支
                    hasFalse |= "false".equalsIgnoreCase(node.getText());

                    node.walk(treeNode1 -> {

                        if (merged.contains(treeNode1.getId())) {
                            return;
                        }


                        if (treeNode1.lastOfBlock()) {
                            if("forUpdater".equalsIgnoreCase(treeNode1.getType())||"while".equalsIgnoreCase(treeNode1.getType())){
                                TreeNode ppNode = treeNode1.getParent();
                                do {

                                    ppNode = ppNode.findPrevious();

                                } while ("condition".equalsIgnoreCase(ppNode.getType()));
                                Relation merge = Relation.builder().from(ppNode.getId()).to(tn.getId()).condition("falseBB").converter(relationText
                                ).build();
                                relationList.add(merge);
                                merged.add(treeNode1.getId());

                            }else if (!"return".equalsIgnoreCase(treeNode1.getType())
                                    && !"condition".equalsIgnoreCase(treeNode1.getType())
                                    && !"if".equalsIgnoreCase(treeNode1.getType())
                                    ) {
                                if (tn.getType().equalsIgnoreCase("while")) {

                                    TreeNode ppNode = tn;
                                    do {
                                        ppNode = ppNode.getParent();
                                    } while (!ppNode.getType().equalsIgnoreCase("if"));
                                    Relation merge = Relation.builder().from(treeNode1.getId()).to(ppNode.getId()).converter(relationText
                                    ).build();
                                    relationList.add(merge);
                                    merged.add(treeNode1.getId());
                                } else {

                                    Relation merge = Relation.builder().from(treeNode1.getId()).to(tn.id).converter(relationText
                                    ).condition("TESTGGGGGGGGGGG").build();
                                    relationList.add(merge);
                                    merged.add(treeNode1.getId());
                                }
                            }
                        }

                    });
                }

                //如果没有else 分支则泽东生成else分支
                if (!hasFalse&&!tn.getType().equalsIgnoreCase(TreeBuilder.FOR_UPDATER)) {
                    Relation merge = Relation.builder().from(tn.getPrevious().getId()).to(tn.id).condition("falseAA").converter(relationText
                    ).build();
                    relationList.add(merge);

                }
            }else{
                relation = Relation.builder().from(pNode.getId()).to(tn.id).converter(relationText).build();
                relationList.add(relation);
            }
                if (tn.getType().equalsIgnoreCase("forUpdater")) {


                    TreeNode ppNode = tn.getParent();
                    do {

                        ppNode = ppNode.findPrevious();

                    } while ("condition".equalsIgnoreCase(ppNode.getType()));

                    relation = Relation.builder().from(tn.id).to(ppNode.getId()).converter(relationText
                    ).build();
                    relationList.add(relation);


                    if (tn.getPrevious() == null) {
                        relation = Relation.builder().from(pNode.getId()).to(tn.id).condition("true").converter(relationText
                        ).build();
                        relationList.add(relation);
                    }
    //                else {
    //                    relation = Relation.builder().from(pNode.getId()).to(tn.id).converter(relationText
    //                    ).build();
    //                    relationList.add(relation);
    //                }


                }


        }
    */
    public String builder(TreeNode treeNode) {
        initNodes(treeNode);
        initRelation(treeNode);

        StringBuilder s = builderString(nodes, relationList);

        return s.toString();
    }

    public abstract StringBuilder builderString(List<Node> nodes, List<Relation> relationList);

    private void createNode(TreeNode tn) {


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

    }
/*
    private void initRelation(TreeNode treeNode) {


        treeNode.walk(treeNode, tn -> {

            if ("condition".equalsIgnoreCase(tn.getType())) {
                return;
            }

            if (tn.findPrevious() == null) {
                return;
            }
            Relation relation;

            if ("condition".equalsIgnoreCase(tn.findPrevious().getType())) {
                TreeNode pNode = tn.findPrevious();
                TreeNode ppNode = pNode;
                do {

                    ppNode = ppNode.findPrevious();

                } while ("condition".equalsIgnoreCase(ppNode.getType()));
                relation = Relation.builder().from(ppNode.getId()).to(tn.id).condition(pNode.text).converter(relationText

                ).build();
                relationList.add(relation);
            } else {
                buildRelations(tn);
            }


        });
    }

 */
}
