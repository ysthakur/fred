enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  enum Foo_kind kind;
  union {
    struct { Foo* foo; Fred* fred; };
    struct { Bar* blech; int gah; };
  };
};
