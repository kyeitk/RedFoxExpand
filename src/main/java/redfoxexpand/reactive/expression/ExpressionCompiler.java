package redfoxexpand.reactive.expression;

import redfoxexpand.reactive.ReactiveLimits;
import redfoxexpand.reactive.runtime.RuntimeContext;
import redfoxexpand.reactive.value.RuntimeValue;
import redfoxexpand.reactive.value.ValueType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict, bounded tokenizer/parser/type-checker for the Schema v3 expression language. */
public final class ExpressionCompiler {
    private final Map<String, ValueType> variables;
    private final Set<String> unsupportedVariables;

    public ExpressionCompiler(Map<String, ValueType> variables) {
        this(variables, Collections.<String>emptySet());
    }

    public ExpressionCompiler(Map<String, ValueType> variables, Set<String> unsupportedVariables) {
        if (variables == null) throw new IllegalArgumentException("variables must not be null");
        this.variables = variables;
        this.unsupportedVariables = unsupportedVariables == null
                ? Collections.<String>emptySet() : unsupportedVariables;
    }

    public CompiledExpression compile(String source) {
        if (source == null) throw failure("expression must not be null");
        if (source.length() == 0) throw failure("expression must not be empty");
        if (source.length() > ReactiveLimits.MAX_EXPRESSION_CHARS) {
            throw failure("expression exceeds " + ReactiveLimits.MAX_EXPRESSION_CHARS + " characters");
        }
        List<Token> tokens = tokenize(source);
        Parser parser = new Parser(source, tokens, variables, unsupportedVariables);
        CompiledExpression.Node root = parser.parseExpression();
        parser.require(TokenType.END, "unexpected token");
        if (root.depth() > ReactiveLimits.MAX_EXPRESSION_DEPTH) {
            throw failure("expression depth exceeds " + ReactiveLimits.MAX_EXPRESSION_DEPTH);
        }
        return new CompiledExpression(source, root);
    }

