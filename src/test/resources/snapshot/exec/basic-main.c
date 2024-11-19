#include "runtime.h"

enum List_kind { Nil_tag, Cons_tag };
struct List {
  int rc;
  enum Color color;
  int addedPCR;
  enum List_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { int value_Cons; struct List* next_Cons; };
  };
};
void $free_List(struct List* this);
void $decr_List(struct List* this);
void $markGray_List(struct List* this);
void $scan_List(struct List* this);
void $scanBlack_List(struct List* this);
void $collectWhite_List(struct List* this);
void $print_List(struct List* this);
int fn$sum(struct List* list);
int main();
void $free_List(struct List* this) {
  fprintf(stderr, "Freeing List\n");
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    break;
  }
  free(this);
}
void $decr_List(struct List* this) {
  fprintf(stderr, "Decrementing List (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      $decr_List(this->next_Cons);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_List(struct List* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    this->next_Cons->rc --;
    $markGray_List(this->next_Cons);
    break;
  }
}
void $scan_List(struct List* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_List(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    $scan_List(this->next_Cons);
    break;
  }
}
void $scanBlack_List(struct List* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      this->next_Cons->rc ++;
      $scanBlack_List(this->next_Cons);
      break;
    }
  }
}
void $collectWhite_List(struct List* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      $collectWhite_List(this->next_Cons);
      break;
    }
    fprintf(stderr, "Removing List\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_List;
  }
}
void $print_List(struct List* this) {
  switch (this->kind) {
  case Nil_tag:
    printf("Nil {");
    printf("}");
    break;
  case Cons_tag:
    printf("Cons {");
    printf("value=");
    printf("%d", this->value_Cons);
    printf(", ");
    printf("next=");
    $print_List(this->next_Cons);
    printf(", ");
    printf("}");
    break;
  }
}
int fn$sum(struct List* list) {
  list->rc ++;
  struct List* matchobj$0 = list;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Nil_tag:
    matchres$1 = 0;
    break;
  case Cons_tag:
    int value = matchobj$0->value_Cons;
    struct List* tail = matchobj$0->next_Cons;
    tail->rc ++;
    matchres$1 = value + fn$sum(tail);
    $decr_List(tail);
    break;
  }
  int ret$2 = matchres$1;
  $decr_List(list);
  return ret$2;
}
int main() {
  struct List* ctorres$3 = malloc(sizeof (struct List));
  ctorres$3->rc = 0;
  ctorres$3->color = kBlack;
  ctorres$3->addedPCR = 0;
  ctorres$3->print = $print_List;
  ctorres$3->kind = Cons_tag;
  ctorres$3->value_Cons = 1;
  struct List* ctorres$4 = malloc(sizeof (struct List));
  ctorres$4->rc = 0;
  ctorres$4->color = kBlack;
  ctorres$4->addedPCR = 0;
  ctorres$4->print = $print_List;
  ctorres$4->kind = Cons_tag;
  ctorres$4->value_Cons = 2;
  struct List* ctorres$5 = malloc(sizeof (struct List));
  ctorres$5->rc = 0;
  ctorres$5->color = kBlack;
  ctorres$5->addedPCR = 0;
  ctorres$5->print = $print_List;
  ctorres$5->kind = Cons_tag;
  ctorres$5->value_Cons = 4;
  struct List* ctorres$6 = malloc(sizeof (struct List));
  ctorres$6->rc = 0;
  ctorres$6->color = kBlack;
  ctorres$6->addedPCR = 0;
  ctorres$6->print = $print_List;
  ctorres$6->kind = Nil_tag;
  ctorres$5->next_Cons = ctorres$6;
  ctorres$5->next_Cons->rc ++;
  ctorres$4->next_Cons = ctorres$5;
  ctorres$4->next_Cons->rc ++;
  ctorres$3->next_Cons = ctorres$4;
  ctorres$3->next_Cons->rc ++;
  struct List* list = ctorres$3;
  list->rc ++;
  printf("%d\n", fn$sum(list));
  int ret$7 = 0;
  $decr_List(list);
  processAllPCRs();
  return ret$7;
}
