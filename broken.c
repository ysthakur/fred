#include "runtime.h"

enum OptT0_kind { SomeT0_tag, NoneT0_tag };
struct OptT0 {
  int rc;
  enum Color color;
  int addedPCR;
  enum OptT0_kind kind;
  void (*print)();
  union {
    struct { struct T0* value_SomeT0; };
    struct {  };
  };
};
enum T0_kind { T0_tag };
struct T0 {
  int rc;
  enum Color color;
  int addedPCR;
  enum T0_kind kind;
  void (*print)();
  struct OptT0* f0;
  union {
    struct {  };
  };
};
void $free_OptT0(struct OptT0* this);
void $free_T0(struct T0* this);
void $decr_OptT0(struct OptT0* this);
void $decr_T0(struct T0* this);
void $markGray_OptT0(struct OptT0* this);
void $markGray_T0(struct T0* this);
void $scan_OptT0(struct OptT0* this);
void $scan_T0(struct T0* this);
void $scanBlack_OptT0(struct OptT0* this);
void $scanBlack_T0(struct T0* this);
void $collectWhite_OptT0(struct OptT0* this);
void $collectWhite_T0(struct T0* this);
void $print_OptT0(struct OptT0* this);
void $print_T0(struct T0* this);
struct OptT0* new$SomeT0(struct T0* value);
struct OptT0* new$NoneT0();
struct T0* new$T0(struct OptT0* f0);
int main();
void $free_OptT0(struct OptT0* this) {
  fprintf(stderr, "Freeing OptT0\n");
  switch (this->kind) {
  case SomeT0_tag:
    break;
  case NoneT0_tag:
    break;
  }
  free(this);
}
void $free_T0(struct T0* this) {
  fprintf(stderr, "Freeing T0\n");
  switch (this->kind) {
  case T0_tag:
    break;
  }
  free(this);
}
void $decr_OptT0(struct OptT0* this) {
  fprintf(stderr, "Decrementing OptT0 (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case SomeT0_tag:
      $decr_T0(this->value_SomeT0);
      break;
    case NoneT0_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_OptT0,
      (void *) $scan_OptT0,
      (void *) $collectWhite_OptT0);
  }
}
void $decr_T0(struct T0* this) {
  fprintf(stderr, "Decrementing T0 (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case T0_tag:
      $decr_OptT0(this->f0);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_T0,
      (void *) $scan_T0,
      (void *) $collectWhite_T0);
  }
}
void $markGray_OptT0(struct OptT0* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case SomeT0_tag:
    this->value_SomeT0->rc --;
    $markGray_T0(this->value_SomeT0);
    break;
  case NoneT0_tag:
    break;
  }
}
void $markGray_T0(struct T0* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case T0_tag:
    this->f0->rc --;
    $markGray_OptT0(this->f0);
    break;
  }
}
void $scan_OptT0(struct OptT0* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_OptT0(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case SomeT0_tag:
    $scan_T0(this->value_SomeT0);
    break;
  case NoneT0_tag:
    break;
  }
}
void $scan_T0(struct T0* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_T0(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case T0_tag:
    $scan_OptT0(this->f0);
    break;
  }
}
void $scanBlack_OptT0(struct OptT0* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case SomeT0_tag:
      this->value_SomeT0->rc ++;
      $scanBlack_T0(this->value_SomeT0);
      break;
    case NoneT0_tag:
      break;
    }
  }
}
void $scanBlack_T0(struct T0* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case T0_tag:
      this->f0->rc ++;
      $scanBlack_OptT0(this->f0);
      break;
    }
  }
}
void $collectWhite_OptT0(struct OptT0* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case SomeT0_tag:
      $collectWhite_T0(this->value_SomeT0);
      break;
    case NoneT0_tag:
      break;
    }
    fprintf(stderr, "Removing OptT0\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_OptT0;
  }
}
void $collectWhite_T0(struct T0* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case T0_tag:
      $collectWhite_OptT0(this->f0);
      break;
    }
    fprintf(stderr, "Removing T0\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_T0;
  }
}
void $print_OptT0(struct OptT0* this) {
  switch (this->kind) {
  case SomeT0_tag:
    printf("SomeT0 {");
    printf("value=");
    $print_T0(this->value_SomeT0);
    printf(", ");
    printf("}");
    break;
  case NoneT0_tag:
    printf("NoneT0 {");
    printf("}");
    break;
  }
}
void $print_T0(struct T0* this) {
  switch (this->kind) {
  case T0_tag:
    printf("T0 {");
    printf("f0=");
    $print_OptT0(this->f0);
    printf(", ");
    printf("}");
    break;
  }
}
struct OptT0* new$SomeT0(struct T0* value) {
  struct OptT0* $res = malloc(sizeof (struct OptT0));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_OptT0;
  $res->kind = SomeT0_tag;
  $res->value_SomeT0 = value;
  $res->value_SomeT0->rc ++;
  return $res;
}
struct OptT0* new$NoneT0() {
  struct OptT0* $res = malloc(sizeof (struct OptT0));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_OptT0;
  $res->kind = NoneT0_tag;
  return $res;
}
struct T0* new$T0(struct OptT0* f0) {
  struct T0* $res = malloc(sizeof (struct T0));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_T0;
  $res->kind = T0_tag;
  $res->f0 = f0;
  $res->f0->rc ++;
  return $res;
}
int main() {
  struct T0* vT0_0 = new$T0(new$NoneT0());
  vT0_0->rc ++;
  struct T0* vT0_2 = new$T0(new$NoneT0());
  vT0_2->rc ++;
  struct T0* vT0_1 = new$T0(new$NoneT0());
  vT0_1->rc ++;
  struct T0* vT0_6 = new$T0(new$NoneT0());
  vT0_6->rc ++;
  struct T0* vT0_5 = new$T0(new$NoneT0());
  vT0_5->rc ++;
  struct T0* vT0_3 = new$T0(new$NoneT0());
  vT0_3->rc ++;
  struct T0* vT0_4 = new$T0(new$NoneT0());
  vT0_4->rc ++;
  struct OptT0* oldValue$0 = vT0_5->f0;
  vT0_5->f0 = new$SomeT0(vT0_6);
  vT0_5->f0->rc ++;
  $decr_OptT0(oldValue$0);
  struct OptT0* oldValue$1 = vT0_0->f0;
  vT0_0->f0 = new$SomeT0(vT0_0);
  vT0_0->f0->rc ++;
  $decr_OptT0(oldValue$1);
  struct OptT0* oldValue$2 = vT0_3->f0;
  vT0_3->f0 = new$SomeT0(vT0_5);
  vT0_3->f0->rc ++;
  $decr_OptT0(oldValue$2);
  vT0_3->f0;
  vT0_0->f0;
  vT0_5->f0;
  int ret$3 = 0;
  $decr_T0(vT0_4);
  $decr_T0(vT0_3);
  $decr_T0(vT0_5);
  $decr_T0(vT0_6);
  $decr_T0(vT0_1);
  $decr_T0(vT0_2);
  $decr_T0(vT0_0);
  processAllPCRs();
  return ret$3;
}
