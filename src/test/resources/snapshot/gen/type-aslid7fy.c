enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  enum Foo_kind kind;
  char* common;
  union {
    struct { struct Foo* foo_Bar; struct Fred* fred_Bar; char* notcommon_Bar; };
    struct { char* blech_Baz; int gah_Baz; int notcommon_Baz; };
  };
};
char* foo (struct Foo* param) {
  
  return param->common;
}