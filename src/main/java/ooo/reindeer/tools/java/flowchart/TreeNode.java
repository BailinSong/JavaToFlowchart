package ooo.reindeer.tools.java.flowchart;

import lombok.*;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 节点
 *
 * @ClassName Node
 * @Author songbailin
 * @Date 2020/5/28 09:45
 * @Version 1.0
 * @Description TODO
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"parent", "previous"})
@Builder
@With
public class TreeNode {

    @Getter
    @Setter
    Object id;

    @Getter
    @Setter
    String text;

    @Getter
    @Setter
    @Builder.Default()
    String type = "";

    @Getter
    @Setter
    Interval interval;
    @Getter
    @Setter
    @Builder.Default()
    TreeNode parent = null;
    @Getter
    @Setter
    @Builder.Default()
    TreeNode previous = null;
    @Getter
    @Setter
    @Builder.Default()
    private List<TreeNode> sub = new ArrayList<>();



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TreeNode treeNode = (TreeNode) o;
        return Objects.equals(id, treeNode.id);
    }

    TreeNode findNode(TreeNode treeNode, String id) {
        for (TreeNode treeNode1 : treeNode.getSub()) {
            if (treeNode1.getId().equals(id)) {
                return treeNode1;
            } else {
                TreeNode treeNode2 = findNode(treeNode1, id);
                if (treeNode2 != null) {
                    return treeNode2;
                }
            }
        }
        return null;
    }

    public TreeNode findNode(String id) {
        return findNode(this, id);
    }

    TreeNode findPrevious() {
        return (this.getPrevious() == null) ? this.getParent() : this.getPrevious();
    }

    TreeNode findeNearNext() {

        if (lastOfBlock()) {
            if (getParent() != null) {
                return getParent().findeNearNext();
            } else {
                return null;
            }
        } else {
            if (getParent() != null) {
                return getParent().sub.get(getParent().sub.indexOf(this) + 1);
            } else {
                return null;
            }
        }

//        for (TreeNode treeNode : pn.getParent().getSub()) {
//            if(treeNode.getPrevious()!=null&&treeNode.getPrevious().getId().equals(pn)){
//                return treeNode;
//            }
//        }
//        return null;
    }

    /**
     * 得到安全的文本
     *
     * @return {@link String}
     */
    public String getSafeText() {
        return text.replace('"', ' ');
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    boolean lastOfBlock() {
        if (parent != null && parent.sub.size() > 1) {
            return parent.sub.get(parent.sub.size() - 1).equals(this);
        } else {
            return true;
        }
    }

    public void walk(Consumer<TreeNode> action) {
        for (TreeNode treeNode : this.getSub()) {
            walk(treeNode, action);
        }
    }

    public static void walk(TreeNode root, Consumer<TreeNode> action) {
        action.accept(root);
        if (root.sub.size() > 0) {
            for (TreeNode treeNode : root.sub) {
                walk(treeNode, action);
            }
        }
    }
    public void removeFirst(Object id){
        boolean removeIt;
        for (TreeNode treeNode : this.getSub()) {
            if(treeNode.getId().equals(id)){
                removeIt=true;
                break;
            }else{
                treeNode.removeFirst(id);
            }
        }
        this.getSub().removeIf(treeNode -> treeNode.getId().equals(id));
    }
}
