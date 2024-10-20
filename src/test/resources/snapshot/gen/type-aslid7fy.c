#include <stdlib.h>
#include <stdio.h>
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
void $del_Foo(struct Foo* obj);
void $decr_Foo(struct Foo* obj);
char* fn$foo(struct Foo* param);
void $del_Foo(struct Foo* obj) {
  switch (obj->kind) {
  case Bar_tag:
    $decr_Foo(obj->foo_Bar);
    break;
  case Baz_tag:
    break;
  }
  free(obj);
}
void $decr_Foo(struct Foo* obj) {
  if (--obj->rc == 0) {
    $del_Foo(obj);
    return;
  }
  switch (obj->kind) {
  case Bar_tag:
    break;
  case Baz_tag:
    break;
  }
}
char* fn$foo(struct Foo* param) {
  param->rc ++;
  char* ret$0 = param->common;
  $decr_Foo(param);
  return ret$0;
}
