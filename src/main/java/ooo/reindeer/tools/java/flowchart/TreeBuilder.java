package ooo.reindeer.tools.java.flowchart;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ooo.reindeer.tools.java.flowchart.java8.Java8Lexer;
import ooo.reindeer.tools.java.flowchart.java8.Java8Parser;
import ooo.reindeer.tools.java.flowchart.java8.Java8ParserBaseVisitor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Stack;


/**
 * @author leer
 * Created at 12/17/19 1:57 PM
 */


@Getter
@Setter
public class TreeBuilder extends Java8ParserBaseVisitor<TreeNode> {


    public static final String FOR_INIT = "forInit", IF = "if", CONDITION = "condition", SWITCH = "switch", SWITCH_END = SWITCH + "_end", WHILE = "while", RETURN = "return", FOR_UPDATER = "forUpdater", BREAK = "break", CONTINUE = "continue", CASE = "case", BEGIN = "begin";

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

    @Builder.Default
    TreeNode root = TreeNode
            .builder()
            .id("0_0")
            .type("begin")
            .text(BEGIN)
            .interval(Interval.of(0, Integer.MAX_VALUE))
            .build();

    @Builder.Default
    Stack<TreeNode> stack = new Stack<>();

    String method = "";

    public TreeBuilder() {
        stack.push(root);
    }

    //
//    @Override
//    public Node visitChildren(RuleNode node) {
//
//
//
//        Node result = new Node();
//        int n = node.getChildCount();
//        for (int i=0; i<n; i++) {
//            if (!shouldVisitNextChild(node, result)) {
//                break;
//            }
//
//            ParseTree c = node.getChild(i);
//
//            Node childResult = c.accept(this);
//
//            if(c instanceof Java8Parser.BlockStatementContext) {
//                result = aggregateResult(result, childResult);
//            }
//        }
//
//        return result;
//    }
//
    @Override
    protected TreeNode aggregateResult(TreeNode aggregate, TreeNode nextResult) {

        return root;

    }

    public TreeNode build(ParseTree t) {
        return super.visit(t);
    }

    public String getExpressionText(ParserRuleContext expr) {
        return expr.getStart().getInputStream().getText(Interval.of(expr.getStart().getStartIndex(), expr.getStop().getStopIndex()));
    }

    public String getID(ParserRuleContext node) {

        return node.getStart().getStartIndex() + "_" + node.getStop().getStopIndex();
    }

    private void record(TreeNode temp, Interval interval) {
        if (stack.lastElement().interval.a > interval.a || stack.lastElement().interval.b < interval.b) {
            while (!(stack.lastElement().interval.a <= interval.a && stack.lastElement().interval.b >= interval.b)) {
                stack.remove(stack.lastElement());
            }
        }

        temp.setParent(stack.lastElement());

        if (stack.lastElement().getSub().size() > 0) {
//            System.out.println(temp.getText()+" previous:"+stack.lastElement().getSub().get(stack.lastElement().getSub().size() - 1).getText());

//            TreeNode Previous=stack.lastElement().getSub().get(stack.lastElement().getSub().size() - 1);
//
//            if(Previous.getType().equalsIgnoreCase("forInit")){
//
//            }

            temp.setPrevious(stack.lastElement().getSub().get(stack.lastElement().getSub().size() - 1));


        }

        stack.lastElement().getSub().add(temp);

        stack.push(temp);
    }

    @Override
    public TreeNode visitBlockStatement(Java8Parser.BlockStatementContext ctx) {

        Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());

        TreeNode temp = TreeNode.builder().id(getID(ctx)).text(getExpressionText(ctx)).interval(interval).build();

        record(temp, interval);

