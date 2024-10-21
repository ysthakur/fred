#include <stdlib.h>
#include <stdio.h>

enum Color { kBlack, kGray, kWhite };
enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Foo_kind kind;
  int field;
  union {
    struct {  };
  };
};
void $decr_Foo(struct Foo* this);
int fn$bar(struct Foo* f);
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    free(this);
  } else {
    // todo
  }
}
int fn$bar(struct Foo* f) {
  f->rc ++;
  f->field = 5;
  int ret$0 = f->field = 6;
  $decr_Foo(f);
  return ret$0;
}
