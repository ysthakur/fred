#include <stdlib.h>
#include <stdio.h>

enum Color { kBlack, kGray, kWhite };
enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Foo_kind kind;
  char* common;
  union {
    struct { struct Foo* foo_Bar; char* notcommon_Bar; };
    struct { char* blech_Baz; int gah_Baz; int notcommon_Baz; };
  };
};
void $decr_Foo(struct Foo* this);
char* fn$foo(struct Foo* param);
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Bar_tag:
      $decr_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
    free(this);
  } else {
    // todo
  }
}
char* fn$foo(struct Foo* param) {
  param->rc ++;
  char* ret$0 = param->common;
  $decr_Foo(param);
  return ret$0;
}
