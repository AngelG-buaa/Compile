package front.parser.syntax.stmt;


import front.parser.syntax.AstNode;
import front.parser.syntax.SynType;
import front.parser.syntax.TokenNode;
import front.parser.syntax.exp.Exp;

import java.util.ArrayList;

/**
 * 对应Stmt → 'printf' '(' FormatString {',' Exp} ')' ';'
 */
public class PrintStmt extends Stmt {
    private TokenNode format;
    private ArrayList<Exp> exps;

    public PrintStmt() {
        super();
        this.format = null;
        this.exps = new ArrayList<>();
    }

    @Override
    public void appendChild(AstNode child) {
        if (child.getNodeType() == SynType.STRCON) {
            format = (TokenNode) child;
        } else if (child.getNodeType() == SynType.Exp) {
            exps.add((Exp) child);
        }

        super.appendChild(child);
    }
    
    public ArrayList<String> splitFormat() {
        String formatStr = format.getContent();
        // 去掉首尾引号（若存在）
        if (formatStr != null && formatStr.length() >= 2
                && formatStr.charAt(0) == '\"'
                && formatStr.charAt(formatStr.length() - 1) == '\"') {
            formatStr = formatStr.substring(1, formatStr.length() - 1);
        }

        ArrayList<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < formatStr.length(); i++) {
            char c = formatStr.charAt(i);
            if (c == '%' && i + 1 < formatStr.length()) {
                // 先把累积的纯文本段推入
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
                char next = formatStr.charAt(i + 1);
                if (next == 'd' || next == 'c') {
                    parts.add("%" + next);
                    i++; // 跳过占位符的类型字符
                } else {
                    // 非法或非支持的占位符，按普通字符处理 '%'
                    current.append('%');
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }
}
