package org.jedit.ruby.parser;

import org.jrubyparser.NodeVisitor;
import org.jrubyparser.ast.*;

public abstract class AbstractRubyVisitor implements NodeVisitor {

    /**
     * This method is called by default for each visited Node.
     */
    protected abstract Object visitNode(Node iVisited);

    public Object visitNullNode() {
        return visitNode(null);
    }

    public Object acceptNode(Node node) {
        if (node == null) {
            return visitNullNode();
        } else {
            return node.accept(this);
        }
    }

    @Override
    public Object visitAliasNode(AliasNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitAndNode(AndNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitArgsNode(ArgsNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitArgsCatNode(ArgsCatNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitArgsPushNode(ArgsPushNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitArrayNode(ArrayNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitAttrAssignNode(AttrAssignNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBackRefNode(BackRefNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBeginNode(BeginNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBignumNode(BignumNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBlockArgNode(BlockArgNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBlockArg18Node(BlockArg18Node iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBlockNode(BlockNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBlockPassNode(BlockPassNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitBreakNode(BreakNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitConstDeclNode(ConstDeclNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitClassVarNode(ClassVarNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitCallNode(CallNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitCaseNode(CaseNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitClassNode(ClassNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitColon2Node(Colon2Node iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitColon3Node(Colon3Node iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitConstNode(ConstNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDAsgnNode(DAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDRegxNode(DRegexpNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDStrNode(DStrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDSymbolNode(DSymbolNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDVarNode(DVarNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDXStrNode(DXStrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDefinedNode(DefinedNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDefnNode(DefnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDefsNode(DefsNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitDotNode(DotNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitEncodingNode(EncodingNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitEnsureNode(EnsureNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitEvStrNode(EvStrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitFCallNode(FCallNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitFalseNode(FalseNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitFixnumNode(FixnumNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitFlipNode(FlipNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitFloatNode(FloatNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitForNode(ForNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitGlobalVarNode(GlobalVarNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitHashNode(HashNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitInstAsgnNode(InstAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitInstVarNode(InstVarNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitIfNode(IfNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitIterNode(IterNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitLiteralNode(LiteralNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitLocalAsgnNode(LocalAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitLocalVarNode(LocalVarNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgn19Node iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitMatch2Node(Match2Node iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitMatch3Node(Match3Node iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitMatchNode(MatchNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitModuleNode(ModuleNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitNewlineNode(NewlineNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitNextNode(NextNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitNilNode(NilNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitNotNode(NotNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitNthRefNode(NthRefNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitOpAsgnNode(OpAsgnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitOrNode(OrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitPreExeNode(PreExeNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitPostExeNode(PostExeNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRedoNode(RedoNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRegexpNode(RegexpNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRescueBodyNode(RescueBodyNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRescueNode(RescueNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRestArgNode(RestArgNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRetryNode(RetryNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitReturnNode(ReturnNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitRootNode(RootNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitSClassNode(SClassNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitSelfNode(SelfNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitSplatNode(SplatNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitStrNode(StrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitSuperNode(SuperNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitSValueNode(SValueNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitSymbolNode(SymbolNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitToAryNode(ToAryNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitTrueNode(TrueNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitUndefNode(UndefNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitUntilNode(UntilNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitVAliasNode(VAliasNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitVCallNode(VCallNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitWhenNode(WhenNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitWhileNode(WhileNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitXStrNode(XStrNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitYieldNode(YieldNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitZArrayNode(ZArrayNode iVisited) {
        return visitNode(iVisited);
    }

    @Override
    public Object visitZSuperNode(ZSuperNode iVisited) {
        return visitNode(iVisited);
    }
}