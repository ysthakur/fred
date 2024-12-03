#include "memcheck.h"
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
  struct OptT0* f1;
  struct OptT0* f3;
  struct OptT0* f0;
  struct OptT0* f4;
  struct OptT0* f2;
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
struct T0* new$T0(struct OptT0* f0, struct OptT0* f1, struct OptT0* f2, struct OptT0* f3, struct OptT0* f4);
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
      $decr_OptT0(this->f1);
      $decr_OptT0(this->f3);
      $decr_OptT0(this->f0);
      $decr_OptT0(this->f4);
      $decr_OptT0(this->f2);
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
    this->f1->rc --;
    $markGray_OptT0(this->f1);
    this->f3->rc --;
    $markGray_OptT0(this->f3);
    this->f0->rc --;
    $markGray_OptT0(this->f0);
    this->f4->rc --;
    $markGray_OptT0(this->f4);
    this->f2->rc --;
    $markGray_OptT0(this->f2);
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
    $scan_OptT0(this->f1);
    $scan_OptT0(this->f3);
    $scan_OptT0(this->f0);
    $scan_OptT0(this->f4);
    $scan_OptT0(this->f2);
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
      this->f1->rc ++;
      $scanBlack_OptT0(this->f1);
      this->f3->rc ++;
      $scanBlack_OptT0(this->f3);
      this->f0->rc ++;
      $scanBlack_OptT0(this->f0);
      this->f4->rc ++;
      $scanBlack_OptT0(this->f4);
      this->f2->rc ++;
      $scanBlack_OptT0(this->f2);
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
      $collectWhite_OptT0(this->f1);
      $collectWhite_OptT0(this->f3);
      $collectWhite_OptT0(this->f0);
      $collectWhite_OptT0(this->f4);
      $collectWhite_OptT0(this->f2);
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
    printf("f1=");
    $print_OptT0(this->f1);
    printf(", ");
    printf("f3=");
    $print_OptT0(this->f3);
    printf(", ");
    printf("f0=");
    $print_OptT0(this->f0);
    printf(", ");
    printf("f4=");
    $print_OptT0(this->f4);
    printf(", ");
    printf("f2=");
    $print_OptT0(this->f2);
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
struct T0* new$T0(struct OptT0* f0, struct OptT0* f1, struct OptT0* f2, struct OptT0* f3, struct OptT0* f4) {
  struct T0* $res = malloc(sizeof (struct T0));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_T0;
  $res->kind = T0_tag;
  $res->f1 = f1;
  $res->f1->rc ++;
  $res->f3 = f3;
  $res->f3->rc ++;
  $res->f0 = f0;
  $res->f0->rc ++;
  $res->f4 = f4;
  $res->f4->rc ++;
  $res->f2 = f2;
  $res->f2->rc ++;
  return $res;
}
int main() {
  struct T0* vT0_5 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_5->rc ++;
  struct T0* vT0_12 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_12->rc ++;
  struct T0* vT0_11 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_11->rc ++;
  struct T0* vT0_0 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_0->rc ++;
  struct T0* vT0_4 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_4->rc ++;
  struct T0* vT0_3 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_3->rc ++;
  struct T0* vT0_7 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_7->rc ++;
  struct T0* vT0_9 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_9->rc ++;
  struct T0* vT0_6 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_6->rc ++;
  struct T0* vT0_10 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_10->rc ++;
  struct T0* vT0_8 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_8->rc ++;
  struct T0* vT0_1 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_1->rc ++;
  struct T0* vT0_2 = new$T0(new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0(), new$NoneT0());
  vT0_2->rc ++;
  struct OptT0* oldValue$0 = vT0_1->f1;
  vT0_1->f1 = new$SomeT0(vT0_8);
  vT0_1->f1->rc ++;
  $decr_OptT0(oldValue$0);
  struct OptT0* oldValue$1 = vT0_12->f2;
  vT0_12->f2 = new$SomeT0(vT0_12);
  vT0_12->f2->rc ++;
  $decr_OptT0(oldValue$1);
  struct OptT0* oldValue$2 = vT0_11->f2;
  vT0_11->f2 = new$SomeT0(vT0_3);
  vT0_11->f2->rc ++;
  $decr_OptT0(oldValue$2);
  struct OptT0* oldValue$3 = vT0_5->f0;
  vT0_5->f0 = new$SomeT0(vT0_8);
  vT0_5->f0->rc ++;
  $decr_OptT0(oldValue$3);
  struct OptT0* oldValue$4 = vT0_6->f0;
  vT0_6->f0 = new$SomeT0(vT0_8);
  vT0_6->f0->rc ++;
  $decr_OptT0(oldValue$4);
  struct OptT0* oldValue$5 = vT0_9->f2;
  vT0_9->f2 = new$SomeT0(vT0_9);
  vT0_9->f2->rc ++;
  $decr_OptT0(oldValue$5);
  struct OptT0* oldValue$6 = vT0_6->f2;
  vT0_6->f2 = new$SomeT0(vT0_8);
  vT0_6->f2->rc ++;
  $decr_OptT0(oldValue$6);
  struct OptT0* oldValue$7 = vT0_2->f2;
  vT0_2->f2 = new$SomeT0(vT0_0);
  vT0_2->f2->rc ++;
  $decr_OptT0(oldValue$7);
  struct OptT0* oldValue$8 = vT0_0->f1;
  vT0_0->f1 = new$SomeT0(vT0_7);
  vT0_0->f1->rc ++;
  $decr_OptT0(oldValue$8);
  struct OptT0* oldValue$9 = vT0_7->f2;
  vT0_7->f2 = new$SomeT0(vT0_5);
  vT0_7->f2->rc ++;
  $decr_OptT0(oldValue$9);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  struct OptT0* oldValue$10 = vT0_8->f1;
  vT0_8->f1 = new$SomeT0(vT0_2);
  vT0_8->f1->rc ++;
  $decr_OptT0(oldValue$10);
  struct OptT0* oldValue$11 = vT0_4->f2;
  vT0_4->f2 = new$SomeT0(vT0_5);
  vT0_4->f2->rc ++;
  $decr_OptT0(oldValue$11);
  struct OptT0* oldValue$12 = vT0_10->f1;
  vT0_10->f1 = new$SomeT0(vT0_11);
  vT0_10->f1->rc ++;
  $decr_OptT0(oldValue$12);
  struct OptT0* oldValue$13 = vT0_12->f1;
  vT0_12->f1 = new$SomeT0(vT0_0);
  vT0_12->f1->rc ++;
  $decr_OptT0(oldValue$13);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  struct OptT0* oldValue$14 = vT0_1->f2;
  vT0_1->f2 = new$SomeT0(vT0_10);
  vT0_1->f2->rc ++;
  $decr_OptT0(oldValue$14);
  struct OptT0* oldValue$15 = vT0_4->f1;
  vT0_4->f1 = new$SomeT0(vT0_6);
  vT0_4->f1->rc ++;
  $decr_OptT0(oldValue$15);
  struct OptT0* oldValue$16 = vT0_10->f2;
  vT0_10->f2 = new$SomeT0(vT0_5);
  vT0_10->f2->rc ++;
  $decr_OptT0(oldValue$16);
  struct OptT0* oldValue$17 = vT0_2->f1;
  vT0_2->f1 = new$SomeT0(vT0_12);
  vT0_2->f1->rc ++;
  $decr_OptT0(oldValue$17);
  struct OptT0* oldValue$18 = vT0_12->f0;
  vT0_12->f0 = new$SomeT0(vT0_11);
  vT0_12->f0->rc ++;
  $decr_OptT0(oldValue$18);
  struct OptT0* oldValue$19 = vT0_2->f4;
  vT0_2->f4 = new$SomeT0(vT0_1);
  vT0_2->f4->rc ++;
  $decr_OptT0(oldValue$19);
  ; $decr_T0(vT0_1);
  struct OptT0* oldValue$20 = vT0_5->f1;
  vT0_5->f1 = new$SomeT0(vT0_7);
  vT0_5->f1->rc ++;
  $decr_OptT0(oldValue$20);
  ; $decr_T0(vT0_7);
  struct OptT0* oldValue$21 = vT0_0->f4;
  vT0_0->f4 = new$SomeT0(vT0_2);
  vT0_0->f4->rc ++;
  $decr_OptT0(oldValue$21);
  struct OptT0* oldValue$22 = vT0_3->f4;
  vT0_3->f4 = new$SomeT0(vT0_12);
  vT0_3->f4->rc ++;
  $decr_OptT0(oldValue$22);
  struct OptT0* oldValue$23 = vT0_4->f3;
  vT0_4->f3 = new$SomeT0(vT0_12);
  vT0_4->f3->rc ++;
  $decr_OptT0(oldValue$23);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  struct OptT0* oldValue$24 = vT0_12->f4;
  vT0_12->f4 = new$SomeT0(vT0_5);
  vT0_12->f4->rc ++;
  $decr_OptT0(oldValue$24);
  ; $decr_T0(vT0_12);
  struct OptT0* oldValue$25 = vT0_9->f1;
  vT0_9->f1 = new$SomeT0(vT0_11);
  vT0_9->f1->rc ++;
  $decr_OptT0(oldValue$25);
  ; $decr_T0(vT0_9);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  ; $decr_T0(vT0_11);
  struct OptT0* oldValue$26 = vT0_5->f2;
  vT0_5->f2 = new$SomeT0(vT0_4);
  vT0_5->f2->rc ++;
  $decr_OptT0(oldValue$26);
  ; $decr_T0(vT0_5);
  struct OptT0* oldValue$27 = vT0_4->f4;
  vT0_4->f4 = new$SomeT0(vT0_6);
  vT0_4->f4->rc ++;
  $decr_OptT0(oldValue$27);
  ; $decr_T0(vT0_4);
  ; $decr_T0(vT0_6);
  struct OptT0* oldValue$28 = vT0_3->f1;
  vT0_3->f1 = new$SomeT0(vT0_10);
  vT0_3->f1->rc ++;
  $decr_OptT0(oldValue$28);
  ; $decr_T0(vT0_10);
  struct OptT0* oldValue$29 = vT0_8->f2;
  vT0_8->f2 = new$SomeT0(vT0_8);
  vT0_8->f2->rc ++;
  $decr_OptT0(oldValue$29);
  ; $decr_T0(vT0_8);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  ; $decr_T0(vT0_8);
  struct OptT0* oldValue$30 = vT0_0->f2;
  vT0_0->f2 = new$SomeT0(vT0_2);
  vT0_0->f2->rc ++;
  $decr_OptT0(oldValue$30);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  ; $decr_T0(vT0_0);
  ; $decr_T0(vT0_2);
  struct OptT0* oldValue$31 = vT0_3->f2;
  vT0_3->f2 = new$SomeT0(vT0_3);
  vT0_3->f2->rc ++;
  $decr_OptT0(oldValue$31);
  ; $decr_T0(vT0_3);
  processAllPCRs(); VALGRIND_DO_LEAK_CHECK;
  ; $decr_T0(vT0_3);
  int ret$32 = 0;
  processAllPCRs();
  return ret$32;
}
