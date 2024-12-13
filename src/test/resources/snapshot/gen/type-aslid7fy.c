#include "runtime.h"

enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  char* common;
  union {
    struct { struct Foo* foo_Bar; char* notcommon_Bar; };
    struct { char* blech_Baz; int gah_Baz; int notcommon_Baz; };
  };
};
void $free_Foo(struct Foo* this);
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
void $print_Foo(struct Foo* this);
struct Foo* new$Bar(char* common, struct Foo* foo, char* notcommon);
struct Foo* new$Baz(char* blech, char* common, int gah, int notcommon);
char* fn$foo(struct Foo* param);
void $free_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    break;
  case Baz_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Bar_tag:
      $decr_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_Foo,
      (void *) $scan_Foo,
      (void *) $collectWhite_Foo);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Bar_tag:
    this->foo_Bar->rc --;
    $markGray_Foo(this->foo_Bar);
    break;
  case Baz_tag:
    break;
  }
}
void $scan_Foo(struct Foo* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Foo(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Bar_tag:
    $scan_Foo(this->foo_Bar);
    break;
  case Baz_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      this->foo_Bar->rc ++;
      $scanBlack_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      $collectWhite_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Foo;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    printf("Bar {");
    printf("foo=");
    $print_Foo(this->foo_Bar);
    printf(", ");
    printf("common=");
    printf("%s", this->common);
    printf(", ");
    printf("notcommon=");
    printf("%s", this->notcommon_Bar);
    printf(", ");
    printf("}");
    break;
  case Baz_tag:
    printf("Baz {");
    printf("blech=");
    printf("%s", this->blech_Baz);
    printf(", ");
    printf("gah=");
    printf("%d", this->gah_Baz);
    printf(", ");
    printf("common=");
    printf("%s", this->common);
    printf(", ");
    printf("notcommon=");
    printf("%d", this->notcommon_Baz);
    printf(", ");
    printf("}");
    break;
  }
}
struct Foo* new$Bar(char* common, struct Foo* foo, char* notcommon) {
  struct Foo* $res = malloc(sizeof (struct Foo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Foo;
  $res->kind = Bar_tag;
  $res->foo_Bar = foo;
  $res->foo_Bar->rc ++;
  $res->common = common;
  $res->notcommon_Bar = notcommon;
  return $res;
}
struct Foo* new$Baz(char* blech, char* common, int gah, int notcommon) {
  struct Foo* $res = malloc(sizeof (struct Foo));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Foo;
  $res->kind = Baz_tag;
  $res->blech_Baz = blech;
  $res->gah_Baz = gah;
  $res->common = common;
  $res->notcommon_Baz = notcommon;
  return $res;
}
char* fn$foo(struct Foo* param) {
  param->rc ++;
  char* ret$0 = param->common;
  $decr_Foo(param);
  return ret$0;
}