        return super.visitBlockStatement(ctx);
    }

    @Override
    public TreeNode visitBreakStatement(Java8Parser.BreakStatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));
        treeNode.setText(getExpressionText(ctx));
        treeNode.setType(BREAK);
        return super.visitBreakStatement(ctx);
    }

    @Override
    public TreeNode visitContinueStatement(Java8Parser.ContinueStatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));
        treeNode.setText(getExpressionText(ctx));
        treeNode.setType(CONTINUE);
        return super.visitContinueStatement(ctx);
    }

    @Override
    public TreeNode visitForStatement(Java8Parser.ForStatementContext ctx) {

        if (ctx.basicForStatement() != null) {
            TreeNode treeNode = root.findNode(getID(ctx));
            treeNode.setText(getExpressionText(ctx.basicForStatement().forInit()));
            treeNode.setType(FOR_INIT);
            Interval interval = Interval.of(ctx.basicForStatement().forInit().getStart().getStartIndex(), ctx.basicForStatement().forInit().getStop().getStopIndex());
            treeNode.setInterval(interval);
            TreeNode result = root;


            ParserRuleContext expr = ctx.basicForStatement().expression();
            interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
            TreeNode temp = TreeNode.builder().id(getID(expr)).text(getExpressionText(expr)).interval(interval).type(IF).build();
            record(temp, interval);

            Java8Parser.StatementContext statement = ctx.basicForStatement().statement();
            interval = Interval.of(statement.getStart().getStartIndex(), statement.getStop().getStopIndex());
            temp = TreeNode.builder().id(getID(statement)).text("true").type(CONDITION).interval(interval).build();
            record(temp, interval);

            result = super.visitStatement(statement);

            ParserRuleContext updater = ctx.basicForStatement().forUpdate();
            interval = Interval.of(statement.getStart().getStartIndex(), statement.getStop().getStopIndex());
            temp = TreeNode.builder().id(getID(updater)).text(getExpressionText(updater)).interval(interval).type(FOR_UPDATER).build();
            record(temp, interval);

            return result;

        } else {
            Java8Parser.EnhancedForStatementContext enhancedForStatement = ctx.enhancedForStatement();
            TreeNode treeNode = root.findNode(getID(ctx));
            treeNode.setText(getExpressionText(enhancedForStatement.unannType())
                    + " " + getExpressionText(enhancedForStatement.variableDeclaratorId())
                    + "=" + getExpressionText(enhancedForStatement.expression()) + ".next()");
            treeNode.setType(FOR_INIT);
            Interval interval = Interval.of(enhancedForStatement.unannType().getStart().getStartIndex(), enhancedForStatement.variableDeclaratorId().getStop().getStopIndex());
            treeNode.setInterval(interval);
            TreeNode result = root;


            ParserRuleContext expr = enhancedForStatement.expression();
            interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
            TreeNode temp = TreeNode.builder().id(getID(expr)).text(getExpressionText(enhancedForStatement.variableDeclaratorId()) + ".isNonNull").interval(interval).type(IF).build();
            record(temp, interval);

            Java8Parser.StatementContext statement = enhancedForStatement.statement();
            interval = Interval.of(statement.getStart().getStartIndex(), statement.getStop().getStopIndex());
            temp = TreeNode.builder().id(getID(statement)).text("true").type(CONDITION).interval(interval).build();
            record(temp, interval);

            result = super.visitStatement(statement);

            ParserRuleContext updater = enhancedForStatement.variableDeclaratorId();
            interval = Interval.of(statement.getStart().getStartIndex(), statement.getStop().getStopIndex());
            temp = TreeNode.builder().id(getID(updater)).text(getExpressionText(enhancedForStatement.variableDeclaratorId())
                    + "=" + getExpressionText(enhancedForStatement.expression()) + ".next()").interval(interval).type(FOR_UPDATER).build();
            record(temp, interval);

            return result;

        }


//        ParserRuleContext init = ctx.forInit();
//        Interval interval = Interval.of(init.getStart().getStartIndex(), init.getStop().getStopIndex());
//        TreeNode temp = TreeNode.builder().id(getID(init)).text(getExpressionText(init)).interval(interval).build();
//        record(temp, interval);
//        return root;
    }

    @Override
    public TreeNode visitIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {


        TreeNode treeNode = root.findNode(getID(ctx));

        if (!(treeNode.getType() == null || treeNode.getType().isEmpty())) {
            //elseif 不改变原有节点重新创建if节点
            TreeNode temp = TreeNode.builder().type(IF).text(getExpressionText(ctx.expression())).interval(treeNode.getInterval()).id(treeNode.getId() + "_elseIf").build();
            record(temp, temp.getInterval());
        } else {
            treeNode.setText(getExpressionText(ctx.expression()));
            treeNode.setType(IF);
        }

        Java8Parser.StatementNoShortIfContext trueBlock = ctx.getRuleContext(Java8Parser.StatementNoShortIfContext.class, 0);


        Interval interval = Interval.of(trueBlock.getStart().getStartIndex(), trueBlock.getStop().getStopIndex());

        TreeNode temp = TreeNode.builder().id(getID(trueBlock)).text("true").type(CONDITION).interval(interval).build();
        //包含
        record(temp, interval);
        super.visitStatementNoShortIf(trueBlock);

        Java8Parser.StatementContext falseBlock = ctx.getRuleContext(Java8Parser.StatementContext.class, 0);

        interval = Interval.of(falseBlock.getStart().getStartIndex(), falseBlock.getStop().getStopIndex());
        temp = TreeNode.builder().id(getID(falseBlock)).text("false").type(CONDITION).interval(interval).build();

        record(temp, interval);

        return super.visitStatement(falseBlock);
    }

    @Override
    public TreeNode visitIfThenStatement(Java8Parser.IfThenStatementContext ctx) {


        TreeNode treeNode = root.findNode(getID(ctx));
        treeNode.setText(getExpressionText(ctx.expression()));
        treeNode.setType(IF);

        Java8Parser.StatementContext trueBlock = ctx.getRuleContext(Java8Parser.StatementContext.class, 0);

        Interval interval = Interval.of(trueBlock.getStart().getStartIndex(), trueBlock.getStop().getStopIndex());

        TreeNode temp = TreeNode.builder().id(getID(trueBlock)).text("true").type(CONDITION).interval(interval).build();

        record(temp, interval);


        return super.visitStatement(trueBlock);
    }

    @Override
    public TreeNode visitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        if (ctx.methodHeader().methodDeclarator().getChild(0).getText().equalsIgnoreCase(method)) {
            return super.visitMethodDeclaration(ctx);
        } else {
            return null;
        }
    }

    @Override
    public TreeNode visitReturnStatement(Java8Parser.ReturnStatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));
        treeNode.setText(getExpressionText(ctx.expression()));
        treeNode.setType(RETURN);

        return super.visitReturnStatement(ctx);
    }

    @Override
    public TreeNode visitStatementWithoutTrailingSubstatement(Java8Parser.StatementWithoutTrailingSubstatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));
        if (treeNode == null) {
            Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());

            TreeNode temp = TreeNode.builder().id(getID(ctx)).text(getExpressionText(ctx)).interval(interval).build();

            record(temp, interval);
        }
        return super.visitStatementWithoutTrailingSubstatement(ctx);
    }

    @Override
    public TreeNode visitSwitchStatement(Java8Parser.SwitchStatementContext ctx) {


        TreeNode treeNode = root.findNode(getID(ctx));
        treeNode.setText(getExpressionText(ctx.expression()));
        treeNode.setType(SWITCH);

        for (Java8Parser.SwitchBlockStatementGroupContext switchBlockStatementGroupContext : ctx.switchBlock().switchBlockStatementGroup()) {
            for (Java8Parser.SwitchLabelContext switchLabelContext : switchBlockStatementGroupContext.switchLabels().switchLabel()) {
                Interval interval;
                if (Objects.nonNull(switchLabelContext.constantExpression())) {
                    interval = Interval.of(switchLabelContext.constantExpression().start.getStartIndex(), switchLabelContext.constantExpression().stop.getStopIndex());
                    TreeNode temp = TreeNode.builder().id(getID(switchLabelContext.constantExpression())).text(getExpressionText(switchLabelContext.constantExpression())).type(CASE).interval(interval).build();
                    record(temp, interval);
                } else if (switchLabelContext.enumConstantName() != null) {
                    interval = Interval.of(switchLabelContext.enumConstantName().start.getStartIndex(), switchLabelContext.enumConstantName().stop.getStopIndex());
                    TreeNode temp = TreeNode.builder().id(getID(switchLabelContext.enumConstantName())).text(getExpressionText(switchLabelContext.enumConstantName())).type(CASE).interval(interval).build();
                    record(temp, interval);

                } else {
                    interval = Interval.of(switchLabelContext.DEFAULT().getSymbol().getStartIndex(), switchLabelContext.DEFAULT().getSymbol().getStopIndex());
                    TreeNode temp = TreeNode.builder().id(getID(switchLabelContext)).text("default").type(CASE).interval(interval).build();
                    record(temp, interval);
                }
                for (Java8Parser.BlockStatementContext blockStatementContext : switchBlockStatementGroupContext.blockStatements().blockStatement()) {
                    visitBlockStatement(blockStatementContext);
                }
            }
        }


        Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
        TreeNode temp = TreeNode.builder().id(getID(ctx) + "_switchEnd").text("").type(SWITCH_END).interval(interval).build();
        record(temp, interval);

        return root;
    }

    @Override
    public TreeNode visitTryStatement(Java8Parser.TryStatementContext ctx) {
        stack.removeElement(stack.lastElement());
        root.getSub().removeIf(treeNode -> treeNode.getId().equals(getID(ctx)));


        return super.visitBlock(ctx.block());
    }
