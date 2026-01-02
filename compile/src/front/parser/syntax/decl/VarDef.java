package front.parser.syntax.decl;

import front.parser.syntax.AstNode;
import front.parser.syntax.BranchNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.exp.ConstExp;

import java.util.ArrayList;


/**
 * VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
 * 包含普通常量、一维数组定义
 * 我这里的做法支持多维数组
 * TODO
 */
public class VarDef extends BranchNode {
    private boolean IS_GETINTK;
    private TokenNode identifier;
    private ArrayList<ConstExp> exps;
    private InitVal initVal;

    public VarDef() {
        super(SynType.VarDef);
        this.IS_GETINTK = false;
        this.identifier = null;
        this.exps = new ArrayList<>();
        this.initVal = null;
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.GETINTTK) {
            this.IS_GETINTK = true;
        } else if (child.getNodeType() == SynType.IDENFR) {
            this.identifier = (TokenNode) child;
        } else if (child.getNodeType() == SynType.ConstExp) {
            this.exps.add((ConstExp) child);
        } else if (child.getNodeType() == SynType.InitVal) {
            this.initVal = (InitVal) child;
        }

        super.appendChild(child);
    }

    /**
     * 获取变量定义的标识符节点
     * @return 标识符的TokenNode，如果不存在则返回null
     */
    public TokenNode getIdentifier() {
        return identifier;
    }

    /**
     * 获取数组维度的常量表达式列表
     * @return 常量表达式列表，如果不是数组则为空列表
     */
    public ArrayList<ConstExp> getConstExps() {
        return new ArrayList<>(exps);
    }

    /**
     * 获取变量初值
     * @return 初值节点，如果不存在则返回null
     */
    public InitVal getInitValue() {
        return initVal;
    }

    /**
     * 判断是否为数组变量
     * @return 如果是数组变量返回true，否则返回false
     */
    public boolean isArray() {
        return !exps.isEmpty();
    }

    /**
     * 获取数组维度数
     * @return 数组维度数，如果不是数组则返回0
     */
    public int getDimension() {
        return exps.size();
    }

    /**
     * 判断是否有初值
     * @return 如果有初值返回true，否则返回false
     */
    public boolean hasInitValue() {
        return initVal != null;
    }

    /**
     * 判断是否为getint特殊变量
     * @return 如果是getint变量返回true，否则返回false
     */
    public boolean isGetintVariable() {
        return IS_GETINTK;
    }
}
