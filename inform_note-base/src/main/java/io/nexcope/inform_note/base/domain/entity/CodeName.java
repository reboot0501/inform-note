package io.nexcope.inform_note.base.domain.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.base.util.json.ValueGroup;

import java.util.Objects;
import java.util.StringTokenizer;

public class CodeName implements ValueGroup {
    //
    private String code;
    private String name;

    public static CodeName of(String code, String name) {
        return new CodeName(code, name);
    }

    public static CodeName fromSimpleString(String codeNameStr) {
        StringTokenizer tokenizer = new StringTokenizer(codeNameStr, ":");
        String code = tokenizer.nextToken();
        String name = tokenizer.nextToken();
        return new CodeName(code, name);
    }

    public String toString() {
        return this.toJson();
    }

    public boolean equals(Object target) {
        if (this == target) {
            return true;
        } else if (target != null && this.getClass() == target.getClass()) {
            CodeName codeName = (CodeName) target;
            return Objects.equals(this.code, codeName.code) && Objects.equals(this.name, codeName.name);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.code + this.name});
    }

    public static CodeName fromJson(String json) {
        return (CodeName) JsonUtil.fromJson(json, CodeName.class);
    }

    public String toSimpleString() {
        return this.code + ":" + this.name;
    }

    public static CodeName sample() {
        String code = "1234";
        String name = "NEXTREE";
        return new CodeName(code, name);
    }

    public static void main(String[] args) {
        System.out.println(sample());
        System.out.println(fromSimpleString(sample().toSimpleString()));
    }

    public static CodeNameBuilder builder() {
        return new CodeNameBuilder();
    }

    public String getCode() {
        return this.code;
    }

    public String getName() {
        return this.name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CodeName() {
    }

    public CodeName(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static class CodeNameBuilder {
        private String code;
        private String name;

        CodeNameBuilder() {
        }

        public CodeNameBuilder code(String code) {
            this.code = code;
            return this;
        }

        public CodeNameBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CodeName build() {
            return new CodeName(this.code, this.name);
        }

        public String toString() {
            return "CodeName.CodeNameBuilder(code=" + this.code + ", name=" + this.name + ")";
        }
    }
}
