#include <stdlib.h>
#include <stdio.h>
enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Foo_kind kind;
  char* common;
  union {
    struct { struct Foo* foo_Bar; struct Fred* fred_Bar; char* notcommon_Bar; };
    struct { char* blech_Baz; int gah_Baz; int notcommon_Baz; };
  };
};
char* fn$foo(struct Foo* param) {
  param->rc ++;
  char* ret$0 = param->common;
  if (--param->rc == 0) free(param);
  return ret$0;
}
