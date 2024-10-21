#include <stdlib.h>
#include <stdio.h>

enum Color { kBlack, kGray, kWhite };
enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Color color;
  enum Foo_kind kind;
  int field;
  union {
    struct {  };
  };
};
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
int fn$bar(struct Foo* f);
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    free(this);
  } else {
    $markGray_Foo(this);
    $scan_Foo(this);
    $collectWhite_Foo(this);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Foo_tag:
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
  case Foo_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    free(this);
  }
}
int fn$bar(struct Foo* f) {
  f->rc ++;
  f->field = 5;
  int ret$0 = f->field = 6;
  $decr_Foo(f);
  return ret$0;
}
