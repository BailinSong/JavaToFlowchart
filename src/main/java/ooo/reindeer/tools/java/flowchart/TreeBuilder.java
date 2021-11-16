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
import sun.security.action.GetPropertyAction;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.AccessController;
import java.util.BitSet;
import java.util.Objects;
import java.util.Stack;




@Getter
@Setter
public class TreeBuilder extends Java8ParserBaseVisitor<TreeNode> {


    public static final String FOR_INIT = "forInit", IF = "if", CONDITION = "condition", SWITCH = "switch", SWITCH_END = SWITCH + "_end", DO = "do", WHILE = "while", RETURN = "return", FOR_UPDATER = "forUpdater", BREAK = "break", CONTINUE = "continue", CASE = "case", BEGIN = "begin", END = "end";

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
        if(expr==null){
            return "end";
        }
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
    TreeNode lmd;
    @Override
    public TreeNode visitBlockStatement(Java8Parser.BlockStatementContext ctx) {

        Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());

        TreeNode temp = TreeNode.builder().id(getID(ctx)).text(getExpressionText(ctx)).interval(interval).build();

        record(temp, interval);
        lmd=temp;
        return super.visitBlockStatement(ctx);
    }

    @Override
    public TreeNode visitBreakStatement(Java8Parser.BreakStatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));
        if (!(treeNode.getType() == null || treeNode.getType().isEmpty())) {
            //elseif 不改变原有节点重新创建if节点
            TreeNode temp = TreeNode.builder().type(BREAK).text(getExpressionText(ctx)).interval(treeNode.getInterval()).id(treeNode.getId() + BREAK).build();
            record(temp, temp.getInterval());
        } else {
            treeNode.setText(getExpressionText(ctx));
            treeNode.setType(BREAK);
        }

        return super.visitBreakStatement(ctx);
    }

    @Override
    public TreeNode visitContinueStatement(Java8Parser.ContinueStatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));
        if (!(treeNode.getType() == null || treeNode.getType().isEmpty())) {
            //elseif 不改变原有节点重新创建if节点
            TreeNode temp = TreeNode.builder().type(CONTINUE).text(getExpressionText(ctx)).interval(treeNode.getInterval()).id(treeNode.getId() + CONTINUE).build();
            record(temp, temp.getInterval());
        } else {
            treeNode.setText(getExpressionText(ctx));
            treeNode.setType(CONTINUE);
        }

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

        if (!(treeNode.getType() == null || treeNode.getType().isEmpty())) {
            //elseif 不改变原有节点重新创建if节点
            TreeNode temp = TreeNode.builder().type(IF).text(getExpressionText(ctx.expression())).interval(treeNode.getInterval()).id(treeNode.getId() + "_elseIf").build();
            record(temp, temp.getInterval());
        } else {
            treeNode.setText(getExpressionText(ctx.expression()));
            treeNode.setType(IF);
        }

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
        if(treeNode.getType().equals(CONDITION)&&root.findNode(getID(ctx)+"X")==null){

            Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());

            TreeNode temp = TreeNode.builder().id(getID(ctx)+"X").text(getExpressionText(ctx)).type(RETURN).interval(interval).build();

            record(temp, interval);
        }else if(root.findNode(getID(ctx)+"X")!=null){
            treeNode=root.findNode(getID(ctx)+"X");
            treeNode.setText(getExpressionText(ctx.expression()));
            treeNode.setType(RETURN);
        }else{
            treeNode.setText(getExpressionText(ctx.expression()));
            treeNode.setType(RETURN);
        }

        return super.visitReturnStatement(ctx);
    }

    @Override
    public TreeNode visitStatementWithoutTrailingSubstatement(Java8Parser.StatementWithoutTrailingSubstatementContext ctx) {
        TreeNode treeNode = root.findNode(getID(ctx));

        if (treeNode == null) {
            Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());

            TreeNode temp = TreeNode.builder().id(getID(ctx)).text(getExpressionText(ctx)).interval(interval).build();

            record(temp, interval);
        }else if(treeNode.getType().contains(CONDITION)){
            if(ctx.expressionStatement()!=null){
                Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());

                TreeNode temp = TreeNode.builder().id(getID(ctx)+"X").text(getExpressionText(ctx)).interval(interval).build();

                record(temp, interval);
            }
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
            }
                for (int i = 1; i < switchBlockStatementGroupContext.children.size(); i++) {
                    visitBlockStatements((Java8Parser.BlockStatementsContext)switchBlockStatementGroupContext.children.get(i));
                }

        }


        Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
        TreeNode temp = TreeNode.builder().id(getID(ctx) + "_switchEnd").text("").type(SWITCH_END).interval(interval).build();
        record(temp, interval);

        return root;
    }

    @Override
    public TreeNode visitTryStatement(Java8Parser.TryStatementContext ctx) {



        if(ctx.block()!=null) {
            stack.removeElement(stack.lastElement());
            root.removeFirst(getID(ctx));
            return super.visitBlock(ctx.block());
        }else{
            return visitTryWithResourcesStatement(ctx.tryWithResourcesStatement());
        }
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
        root.removeFirst(getID(ctx));
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


    @Override
    public TreeNode visitLambdaBody(Java8Parser.LambdaBodyContext ctx) {
        String text= null;
        try {
            String et=getExpressionText(ctx);
            if(et.startsWith("{")&&et.endsWith("}")) {
                text = "<<TABLE><TR><TD HREF=\"/#" + encode(trimBigParenthesis(getExpressionText(ctx)), "UTF-8") + "\" TARGET=\"_Blank\">" + htmlSafeText(lmd.getText().replace(getExpressionText(ctx), "{...}")) + "</TD></TR></TABLE>>";
            }else{
                text=lmd.getText();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        lmd.setText(text);
//        System.out.println("LambdaBody = " + ctx.getText());
//        TreeNode tree = TreeBuilder.parseString(ctx.getText());
//        FlowchartBuilder flowchartBuilder = new DotBuilder();
////        flowchartBuilder.setDebug(true);
//        String r = flowchartBuilder.builder(tree);
//
//        System.out.println("LambdaBody = " + r);
//
//        Interval interval = Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
//        TreeNode temp = TreeNode.builder().id(getID(ctx)).text("lambada").type("Lambada").interval(interval).build();
//        record(temp, interval);
//        return super.visitStatement(ctx.);
        return root;
    }
    private  String trimBigParenthesis(String text){
        if (text.startsWith("{")) {
            if(text.endsWith("}")){
                return text.substring(1,text.length()-1);
            }
        }
        return text;
    }

    private String htmlSafeText(String text){

        text=text.replace("&","&amp;");
//        text=text.replace("\"","&quot");
        text=text.replace("<","&lt;");
        text=text.replace(">","&gt;");
        return text;

    }

    public static String encode(String s, String enc)
            throws UnsupportedEncodingException {

        boolean needToChange = false;
        StringBuffer out = new StringBuffer(s.length());
        Charset charset;
        CharArrayWriter charArrayWriter = new CharArrayWriter();

        if (enc == null) {
            throw new NullPointerException("charsetName");
        }

        try {
            charset = Charset.forName(enc);
        } catch (IllegalCharsetNameException e) {
            throw new UnsupportedEncodingException(enc);
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(enc);
        }

        for (int i = 0; i < s.length();) {
            int c = (int) s.charAt(i);
            //System.out.println("Examining character: " + c);
            if (dontNeedEncoding.get(c)) {
                if (c == ' ') {
//                    c = '+';
                    needToChange = true;
                }
                //System.out.println("Storing: " + c);
                out.append((char)c);
                i++;
            } else {
                // convert to external encoding before hex conversion
                do {
                    charArrayWriter.write(c);
                    /*
                     * If this character represents the start of a Unicode
                     * surrogate pair, then pass in two characters. It's not
                     * clear what should be done if a bytes reserved in the
                     * surrogate pairs range occurs outside of a legal
                     * surrogate pair. For now, just treat it as if it were
                     * any other character.
                     */
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        /*
                          System.out.println(Integer.toHexString(c)
                          + " is high surrogate");
                        */
                        if ( (i+1) < s.length()) {
                            int d = (int) s.charAt(i+1);
                            /*
                              System.out.println("\tExamining "
                              + Integer.toHexString(d));
                            */
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                /*
                                  System.out.println("\t"
                                  + Integer.toHexString(d)
                                  + " is low surrogate");
                                */
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = (int) s.charAt(i))));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba = str.getBytes(charset);
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }

        return (needToChange? out.toString() : s);
    }

    static BitSet dontNeedEncoding;
    static final int caseDiff = ('a' - 'A');
    static String dfltEncName = null;

    static {

        /* The list of characters that are not encoded has been
         * determined as follows:
         *
         * RFC 2396 states:
         * -----
         * Data characters that are allowed in a URI but do not have a
         * reserved purpose are called unreserved.  These include upper
         * and lower case letters, decimal digits, and a limited set of
         * punctuation marks and symbols.
         *
         * unreserved  = alphanum | mark
         *
         * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
         *
         * Unreserved characters can be escaped without changing the
         * semantics of the URI, but this should not be done unless the
         * URI is being used in a context that does not allow the
         * unescaped character to appear.
         * -----
         *
         * It appears that both Netscape and Internet Explorer escape
         * all special characters from this list with the exception
         * of "-", "_", ".", "*". While it is not clear why they are
         * escaping the other characters, perhaps it is safest to
         * assume that there might be contexts in which the others
         * are unsafe if not escaped. Therefore, we will use the same
         * list. It is also noteworthy that this is consistent with
         * O'Reilly's "HTML: The Definitive Guide" (page 164).
         *
         * As a last note, Intenet Explorer does not encode the "@"
         * character which is clearly not unreserved according to the
         * RFC. We are being consistent with the RFC in this matter,
         * as is Netscape.
         *
         */

        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set(' '); /* encoding a space to a + is done
         * in the encode() method */
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');

        dfltEncName = AccessController.doPrivileged(
                new GetPropertyAction("file.encoding")
        );
    }
}
