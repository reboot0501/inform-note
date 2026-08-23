package io.nexcope.inform_note.base.domain.entity;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CodeNameList implements JsonSerializable {
   //
   private List<CodeName> codeNames;

   public CodeNameList(int capacity) {
      this.codeNames = new ArrayList(capacity);
   }

   public CodeNameList() {
      this.codeNames = new ArrayList();
   }

   public CodeNameList(CodeName codeName) {
      this();
      this.codeNames.add(codeName);
   }

   public CodeNameList(String code, String name) {
      this();
      this.codeNames.add(new CodeName(code, name));
   }

   private CodeNameList(List<CodeName> codeNames) {
      this();
      this.codeNames.addAll(codeNames);
   }

   private CodeNameList(CodeNameList codeNames) {
      this();
      this.codeNames.addAll(codeNames.list());
   }

   public static CodeNameList newInstance() {
      return new CodeNameList();
   }

   public static CodeNameList of(String... codeNames) {
      if (codeNames.length != 0 && codeNames.length % 2 == 0) {
         CodeNameList instance = new CodeNameList();

         for(int i = 0; i < codeNames.length / 2; ++i) {
            instance.add(codeNames[i * 2], codeNames[i * 2 + 1]);
         }

         return instance;
      } else {
         throw new IllegalArgumentException("Invalid code-name pairs");
      }
   }

   public static CodeNameList of(CodeName... codeNames) {
      if (codeNames.length == 0) {
         throw new IllegalArgumentException("Empty codeNames");
      } else {
         CodeNameList instance = new CodeNameList();
         CodeName[] var2 = codeNames;
         int var3 = codeNames.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            CodeName codeName = var2[var4];
            instance.add(codeName);
         }

         return instance;
      }
   }

   public static CodeNameList from(List<CodeName> codeNames) {
      return new CodeNameList(codeNames);
   }

   public static CodeNameList from(CodeNameList codeNames) {
      return new CodeNameList(codeNames);
   }

   public static CodeNameList filter(CodeNameList codeNames, String... codes) {
      List<CodeName> filteredCodeNames = codeNames.list().stream().filter((codeName) -> {
         return Arrays.asList(codes).contains(codeName.getCode());
      }).toList();
      return from(filteredCodeNames);
   }

   public String toString() {
      return this.toJson();
   }

   public static CodeNameList fromJson(String json) {
      return (CodeNameList)JsonUtil.fromJson(json, CodeNameList.class);
   }

   public CodeNameList add(CodeName codeName) {
      this.codeNames.add(codeName);
      return this;
   }

   public CodeNameList add(String code, String name) {
      this.codeNames.add(new CodeName(code, name));
      return this;
   }

   public void addAll(List<CodeName> codeNames) {
      this.codeNames.addAll(codeNames);
   }

   public List<CodeName> list() {
      return this.codeNames;
   }

   public void removeByCode(String code) {
      Iterator var2 = this.getByCode(code).iterator();

      while(var2.hasNext()) {
         CodeName codeName = (CodeName)var2.next();
         this.codeNames.remove(codeName);
      }

   }

   public List<CodeName> getByCode(String id) {
      List<CodeName> foundIdNames = new ArrayList();
      Iterator var3 = this.codeNames.iterator();

      while(var3.hasNext()) {
         CodeName codeName = (CodeName)var3.next();
         if (codeName.getCode().equals(id)) {
            foundIdNames.add(codeName);
         }
      }

      return foundIdNames;
   }

   public boolean isEmpty() {
      return this.codeNames.isEmpty();
   }

   public int size() {
      return this.codeNames.size();
   }

   public static CodeNameList sample() {
      return new CodeNameList(CodeName.sample());
   }

   public static void main(String[] args) {
      System.out.println(sample());
   }

   public List<CodeName> getCodeNames() {
      return this.codeNames;
   }

   public void setCodeNames(List<CodeName> codeNames) {
      this.codeNames = codeNames;
   }
}
