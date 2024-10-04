enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  enum Foo_kind kind;
  union {
    struct { Foo* foo_Bar; Fred* fred_Bar; };
    struct { char* blech_Baz; int gah_Baz; };
  };
};
