#include <stdlib.h>
enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Foo_kind kind;
  union {
    struct { int x_Bar; int y_Bar; };
    struct { char* a_Baz; int b_Baz; };
  };
};
int fn$foo(struct Foo* foo) {
  foo->rc ++;
  struct Foo* matchobj$0 = foo;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Bar_tag:
    int x = matchobj$0->x_Bar;
    int yyy = matchobj$0->y_Bar;
    matchres$1 = x + yyy;
    break;
  case Baz_tag:
    char* astr = matchobj$0->a_Baz;
    int b = matchobj$0->b_Baz;
    matchres$1 = b;
    break;
  }
  int ret$2 = matchres$1;
  if (--foo->rc == 0) free(foo);
  return ret$2;
}
int main() {
  struct Foo* ctorres$3 = malloc(sizeof (struct Foo));
  ctorres$3->kind = Bar_tag;
  ctorres$3->x_Bar = 1;
  ctorres$3->y_Bar = 2;
  struct Foo* foo = ctorres$3;
  foo->rc ++;
  int ret$4 = fn$foo(foo);
  if (--foo->rc == 0) free(foo);
  return ret$4;
}