/*
    Node findRealParent(ParserRuleContext ctx) {
        Node temp = findRealParent(ctx, root.getSub());
        if (temp != null) {
            return temp;
        } else {
            return root;
        }
    }

    Node findRealParent(ParserRuleContext ctx, List<Node> nodes) {

        Node temp = null;

        for (Node node : nodes) {
            if (node.getId().equals(getID(ctx))) {
                return node;
            }
            if (node.getSub().size() > 0) {
                temp = findRealParent(ctx, node.getSub());
                if (temp != null) {
                    return temp;
                }
            }

        }
        return null;

    }*/

    @Override
    public TreeNode visitTryWithResourcesStatement(Java8Parser.TryWithResourcesStatementContext ctx) {
        stack.removeElement(stack.lastElement());
        root.getSub().removeIf(treeNode -> treeNode.getId().equals(getID(ctx)));
        return super.visitBlock(ctx.block());
    }

    @Override
    public TreeNode visitWhileStatement(Java8Parser.WhileStatementContext ctx) {
        TreeNode result = root;
        TreeNode treeNode = root.findNode(getID(ctx));
        treeNode.setText(getExpressionText(ctx.expression()));
        treeNode.setType(IF);
        Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
        treeNode.setInterval(interval);

        Java8Parser.StatementContext statement = ctx.statement();
        interval = Interval.of(statement.getStart().getStartIndex(), statement.getStop().getStopIndex());
        TreeNode temp = TreeNode.builder().id(getID(statement)).text("true").type(CONDITION).interval(interval).build();
        record(temp, interval);

        result = super.visitStatement(statement);


        interval = Interval.of(statement.getStart().getStartIndex(), statement.getStop().getStopIndex());
        temp = TreeNode.builder().id(getID(statement) + "_" + WHILE).text(WHILE).interval(interval).type(WHILE).build();
        record(temp, interval);

        return result;
    }

}
