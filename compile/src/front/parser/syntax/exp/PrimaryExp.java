package front.parser.syntax.exp;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;


/**
 * PrimaryExp → '(' Exp ')' | LVal | Number
 */
public class PrimaryExp extends BranchNode {
    private Exp exp;
    private LVal lVal;
    private Number num;

    public PrimaryExp() {
        super(SynType.PrimaryExp);
        this.exp = null;
        this.lVal = null;
        this.num = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.Exp) {
            exp = (Exp) child;
        } else if (child.getNodeType() == SynType.LVal) {
            lVal = (LVal) child;
        } else if (child.getNodeType() == SynType.Number) {
            num = (Number) child;
        }
        super.appendChild(child);
    }
    
    /**
     * 获取LVal节点
     * @return LVal节点，如果不存在则返回null
     */
    public LVal getLVal() {
        return lVal;
    }
    
    /**
     * 获取具体数字
     * @return 具体数字
     */
    public int getNumber() {
        return num.getIntValue();
    }
    
    /**
     * 获取Exp节点（括号表达式）
     * @return Exp节点，如果不存在则返回null
     */
    public Exp getExp() {
        return exp;
    }

    public boolean isExp() {
        return exp != null;
    }

    public boolean isLVal() {
        return lVal != null;
    }

    public boolean isNumber() {
        return num != null;
    }

}