    private static List<Token> tokenize(String source) {
        List<Token> result = new ArrayList<Token>();
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            int start = index;
            if (Character.isDigit(current) || (current == '.' && index + 1 < source.length()
                    && Character.isDigit(source.charAt(index + 1)))) {
                index = numberEnd(source, index);
                String raw = source.substring(start, index);
                double value;
                try {
                    value = Double.parseDouble(raw);
                } catch (NumberFormatException error) {
                    throw at(source, start, "invalid number: " + raw);
                }
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    throw at(source, start, "number must be finite");
                }
                add(result, new Token(TokenType.NUMBER, raw, RuntimeValue.number(value), start));
                continue;
            }
            if (Character.isLetter(current) || current == '_') {
                index++;
                while (index < source.length()) {
                    char next = source.charAt(index);
                    if (!Character.isLetterOrDigit(next) && next != '_' && next != '.') break;
                    index++;
                }
                String name = source.substring(start, index);
                if ("true".equals(name)) add(result, new Token(TokenType.VALUE, name, RuntimeValue.bool(true), start));
                else if ("false".equals(name)) add(result, new Token(TokenType.VALUE, name, RuntimeValue.bool(false), start));
                else add(result, new Token(TokenType.IDENTIFIER, name, null, start));
                continue;
            }
            if (current == '"') {
                StringBuilder value = new StringBuilder();
                index++;
                boolean closed = false;
                while (index < source.length()) {
                    char next = source.charAt(index++);
                    if (next == '"') {
                        closed = true;
                        break;
                    }
                    if (next == '\\') {
                        if (index >= source.length()) throw at(source, start, "unterminated string escape");
                        char escaped = source.charAt(index++);
                        if (escaped == '"' || escaped == '\\') value.append(escaped);
                        else if (escaped == 'n') value.append('\n');
                        else if (escaped == 'r') value.append('\r');
                        else if (escaped == 't') value.append('\t');
                        else throw at(source, index - 1, "unsupported string escape");
                    } else {
                        value.append(next);
                    }
                }
                if (!closed) throw at(source, start, "unterminated string literal");
                add(result, new Token(TokenType.VALUE, source.substring(start, index),
                        RuntimeValue.string(value.toString()), start));
                continue;
            }
            TokenType type;
            String text;
            if (index + 1 < source.length()) {
                text = source.substring(index, index + 2);
                type = twoCharacter(text);
                if (type != null) {
                    add(result, new Token(type, text, null, start));
                    index += 2;
                    continue;
                }
            }
            text = String.valueOf(current);
            type = oneCharacter(current);
            if (type == null) throw at(source, start, "illegal token: " + text);
            add(result, new Token(type, text, null, start));
            index++;
        }
        add(result, new Token(TokenType.END, "", null, source.length()));
        return result;
    }

    private static int numberEnd(String source, int index) {
        boolean dot = false;
        while (index < source.length()) {
            char value = source.charAt(index);
            if (Character.isDigit(value)) {
                index++;
            } else if (value == '.' && !dot) {
                dot = true;
                index++;
            } else {
                break;
            }
        }
        if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            index++;
            if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
            int digits = index;
            while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            if (digits == index) return source.length();
        }
        return index;
    }

    private static void add(List<Token> tokens, Token token) {
        if (tokens.size() >= ReactiveLimits.MAX_EXPRESSION_TOKENS) {
            throw failure("expression exceeds " + ReactiveLimits.MAX_EXPRESSION_TOKENS + " tokens");
        }
        tokens.add(token);
    }

    private static TokenType twoCharacter(String value) {
        if ("<=".equals(value)) return TokenType.LE;
        if (">=".equals(value)) return TokenType.GE;
        if ("==".equals(value)) return TokenType.EQ;
        if ("!=".equals(value)) return TokenType.NE;
        if ("&&".equals(value)) return TokenType.AND;
        if ("||".equals(value)) return TokenType.OR;
        return null;
    }

    private static TokenType oneCharacter(char value) {
        switch (value) {
            case '<': return TokenType.LT;
            case '>': return TokenType.GT;
            case '!': return TokenType.NOT;
            case '+': return TokenType.PLUS;
            case '-': return TokenType.MINUS;
            case '*': return TokenType.STAR;
            case '/': return TokenType.SLASH;
            case '(': return TokenType.LEFT_PAREN;
            case ')': return TokenType.RIGHT_PAREN;
            case ',': return TokenType.COMMA;
            default: return null;
        }
    }

    private static IllegalArgumentException failure(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException at(String source, int position, String message) {
        return failure(message + " at character " + position + " in `" + source + "`");
    }

    private enum TokenType {
        NUMBER, VALUE, IDENTIFIER,
        LT, LE, GT, GE, EQ, NE, AND, OR, NOT,
        PLUS, MINUS, STAR, SLASH,
        LEFT_PAREN, RIGHT_PAREN, COMMA, END
    }

    private static final class Token {
        final TokenType type;
        final String text;
        final RuntimeValue value;
        final int position;

        Token(TokenType type, String text, RuntimeValue value, int position) {
            this.type = type;
            this.text = text;
            this.value = value;
            this.position = position;
        }
    }

    private static final class Parser {
        private final String source;
        private final List<Token> tokens;
        private final Map<String, ValueType> variables;
        private final Set<String> unsupportedVariables;
        private int index;

        Parser(String source, List<Token> tokens, Map<String, ValueType> variables,
               Set<String> unsupportedVariables) {
            this.source = source;
            this.tokens = tokens;
            this.variables = variables;
            this.unsupportedVariables = unsupportedVariables;
        }

        CompiledExpression.Node parseExpression() {
            return parseOr();
        }

        private CompiledExpression.Node parseOr() {
            CompiledExpression.Node node = parseAnd();
            while (match(TokenType.OR)) node = binary("||", node, parseAnd(), ValueType.BOOLEAN, ValueType.BOOLEAN);
            return node;
        }

        private CompiledExpression.Node parseAnd() {
            CompiledExpression.Node node = parseEquality();
            while (match(TokenType.AND)) node = binary("&&", node, parseEquality(), ValueType.BOOLEAN, ValueType.BOOLEAN);
            return node;
        }

        private CompiledExpression.Node parseEquality() {
            CompiledExpression.Node node = parseComparison();
            while (peek(TokenType.EQ) || peek(TokenType.NE)) {
                String operation = next().text;
                CompiledExpression.Node right = parseComparison();
                if (node.type() != right.type()) throw here("equality operands must have the same type");
                node = checked(new BinaryNode(operation, node, right, ValueType.BOOLEAN));
            }
            return node;
        }

        private CompiledExpression.Node parseComparison() {
            CompiledExpression.Node node = parseAdditive();
            while (peek(TokenType.LT) || peek(TokenType.LE) || peek(TokenType.GT) || peek(TokenType.GE)) {
                String operation = next().text;
                node = binary(operation, node, parseAdditive(), ValueType.NUMBER, ValueType.BOOLEAN);
            }
            return node;
        }

        private CompiledExpression.Node parseAdditive() {
            CompiledExpression.Node node = parseMultiplicative();
            while (peek(TokenType.PLUS) || peek(TokenType.MINUS)) {
                String operation = next().text;
                node = binary(operation, node, parseMultiplicative(), ValueType.NUMBER, ValueType.NUMBER);
            }
            return node;
        }

        private CompiledExpression.Node parseMultiplicative() {
            CompiledExpression.Node node = parseUnary();
            while (peek(TokenType.STAR) || peek(TokenType.SLASH)) {
                String operation = next().text;
                node = binary(operation, node, parseUnary(), ValueType.NUMBER, ValueType.NUMBER);
            }
            return node;
        }

        private CompiledExpression.Node parseUnary() {
            if (match(TokenType.NOT)) return unary("!", parseUnary(), ValueType.BOOLEAN);
            if (match(TokenType.MINUS)) return unary("-", parseUnary(), ValueType.NUMBER);
            if (match(TokenType.PLUS)) return unary("+", parseUnary(), ValueType.NUMBER);
            return parsePrimary();
        }

        private CompiledExpression.Node parsePrimary() {
            if (peek(TokenType.NUMBER) || peek(TokenType.VALUE)) {
                return new LiteralNode(next().value);
            }
            if (match(TokenType.LEFT_PAREN)) {
                CompiledExpression.Node value = parseExpression();
                require(TokenType.RIGHT_PAREN, "missing closing parenthesis");
                return value;
            }
            if (peek(TokenType.IDENTIFIER)) {
                Token identifier = next();
                if (match(TokenType.LEFT_PAREN)) return function(identifier);
                ValueType type = variables.get(identifier.text);
                if (type == null) throw at(source, identifier.position, "unknown variable: " + identifier.text);
                if (unsupportedVariables.contains(identifier.text)) {
                    throw at(source, identifier.position,
                            "unsupported capability for variable: " + identifier.text);
                }
                return new VariableNode(identifier.text, type);
            }
            throw here("expected literal, variable, function, or group");
        }

        private CompiledExpression.Node function(Token identifier) {
            List<CompiledExpression.Node> arguments = new ArrayList<CompiledExpression.Node>();
            if (!peek(TokenType.RIGHT_PAREN)) {
                do {
                    if (arguments.size() >= ReactiveLimits.MAX_FUNCTION_ARGUMENTS) {
                        throw at(source, identifier.position, "function has too many arguments");
                    }
                    arguments.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            require(TokenType.RIGHT_PAREN, "missing function closing parenthesis");
            int required;
            if ("abs".equals(identifier.text)) required = 1;
            else if ("min".equals(identifier.text) || "max".equals(identifier.text)
                    || "hypot".equals(identifier.text)) required = 2;
            else if ("clamp".equals(identifier.text) || "lerp".equals(identifier.text)) required = 3;
            else throw at(source, identifier.position, "unknown function: " + identifier.text);
            if (arguments.size() != required) {
                throw at(source, identifier.position, identifier.text + " requires " + required + " argument(s)");
            }
            for (CompiledExpression.Node argument : arguments) {
                requireType(argument, ValueType.NUMBER, "function arguments must be numbers");
            }
            return checked(new FunctionNode(identifier.text, arguments));
        }

        private CompiledExpression.Node binary(String operation, CompiledExpression.Node left,
                                               CompiledExpression.Node right, ValueType input,
                                               ValueType output) {
            requireType(left, input, "left operand of " + operation + " must be " + input);
            requireType(right, input, "right operand of " + operation + " must be " + input);
            return checked(new BinaryNode(operation, left, right, output));
        }

        private CompiledExpression.Node unary(String operation, CompiledExpression.Node value, ValueType input) {
            requireType(value, input, "operand of " + operation + " must be " + input);
            return checked(new UnaryNode(operation, value));
        }

        private CompiledExpression.Node checked(CompiledExpression.Node node) {
            if (node.depth() > ReactiveLimits.MAX_EXPRESSION_DEPTH) {
                throw here("expression depth exceeds " + ReactiveLimits.MAX_EXPRESSION_DEPTH);
            }
            return node;
        }

        private void requireType(CompiledExpression.Node node, ValueType expected, String message) {
            if (node.type() != expected) throw here(message);
        }

        private boolean match(TokenType type) {
            if (!peek(type)) return false;
            index++;
            return true;
        }

        private boolean peek(TokenType type) {
            return tokens.get(index).type == type;
        }

        private Token next() {
            return tokens.get(index++);
        }

        void require(TokenType type, String message) {
            if (!match(type)) throw here(message);
        }

        private IllegalArgumentException here(String message) {
            return at(source, tokens.get(index).position, message);
        }
    }

    private static final class LiteralNode implements CompiledExpression.Node {
        private final RuntimeValue value;

        LiteralNode(RuntimeValue value) {
            this.value = value;
        }

        public RuntimeValue evaluate(RuntimeContext context) { return value; }
        public ValueType type() { return value.getType(); }
        public int depth() { return 1; }
    }

    private static final class VariableNode implements CompiledExpression.Node {
        private final String name;
        private final ValueType type;

        VariableNode(String name, ValueType type) {
            this.name = name;
            this.type = type;
        }

        public RuntimeValue evaluate(RuntimeContext context) {
            RuntimeValue value = context.get(name);
            if (value == null) throw new ExpressionEvaluationException("missing runtime variable: " + name);
            if (value.getType() != type) throw new ExpressionEvaluationException("runtime type changed for " + name);
            return value;
        }

        public ValueType type() { return type; }
        public int depth() { return 1; }
    }

    private static final class UnaryNode implements CompiledExpression.Node {
        private final String operation;
        private final CompiledExpression.Node value;

        UnaryNode(String operation, CompiledExpression.Node value) {
            this.operation = operation;
            this.value = value;
        }

        public RuntimeValue evaluate(RuntimeContext context) {
            RuntimeValue evaluated = value.evaluate(context);
            if ("!".equals(operation)) return RuntimeValue.bool(!evaluated.asBoolean());
            if ("-".equals(operation)) return finite(-evaluated.asNumber(), "unary minus");
            return finite(evaluated.asNumber(), "unary plus");
        }

        public ValueType type() { return value.type(); }
        public int depth() { return 1 + value.depth(); }
    }

    private static final class BinaryNode implements CompiledExpression.Node {
        private final String operation;
        private final CompiledExpression.Node left;
        private final CompiledExpression.Node right;
        private final ValueType type;

        BinaryNode(String operation, CompiledExpression.Node left,
                   CompiledExpression.Node right, ValueType type) {
            this.operation = operation;
            this.left = left;
            this.right = right;
            this.type = type;
        }

        public RuntimeValue evaluate(RuntimeContext context) {
            RuntimeValue first = left.evaluate(context);
            if ("&&".equals(operation) && !first.asBoolean()) return RuntimeValue.bool(false);
            if ("||".equals(operation) && first.asBoolean()) return RuntimeValue.bool(true);
            RuntimeValue second = right.evaluate(context);
            if ("&&".equals(operation)) return RuntimeValue.bool(second.asBoolean());
            if ("||".equals(operation)) return RuntimeValue.bool(second.asBoolean());
            if ("==".equals(operation) || "!=".equals(operation)) {
                boolean equal;
                if (first.getType() == ValueType.NUMBER) equal = first.asNumber() == second.asNumber();
                else if (first.getType() == ValueType.BOOLEAN) equal = first.asBoolean() == second.asBoolean();
                else equal = first.asString().equals(second.asString());
                return RuntimeValue.bool("==".equals(operation) ? equal : !equal);
            }
            double a = first.asNumber();
            double b = second.asNumber();
            if ("<".equals(operation)) return RuntimeValue.bool(a < b);
            if ("<=".equals(operation)) return RuntimeValue.bool(a <= b);
            if (">".equals(operation)) return RuntimeValue.bool(a > b);
            if (">=".equals(operation)) return RuntimeValue.bool(a >= b);
            if ("+".equals(operation)) return finite(a + b, "addition");
            if ("-".equals(operation)) return finite(a - b, "subtraction");
            if ("*".equals(operation)) return finite(a * b, "multiplication");
            if (b == 0.0D) throw new ExpressionEvaluationException("division by zero");
            return finite(a / b, "division");
        }

        public ValueType type() { return type; }
        public int depth() { return 1 + Math.max(left.depth(), right.depth()); }
    }

    private static final class FunctionNode implements CompiledExpression.Node {
        private final String name;
        private final List<CompiledExpression.Node> arguments;
        private final int depth;

        FunctionNode(String name, List<CompiledExpression.Node> arguments) {
            this.name = name;
            this.arguments = new ArrayList<CompiledExpression.Node>(arguments);
            int maximum = 0;
            for (CompiledExpression.Node argument : arguments) maximum = Math.max(maximum, argument.depth());
            this.depth = maximum + 1;
        }

        public RuntimeValue evaluate(RuntimeContext context) {
            double first = arguments.get(0).evaluate(context).asNumber();
            if ("abs".equals(name)) return finite(Math.abs(first), "abs");
            double second = arguments.get(1).evaluate(context).asNumber();
            if ("min".equals(name)) return finite(Math.min(first, second), "min");
            if ("max".equals(name)) return finite(Math.max(first, second), "max");
            if ("hypot".equals(name)) return finite(Math.hypot(first, second), "hypot");
            double third = arguments.get(2).evaluate(context).asNumber();
            if ("clamp".equals(name)) {
                if (second > third) throw new ExpressionEvaluationException("clamp minimum exceeds maximum");
                return finite(Math.max(second, Math.min(third, first)), "clamp");
            }
            return finite(first + (second - first) * third, "lerp");
        }

        public ValueType type() { return ValueType.NUMBER; }
        public int depth() { return depth; }
    }

    private static RuntimeValue finite(double value, String operation) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ExpressionEvaluationException(operation + " produced a non-finite number");
        }
        return RuntimeValue.number(value);
    }
}
